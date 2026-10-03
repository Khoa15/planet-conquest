package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;

import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.util.ColorUtil;
import com.planetconquest.game.engine.util.MathUtil;

import java.util.Random;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.MathUtil.cos;
import static com.planetconquest.game.engine.util.MathUtil.sin;

/**
 * Nền động của màn Welcome: tinh vân trôi, hố đen có đĩa bồi tụ xé nát một hành tinh nhỏ, hành tinh xanh
 * của người chơi định kỳ bắn đá sang hành tinh đỏ, các hành tinh đối thủ, vành đai thiên thạch chéo và
 * thiên thạch tiền cảnh mờ. Camera đẩy vào rất chậm, mỗi lớp lệch một chút theo độ sâu (parallax).
 * Shader, path và số ngẫu nhiên tạo sẵn; mỗi khung hình chỉ biến đổi canvas, không cấp phát.
 * Chừa trống phần trên giữa (tiêu đề) và khoảng 25% dưới cùng (nút).
 */
public final class WelcomeScene {

    // x, y (tỉ lệ màn hình), bán kính (× cạnh ngắn), phe, độ sâu parallax, tốc độ trôi dải mây, số đá quay quanh
    private static final float[][] PL = {
            {.50f, .50f, .17f, 0, 1f, .35f, 0},
            {.19f, .335f, .075f, 1, .92f, -.5f, 10},
            {.86f, .635f, .062f, 2, 1.08f, .42f, 8},
            {.13f, .615f, .05f, 3, .78f, -.3f, 0},
            {.085f, .45f, .028f, 4, .6f, .6f, 0},
    };
    private static final int SAND = 8;                          // màu cát xám cho hành tinh bị xé
    private static final float TILT = -16f, SQ = .3f;           // góc nghiêng và độ dẹt của đĩa bồi tụ
    private static final float CAM_PERIOD = 24f, LAUNCH_PERIOD = 4.2f, FLIGHT = 1.5f, LAUNCH_GAP = .09f;
    private static final int LAUNCH_N = 9, BELT = 46, DISK_P = 64, ARCS = 16, FRAG = 60, DUST = 22, FG = 5;
    private static final float[][] NEB = {                      // x, y, bán kính (× cạnh dài), màu, độ đậm
            {.18f, .30f, .42f, 0x6A3CE0, .16f}, {.85f, .20f, .35f, 0x2A70E0, .14f},
            {.70f, .78f, .45f, 0x5A3CC8, .12f}, {.30f, .70f, .30f, 0x288CC8, .10f}};
    private static final float[] FG_Y = {.30f, .47f, .62f, .70f, .40f};
    // 3 vòng đá quanh hành tinh xanh: số đá, tốc độ góc. Loạt phóng lấy đá từ chính các vòng này.
    private static final int[] RING_CNT = {12, 18, 24};
    private static final float[] RING_SPD = {.6f, -.4f, .28f};
    private static final int[] SLOT_OFF = {0, 2, -2};
    // Mốc thời gian trong một chu kỳ phóng: tụ sáng, rời vòng, mọc lại đá mới
    private static final float CHARGE = .3f, LAUNCH_AT = .35f, REGROW_A = 2.9f, REGROW_B = 3.8f;
    // Hành tinh bị xé: đường xoắn vào hố đen (góc đầu, bán kính đầu × cạnh ngắn, góc quét), vết khoét
    private static final float TORN_TH0 = 3.0f, TORN_RS = .30f, TORN_SW = -2.6f, BITE = 1.25f;
    private static final int BITE_N = 18, CRACKS = 4;

    private final float dp;
    private float W, H, U;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG),
            disk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path[] rockShape = new Path[8];
    private final Matrix mtx = new Matrix();
    private final RectF oval = new RectF();
    private final int[] FC = Faction.COLORS, FL = Faction.LIGHT;

    private final Shader[] glowS = new Shader[PL.length], bodyS = new Shader[PL.length], nebS = new Shader[NEB.length];
    private Shader sandS, heatS, haloS, shadowS, diskS, blurS, topS, botS;
    private float bx, by, rh;                                   // tâm và bán kính chân trời sự kiện
    private float b0x, b0y, bdx, bdy, bLen;                     // vành đai: điểm đầu, hướng, chiều dài
    private float zoom, swayX, swayY;
    private final int[] lSlot = new int[LAUNCH_N];              // vị trí trên vòng của từng viên đá trong loạt phóng
    private final float[] biteF = new float[BITE_N + 1];        // độ sâu vết khoét lởm chởm
    private final int[] crackK = new int[CRACKS];
    private final float[] crackL = new float[CRACKS], crackB = new float[CRACKS];
    private final Path tornBody = new Path(), tornEdge = new Path(), tornCracks = new Path(), tornGhost = new Path();
    private float tornR;

    private final float[] beltS = new float[BELT], beltN = new float[BELT], beltSize = new float[BELT],
            beltRot = new float[BELT], beltSpin = new float[BELT], beltDepth = new float[BELT], beltCol = new float[BELT];
    private final float[] diskR = new float[DISK_P], diskA = new float[DISK_P], diskSz = new float[DISK_P];
    private final float[] arcR = new float[ARCS], arcA = new float[ARCS], arcSw = new float[ARCS], arcW = new float[ARCS], arcAl = new float[ARCS];
    private final float[] jit = new float[FRAG + DUST];
    private final float[] fgX = new float[FG], fgV = new float[FG], fgSize = new float[FG];

    public WelcomeScene(float dp) {
        this.dp = dp;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        disk.setStyle(Paint.Style.STROKE);
        disk.setColor(0xFFFFFFFF);
        Random r = new Random(7);
        for (int k = 0; k < rockShape.length; k++) {
            Path p = new Path();
            for (int i = 0; i < 8; i++) {
                float a = i / 8f * MathUtil.TAU, rad = .72f + r.nextFloat() * .38f;
                if (i == 0) p.moveTo(cos(a) * rad, sin(a) * rad); else p.lineTo(cos(a) * rad, sin(a) * rad);
            }
            p.close();
            rockShape[k] = p;
        }
        for (int i = 0; i < BELT; i++) {
            beltS[i] = r.nextFloat();
            beltN[i] = (float) r.nextGaussian() * .45f;
            beltDepth[i] = .55f + r.nextFloat() * .6f;
            beltSize[i] = (1.8f + r.nextFloat() * 3.6f) * beltDepth[i];
            beltRot[i] = r.nextFloat() * 360;
            beltSpin[i] = (r.nextFloat() - .5f) * 60;
            beltCol[i] = r.nextFloat();
        }
        for (int i = 0; i < DISK_P; i++) {
            diskR[i] = 1.3f + r.nextFloat() * 1.7f;
            diskA[i] = r.nextFloat() * MathUtil.TAU;
            diskSz[i] = .8f + r.nextFloat() * .9f;
        }
        for (int i = 0; i < ARCS; i++) {
            arcR[i] = 1.7f + r.nextFloat() * 1.9f;
            arcA[i] = r.nextFloat() * 360;
            arcSw[i] = 10 + r.nextFloat() * 30;
            arcW[i] = .6f + r.nextFloat() * .6f;
            arcAl[i] = .15f + r.nextFloat() * .3f;
        }
        for (int i = 0; i < jit.length; i++) jit[i] = r.nextFloat() - .5f;
        for (int k = 0; k <= BITE_N; k++) {
            float base = (float) Math.pow(Math.sin(Math.PI * k / BITE_N), .6);
            biteF[k] = 1 - (.36f + (k % 2 == 0 ? .14f : 0) + r.nextFloat() * .12f) * base;
        }
        for (int i = 0; i < CRACKS; i++) {
            crackK[i] = 3 + i * (BITE_N - 6) / (CRACKS - 1);
            crackL[i] = .35f + r.nextFloat() * .3f;
            crackB[i] = (r.nextFloat() - .5f) * .5f;
        }
        for (int i = 0; i < FG; i++) {
            fgX[i] = r.nextFloat();
            fgV[i] = 34 + r.nextFloat() * 26;
            fgSize[i] = 10 + r.nextFloat() * 12;
        }
    }

    public void setSize(float w, float h) {
        W = w; H = h; U = Math.min(w, h);
        if (W <= 0 || H <= 0) return;
        for (int i = 0; i < PL.length; i++) {
            float[] p = PL[i];
            float r = p[2] * U;
            int o = (int) p[3], fog = 0xFF0A0D20;
            float haze = Math.max(0, 1 - p[4]) * .6f;
            int col = ColorUtil.mix(FC[o], fog, haze), lt = ColorUtil.mix(FL[o], fog, haze);
            int dk = ColorUtil.mix(col, 0xFF000000, .62f);
            glowS[i] = new RadialGradient(0, 0, r * 2.2f, new int[]{alpha(col, .38f), alpha(col, .3f), alpha(col, 0)},
                    new float[]{0, .45f, 1}, Shader.TileMode.CLAMP);
            bodyS[i] = new RadialGradient(-r * .35f, -r * .4f, r * 1.45f, new int[]{lt, col, dk}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
        }
        buildTorn();
        for (int i = 0; i < NEB.length; i++) {
            int c = (int) NEB[i][3];
            nebS[i] = new RadialGradient(0, 0, NEB[i][2] * Math.max(W, H), new int[]{alpha(c, NEB[i][4]), alpha(c, NEB[i][4] * .4f), alpha(c, 0)},
                    new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
        }
        bx = W * .80f; by = H * .30f; rh = .065f * U;
        haloS = new RadialGradient(0, 0, rh * 5, new int[]{alpha(0xFFFFB45A, .30f), alpha(0xFFFF8040, .12f), alpha(0xFF8040C0, .05f), 0},
                new float[]{.18f, .4f, .7f, 1}, Shader.TileMode.CLAMP);
        shadowS = new RadialGradient(0, 0, rh * 1.7f, new int[]{0xFF000000, 0xFF000000, 0}, new float[]{0, .58f, 1}, Shader.TileMode.CLAMP);
        diskS = new SweepGradient(0, 0, new int[]{0xFFFFF3D6, 0xFFFFC46B, 0xFFFF8A3D, 0xFFE0602A, 0xFFFF9A48, 0xFFFFD58A, 0xFFFFF3D6}, null);
        blurS = new RadialGradient(0, 0, 100, new int[]{0xFF6B5A4D, 0xFF4E4239, 0x004E4239}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP);
        topS = new LinearGradient(0, 0, 0, H * .26f, alpha(Palette.C_BG, .6f), alpha(Palette.C_BG, 0), Shader.TileMode.CLAMP);
        botS = new LinearGradient(0, H * .66f, 0, H, alpha(Palette.C_BG, 0), alpha(Palette.C_BG, .8f), Shader.TileMode.CLAMP);
        float x0 = -.15f * W, y0 = .64f * H, x1 = 1.15f * W, y1 = .40f * H;
        bLen = (float) Math.hypot(x1 - x0, y1 - y0);
        b0x = x0; b0y = y0; bdx = (x1 - x0) / bLen; bdy = (y1 - y0) / bLen;
    }

    public void draw(Canvas c, float t) {
        if (W <= 0 || haloS == null) return;
        float ph = MathUtil.TAU * t / CAM_PERIOD;
        zoom = .03f * (.5f - .5f * cos(ph));
        swayX = sin(ph) * 5 * dp;
        swayY = (cos(ph) - 1) * 3 * dp;
        drawNebula(c, t);
        drawPlanet(c, 4, t);
        drawPlanet(c, 3, t);
        drawBlackHole(c, t);
        drawBelt(c, t);
        drawPlanet(c, 1, t);
        launchSlots(t);
        drawPlanet(c, 0, t);
        drawLaunch(c, t);
        drawPlanet(c, 2, t);
        drawForeground(c, t);
        fill.setShader(topS); fill.setAlpha(255); c.drawRect(0, 0, W, H * .26f, fill);
        fill.setShader(botS); c.drawRect(0, H * .66f, W, H, fill);
        fill.setShader(null);
    }

    // Camera: phóng to quanh tâm rồi lệch nhẹ; lớp càng gần (depth lớn) càng chuyển động nhiều
    private void cam(Canvas c, float depth) {
        c.save();
        c.translate(swayX * depth, swayY * depth);
        float s = 1 + zoom * depth;
        c.scale(s, s, W * .5f, H * .45f);
    }

    private float camX(float x, float d) { return (x - W * .5f) * (1 + zoom * d) + W * .5f + swayX * d; }
    private float camY(float y, float d) { return (y - H * .45f) * (1 + zoom * d) + H * .45f + swayY * d; }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    private void drawNebula(Canvas c, float t) {
        cam(c, .3f);
        for (int i = 0; i < NEB.length; i++) {
            float x = NEB[i][0] * W + sin(t * .025f + i * 1.7f) * 18 * dp, y = NEB[i][1] * H + cos(t * .02f + i) * 12 * dp;
            float k = 1 + .06f * sin(t * .03f + i * 2.3f);
            c.save();
            c.translate(x, y);
            c.scale(k, k);
            fill.setShader(nebS[i]);
            fill.setAlpha(255);
            c.drawCircle(0, 0, NEB[i][2] * Math.max(W, H), fill);
            c.restore();
        }
        fill.setShader(null);
        c.restore();
    }

    private void drawPlanet(Canvas c, int i, float t) {
        float[] p = PL[i];
        float r = p[2] * U, spin = p[5];
        int o = (int) p[3];
        cam(c, p[4]);
        c.translate(p[0] * W, p[1] * H);
        float pulse = .5f + .5f * sin(t * (i == 0 ? 1.7f : .9f) + i);
        c.save();
        float k = 1 + (i == 0 ? .07f : .03f) * pulse;
        c.scale(k, k);
        fill.setShader(glowS[i]);
        fill.setAlpha((int) (255 * (.75f + .25f * pulse)));
        c.drawCircle(0, 0, r * 2.2f, fill);
        c.restore();
        fill.setShader(bodyS[i]);
        fill.setAlpha(255);
        c.drawCircle(0, 0, r, fill);
        fill.setShader(null);

        c.save();
        path.reset();
        path.addCircle(0, 0, r, Path.Direction.CW);
        c.clipPath(path);
        for (int b = 0; b < 5; b++) {                       // dải mây uốn lượn trôi chậm
            float kb = -.64f + b * .32f, w = i * 1.7f + b * 2.1f + t * spin;
            stroke.setColor(b % 2 == 0 ? 0x2A000000 : 0x1CFFFFFF);
            stroke.setStrokeWidth(r * (b % 2 == 0 ? .14f : .08f));
            path.reset();
            path.moveTo(-r * 1.1f, r * kb);
            path.cubicTo(-r * .4f, r * (kb + .12f * sin(w)), r * .3f, r * (kb - .12f * sin(w * .8f + 1.3f)), r * 1.1f, r * kb);
            c.drawPath(path, stroke);
        }
        float u = frac(t * Math.abs(spin) * .08f + i * .37f), sx = (u * 2.6f - 1.3f) * r * Math.signum(spin);
        oval.set(sx - r * .22f, r * .12f, sx + r * .22f, r * .32f);   // cơn bão trôi ngang: cảm giác hành tinh tự quay
        fill.setColor(0x22FFFFFF);
        c.drawOval(oval, fill);
        c.restore();
        stroke.setColor(0x47FFFFFF);
        stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(0, 0, r, stroke);

        if (i == 0) {
            float lt = t % LAUNCH_PERIOD;
            for (int ring = 0; ring < 3; ring++) {
                float rad = ringRad(ring);
                stroke.setColor(alpha(FC[0], .12f)); stroke.setStrokeWidth(dp);
                c.drawCircle(0, 0, rad, stroke);
                for (int d = 0; d < RING_CNT[ring]; d++) {
                    float sc = dotScale(ring, d, lt);
                    if (sc <= 0) continue;                  // đá này đã rời vòng, đang bay hoặc chưa mọc lại
                    float a = t * RING_SPD[ring] + d * MathUtil.TAU / RING_CNT[ring], dx = cos(a) * rad, dy = sin(a) * rad;
                    float hot = Math.max(0, sc - 1) / .6f;  // tụ sáng ngay trước khi phóng
                    fill.setColor(alpha(FC[0], .18f + .25f * hot)); c.drawCircle(dx, dy, 4.5f * dp * sc, fill);
                    fill.setColor(alpha(ColorUtil.mix(FL[0], 0xFFFFFFFF, hot), .95f)); c.drawCircle(dx, dy, 2.1f * dp * sc, fill);
                }
            }
        } else if (p[6] > 0) {
            int n = (int) p[6];
            float rad = r + 9 * dp;
            fill.setColor(alpha(FC[o], .9f));
            for (int d = 0; d < n; d++) {
                float a = t * (i % 2 == 0 ? .5f : -.5f) + d * MathUtil.TAU / n;
                c.drawCircle(cos(a) * rad, sin(a) * rad, 1.8f * dp, fill);
            }
        }
        c.restore();
    }

    private float ringRad(int k) { return PL[0][2] * U + (14 + 10 * k) * dp; }

    // Chọn cho mỗi viên trong loạt phóng một chỗ trên vòng: chỗ đang ở phía hướng về hành tinh đỏ lúc phóng
    private void launchSlots(float t) {
        float cs = t - t % LAUNCH_PERIOD;
        float tg = (float) Math.atan2((PL[1][1] - PL[0][1]) * H, (PL[1][0] - PL[0][0]) * W);
        for (int i = 0; i < LAUNCH_N; i++) {
            int k = i % 3, n = RING_CNT[k];
            float li = cs + LAUNCH_AT + i * LAUNCH_GAP;
            int base = Math.round((tg - li * RING_SPD[k]) / (MathUtil.TAU / n));
            lSlot[i] = ((base + SLOT_OFF[i / 3]) % n + n) % n;
        }
    }

    // Tỉ lệ vẽ một viên đá trên vòng: >1 đang tụ sáng, 0 đã bay đi, 0..1 đang mọc lại, 1 bình thường
    private float dotScale(int ring, int d, float lt) {
        for (int i = ring; i < LAUNCH_N; i += 3) {
            if (lSlot[i] != d) continue;
            float li = LAUNCH_AT + i * LAUNCH_GAP;
            if (lt < li - CHARGE) return 1;
            if (lt < li) return 1 + .6f * (lt - (li - CHARGE)) / CHARGE;
            if (lt < REGROW_A) return 0;
            if (lt < REGROW_B) return (lt - REGROW_A) / (REGROW_B - REGROW_A);
            return 1;
        }
        return 1;
    }

    // Mỗi chu kỳ, 9 viên đá tách khỏi 3 vòng quỹ đạo, văng theo hướng quay rồi lượn sang hành tinh đỏ
    private void drawLaunch(Canvas c, float t) {
        float lt = t % LAUNCH_PERIOD, cs = t - lt;
        float[] a = PL[0], b = PL[1];
        float ax = a[0] * W, ay = a[1] * H, wtx = b[0] * W, wty = b[1] * H, wtr = b[2] * U;
        float dx = wtx - ax, dy = wty - ay, d = (float) Math.hypot(dx, dy);
        float wex = wtx - dx / d * wtr * .9f, wey = wty - dy / d * wtr * .9f;
        for (int i = 0; i < LAUNCH_N; i++) {
            float li = LAUNCH_AT + i * LAUNCH_GAP, u = (lt - li) / FLIGHT;
            if (u < 0 || u > 1) continue;
            int k = i % 3;
            float rad = ringRad(k), an = (cs + li) * RING_SPD[k] + lSlot[i] * MathUtil.TAU / RING_CNT[k], sg = Math.signum(RING_SPD[k]);
            float p0x = ax + cos(an) * rad, p0y = ay + sin(an) * rad;
            float vx = wex - p0x, vy = wey - p0y, dd = (float) Math.hypot(vx, vy);
            float p1x = p0x - sin(an) * sg * dd * .22f + cos(an) * dd * .1f, p1y = p0y + cos(an) * sg * dd * .22f + sin(an) * dd * .1f;
            float p2x = (p0x + wex) / 2 - vy * .22f, p2y = (p0y + wey) / 2 + vx * .22f;
            float e = u * (.6f + .4f * u);
            for (int tr = 3; tr >= 0; tr--) {
                float q = e - tr * .03f;
                if (q < 0) continue;
                float m = 1 - q, w0 = m * m * m, w1 = 3 * m * m * q, w2 = 3 * m * q * q, w3 = q * q * q;
                float x = w0 * p0x + w1 * p1x + w2 * p2x + w3 * wex, y = w0 * p0y + w1 * p1y + w2 * p2y + w3 * wey;
                float dep = 1 - .08f * q, sx = camX(x, dep), sy = camY(y, dep);
                if (tr == 0) { fill.setColor(alpha(FC[0], .3f)); c.drawCircle(sx, sy, 4.5f * dp, fill); }
                fill.setColor(alpha(tr == 0 ? FL[0] : FC[0], 1 - tr / 4f));
                c.drawCircle(sx, sy, (2.4f - .4f * tr) * dp, fill);
            }
        }
        float ex = camX(wex, b[4]), ey = camY(wey, b[4]), tx = camX(wtx, b[4]), ty = camY(wty, b[4]), tr = wtr * (1 + zoom * b[4]);
        for (int i = 0; i < LAUNCH_N; i++) {               // chớp sáng khi trúng đích
            float age = lt - (LAUNCH_AT + i * LAUNCH_GAP + FLIGHT);
            if (age < 0 || age > .5f) continue;
            float f = 1 - age / .5f;
            fill.setColor(alpha(0xFFFFFFFF, f * .45f));
            c.drawCircle(ex, ey, (5 + 12 * age / .5f) * dp, fill);
            stroke.setColor(alpha(FL[1], f * .8f)); stroke.setStrokeWidth(2 * dp);
            c.drawCircle(ex, ey, (4 + 26 * age) * dp, stroke);
            stroke.setColor(alpha(FL[1], f * .3f)); stroke.setStrokeWidth(1.5f * dp);
            c.drawCircle(tx, ty, tr * (1 + age * .6f), stroke);
            fill.setColor(alpha(FL[0], f));
            for (int j = 0; j < 5; j++) {
                float ang = i * 1.3f + j * MathUtil.TAU / 5;
                c.drawCircle(ex + cos(ang) * age * 40 * dp, ey + sin(ang) * age * 40 * dp, 1.4f * dp, fill);
            }
        }
    }

    private void drawBlackHole(Canvas c, float t) {
        cam(c, .8f);
        c.translate(bx, by);
        for (int i = 0; i < ARCS; i++) {                   // ánh sao bị bẻ cong thành những cung quanh hố đen
            float r = arcR[i] * rh;
            oval.set(-r, -r, r, r);
            stroke.setColor(alpha(0xFFDDE6FF, arcAl[i]));
            stroke.setStrokeWidth(arcW[i] * dp);
            c.drawArc(oval, (arcA[i] - t * 8 / arcR[i]) % 360, arcSw[i], false, stroke);
        }
        fill.setShader(haloS);
        fill.setAlpha((int) (255 * (.85f + .15f * sin(t * .8f))));
        c.drawCircle(0, 0, rh * 5, fill);

        mtx.setRotate(-(t * 20) % 360);
        diskS.setLocalMatrix(mtx);
        c.save(); c.rotate(TILT); c.scale(1, SQ);
        diskRing(c, 180);                                    // nửa sau của đĩa, bị chân trời che
        diskDots(c, t, false);
        c.restore();

        fill.setShader(shadowS); fill.setAlpha(255);
        c.drawCircle(0, 0, rh * 1.7f, fill);
        fill.setShader(null);
        fill.setColor(0xFF000000);
        c.drawCircle(0, 0, rh, fill);

        c.save(); c.rotate(TILT);                            // mặt sau đĩa bị thấu kính hấp dẫn uốn vòng qua đỉnh
        stroke.setShader(diskS);
        stroke.setColor(0xFFFFFFFF);
        stroke.setAlpha(190); stroke.setStrokeWidth(rh * .2f);
        oval.set(-rh * 1.45f, -rh * 1.32f, rh * 1.45f, rh * 1.32f);
        c.drawArc(oval, 195, 150, false, stroke);
        stroke.setAlpha(90); stroke.setStrokeWidth(rh * .1f);
        oval.set(-rh * 1.3f, -rh * 1.2f, rh * 1.3f, rh * 1.2f);
        c.drawArc(oval, 25, 130, false, stroke);
        stroke.setShader(null);
        c.restore();
        stroke.setColor(alpha(0xFFFFE7B8, .9f)); stroke.setStrokeWidth(1.6f * dp);
        c.drawCircle(0, 0, rh * 1.04f, stroke);

        c.save(); c.rotate(TILT); c.scale(1, SQ);
        diskRing(c, 0);                                      // nửa trước của đĩa, đè lên chân trời
        diskDots(c, t, true);
        c.restore();

        stream(c, t, FRAG, DUST / 2, 1.2f, .33f * U, -2.2f, .08f, 1.6f, .45f);   // bụi thiên thạch lượn vào
        stream(c, t, FRAG + DUST / 2, DUST / 2, 1.9f, .36f * U, -1.5f, .07f, 1.4f, .35f);
        drawTornPlanet(c, t);
        c.restore();
    }

    private void diskRing(Canvas c, float start) {
        int n = 8;
        float r0 = 1.32f * rh, r1 = 3f * rh, w = (r1 - r0) / n;
        disk.setShader(diskS);
        for (int j = 0; j < n; j++) {
            float r = r0 + (j + .5f) * w, a = j == 0 ? 1 : .9f * (float) Math.pow(1 - j / (float) n, 1.4f) + .06f;
            disk.setStrokeWidth(w * 1.25f);
            disk.setAlpha((int) (255 * a));
            oval.set(-r, -r, r, r);
            c.drawArc(oval, start, 180, false, disk);
        }
        disk.setShader(null);
        disk.setColor(alpha(0xFFFFF6E0, .9f));
        disk.setStrokeWidth(w * .5f);
        oval.set(-r0, -r0, r0, r0);
        c.drawArc(oval, start, 180, false, disk);
        disk.setColor(0xFFFFFFFF);
    }

    private void diskDots(Canvas c, float t, boolean front) {
        fill.setColor(alpha(0xFFFFE0A8, .7f));
        for (int i = 0; i < DISK_P; i++) {
            float a = diskA[i] - t * 1.6f / (float) Math.pow(diskR[i], 1.5f);
            float s = sin(a);
            if ((s >= 0) != front) continue;
            float r = diskR[i] * rh;
            c.drawCircle(cos(a) * r, s * r, diskSz[i] * dp, fill);
        }
    }

    // Điểm trên đường xoắn từ (th0, rs) vào mép đĩa, trong hệ toạ độ đã dời tới tâm hố đen
    private float spX, spY;
    private float spPow = 1.5f;
    private void spiral(float u, float th0, float rs, float sweep) {
        float e = (float) Math.pow(Math.max(0, u), spPow);
        float th = th0 + sweep * e, r = rs + (2.85f * rh - rs) * e, q = .82f + (SQ - .82f) * e;
        float x = cos(th) * r, y = sin(th) * r * q, ct = cos((float) Math.toRadians(TILT)), st = sin((float) Math.toRadians(TILT));
        spX = ct * x - st * y;
        spY = st * x + ct * y;
    }

    private void stream(Canvas c, float t, int j0, int n, float th0, float rs, float sweep, float speed, float size, float al) {
        for (int i = 0; i < n; i++) {
            float u = frac(t * speed * (1 + jit[j0 + i] * .3f) + i / (float) n);
            float fade = Math.min(1, u * 12) * Math.min(1, (1 - u) * 8) * al;
            spiral(Math.max(0, u - .02f), th0, rs, sweep);
            float px = spX, py = spY;
            spiral(u, th0, rs, sweep);
            float off = jit[j0 + i] * 16 * dp * (1 - u), nx = -(spY - py), ny = spX - px, nl = (float) Math.hypot(nx, ny);
            if (nl > 0) { nx = nx / nl * off; ny = ny / nl * off; }
            stroke.setColor(alpha(ColorUtil.mix(FC[SAND], 0xFFFFA04A, u), fade));
            stroke.setStrokeWidth(size * (1 - .6f * u) * dp);
            c.drawLine(px + nx, py + ny, spX + nx, spY + ny, stroke);
        }
    }

    // Hình hành tinh bị xé trong hệ toạ độ riêng: +x hướng về hố đen. Nửa sau còn nguyên (cung tròn),
    // nửa hướng về hố đen bị khoét lởm chởm; vài vết nứt toả vào trong từ mép khoét.
    private void buildTorn() {
        float R = tornR = .046f * U;
        float bd = (float) Math.toDegrees(BITE);
        tornBody.reset(); tornEdge.reset(); tornCracks.reset(); tornGhost.reset();
        oval.set(-R, -R, R, R);
        tornBody.arcTo(oval, bd, 360 - 2 * bd, true);
        for (int k = 0; k <= BITE_N; k++) {
            float a = -BITE + 2 * BITE * k / BITE_N, rr = R * biteF[k];
            tornBody.lineTo(cos(a) * rr, sin(a) * rr);
            if (k == 0) tornEdge.moveTo(cos(a) * rr, sin(a) * rr); else tornEdge.lineTo(cos(a) * rr, sin(a) * rr);
        }
        tornBody.close();
        tornGhost.addPath(tornEdge);                         // phần thân cũ đã bị rút ra: giữa mép khoét và đường tròn ban đầu
        tornGhost.arcTo(oval, bd, -2 * bd, false);
        tornGhost.close();
        for (int i = 0; i < CRACKS; i++) {
            int k = crackK[i];
            float a = -BITE + 2 * BITE * k / BITE_N, rr = R * biteF[k], x = cos(a) * rr, y = sin(a) * rr, L = crackL[i] * R;
            tornCracks.moveTo(x, y);
            tornCracks.lineTo(x - L * .5f, y + (crackB[i] + y / R * .3f) * L * .6f);
            tornCracks.lineTo(x - L, y + (crackB[i] * .2f + y / R * .5f) * L);
        }
        sandS = new RadialGradient(R * .35f, -R * .3f, R * 1.6f,
                new int[]{FL[SAND], ColorUtil.mix(FC[SAND], 0xFF8A5A40, .4f), ColorUtil.mix(FC[SAND], 0xFF000000, .72f)},
                new float[]{0, .45f, 1}, Shader.TileMode.CLAMP);
        heatS = new RadialGradient(0, 0, R * 1.6f, new int[]{alpha(0xFFFF9A40, .55f), alpha(0xFFFF6A20, .18f), 0},
                new float[]{0, .45f, 1}, Shader.TileMode.CLAMP);
    }

    /**
     * Hành tinh bị hố đen xé, đủ 3 phần: phần chưa bị hút (thân còn nguyên, viền sắc), phần đang bị hút
     * (mép khoét nóng chảy, vết nứt sáng, mảng đá lớn đang bong ra) và phần đã bị hút (đá nhỏ dần, nóng dần,
     * cuối cùng thành vệt sáng chảy vào đĩa bồi tụ).
     */
    private void drawTornPlanet(Canvas c, float t) {
        float R = tornR, rs = TORN_RS * U;
        spPow = 2.2f;                                        // đá lưu lại lâu gần hành tinh rồi mới tăng tốc vào đĩa
        spiral(0, TORN_TH0, rs, TORN_SW);
        float sx0 = spX, sy0 = spY, dl = (float) Math.hypot(sx0, sy0), hx = -sx0 / dl, hy = -sy0 / dl;
        float px = sx0 - hx * R * .62f, py = sy0 - hy * R * .62f, ang = (float) Math.toDegrees(Math.atan2(hy, hx));

        float[] seg = {0, .3f, .65f, 1};                   // vệt bụi mờ dọc đường bị hút, thuôn dần
        for (int sgi = 0; sgi < 3; sgi++) {
            path.reset();
            for (int k = 0; k <= 12; k++) {
                spiral(seg[sgi] + (seg[sgi + 1] - seg[sgi]) * k / 12f, TORN_TH0, rs, TORN_SW);
                if (k == 0) path.moveTo(spX, spY); else path.lineTo(spX, spY);
            }
            stroke.setColor(alpha(0xFFFF9050, sgi == 0 ? .14f : .1f));
            stroke.setStrokeWidth(R * (sgi == 0 ? 1.3f : sgi == 1 ? .6f : .25f));
            c.drawPath(path, stroke);
        }
        c.save();
        c.translate(px + hx * R * .55f, py + hy * R * .55f);
        fill.setShader(heatS);
        fill.setAlpha((int) (255 * (.8f + .2f * sin(t * 2.3f))));
        c.drawCircle(0, 0, R * 1.6f, fill);
        fill.setShader(null);
        c.restore();

        drawFragments(c, t, R, rs);

        c.save();
        c.translate(px, py);
        c.rotate(ang);
        c.scale(1.1f + .03f * sin(t * 1.3f), .93f);         // phình nhẹ về phía hố đen do lực thuỷ triều
        float gl = .5f + .5f * sin(t * 1.9f);
        fill.setColor(alpha(0xFFFF6A2A, .16f + .08f * gl));
        c.drawPath(tornGhost, fill);
        stroke.setColor(alpha(0xFFFFB070, .35f)); stroke.setStrokeWidth(.8f * dp);
        dashGhost(c, t);
        fill.setShader(sandS); fill.setAlpha(255);
        c.drawPath(tornBody, fill);
        fill.setShader(null);
        c.save();
        c.clipPath(tornBody);
        stroke.setColor(0x2A000000);
        stroke.setStrokeWidth(R * .14f);
        for (int b = 0; b < 3; b++) {
            float k = -.45f + b * .48f;
            path.reset();
            path.moveTo(-R * 1.1f, R * k);
            path.quadTo(0, R * (k + .15f * sin(b * 2.1f + t * .3f)), R * 1.1f, R * k);
            c.drawPath(path, stroke);
        }
        c.translate(R * .7f, 0);                             // bề mặt sát vết khoét bị nung đỏ
        c.scale(.6f, .6f);
        fill.setShader(heatS); fill.setAlpha(210);
        c.drawCircle(0, 0, R * 1.6f, fill);
        fill.setShader(null);
        c.restore();
        float bd = (float) Math.toDegrees(BITE);
        oval.set(-R, -R, R, R);
        stroke.setColor(0x70FFFFFF); stroke.setStrokeWidth(1.2f * dp);
        c.drawArc(oval, bd + 6, 360 - 2 * bd - 12, false, stroke);
        float fl = .55f + .45f * sin(t * 3.1f);
        stroke.setColor(alpha(0xFFFF8A3A, .35f * fl)); stroke.setStrokeWidth(3 * dp);
        c.drawPath(tornCracks, stroke);
        stroke.setColor(alpha(0xFFFFD08A, .85f * fl)); stroke.setStrokeWidth(dp);
        c.drawPath(tornCracks, stroke);
        stroke.setColor(alpha(0xFFFF7A2A, .5f)); stroke.setStrokeWidth(4 * dp);
        c.drawPath(tornEdge, stroke);
        stroke.setColor(alpha(0xFFFFE2A0, .95f)); stroke.setStrokeWidth(1.4f * dp);
        c.drawPath(tornEdge, stroke);
        c.restore();
        spPow = 1.5f;
    }

    // Đường viền cũ của hành tinh phía bị khoét: chỉ còn những đoạn đứt quãng mờ dần, đang tan ra
    private void dashGhost(Canvas c, float t) {
        float R = tornR, bd = (float) Math.toDegrees(BITE);
        oval.set(-R, -R, R, R);
        for (int k = 0; k < 7; k++) {
            float a0 = -bd + k * 2 * bd / 7f, f = .5f + .5f * sin(t * 1.4f + k * 1.9f);
            stroke.setAlpha((int) (50 + 70 * f));
            c.drawArc(oval, a0, 2 * bd / 7f * (.3f + .4f * f), false, stroke);
        }
    }

    private void drawFragments(Canvas c, float t, float R, float rs) {
        for (int i = 0; i < FRAG; i++) {
            float jt = jit[i], jt2 = jit[(i * 7 + 3) % FRAG], u = frac(t * .05f * (1 + jt * .3f) + i / (float) FRAG);
            float fade = Math.min(1, u * 40) * Math.min(1, (1 - u) * 8);
            spiral(Math.min(1, u + .01f), TORN_TH0, rs, TORN_SW);
            float nx2 = spX, ny2 = spY;
            spiral(u, TORN_TH0, rs, TORN_SW);
            float nx = -(ny2 - spY), ny = nx2 - spX, nl = (float) Math.hypot(nx, ny), off = (jt * 1.6f + jt2 * .5f) * R * (float) Math.pow(1 - u, 1.4f);
            if (nl > 0) { nx = nx / nl * off; ny = ny / nl * off; } else { nx = 0; ny = 0; }
            float x = spX + nx, y = spY + ny;
            if (u < .62f) {
                boolean near = u < .25f;                     // mảng lớn vừa bong khỏi vết khoét
                float size = (near ? R * (.26f - .14f * u / .25f) : R * .12f + (1.5f * dp - R * .12f) * (u - .25f) / .37f) * (1 + jt2 * .5f);
                float heat = near ? u / .25f * .3f : .3f + .7f * (u - .25f) / .37f;
                if (heat > .3f) { fill.setColor(alpha(0xFFFF8A3A, .35f * heat * fade)); c.drawCircle(x, y, size * 2, fill); }
                Path rock = rockShape[i % rockShape.length];
                c.save();
                c.translate(x, y);
                c.rotate(jt * 720 + t * (40 + jt * 80) * (1 + u * 3));
                c.scale(size, size);
                fill.setColor(alpha(ColorUtil.mix(0xFF9C8E7E, 0xFFFF9A48, heat), fade));
                c.drawPath(rock, fill);
                stroke.setColor(alpha(0xFFFFB070, (.25f + .6f * heat) * fade));
                stroke.setStrokeWidth(dp / size);
                c.drawPath(rock, stroke);
                c.restore();
            } else {                                         // đã bị hút: vệt nóng sáng chảy vào đĩa
                float k = (u - .62f) / .38f;
                spiral(u - .025f, TORN_TH0, rs, TORN_SW);
                float qx = spX + nx, qy = spY + ny;
                stroke.setColor(alpha(0xFFFF8A3A, .25f * fade)); stroke.setStrokeWidth(3.5f * dp);
                c.drawLine(qx, qy, x, y, stroke);
                stroke.setColor(alpha(ColorUtil.mix(0xFFFF9A48, 0xFFFFF4D0, k), fade)); stroke.setStrokeWidth((2.2f - 1.2f * k) * dp);
                c.drawLine(qx, qy, x, y, stroke);
            }
        }
    }

    private void drawBelt(Canvas c, float t) {
        cam(c, .85f);
        float half = .05f * H, nx = -bdy, ny = bdx;
        for (int i = 0; i < BELT; i++) {
            float s = frac(beltS[i] + t * beltDepth[i] * 9 * dp / bLen) * bLen;
            float x = b0x + bdx * s + nx * beltN[i] * half, y = b0y + bdy * s + ny * beltN[i] * half;
            float sz = beltSize[i] * dp, a = .45f + .5f * (beltDepth[i] - .55f) / .6f;
            c.save();
            c.translate(x, y);
            c.rotate(beltRot[i] + t * beltSpin[i]);
            c.scale(sz, sz);
            fill.setColor(alpha(ColorUtil.mix(0xFF7B7168, 0xFF8E7B66, beltCol[i]), a));
            Path rock = rockShape[i % rockShape.length];
            c.drawPath(rock, fill);
            c.translate(.22f, .22f);
            c.scale(.65f, .65f);
            fill.setColor(alpha(0xFF000000, .25f * a));
            c.drawPath(rock, fill);
            c.restore();
        }
        c.restore();
    }

    // Thiên thạch tiền cảnh: to, mờ nhoè (ngoài vùng nét) và trôi nhanh hơn để tạo parallax
    private void drawForeground(Canvas c, float t) {
        cam(c, 1.8f);
        float slope = bdy / bdx, m = 40 * dp;
        fill.setShader(blurS);
        for (int i = 0; i < FG; i++) {
            float x = frac(fgX[i] + t * fgV[i] * dp / (W + 2 * m)) * (W + 2 * m) - m;
            float y = FG_Y[i] * H + slope * x, k = fgSize[i] * dp / 100;
            c.save();
            c.translate(x, y);
            c.scale(k, k);
            fill.setAlpha(120 + i * 15);
            c.drawCircle(0, 0, 100, fill);
            c.restore();
        }
        fill.setShader(null);
        c.restore();
    }
}
