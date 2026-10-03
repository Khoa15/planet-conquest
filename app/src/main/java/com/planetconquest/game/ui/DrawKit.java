package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.planetconquest.game.engine.util.ColorUtil;

import java.util.ArrayList;

import static com.planetconquest.game.ui.Palette.*;

/** Bộ công cụ vẽ dùng chung: kích thước màn hình, Paint, font và các hình cơ bản (chữ, panel, nút, viên thuốc). */
public final class DrawKit {
    public final float dp;
    public float w, h;
    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint.FontMetrics fm = new Paint.FontMetrics();
    public final Path path = new Path();
    public final RectF tmp = new RectF();
    public final Typeface reg = Typeface.create("sans-serif", Typeface.NORMAL),
            bold = Typeface.create("sans-serif", Typeface.BOLD),
            title = Typeface.create("sans-serif-black", Typeface.NORMAL);

    public DrawKit(float dp) {
        this.dp = dp;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setSize(float w, float h) { this.w = w; this.h = h; }

    public void text(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align al) {
        txt.setTextSize(size); txt.setColor(color); txt.setTypeface(tf); txt.setTextAlign(al);
        txt.getFontMetrics(fm);
        c.drawText(s, x, y - (fm.ascent + fm.descent) / 2, txt);
    }

    public ArrayList<String> wrap(String s, float size, Typeface tf, float maxW) {
        txt.setTextSize(size); txt.setTypeface(tf);
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String cand = line.length() == 0 ? word : line + " " + word;
            if (txt.measureText(cand) <= maxW || line.length() == 0) { line.setLength(0); line.append(cand); }
            else { out.add(line.toString()); line.setLength(0); line.append(word); }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    public void panel(Canvas c, RectF r, float rad, int strokeColor) {
        fill.setColor(C_PANEL); c.drawRoundRect(r, rad, rad, fill);
        stroke.setColor(strokeColor); stroke.setStrokeWidth(dp); c.drawRoundRect(r, rad, rad, stroke);
    }

    public void pill(Canvas c, float x, float y, String s, int col) {
        txt.setTextSize(12.5f * dp); txt.setTypeface(bold);
        float pw = txt.measureText(s) + 20 * dp, ph = 26 * dp;
        float px = Math.max(6 * dp, Math.min(x - pw / 2, w - pw - 6 * dp)), py = Math.max(y - ph / 2, 6 * dp);
        tmp.set(px, py, px + pw, py + ph);
        fill.setColor(0xE6080A1A); c.drawRoundRect(tmp, ph / 2, ph / 2, fill);
        stroke.setColor(col); stroke.setStrokeWidth(1.5f * dp); c.drawRoundRect(tmp, ph / 2, ph / 2, stroke);
        text(c, s, px + pw / 2, py + ph / 2, 12.5f * dp, 0xFFFFFFFF, bold, Paint.Align.CENTER);
    }

    public void dashed(boolean on, float phase) {
        stroke.setPathEffect(on ? new DashPathEffect(new float[]{7 * dp, 7 * dp}, 14 * dp - (phase * 50 * dp) % (14 * dp)) : null);
    }

    public void arc(Canvas c, float x, float y, float r, float frac) {
        tmp.set(x - r, y - r, x + r, y + r);
        c.drawArc(tmp, -90, 360 * frac, false, stroke);
    }

    public void dim(Canvas c) { fill.setColor(0xC8030510); c.drawRect(0, 0, w, h, fill); }

    public void drawButton(Canvas c, UiButton b) {
        if (!b.visible) return;
        RectF r = b.r;
        float rad = b.style == UiButton.Style.ICON ? 14 * dp : Math.min(r.height() / 2, 16 * dp);
        c.save();
        if (b.pressed) c.scale(.96f, .96f, r.centerX(), r.centerY());
        int fillC, textC, strokeC = 0;
        switch (b.style) {
            case PRIMARY: fillC = b.pressed ? ColorUtil.mix(C_YOU, 0xFF000000, .18f) : C_YOU; textC = 0xFF04170F; break;
            case SECONDARY: fillC = b.pressed ? 0x2EFFFFFF : 0x14FFFFFF; textC = C_INK; strokeC = 0x55A0AFFF; break;
            case GHOST: fillC = b.pressed ? 0x1AFFFFFF : 0; textC = C_MUTED; break;
            case SMALL: fillC = b.pressed ? ColorUtil.mix(C_GOLD, 0xFF000000, .18f) : C_GOLD; textC = 0xFF241A00; break;
            default: fillC = b.pressed ? 0xF01E2650 : C_PANEL; textC = C_INK; strokeC = C_LINE; break;
        }
        if (fillC != 0) { fill.setColor(fillC); c.drawRoundRect(r, rad, rad, fill); }
        if (strokeC != 0) { stroke.setColor(strokeC); stroke.setStrokeWidth(1.2f * dp); c.drawRoundRect(r, rad, rad, stroke); }
        if (b.style == UiButton.Style.ICON) drawIcon(c, b.icon, r.centerX(), r.centerY(), textC);
        else text(c, b.label, r.centerX(), r.centerY(), (b.style == UiButton.Style.SMALL ? 13.5f : 16.5f) * dp, textC, bold, Paint.Align.CENTER);
        c.restore();
    }

    private void drawIcon(Canvas c, UiButton.Icon icon, float cx, float cy, int col) {
        if (icon == UiButton.Icon.PAUSE) {
            fill.setColor(col);
            tmp.set(cx - 7 * dp, cy - 8 * dp, cx - 2.5f * dp, cy + 8 * dp); c.drawRoundRect(tmp, 1.5f * dp, 1.5f * dp, fill);
            tmp.set(cx + 2.5f * dp, cy - 8 * dp, cx + 7 * dp, cy + 8 * dp); c.drawRoundRect(tmp, 1.5f * dp, 1.5f * dp, fill);
        } else if (icon == UiButton.Icon.BACK) {
            stroke.setColor(col); stroke.setStrokeWidth(2.6f * dp);
            path.reset(); path.moveTo(cx + 4 * dp, cy - 8 * dp); path.lineTo(cx - 4 * dp, cy); path.lineTo(cx + 4 * dp, cy + 8 * dp);
            c.drawPath(path, stroke);
        }
    }
}
