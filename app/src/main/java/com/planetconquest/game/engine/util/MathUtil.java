package com.planetconquest.game.engine.util;

/** Hàm toán dùng chung cho engine và giao diện (float, không cấp phát). */
public final class MathUtil {
    private MathUtil() {}

    public static final float TAU = (float) (Math.PI * 2);

    public static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    public static float hyp(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    public static float cos(float a) { return (float) Math.cos(a); }
    public static float sin(float a) { return (float) Math.sin(a); }

    /** Điểm (x, y) nằm trong đa giác n đỉnh (xs, ys) theo quy tắc chẵn lẻ. */
    public static boolean inPoly(float x, float y, float[] xs, float[] ys, int n) {
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((ys[i] > y) != (ys[j] > y) && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) inside = !inside;
        }
        return inside;
    }

    /** Cạnh ngắn của hộp bao quanh n điểm. */
    public static float bboxMin(float[] xs, float[] ys, int n) {
        float x0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) { x0 = Math.min(x0, xs[i]); x1 = Math.max(x1, xs[i]); y0 = Math.min(y0, ys[i]); y1 = Math.max(y1, ys[i]); }
        return Math.min(x1 - x0, y1 - y0);
    }
}
