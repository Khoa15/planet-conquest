package com.planetconquest.game;

import android.graphics.RectF;

/**
 * Nút chuẩn của game. Một bộ kiểu dùng thống nhất cho mọi màn hình:
 * PRIMARY (hành động chính), SECONDARY (hành động phụ), GHOST (quay lại/thoát),
 * ICON (nút biểu tượng vuông bo góc), SMALL (nút nhỏ trong thanh trạng thái).
 */
final class UiButton {
    static final int PRIMARY = 0, SECONDARY = 1, GHOST = 2, ICON = 3, SMALL = 4;
    static final int I_NONE = 0, I_PAUSE = 1, I_BACK = 2;

    final int id;
    final String label;
    final int style, icon;
    final RectF r = new RectF();
    boolean pressed, visible = true;

    UiButton(int id, String label, int style, int icon) {
        this.id = id;
        this.label = label;
        this.style = style;
        this.icon = icon;
    }

    UiButton at(float l, float t, float rr, float b) {
        r.set(l, t, rr, b);
        return this;
    }

    boolean contains(float x, float y, float slop) {
        return visible && x >= r.left - slop && x <= r.right + slop && y >= r.top - slop && y <= r.bottom + slop;
    }
}
