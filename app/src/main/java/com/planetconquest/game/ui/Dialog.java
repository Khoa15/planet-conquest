package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;
import java.util.List;

import static com.planetconquest.game.ui.Palette.*;

/** Hộp thoại dùng chung cho màn Brief / Tạm dừng / Kết thúc: tiêu đề, phụ đề, các mục chữ và dãy nút. */
public final class Dialog extends Painter {
    private String title, sub;
    private int titleColor;
    private final ArrayList<String> labels = new ArrayList<String>(), texts = new ArrayList<String>();
    private final ArrayList<Integer> colors = new ArrayList<Integer>();
    private final ArrayList<UiButton> buttons = new ArrayList<UiButton>();
    private final ArrayList<String> subLines = new ArrayList<String>(), titleLines = new ArrayList<String>();
    private final ArrayList<ArrayList<String>> secLines = new ArrayList<ArrayList<String>>();
    private final RectF card = new RectF();

    public Dialog(DrawKit kit) { super(kit); }

    public Dialog begin(String title, int color, String sub) {
        this.title = title; titleColor = color; this.sub = sub;
        labels.clear(); texts.clear(); colors.clear(); buttons.clear();
        return this;
    }

    public Dialog section(String label, String text, int color) {
        labels.add(label); texts.add(text); colors.add(color);
        return this;
    }

    public Dialog button(String label, UiButton.Style style, Runnable onClick) {
        buttons.add(new UiButton(label, style, UiButton.Icon.NONE, onClick));
        return this;
    }

    public List<UiButton> buttons() { return buttons; }

    public void layout() {
        float W = W(), H = H();
        float cw = Math.min(W - 32 * dp, 420 * dp), pad = 22 * dp, inner = cw - 2 * pad;
        titleLines.clear();
        titleLines.addAll(wrap(title, 23 * dp, tfTitle, inner));
        subLines.clear();
        if (sub != null) subLines.addAll(wrap(sub, 14.5f * dp, tfReg, inner));
        secLines.clear();
        for (String s : texts) secLines.add(wrap(s, 15.5f * dp, tfReg, inner));
        float h = pad + titleLines.size() * 31 * dp;
        if (!subLines.isEmpty()) h += 4 * dp + subLines.size() * 21 * dp;
        for (ArrayList<String> ls : secLines) h += 14 * dp + 18 * dp + ls.size() * 23 * dp;
        int n = buttons.size();
        float bh = 52 * dp, gap = 10 * dp, bblock = n * bh + (n - 1) * gap;
        h += 22 * dp + bblock + pad;
        float top = Math.max(16 * dp, (H - h) / 2);
        card.set((W - cw) / 2, top, (W + cw) / 2, top + h);
        float by = card.bottom - pad - bblock;
        for (UiButton b : buttons) { b.at(card.left + pad, by, card.right - pad, by + bh); by += bh + gap; }
    }

    public void draw(Canvas c) {
        RectF r = card;
        fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xF71A2046, 0xF70B0E22, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 22 * dp, 22 * dp, fill);
        fill.setShader(null);
        stroke.setColor(C_LINE); stroke.setStrokeWidth(dp); c.drawRoundRect(r, 22 * dp, 22 * dp, stroke);
        float pad = 22 * dp, x = r.left + pad, y = r.top + pad;
        for (String s : titleLines) { text(c, s, x, y + 14 * dp, 23 * dp, titleColor, tfTitle, Paint.Align.LEFT); y += 31 * dp; }
        if (!subLines.isEmpty()) {
            y += 4 * dp;
            for (String s : subLines) { text(c, s, x, y + 10 * dp, 14.5f * dp, C_MUTED, tfReg, Paint.Align.LEFT); y += 21 * dp; }
        }
        for (int i = 0; i < labels.size(); i++) {
            y += 14 * dp;
            text(c, labels.get(i), x, y + 8 * dp, 12 * dp, C_MUTED, tfBold, Paint.Align.LEFT);
            y += 18 * dp;
            for (String s : secLines.get(i)) { text(c, s, x, y + 11 * dp, 15.5f * dp, colors.get(i), tfReg, Paint.Align.LEFT); y += 23 * dp; }
        }
    }
}
