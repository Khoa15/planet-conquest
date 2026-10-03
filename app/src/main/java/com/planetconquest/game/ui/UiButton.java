package com.planetconquest.game.ui;

import android.graphics.RectF;

/**
 * Nút chuẩn của game. Một bộ kiểu dùng thống nhất cho mọi màn hình:
 * PRIMARY (hành động chính), SECONDARY (hành động phụ), GHOST (quay lại/thoát),
 * ICON (nút biểu tượng vuông bo góc), SMALL (nút nhỏ trong thanh trạng thái).
 */
public final class UiButton {
    public enum Style { PRIMARY, SECONDARY, GHOST, ICON, SMALL }
    public enum Icon { NONE, PAUSE, BACK }

    public final int id;
    public final String label;
    public final Style style;
    public final Icon icon;
    public final RectF r = new RectF();
    public boolean pressed, visible = true;

    public UiButton(int id, String label, Style style, Icon icon) {
        this.id = id;
        this.label = label;
        this.style = style;
        this.icon = icon;
    }

    public UiButton at(float l, float t, float rr, float b) {
        r.set(l, t, rr, b);
        return this;
    }

    public boolean contains(float x, float y, float slop) {
        return visible && x >= r.left - slop && x <= r.right + slop && y >= r.top - slop && y <= r.bottom + slop;
    }
}
