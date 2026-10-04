package com.planetconquest.game.ui;

import android.graphics.RectF;

/**
 * Nút chuẩn của game. Một bộ kiểu dùng thống nhất cho mọi màn hình:
 * PRIMARY (hành động chính), SECONDARY (hành động phụ), GHOST (quay lại/thoát),
 * ICON (nút biểu tượng vuông bo góc), SMALL (nút nhỏ trong thanh trạng thái).
 * Nút giữ luôn hành động của nó (Command): màn hình không cần switch theo id.
 */
public final class UiButton {
    public enum Style { PRIMARY, SECONDARY, GHOST, ICON, SMALL }
    public enum Icon { NONE, PAUSE, BACK, SOUND_ON, SOUND_OFF, LANGUAGE }

    public final String label;
    public final Style style;
    public final Icon icon;
    public final RectF r = new RectF();
    public boolean pressed, visible = true;
    /** Mọi nhãn có thể hiện trên nút (nút LANGUAGE): bề rộng cụm canh theo nhãn rộng nhất để nút không nhảy khi đổi nhãn. */
    public String[] labelSlot = new String[0];
    private final Runnable onClick;

    public UiButton(String label, Style style, Icon icon, Runnable onClick) {
        this.label = label;
        this.style = style;
        this.icon = icon;
        this.onClick = onClick;
    }

    public UiButton at(float l, float t, float rr, float b) {
        r.set(l, t, rr, b);
        return this;
    }

    public UiButton slot(String... labels) {
        labelSlot = labels;
        return this;
    }

    public boolean contains(float x, float y, float slop) {
        return visible && x >= r.left - slop && x <= r.right + slop && y >= r.top - slop && y <= r.bottom + slop;
    }

    public void click() { onClick.run(); }
}
