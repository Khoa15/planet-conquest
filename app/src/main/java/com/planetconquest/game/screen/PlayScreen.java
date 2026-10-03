package com.planetconquest.game.screen;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.planetconquest.game.R;
import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.engine.input.GestureMode;
import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.model.Selection;
import com.planetconquest.game.session.GameSession;
import com.planetconquest.game.ui.UiButton;
import com.planetconquest.game.ui.WorldRenderer;

import java.util.ArrayList;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.ui.Palette.*;

/** Màn chơi: chuyển cảm ứng cho engine, vẽ thế giới và HUD (chip, thanh vùng chọn, khung hướng dẫn). */
public final class PlayScreen extends BaseScreen {
    private final GameSession session;
    private final WorldRenderer world;
    private UiButton cancelSelBtn;
    private float hudBottom, chipY;

    PlayScreen(ScreenHost host) {
        super(host);
        session = host.session();
        world = new WorldRenderer(kit, eng, session, tx);
    }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        float W = W();
        add(new UiButton(null, UiButton.Style.ICON, UiButton.Icon.PAUSE, new Runnable() {
            @Override public void run() { eng.cancelPointer(); host.go(host.screens().pause()); }
        })).at(W - 56 * dp, 9 * dp, W - 12 * dp, 53 * dp);
        cancelSelBtn = add(new UiButton(tx.s(R.string.btn_cancel), UiButton.Style.SMALL, UiButton.Icon.NONE, new Runnable() {
            @Override public void run() { eng.cancelSelection(); }
        }));
        cancelSelBtn.visible = false;
    }

    @Override
    public void update(float dt) {
        eng.step(dt);                           // có thể kết thúc màn và chuyển sang màn kết thúc
        if (!eng.over()) eng.fx(dt);
    }

    @Override
    public void draw(Canvas c) {
        drawWorld(c, true);
        drawHud(c, true);
    }

    /** active = false khi chỉ làm nền cho hộp thoại Tạm dừng / Kết thúc (không có hướng dẫn cảm ứng). */
    void drawWorld(Canvas c, boolean active) { world.draw(c, active); }

    @Override public void onDown(float x, float y) { eng.down(x, y); }
    @Override public void onMove(float x, float y) { eng.move(x, y); }
    @Override public void onUp(float x, float y) { eng.up(x, y); }
    @Override public void onCancel() { eng.cancelPointer(); }
    @Override public boolean onBack() { host.go(host.screens().pause()); return true; }
    @Override public void onHostPause() { host.go(host.screens().pause()); }
    @Override public boolean needsLoop() { return true; }
    @Override public MusicTrack musicTrack() { return MusicTrack.GAME; }
    @Override public float toastTop() { return hudBottom + 10 * dp; }

    // ================= HUD khi chơi =================
    private float chip(Canvas c, float x, float y, String label, String val, int vcol) {
        float ls = 11.5f * dp, vs = 16.5f * dp;
        txt.setTypeface(tfReg); txt.setTextSize(ls);
        float lw = txt.measureText(label);
        txt.setTypeface(tfBold); txt.setTextSize(vs);
        float vw = txt.measureText(val);
        float w = 10 * dp + lw + 6 * dp + vw + 10 * dp, h = 38 * dp;
        if (x + w > W() - 64 * dp && x > 12 * dp) { x = 12 * dp; y += 44 * dp; }
        tmp.set(x, y, x + w, y + h);
        panel(c, tmp, 12 * dp, C_LINE);
        text(c, label, x + 10 * dp, y + h / 2 + dp, ls, C_MUTED, tfReg, Paint.Align.LEFT);
        text(c, val, x + 10 * dp + lw + 6 * dp, y + h / 2, vs, vcol, tfBold, Paint.Align.LEFT);
        hudBottom = Math.max(hudBottom, y + h);
        chipY = y;
        return x + w;
    }

    void drawHud(Canvas c, boolean active) {
        hudBottom = 0;
        chipY = 12 * dp;
        float x = 12 * dp;
        x = chip(c, x, chipY, tx.s(R.string.hud_planets), eng.playerPlanets() + "/" + eng.planets().size(), C_INK) + 6 * dp;
        x = chip(c, x, chipY, tx.s(R.string.hud_rocks), String.valueOf(eng.playerRocks()), C_INK) + 6 * dp;
        Level L = eng.level();
        if (eng.rules().timeLimit() > 0) {
            int left = Math.max(0, (int) Math.ceil(eng.rules().timeLimit() - eng.time()));
            chip(c, x, chipY, tx.s(R.string.hud_time_left), tx.s(R.string.hud_seconds, left), left <= 15 ? C_DANGER : C_INK);
        } else if (L.endless) chip(c, x, chipY, tx.s(R.string.hud_map), String.valueOf(session.endlessMap()), C_INK);
        hudBottom = Math.max(hudBottom, 53 * dp);
        if (L.intro && session.introStep() < GameSession.INTRO_STEP_COUNT) hudBottom = drawCoach(c, hudBottom + 8 * dp);

        // Thanh vùng chọn: số đá đã chọn + nút Hủy (vùng ngón cái)
        Selection s = eng.selection();
        boolean show = active && s != null && (eng.pointer() == null || eng.pointer().mode != GestureMode.CARRY);
        if (cancelSelBtn != null) cancelSelBtn.visible = show;
        if (show) {
            String msg = tx.s(R.string.sel_hint, s.total());
            txt.setTextSize(13.5f * dp); txt.setTypeface(tfReg);
            float tw2 = txt.measureText(msg), bw = 64 * dp, h = 46 * dp, w = 18 * dp + tw2 + 12 * dp + bw + 5 * dp;
            if (w > W() - 24 * dp) { msg = tx.s(R.string.sel_short, s.total()); tw2 = txt.measureText(msg); w = 18 * dp + tw2 + 12 * dp + bw + 5 * dp; }
            float bx = (W() - w) / 2, by = H() - 16 * dp - h;
            tmp.set(bx, by, bx + w, by + h);
            panel(c, tmp, h / 2, C_LINE);
            text(c, msg, bx + 18 * dp, by + h / 2, 13.5f * dp, C_INK, tfReg, Paint.Align.LEFT);
            cancelSelBtn.at(bx + w - 5 * dp - bw, by + 5 * dp, bx + w - 5 * dp, by + h - 5 * dp);
        }
    }

    private float drawCoach(Canvas c, float top) {
        float x = 12 * dp, w = W() - 24 * dp, pad = 14 * dp;
        ArrayList<String> lines = wrap(tx.introSteps()[session.introStep()], 14.5f * dp, tfReg, w - 2 * pad);
        float h = pad + 18 * dp + lines.size() * 21 * dp + pad - 2 * dp;
        tmp.set(x, top, x + w, top + h);
        panel(c, tmp, 16 * dp, alpha(C_GOLD, .45f));
        text(c, tx.s(R.string.coach_header, session.introStep() + 1, GameSession.INTRO_STEP_COUNT), x + pad, top + pad + 6 * dp, 12 * dp, C_GOLD, tfBold, Paint.Align.LEFT);
        for (int i = 0; i < GameSession.INTRO_STEP_COUNT; i++) {
            fill.setColor(i < session.introStep() ? C_GOLD : (i == session.introStep() ? alpha(C_GOLD, .8f) : alpha(C_MUTED, .4f)));
            c.drawCircle(x + w - pad - (GameSession.INTRO_STEP_COUNT - 1 - i) * 12 * dp, top + pad + 6 * dp, (i == session.introStep() ? 4 : 3) * dp, fill);
        }
        float y = top + pad + 18 * dp;
        for (String s : lines) { text(c, s, x + pad, y + 10 * dp, 14.5f * dp, C_INK, tfReg, Paint.Align.LEFT); y += 21 * dp; }
        return top + h;
    }

}
