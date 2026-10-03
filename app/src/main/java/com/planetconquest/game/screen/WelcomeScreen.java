package com.planetconquest.game.screen;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.planetconquest.game.AppInfo;
import com.planetconquest.game.R;
import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.ui.UiButton;
import com.planetconquest.game.ui.WelcomeScene;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.ui.Palette.*;

/** Màn Welcome: hoạt ảnh nền, tiêu đề, nút Chơi / Hướng dẫn / Âm thanh. */
final class WelcomeScreen extends BaseScreen {
    private final WelcomeScene scene;
    private float t, sceneW, sceneH;

    WelcomeScreen(ScreenHost host) {
        super(host);
        scene = new WelcomeScene(dp);
    }

    @Override
    public void layout() {
        buttons.clear();
        float W = W(), H = H();
        if (W <= 0) return;
        if (W != sceneW || H != sceneH) { scene.setSize(W, H); sceneW = W; sceneH = H; }
        float bw = Math.min(W - 48 * dp, 420 * dp), bx = (W - bw) / 2;
        add(new UiButton(tx.s(R.string.btn_play), UiButton.Style.PRIMARY, UiButton.Icon.NONE, new Runnable() {
            @Override public void run() { host.go(host.screens().levels()); }
        })).at(bx, H - 190 * dp, bx + bw, H - 134 * dp);
        add(new UiButton(tx.s(R.string.btn_tutorial), UiButton.Style.SECONDARY, UiButton.Icon.NONE, new Runnable() {
            @Override public void run() { startLevelAndPlay(0); }
        })).at(bx, H - 122 * dp, bx + bw, H - 66 * dp);
        add(new UiButton(soundLabel(), UiButton.Style.SMALL, UiButton.Icon.NONE, new Runnable() {
            @Override public void run() { toggleSound(); }
        })).at(W - 150 * dp, 14 * dp, W - 14 * dp, 48 * dp);
    }

    @Override public void update(float dt) { t += dt; }
    @Override public boolean needsLoop() { return true; }
    @Override public float animTime() { return t; }

    // ================= Màn Welcome =================
    @Override
    public void draw(Canvas c) {
        scene.draw(c, t);
        float ty = Math.max(80 * dp, H() * .13f);
        text(c, tx.s(R.string.title_planet), W() / 2, ty, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, tx.s(R.string.title_conquest), W() / 2, ty + 48 * dp, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, tx.s(R.string.tagline), W() / 2, ty + 90 * dp, 14.5f * dp, C_MUTED, tfReg, Paint.Align.CENTER);
        if (host.progress().bestEndless() > 0) text(c, tx.s(R.string.best_endless, host.progress().bestEndless()), W() / 2, H() - 44 * dp, 12.5f * dp, C_MUTED, tfReg, Paint.Align.CENTER);
        text(c, tx.s(R.string.version, AppInfo.VERSION), W() / 2, H() - 24 * dp, 11.5f * dp, alpha(C_MUTED, .7f), tfReg, Paint.Align.CENTER);
    }
}
