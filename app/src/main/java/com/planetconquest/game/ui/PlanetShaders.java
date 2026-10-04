package com.planetconquest.game.ui;

import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;

/**
 * Bộ nhớ đệm shader quầng sáng + thân hành tinh theo (phe, bán kính). Shader dựng quanh gốc toạ độ (0,0),
 * nơi gọi dịch canvas tới tâm hành tinh, nên không tạo RadialGradient mới mỗi khung hình.
 */
final class PlanetShaders {
    /** Cặp shader của một (phe, bán kính). */
    static final class Pair {
        final int owner;
        final float radius;
        final Shader glow, body;

        Pair(int owner, float radius, int col, int light, int dark) {
            this.owner = owner;
            this.radius = radius;
            glow = new RadialGradient(0, 0, radius * 2.1f, new int[]{alpha(col, .34f), alpha(col, .34f), alpha(col, 0)}, new float[]{0, .43f, 1}, Shader.TileMode.CLAMP);
            body = new RadialGradient(-radius * .35f, -radius * .4f, radius * 1.45f, new int[]{light, col, dark}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
        }
    }

    private static final int MAX = 64;      // bán kính chỉ nhận vài giá trị rời rạc (theo cấp); quá ngưỡng thì dọn lại
    private final ArrayList<Pair> cache = new ArrayList<Pair>();

    Pair get(int owner, float radius, int col, int light, int dark) {
        for (int i = 0; i < cache.size(); i++) {
            Pair p = cache.get(i);
            if (p.owner == owner && p.radius == radius) return p;
        }
        if (cache.size() >= MAX) cache.clear();
        Pair p = new Pair(owner, radius, col, light, dark);
        cache.add(p);
        return p;
    }
}
