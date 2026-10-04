package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.Shader;

import com.planetconquest.game.engine.util.ColorUtil;

import java.util.Random;

import static com.planetconquest.game.engine.util.MathUtil.*;

/**
 * Đường nối cong vẹo giữa hai hành tinh trên bản đồ chọn màn: nội suy thẳng cộng lệch ngang bằng hai sóng sin,
 * bao bởi sin(pi*t) để hai đầu khớp mép hành tinh. Chưa mở thì nét đứt mờ; mở rồi thì sáng chuyển màu kèm hạt chạy.
 */
public final class LevelPath {
    private static final int SAMPLES = 48, PARTICLES = 3;
    private static final float WOBBLE = 26, GAP = 10, WIDTH = 3, GLOW_EXTRA = 7, PARTICLE_R = 2.2f, PARTICLE_SPEED = .18f;

    private final float[] xs = new float[SAMPLES + 1], ys = new float[SAMPLES + 1];
    private final float ax, ay, bx, by;

    /** Toạ độ và bán kính hai hành tinh theo px; seed cố định để hình dạng đường không đổi giữa các lần mở. */
    public LevelPath(float ax, float ay, float ar, float bx, float by, float br, int seed, float dp) {
        this.ax = ax; this.ay = ay; this.bx = bx; this.by = by;
        Random r = new Random(seed * 977L + 13);
        float f1 = 1.3f + r.nextFloat() * 1.1f, f2 = 2.6f + r.nextFloat() * 1.6f, p1 = r.nextFloat() * TAU, p2 = r.nextFloat() * TAU;
        float flip = r.nextBoolean() ? -1 : 1;
        float dx = bx - ax, dy = by - ay, len = hyp(dx, dy), nx = -dy / len, ny = dx / len;
        float t0 = (ar + GAP * dp) / len, t1 = 1 - (br + GAP * dp) / len;
        for (int k = 0; k <= SAMPLES; k++) {
            float t = t0 + (t1 - t0) * k / SAMPLES, env = (float) Math.pow(sin((float) Math.PI * t), .8);
            float off = flip * WOBBLE * dp * env * (sin(t * TAU * f1 + p1) * .65f + sin(t * TAU * f2 + p2) * .35f);
            xs[k] = ax + dx * t + nx * off;
            ys[k] = ay + dy * t + ny * off;
        }
    }

    /** Tung độ nhỏ nhất / lớn nhất của đường, để bỏ qua khi nằm ngoài màn hình. */
    public float top() { return Math.min(ay, by) - WOBBLE * 2; }
    public float bottom() { return Math.max(ay, by) + WOBBLE * 2; }

    /** lit = 0: đường chưa mở; 1: sáng hết. colA, colB là màu hai hành tinh ở hai đầu. */
    public void draw(Canvas c, DrawKit kit, int colA, int colB, float lit, float clock) {
        float dp = kit.dp;
        build(kit.path);
        kit.stroke.setPathEffect(kit.dotted);
        kit.stroke.setStrokeWidth((WIDTH - 1) * dp);
        kit.stroke.setColor(0x47A0AFFF);
        c.drawPath(kit.path, kit.stroke);
        kit.stroke.setPathEffect(null);
        if (lit <= .01f) return;

        kit.stroke.setColor(0xFFFFFFFF);   // paint dùng chung: đặt lại màu đục trước khi vẽ bằng shader
        kit.stroke.setShader(new LinearGradient(ax, ay, bx, by, ColorUtil.alpha(colA, lit), ColorUtil.alpha(colB, lit), Shader.TileMode.CLAMP));
        kit.stroke.setStrokeWidth((WIDTH + GLOW_EXTRA) * dp);
        kit.stroke.setAlpha(33);
        c.drawPath(kit.path, kit.stroke);
        kit.stroke.setAlpha(255);
        kit.stroke.setStrokeWidth(WIDTH * dp);
        c.drawPath(kit.path, kit.stroke);
        kit.stroke.setShader(null);

        kit.fill.setColor(ColorUtil.alpha(0xFFFFFFFF, .85f * lit));
        for (int k = 0; k < PARTICLES; k++) {
            float u = ((clock * PARTICLE_SPEED + (float) k / PARTICLES) % 1) * SAMPLES;
            int a = (int) u, b = Math.min(a + 1, SAMPLES);
            float f = u - a;
            c.drawCircle(xs[a] + (xs[b] - xs[a]) * f, ys[a] + (ys[b] - ys[a]) * f, PARTICLE_R * dp, kit.fill);
        }
    }

    private void build(Path p) {
        p.reset();
        p.moveTo(xs[0], ys[0]);
        for (int k = 1; k <= SAMPLES; k++) p.lineTo(xs[k], ys[k]);
    }
}
