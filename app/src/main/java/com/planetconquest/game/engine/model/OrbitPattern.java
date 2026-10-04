package com.planetconquest.game.engine.model;

import static com.planetconquest.game.engine.util.MathUtil.*;

/**
 * Quỹ đạo hỗn loạn của các viên đá đang chờ quanh hành tinh. Mỗi viên có tham số riêng suy ra từ chỉ số (hash ổn định):
 * tốc độ góc và chiều quay, bán kính trung bình trong một dải, dao động hướng tâm, nhiễu góc. Vị trí là hàm thuần của
 * thời gian nên không cần lưu trạng thái; viên đá không bao giờ rời dải quanh hành tinh. Khớp với planet-ui/planet.js (ORBIT_*).
 */
public final class OrbitPattern {
    public static final int MAX_DOTS = 60;
    public static final float BAND_RINGS = 4f;                 // độ dày dải quỹ đạo, tính theo gap
    public static final float SPEED_MIN = .5f, SPEED_MAX = 1.6f;   // rad/s
    public static final float RADIAL_AMP = 1.3f;               // biên độ dao động hướng tâm (gap)
    public static final float RADIAL_FREQ_MIN = .6f, RADIAL_FREQ_MAX = 1.9f;   // rad/s
    public static final float JITTER_AMP = .6f;                // nhiễu góc (rad)
    public static final float JITTER_FREQ_MIN = .8f, JITTER_FREQ_MAX = 2.4f;   // rad/s

    private final float gap, first;

    public OrbitPattern(float dp, float unit) {
        gap = Math.max(6 * dp, unit * .017f);
        first = Math.max(9 * dp, unit * .026f);
    }

    /** Bán kính ngoài cùng mà một viên đá có thể tới, tính từ tâm hành tinh bán kính planetRadius. */
    public float outerRadius(float planetRadius) { return planetRadius + first + (BAND_RINGS + RADIAL_AMP) * gap; }

    /** Ghi vị trí tuyệt đối của đá số i vào out[0], out[1]. */
    public void position(int i, float seed, float clock, float cx, float cy, float planetRadius, float[] out) {
        float w = (hash(i, 1) < .5f ? -1 : 1) * lerp(SPEED_MIN, SPEED_MAX, hash(i, 2));
        float base = planetRadius + first + hash(i, 3) * BAND_RINGS * gap;
        float rad = base + RADIAL_AMP * gap * sin(clock * lerp(RADIAL_FREQ_MIN, RADIAL_FREQ_MAX, hash(i, 4)) + hash(i, 5) * TAU);
        float jit = JITTER_AMP * sin(clock * lerp(JITTER_FREQ_MIN, JITTER_FREQ_MAX, hash(i, 6)) + hash(i, 7) * TAU);
        float a = seed + hash(i, 8) * TAU + clock * w + jit;
        out[0] = cx + cos(a) * rad;
        out[1] = cy + sin(a) * rad;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float hash(int i, int salt) {
        double v = Math.sin(i * 127.1 + salt * 311.7) * 43758.5453;
        return (float) (v - Math.floor(v));
    }
}
