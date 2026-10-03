package com.planetconquest.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.SystemClock;

import com.planetconquest.game.engine.Engine;

/**
 * Âm thanh của game: hiệu ứng ngắn (SoundPool, độ trễ thấp) và nhạc nền lặp (MediaPlayer).
 * Bật/tắt chung một công tắc, lưu trong SharedPreferences. Mọi lỗi âm thanh đều bị nuốt
 * để không bao giờ làm sập game.
 */
final class Sfx {
    static final int CLICK = 0, LASSO = 1, MOVE = 2, POINT = 3, ATTACK = 4, UPGRADE = 5, CAPTURE = 6, LEVELUP = 7, WIN = 8, LOSE = 9;
    static final int TRACK_NONE = 0, TRACK_MENU = 1, TRACK_GAME = 2;

    private static final int[] RES = {R.raw.sfx_click, R.raw.sfx_lasso, R.raw.sfx_move, R.raw.sfx_point, R.raw.sfx_attack,
            R.raw.sfx_upgrade, R.raw.sfx_capture, R.raw.sfx_levelup, R.raw.sfx_win, R.raw.sfx_lose};
    private static final float[] VOL = {.6f, .5f, .5f, .5f, .45f, .7f, .8f, .8f, .9f, .9f};
    private static final long MIN_GAP_MS = 70;      // chống dồn tiếng khi nhiều sự kiện liên tiếp
    private static final float MUSIC_VOL = .45f;

    private final Context ctx;
    private final SharedPreferences prefs;
    private final SoundPool pool;
    private final int[] ids = new int[RES.length];
    private final long[] last = new long[RES.length];
    private MediaPlayer mp;
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
            mp = MediaPlayer.create(ctx, t == TRACK_MENU ? R.raw.bgm_menu : R.raw.bgm_game);
            if (mp == null) return;
            mp.setLooping(true);
            applyVolume();
            if (hostActive) mp.start();
        } catch (RuntimeException e) { stopPlayer(); }
    }

    /** Hạ nhạc khi tạm dừng để hộp thoại dễ nghe. */
    void duck(boolean d) { duck = d; applyVolume(); }

    void onHostPause() {
        hostActive = false;
        try { if (mp != null && mp.isPlaying()) mp.pause(); } catch (RuntimeException ignored) { }
    }

    void onHostResume() {
        hostActive = true;
        try { if (on && mp != null && !mp.isPlaying()) mp.start(); } catch (RuntimeException ignored) { }
    }

    void release() {
        stopPlayer();
        pool.release();
    }

    private void applyVolume() {
        float v = duck ? MUSIC_VOL * .4f : MUSIC_VOL;
        try { if (mp != null) mp.setVolume(v, v); } catch (RuntimeException ignored) { }
    }

    private void stopPlayer() {
        if (mp == null) return;
        try { mp.stop(); } catch (RuntimeException ignored) { }
        mp.release();
        mp = null;
    }
}
