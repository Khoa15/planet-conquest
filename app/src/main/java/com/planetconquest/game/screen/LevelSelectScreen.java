package com.planetconquest.game.screen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.planetconquest.game.engine.Levels;
import com.planetconquest.game.ui.UiButton;

import java.util.ArrayList;

import static com.planetconquest.game.ui.Palette.*;

/** Màn chọn màn: danh sách thẻ cuộn được (Hướng dẫn, chiến dịch, Endless). */
final class LevelSelectScreen extends BaseScreen {
    private float scroll, scrollMax, downY, scrollAtDown;
    private boolean scrolling;
    private int downCard = -1;

    LevelSelectScreen(ScreenHost host) { super(host); }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        add(new UiButton(null, UiButton.Style.ICON, UiButton.Icon.BACK, new Runnable() {
            @Override public void run() { host.go(host.screens().welcome()); }
        })).at(16 * dp, 18 * dp, 60 * dp, 62 * dp);
        scrollMax = Math.max(0, cardTop() + (Levels.ALL.length + 1) * (cardH() + 10 * dp) + 24 * dp - H());
        scroll = Math.max(0, Math.min(scroll, scrollMax));
    }

    @Override public boolean onBack() { host.go(host.screens().welcome()); return true; }

    @Override
    public void onDown(float x, float y) {
        scrolling = false; downY = y; scrollAtDown = scroll; downCard = cardAt(x, y);
    }

    @Override
    public void onMove(float x, float y) {
        if (!scrolling && Math.abs(y - downY) > 8 * dp) scrolling = true;
        if (scrolling) { scroll = Math.max(0, Math.min(scrollMax, scrollAtDown - (y - downY))); host.invalidate(); }
    }

    @Override
    public void onUp(float x, float y) {
        if (!scrolling && downCard >= 0 && cardAt(x, y) == downCard) openCard(downCard);
    }

    private void openCard(int i) {
        if (i == 0) startLevelAndPlay(0);
        else if (i < Levels.ALL.length) host.go(host.screens().brief(i));
        else host.go(host.screens().brief(-1));
    }

    // ================= Màn chọn màn =================
    private float cardTop() { return 84 * dp; }

    private float cardH() { return 82 * dp; }

    private void cardRect(int i, RectF out) {
        float y = cardTop() + i * (cardH() + 10 * dp) - scroll;
        out.set(16 * dp, y, W() - 16 * dp, y + cardH());
    }

    private int cardAt(float x, float y) {
        if (y < cardTop() - 4 * dp) return -1;
        RectF r = new RectF();
        for (int i = 0; i <= Levels.ALL.length; i++) { cardRect(i, r); if (r.contains(x, y)) return i; }
        return -1;
    }

    @Override
    public void draw(Canvas c) {
        RectF r = new RectF();
        c.save();
        c.clipRect(0, cardTop() - 6 * dp, W(), H());
        int n = Levels.ALL.length;
        for (int i = 0; i <= n; i++) {
            cardRect(i, r);
            if (r.bottom < cardTop() - 10 * dp || r.top > H()) continue;
            boolean endless = i == n, intro = i == 0, isDone = !endless && (intro ? host.progress().isIntroDone() : host.progress().isLevelDone(i));
            fill.setColor(0x0FFFFFFF); c.drawRoundRect(r, 16 * dp, 16 * dp, fill);
            stroke.setColor(C_LINE); stroke.setStrokeWidth(dp); c.drawRoundRect(r, 16 * dp, 16 * dp, stroke);
            float ccx = r.left + 34 * dp, ccy = r.centerY();
            fill.setColor(endless || intro ? C_GOLD : (isDone ? 0xFF27325C : C_YOU));
            c.drawCircle(ccx, ccy, 18 * dp, fill);
            String num = endless ? "∞" : intro ? "?" : String.valueOf(i);
            text(c, num, ccx, ccy, (endless ? 19 : 16) * dp, isDone && !intro ? C_YOU : 0xFF141400, tfBold, Paint.Align.CENTER);
            String name = endless ? "Endless" : Levels.ALL[i].name;
            String desc = endless ? "Bản đồ ngẫu nhiên, không hạn chế. Thiên thạch đâm vào hành tinh."
                    : intro ? "Học cách chơi: bạn và một đối thủ, bạn có nhiều đá hơn." : Levels.ALL[i].limit;
            String right = endless ? (host.progress().bestEndless() > 0 ? "Kỷ lục " + host.progress().bestEndless() : "3–10 hành tinh") : Levels.ALL[i].planets + " hành tinh";
            float tx = r.left + 64 * dp;
            text(c, name, tx, r.top + 24 * dp, 16 * dp, C_INK, tfBold, Paint.Align.LEFT);
            if (isDone) {
                txt.setTextSize(16 * dp); txt.setTypeface(tfBold);
                text(c, intro ? "Đã học" : "Đã qua", tx + txt.measureText(name) + 10 * dp, r.top + 24 * dp, 12 * dp, C_YOU, tfBold, Paint.Align.LEFT);
            }
            text(c, right, r.right - 14 * dp, r.top + 24 * dp, 12 * dp, C_MUTED, tfReg, Paint.Align.RIGHT);
            ArrayList<String> ls = wrap(desc, 12.5f * dp, tfReg, r.right - tx - 14 * dp);
            for (int k = 0; k < Math.min(2, ls.size()); k++) {
                String s = ls.get(k);
                if (k == 1 && ls.size() > 2) s = s + "…";
                text(c, s, tx, r.top + 47 * dp + k * 17 * dp, 12.5f * dp, C_MUTED, tfReg, Paint.Align.LEFT);
            }
        }
        c.restore();
        text(c, "Chọn màn", 74 * dp, 40 * dp, 22 * dp, C_INK, tfBold, Paint.Align.LEFT);
    }

}
