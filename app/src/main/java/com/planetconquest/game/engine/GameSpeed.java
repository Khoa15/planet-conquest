package com.planetconquest.game.engine;

/** Tốc độ game: xoay vòng x1 → x1.25 → x1.5 → x2 → x1. Hệ số nhân vào dt của mô phỏng (số liệu khớp planet-ui/speed.js). */
public final class GameSpeed {
    public static final float[] STEPS = {1f, 1.25f, 1.5f, 2f};
    private static final String[] TEXTS = {"1", "1.25", "1.5", "2"};

    private int index = 0;

    public float factor() { return STEPS[index]; }
    /** Số hiển thị không kèm chữ "x": "1", "1.25", "1.5", "2". */
    public String valueText() { return TEXTS[index]; }
    public boolean isFast() { return index > 0; }

    public void next() { index = (index + 1) % STEPS.length; }
    public void reset() { index = 0; }
}
