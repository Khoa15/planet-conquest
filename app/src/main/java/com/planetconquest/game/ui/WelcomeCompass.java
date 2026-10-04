package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.planetconquest.game.engine.model.Faction;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.MathUtil.*;
import static com.planetconquest.game.ui.Palette.*;

/** Nút Bản đồ dạng la bàn: mặt kính, 12 vạch, ba hành tinh nhỏ, kim vàng lắc nhẹ (hoặc quay khi vừa chạm). Số liệu lấy từ welcome.js. */
public final class WelcomeCompass extends Painter {
    public static final float RADIUS = 34, HIT_SCALE = 1.35f, PRESS_SCALE = .92f, SPIN_SECONDS = 1f;
    private static final float SPIN_TURNS = 2;
    private static final float[][] MINI = {{-2.2f, 2}, {.4f, 3}, {2.5f, 4}};   // góc, phe của ba hành tinh nhỏ
    private final DashPathEffect dots;
    private Shader faceS, glowS;
    private float cachedR = -1;

    public WelcomeCompass(DrawKit kit) {
        super(kit);
        dots = new DashPathEffect(new float[]{2 * dp, 5 * dp}, 0);
    }

    /** spin: tiến độ quay kim 0..1 sau khi chạm, âm nghĩa là đang chờ. */
    public void draw(Canvas c, float cx, float cy, float R, boolean pressed, float t, float spin) {
        float needle = sin(t * .9f) * .35f + sin(t * 2.3f) * .08f - .5f, glow = 0;
        if (spin >= 0) { float u = Math.min(1, spin), e = 1 - (float) Math.pow(1 - u, 3); needle = -.5f + e * TAU * SPIN_TURNS; glow = 1 - u; }
        if (R != cachedR) {
            cachedR = R;
            faceS = new RadialGradient(-R * .3f, -R * .35f, R * 1.1f, new int[]{0x29FFFFFF, 0xE00E122A}, null, Shader.TileMode.CLAMP);
            glowS = new RadialGradient(0, 0, R * 2.4f, new int[]{alpha(C_GOLD, .35f), alpha(C_GOLD, .35f), alpha(C_GOLD, 0)}, new float[]{0, .33f, 1}, Shader.TileMode.CLAMP);
        }
        c.save();
        c.translate(cx, cy);
        if (pressed) c.scale(PRESS_SCALE, PRESS_SCALE);
        fill.setColor(0xFFFFFFFF);
        if (glow > 0) {
            fill.setShader(glowS); fill.setAlpha((int) (255 * glow));   // shader giữ màu, alpha của paint nhân vào
            c.drawCircle(0, 0, R * 2.4f, fill);
        }
        fill.setColor(0xFFFFFFFF);
        fill.setShader(faceS);
        c.drawCircle(0, 0, R, fill);
        fill.setShader(null);
        stroke.setShader(null);
        stroke.setColor(alpha(C_GOLD, pressed ? 1 : .7f)); stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(0, 0, R, stroke);
        stroke.setPathEffect(dots); stroke.setColor(alpha(C_GOLD, .3f)); stroke.setStrokeWidth(dp);
        c.drawCircle(0, 0, R * .78f, stroke);
        stroke.setPathEffect(null);
        stroke.setColor(0x80E9EDFF);
        for (int k = 0; k < 12; k++) {                       // vạch chia
            float a = k * TAU / 12, r0 = R * (k % 3 == 0 ? .82f : .88f);
            stroke.setStrokeWidth((k % 3 == 0 ? 1.6f : 1) * dp);
            c.drawLine(cos(a) * r0, sin(a) * r0, cos(a) * R * .96f, sin(a) * R * .96f, stroke);
        }
        for (float[] m : MINI) { fill.setColor(Faction.COLORS[(int) m[1]]); c.drawCircle(cos(m[0]) * R * .62f, sin(m[0]) * R * .62f, 2.6f * dp, fill); }
        c.save();
        c.rotate((float) Math.toDegrees(needle));            // kim: đầu vàng, đuôi trắng
        path.reset(); path.moveTo(0, -R * .72f); path.lineTo(R * .14f, 0); path.lineTo(-R * .14f, 0); path.close();
        fill.setColor(C_GOLD); c.drawPath(path, fill);
        path.reset(); path.moveTo(0, R * .55f); path.lineTo(R * .14f, 0); path.lineTo(-R * .14f, 0); path.close();
        fill.setColor(alpha(0xFFE9EDFF, .85f)); c.drawPath(path, fill);
        c.restore();
        fill.setColor(C_BG); c.drawCircle(0, 0, 2.6f * dp, fill);
        stroke.setColor(C_GOLD); stroke.setStrokeWidth(dp); c.drawCircle(0, 0, 2.6f * dp, stroke);
        c.restore();
    }
}
