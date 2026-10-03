package com.planetconquest.game.engine.util;

/** Màu ARGB dạng int; Java thuần nên dùng được cả trong engine lẫn kiểm thử JVM. */
public final class ColorUtil {
    private ColorUtil() {}

    /** Pha màu c về phía màu t với tỉ lệ a (0..1); kết quả đục hoàn toàn. */
    public static int mix(int c, int t, float a) {
        int r = (int) (((c >> 16) & 255) + ((((t >> 16) & 255) - ((c >> 16) & 255)) * a));
        int g = (int) (((c >> 8) & 255) + ((((t >> 8) & 255) - ((c >> 8) & 255)) * a));
        int b = (int) ((c & 255) + (((t & 255) - (c & 255)) * a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Giữ RGB của c, đặt độ trong suốt a (0..1). */
    public static int alpha(int c, float a) { return (c & 0x00FFFFFF) | (((int) (Math.max(0, Math.min(1, a)) * 255)) << 24); }
}
