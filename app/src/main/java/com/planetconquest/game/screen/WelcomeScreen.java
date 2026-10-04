package com.planetconquest.game.screen;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.planetconquest.game.AppInfo;
import com.planetconquest.game.R;
import com.planetconquest.game.ui.UiButton;
import com.planetconquest.game.ui.WelcomeCompass;
import com.planetconquest.game.ui.WelcomePlayIcon;
import com.planetconquest.game.ui.WelcomeScene;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.ui.Palette.*;

/**
 * Màn Welcome: hoạt ảnh nền, tiêu đề; hành tinh xanh của người chơi là nút Chơi ngay (vào Hướng dẫn / màn kế tiếp /
 * Endless), la bàn là nút Bản đồ, icon loa góc dưới trái bật/tắt âm thanh. Số liệu lấy từ planet-ui/welcome.js.
 */
final class WelcomeScreen extends BaseScreen {
    private static final float PLAY_PRESS_SCALE = .965f, PLAY_HIT_SCALE = 1.15f, PLAY_PRESS_GLOW = .14f;
    private static final float BAR_MARGIN = 28, BAR_Y_FROM_BOTTOM = 76, SOUND_BTN = 44;
    private enum Target { NONE, PLAY, MAP }

    private final WelcomeScene scene;
    private final WelcomePlayIcon playIcon;
    private final WelcomeCompass compass;
    private float t, sceneW, sceneH;
    private Target down = Target.NONE;
    private boolean inside;

    WelcomeScreen(ScreenHost host) {
        super(host);
        scene = new WelcomeScene(dp);
        playIcon = new WelcomePlayIcon(host.kit());
        compass = new WelcomeCompass(host.kit());
    }

    @Override
    public void enter() {
        super.enter();
        down = Target.NONE; inside = false;
    }

    @Override
    public void layout() {
        buttons.clear();
        float W = W(), H = H();
        if (W <= 0) return;
        if (W != sceneW || H != sceneH) { scene.setSize(W, H); sceneW = W; sceneH = H; }
        UiButton.Icon icon = host.sfx().isOn() ? UiButton.Icon.SOUND_ON : UiButton.Icon.SOUND_OFF;
        float cy = H - BAR_Y_FROM_BOTTOM * dp;
        add(new UiButton(null, UiButton.Style.ICON, icon, new Runnable() {
            @Override public void run() { toggleSound(); }
        })).at(BAR_MARGIN * dp, cy - SOUND_BTN * dp / 2, (BAR_MARGIN + SOUND_BTN) * dp, cy + SOUND_BTN * dp / 2);
    }

    @Override public void update(float dt) { t += dt; }

    private void go(Target target) {
        if (target == Target.PLAY) { host.session().startNext(); host.go(host.screens().play()); }
        else host.go(host.screens().levels());
    }

    @Override public boolean needsLoop() { return true; }
    @Override public float animTime() { return t; }

    // ================= Cảm ứng: hành tinh xanh và la bàn =================
    private float compassX() { return W() - (BAR_MARGIN + WelcomeCompass.RADIUS) * dp; }
    private float compassY() { return H() - BAR_Y_FROM_BOTTOM * dp; }

    private Target hit(float x, float y) {
        if (Math.hypot(x - scene.playX(), y - scene.playY()) <= scene.playRadius() * PLAY_HIT_SCALE) return Target.PLAY;
        if (Math.hypot(x - compassX(), y - compassY()) <= WelcomeCompass.RADIUS * dp * WelcomeCompass.HIT_SCALE) return Target.MAP;
        return Target.NONE;
    }

    @Override public void onDown(float x, float y) { down = hit(x, y); inside = down != Target.NONE; }
    @Override public void onMove(float x, float y) { if (down != Target.NONE) inside = hit(x, y) == down; }
    @Override public void onCancel() { down = Target.NONE; inside = false; }

    @Override
    public void onUp(float x, float y) {
        Target d = down;
        boolean fire = d != Target.NONE && inside;
        down = Target.NONE; inside = false;
        if (fire) go(d);                                          // chuyển màn ngay khi thả tay, không trễ
    }

    // ================= Vẽ =================
    @Override
    public void draw(Canvas c) {
        boolean pressedPlay = down == Target.PLAY && inside;     // nhấn giữ: hành tinh nhỏ lại và sáng hơn
        float scale = pressedPlay ? PLAY_PRESS_SCALE : 1, glow = pressedPlay ? PLAY_PRESS_GLOW : 0;
        scene.setPlayFx(scale, glow);
        scene.draw(c, t);
        float ty = Math.max(80 * dp, H() * .13f);
        text(c, tx.s(R.string.title_planet), W() / 2, ty, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, tx.s(R.string.title_conquest), W() / 2, ty + 48 * dp, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, tx.s(R.string.version, AppInfo.VERSION), W() / 2, H() - 24 * dp, 11.5f * dp, alpha(C_MUTED, .7f), tfReg, Paint.Align.CENTER);
        playIcon.draw(c, scene.playX(), scene.playY(), scene.playRadius(), scale, t);
        compass.draw(c, compassX(), compassY(), WelcomeCompass.RADIUS * dp, down == Target.MAP && inside, t, -1);
    }
}
