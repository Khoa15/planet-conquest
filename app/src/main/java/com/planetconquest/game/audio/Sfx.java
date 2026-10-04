package com.planetconquest.game.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.planetconquest.game.R;
import com.planetconquest.game.engine.GameEvent;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Âm thanh của game: hiệu ứng ngắn (SoundPool, độ trễ thấp) và nhạc nền lặp (AudioTrack).
 * Bật/tắt chung một công tắc, lưu trong SharedPreferences. Mọi lỗi âm thanh đều bị nuốt
 * để không bao giờ làm sập game.
 */
public final class Sfx {
    private static final long MIN_GAP_MS = 70;      // chống dồn tiếng khi nhiều sự kiện liên tiếp
    private static final float MUSIC_VOL = .45f;
    private static final int FADE_MS = 1200, TICK_MS = 40;   // crossfade giữa hai bài nhạc

    /** Một bài nhạc đang phát; gain 0..1 chạy về target để vào/ra dần. */
    private static final class Bgm {
        final AudioTrack tr;
        float gain, target;
        Bgm(AudioTrack tr, float gain, float target) { this.tr = tr; this.gain = gain; this.target = target; }
    }

    private final Context ctx;
    private final SharedPreferences prefs;
    private final SoundPool pool;
    private final int[] ids = new int[Sound.values().length];
    private final long[] last = new long[Sound.values().length];
    private final ArrayList<Bgm> tracks = new ArrayList<>();   // bài đang phát + các bài đang nhỏ dần
    private Bgm cur;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { if (fadeStep()) handler.postDelayed(this, TICK_MS); }
    };
    private MusicTrack track = MusicTrack.NONE;
    private final PcmDecoder decoder;
    private final ExecutorService decodeThread = Executors.newSingleThreadExecutor();
    private int musicTicket;                          // mỗi lần đổi bài tăng 1; kết quả giải mã cũ bị bỏ
    private boolean on, hostActive = true, duck, released;

    public Sfx(Context ctx, SharedPreferences prefs) {
        this.ctx = ctx.getApplicationContext();
        this.decoder = new PcmDecoder(this.ctx);
        this.prefs = prefs;
        on = prefs.getBoolean("sound", true);
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(aa).build();
        for (Sound s : Sound.values()) ids[s.ordinal()] = pool.load(this.ctx, s.res, 1);
    }

    public boolean isOn() { return on; }

    public void toggle() {
        on = !on;
        prefs.edit().putBoolean("sound", on).apply();
        if (on) { MusicTrack t = track; track = MusicTrack.NONE; music(t); } else { handler.removeCallbacks(ticker); stopAll(); }
    }

    /** Ánh xạ sự kiện của engine sang tiếng tương ứng. */
    public void onEngineEvent(GameEvent ev) {
        switch (ev) {
            case ATTACK: play(Sound.ATTACK); break;
            case MOVE: play(Sound.MOVE); break;
            case POINT: play(Sound.POINT); break;
            case LASSO: play(Sound.LASSO); break;
            case UPGRADE: play(Sound.UPGRADE); break;
            case CAPTURE: play(Sound.CAPTURE); break;
            case LEVELUP: play(Sound.LEVELUP); break;
            default: break;
        }
    }

    public void play(Sound fx) {
        if (!on || !hostActive) return;
        int i = fx.ordinal();
        long now = SystemClock.uptimeMillis();
        if (now - last[i] < MIN_GAP_MS) return;
        last[i] = now;
        try { pool.play(ids[i], fx.volume, fx.volume, 1, 0, 1f); } catch (RuntimeException ignored) { }
    }

    /** Chọn bài nhạc nền; bài cũ nhỏ dần và bài mới lớn dần chồng lên nhau (crossfade). */
    public void music(MusicTrack t) {
        if (t == track && (cur != null || t == MusicTrack.NONE || !on)) return;
        track = t;
        if (cur != null) { cur.target = 0; cur = null; }
        final int ticket = ++musicTicket;
        if (!on || t == MusicTrack.NONE) { kick(); return; }
        kick();
        decodeThread.execute(new Runnable() {          // giải mã trên luồng nền để không giật khi đổi màn
            @Override public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                PcmDecoder.Pcm pcm = null;
                try { pcm = decoder.decode(t.res); } catch (java.io.IOException | RuntimeException ignored) { }
                final PcmDecoder.Pcm result = pcm;
                handler.post(new Runnable() { @Override public void run() { if (ticket == musicTicket) startTrack(result); } });
            }
        });
    }

    /** Tạo AudioTrack lặp vòng từ PCM đã giải mã và cho vào dần. Chạy trên luồng chính. */
    private void startTrack(PcmDecoder.Pcm pcm) {
        if (pcm == null || !on || released) return;
        try {
            AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat af = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(pcm.sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            // MODE_STATIC + vòng lặp trong bộ đệm: lặp vô hạn, không có khe hở như MediaPlayer
            AudioTrack tr = new AudioTrack(aa, af, pcm.data.length, AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);
            tr.write(pcm.data, 0, pcm.data.length);
            tr.setLoopPoints(0, pcm.data.length / 2, -1);
            boolean first = tracks.isEmpty();
            cur = new Bgm(tr, first ? 1f : 0f, 1f);       // bài đầu tiên vào luôn, các bài sau vào dần
            tracks.add(cur);
            applyVolume(cur);
            if (hostActive) tr.play();
        } catch (RuntimeException e) { cur = null; }
        kick();
    }

    private void kick() { handler.removeCallbacks(ticker); handler.post(ticker); }

    /** Một bước crossfade. Trả về true nếu còn bài đang đổi âm lượng. */
    private boolean fadeStep() {
        float d = TICK_MS / (float) FADE_MS;
        boolean more = false;
        for (int i = tracks.size() - 1; i >= 0; i--) {
            Bgm b = tracks.get(i);
            if (b.gain < b.target) b.gain = Math.min(b.target, b.gain + d);
            else if (b.gain > b.target) b.gain = Math.max(b.target, b.gain - d);
            if (b.gain == 0 && b.target == 0) { release(b); tracks.remove(i); continue; }
            applyVolume(b);
            if (b.gain != b.target) more = true;
        }
        return more;
    }

    private void release(Bgm b) {
        try { b.tr.stop(); } catch (RuntimeException ignored) { }
        b.tr.release();
    }

    /** Hạ nhạc khi tạm dừng để hộp thoại dễ nghe. */
    public void duck(boolean d) { duck = d; for (Bgm b : tracks) applyVolume(b); }

    public void onHostPause() {
        hostActive = false;
        for (Bgm b : tracks) try { b.tr.pause(); } catch (RuntimeException ignored) { }
    }

    public void onHostResume() {
        hostActive = true;
        if (on) for (Bgm b : tracks) try { b.tr.play(); } catch (RuntimeException ignored) { }
    }

    public void release() {
        released = true;
        musicTicket++;
        decodeThread.shutdown();
        handler.removeCallbacks(ticker);
        stopAll();
        pool.release();
    }

    private void applyVolume(Bgm b) {
        float v = (duck ? MUSIC_VOL * .4f : MUSIC_VOL) * b.gain;
        try { b.tr.setVolume(v); } catch (RuntimeException ignored) { }
    }

    private void stopAll() {
        for (Bgm b : tracks) release(b);
        tracks.clear();
        cur = null;
    }
}
