package com.planetconquest.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.SystemClock;

import com.planetconquest.game.engine.Engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Âm thanh của game: hiệu ứng ngắn (SoundPool, độ trễ thấp) và nhạc nền lặp (AudioTrack).
 * Bật/tắt chung một công tắc, lưu trong SharedPreferences. Mọi lỗi âm thanh đều bị nuốt
 * để không bao giờ làm sập game.
 */
final class Sfx {
    static final int CLICK = 0, LASSO = 1, MOVE = 2, POINT = 3, ATTACK = 4, UPGRADE = 5, CAPTURE = 6, LEVELUP = 7, WIN = 8, LOSE = 9;
    static final int TRACK_NONE = 0, TRACK_MENU = 1, TRACK_GAME = 2;

    private static final int[] RES = {R.raw.sfx_click, R.raw.sfx_lasso, R.raw.sfx_move, R.raw.sfx_point, R.raw.sfx_attack,
            R.raw.sfx_upgrade, R.raw.sfx_capture, R.raw.sfx_levelup, R.raw.sfx_win, R.raw.sfx_lose};
    private static final int BGM_RATE = 32000;     // phải khớp tools/gen_audio.py
    private static final float[] VOL = {.6f, .5f, .5f, .5f, .45f, .7f, .8f, .8f, .9f, .9f};
    private static final long MIN_GAP_MS = 70;      // chống dồn tiếng khi nhiều sự kiện liên tiếp
    private static final float MUSIC_VOL = .45f;

    private final Context ctx;
    private final SharedPreferences prefs;
    private final SoundPool pool;
    private final int[] ids = new int[RES.length];
    private final long[] last = new long[RES.length];
    private AudioTrack mp;
    private int track = TRACK_NONE;
    private boolean on, hostActive = true, duck;

    Sfx(Context ctx, SharedPreferences prefs) {
        this.ctx = ctx.getApplicationContext();
        this.prefs = prefs;
        on = prefs.getBoolean("sound", true);
        AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(aa).build();
        for (int i = 0; i < RES.length; i++) ids[i] = pool.load(this.ctx, RES[i], 1);
    }

    boolean isOn() { return on; }

    void toggle() {
        on = !on;
        prefs.edit().putBoolean("sound", on).apply();
        if (on) { int t = track; track = TRACK_NONE; music(t); } else stopPlayer();
    }

    /** Ánh xạ sự kiện của engine sang tiếng tương ứng. */
    void onEngineEvent(int ev) {
        switch (ev) {
            case Engine.EV_ATTACK: play(ATTACK); break;
            case Engine.EV_MOVE: play(MOVE); break;
            case Engine.EV_POINT: play(POINT); break;
            case Engine.EV_LASSO: play(LASSO); break;
            case Engine.EV_UPGRADE: play(UPGRADE); break;
            case Engine.EV_CAPTURE: play(CAPTURE); break;
            case Engine.EV_LEVELUP: play(LEVELUP); break;
            default: break;
        }
    }

    void play(int fx) {
        if (!on || !hostActive) return;
        long now = SystemClock.uptimeMillis();
        if (now - last[fx] < MIN_GAP_MS) return;
        last[fx] = now;
        try { pool.play(ids[fx], VOL[fx], VOL[fx], 1, 0, 1f); } catch (RuntimeException ignored) { }
    }

    /** Chọn bài nhạc nền; gọi lại với cùng bài thì không làm gì. */
    void music(int t) {
        if (t == track && (mp != null || t == TRACK_NONE)) return;
        track = t;
        stopPlayer();
        if (!on || t == TRACK_NONE) return;
        try {
            byte[] pcm = readPcm(t == TRACK_MENU ? R.raw.bgm_menu : R.raw.bgm_game);
            AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat af = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(BGM_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            // MODE_STATIC + vòng lặp trong bộ đệm: lặp vô hạn, không có khe hở như MediaPlayer
            AudioTrack tr = new AudioTrack(aa, af, pcm.length, AudioTrack.MODE_STATIC, android.media.AudioManager.AUDIO_SESSION_ID_GENERATE);
            tr.write(pcm, 0, pcm.length);
            tr.setLoopPoints(0, pcm.length / 2, -1);
            mp = tr;
            applyVolume();
            if (hostActive) mp.play();
        } catch (RuntimeException | java.io.IOException e) { stopPlayer(); }
    }

    /** Đọc dữ liệu PCM 16-bit từ file WAV trong res/raw (bỏ qua mọi chunk trước chunk "data"). */
    private byte[] readPcm(int res) throws java.io.IOException {
        InputStream in = ctx.getResources().openRawResource(res);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(1 << 20);
            byte[] buf = new byte[16384];
            for (int n; (n = in.read(buf)) > 0; ) bo.write(buf, 0, n);
            byte[] w = bo.toByteArray();
            int p = 12;
            while (p + 8 <= w.length) {
                int len = (w[p + 4] & 255) | (w[p + 5] & 255) << 8 | (w[p + 6] & 255) << 16 | (w[p + 7] & 255) << 24;
                if (w[p] == 'd' && w[p + 1] == 'a' && w[p + 2] == 't' && w[p + 3] == 'a') {
                    len = Math.min(len, w.length - (p + 8)) & ~1;
                    byte[] pcm = new byte[len];
                    System.arraycopy(w, p + 8, pcm, 0, len);
                    return pcm;
                }
                p += 8 + len + (len & 1);
            }
            throw new java.io.IOException("WAV không có chunk data");
        } finally { in.close(); }
    }

    /** Hạ nhạc khi tạm dừng để hộp thoại dễ nghe. */
    void duck(boolean d) { duck = d; applyVolume(); }

    void onHostPause() {
        hostActive = false;
        try { if (mp != null) mp.pause(); } catch (RuntimeException ignored) { }
    }

    void onHostResume() {
        hostActive = true;
        try { if (on && mp != null) mp.play(); } catch (RuntimeException ignored) { }
    }

    void release() {
        stopPlayer();
        pool.release();
    }

    private void applyVolume() {
        float v = duck ? MUSIC_VOL * .4f : MUSIC_VOL;
        try { if (mp != null) mp.setVolume(v); } catch (RuntimeException ignored) { }
    }

    private void stopPlayer() {
        if (mp == null) return;
        try { mp.stop(); } catch (RuntimeException ignored) { }
        mp.release();
        mp = null;
    }
}
