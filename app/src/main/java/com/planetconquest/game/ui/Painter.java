package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.ArrayList;

/** Lớp nền cho mọi thứ tự vẽ: đưa DrawKit dùng chung về dạng truy cập ngắn gọn (fill, stroke, text(), W()...). */
public abstract class Painter {
    protected final DrawKit kit;
    protected final float dp;
    protected final Paint fill, stroke, txt;
    protected final Path path;
    protected final RectF tmp;
    protected final Typeface tfReg, tfBold, tfTitle;

    protected Painter(DrawKit kit) {
        this.kit = kit;
        dp = kit.dp; fill = kit.fill; stroke = kit.stroke; txt = kit.txt; path = kit.path; tmp = kit.tmp;
        tfReg = kit.reg; tfBold = kit.bold; tfTitle = kit.title;
    }

    protected final float W() { return kit.w; }
    protected final float H() { return kit.h; }

    protected final void text(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align al) { kit.text(c, s, x, y, size, color, tf, al); }
    protected final ArrayList<String> wrap(String s, float size, Typeface tf, float maxW) { return kit.wrap(s, size, tf, maxW); }
    protected final void panel(Canvas c, RectF r, float rad, int strokeColor) { kit.panel(c, r, rad, strokeColor); }
    protected final void pill(Canvas c, float x, float y, String s, int col) { kit.pill(c, x, y, s, col); }
    protected final void dashed(boolean on, float phase) { kit.dashed(on, phase); }
    protected final void arc(Canvas c, float x, float y, float r, float frac) { kit.arc(c, x, y, r, frac); }
    protected final void dim(Canvas c) { kit.dim(c); }
}
