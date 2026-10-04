package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.Random;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.ColorUtil.mix;
import static com.planetconquest.game.engine.util.MathUtil.*;

/**
 * Vẽ hành tinh của bản đồ chọn màn. lit = 0: tối, thô sơ, không quầng sáng; lit = 1: sáng đủ màu.
 * Mọi con số lấy từ planet-ui/levels.js (SKIN.*). Vẽ tách khỏi dữ liệu: màu và kiểu nằm ở {@link LevelPlanet}.
 */
public final class LevelPlanetRenderer extends Painter {
    private static final int DARK_BASE = 0xFF0B0E1C;
    private static final float DARK_AMOUNT = .78f;
    private static final int BELT_ROCKS = 16;
    private static final float[] BELT_ANGLE = new float[BELT_ROCKS], BELT_DIST = new float[BELT_ROCKS], BELT_SIZE = new float[BELT_ROCKS];

    static {
        Random r = new Random(5);
        for (int k = 0; k < BELT_ROCKS; k++) { BELT_ANGLE[k] = r.nextFloat() * TAU; BELT_DIST[k] = 1.5f + r.nextFloat() * .5f; BELT_SIZE[k] = 1 + r.nextFloat() * 1.4f; }
    }

    public LevelPlanetRenderer(DrawKit kit) { super(kit); }

    /** Màu hiển thị theo độ sáng. */
    private static int shade(int col, float lit) { return mix(col, DARK_BASE, DARK_AMOUNT * (1 - lit)); }

    public void draw(Canvas c, LevelPlanet p, float x, float y, float R, float lit, float t) {
        stroke.setPathEffect(null);
        stroke.setShader(null);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        int col = p.color;
        switch (p) {
            case SPROUT: sprout(c, x, y, R, col, lit); break;
            case OCEAN: ocean(c, x, y, R, col, lit); break;
            case CARGO: cargo(c, x, y, R, col, lit); break;
            case DESERT: desert(c, x, y, R, col, lit); break;
            case SPEED: speed(c, x, y, R, col, lit, t); break;
            case RANGE: range(c, x, y, R, col, lit, t); break;
            case FOG: fog(c, x, y, R, col, lit, t); break;
            case BELT: belt(c, x, y, R, col, lit, t); break;
            case RELOAD: reload(c, x, y, R, col, lit, t); break;
            case SURGE: surge(c, x, y, R, col, lit, t); break;
            case VOYAGE: voyage(c, x, y, R, col, lit, t); break;
            default: blackHole(c, x, y, R, lit, t); break;
        }
    }

    // ---------- Thành phần dùng chung ----------
    private void body(Canvas c, float x, float y, float R, int col, float lit) {
        int s = shade(col, lit);
        // Paint dùng chung: alpha còn sót của lần fill.setColor trước nhân vào shader (xem WorldRenderer.drawPlanetBody)
        fill.setColor(0xFFFFFFFF);
        if (lit > .02f) {
            fill.setShader(new RadialGradient(x, y, R * 2.2f, new int[]{alpha(col, .34f * lit), alpha(col, .34f * lit), alpha(col, 0)}, new float[]{0, .43f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, R * 2.2f, fill);
        }
        fill.setShader(new RadialGradient(x - R * .35f, y - R * .4f, R * 1.45f,
                new int[]{mix(s, 0xFFFFFFFF, .12f + .38f * lit), s, mix(s, 0xFF000000, .62f)}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R, fill);
        fill.setShader(null);
    }

    private void rim(Canvas c, float x, float y, float R, float lit) {
        stroke.setColor(alpha(0xFFFFFFFF, .08f + .2f * lit)); stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(x, y, R, stroke);
    }

    private void clipBody(Canvas c, float x, float y, float R) {
        c.save();
        path.reset(); path.addCircle(x, y, R, Path.Direction.CW);
        c.clipPath(path);
    }

    /** Nửa vành elip: back = nửa sau hành tinh, ngược lại nửa trước. */
    private void ring(Canvas c, float x, float y, float rx, float ry, float rot, boolean back, int color, float w) {
        c.save(); c.translate(x, y); c.rotate((float) Math.toDegrees(rot));
        stroke.setColor(color); stroke.setStrokeWidth(w);
        tmp.set(-rx, -ry, rx, ry);
        c.drawArc(tmp, back ? 180 : 0, 180, false, stroke);
        c.restore();
    }

    private void blob(Canvas c, float x, float y, float rx, float ry, float rot, int color) {
        c.save(); c.translate(x, y); c.rotate((float) Math.toDegrees(rot));
        fill.setColor(color); tmp.set(-rx, -ry, rx, ry);
        c.drawOval(tmp, fill);
        c.restore();
    }

    /** Đường cong bậc hai ngang qua thân (dải mây, sóng). */
    private void wave(Canvas c, float x, float y, float R, float k, float bend) {
        path.reset(); path.moveTo(x - R, y + R * k); path.quadTo(x, y + R * k + bend * R, x + R, y + R * k);
        c.drawPath(path, stroke);
    }

    // ---------- Từng hành tinh ----------
    private void sprout(Canvas c, float x, float y, float R, int col, float lit) {
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        int d = alpha(mix(shade(col, lit), 0xFF000000, .35f), .55f);
        blob(c, x - R * .3f, y + R * .1f, R * .5f, R * .3f, .5f, d);
        blob(c, x + R * .35f, y - R * .25f, R * .32f, R * .2f, -.4f, d);
        c.restore(); rim(c, x, y, R, lit);
    }

    private void ocean(Canvas c, float x, float y, float R, int col, float lit) {
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        int land = alpha(shade(0xFF7FE0B0, lit), .85f);
        blob(c, x - R * .4f, y - R * .1f, R * .5f, R * .3f, .4f, land);
        blob(c, x + R * .45f, y + R * .35f, R * .4f, R * .22f, -.3f, land);
        blob(c, x + R * .1f, y - R * .55f, R * .3f, R * .16f, 0, land);
        stroke.setColor(alpha(0xFFFFFFFF, .1f + .3f * lit)); stroke.setStrokeWidth(R * .1f);
        wave(c, x, y, R, -.45f, -.18f); wave(c, x, y, R, .2f, -.18f); wave(c, x, y, R, .6f, -.18f);
        c.restore(); rim(c, x, y, R, lit);
    }

    private void cargo(Canvas c, float x, float y, float R, int col, float lit) {
        int rc = alpha(shade(0xFFFFE0A0, lit), .35f + .5f * lit);
        ring(c, x, y, R * 1.7f, R * .46f, -.32f, true, rc, 3 * dp);
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        fill.setColor(alpha(mix(shade(col, lit), 0xFF000000, .45f), .5f));
        for (float k : new float[]{-.55f, -.1f, .35f}) c.drawRect(x - R, y + R * k, x + R, y + R * k + R * .2f, fill);
        c.restore(); rim(c, x, y, R, lit);
        ring(c, x, y, R * 1.7f, R * .46f, -.32f, false, rc, 3 * dp);
    }

    private void desert(Canvas c, float x, float y, float R, int col, float lit) {
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        stroke.setColor(alpha(mix(shade(col, lit), 0xFF000000, .4f), .6f)); stroke.setStrokeWidth(R * .07f);
        for (float k : new float[]{-.5f, -.05f, .4f}) {
            path.reset(); path.moveTo(x - R, y + R * k);
            path.cubicTo(x - R * .3f, y + R * (k - .25f), x + R * .3f, y + R * (k + .25f), x + R, y + R * k);
            c.drawPath(path, stroke);
        }
        stroke.setStrokeWidth(1.4f * dp);
        path.reset(); path.moveTo(x - R * .1f, y - R * .9f); path.lineTo(x + R * .05f, y - R * .4f); path.lineTo(x - R * .12f, y - R * .05f);
        path.lineTo(x + R * .1f, y + R * .35f); path.lineTo(x, y + R * .9f);
        c.drawPath(path, stroke);
        c.restore(); rim(c, x, y, R, lit);
    }

    private void speed(Canvas c, float x, float y, float R, int col, float lit, float t) {
        for (int k = 0; k < 3; k++) {   // vệt tốc độ phía sau
            float yy = y + (k - 1) * R * .5f, ph = (t * 1.6f + k * .33f) % 1, len = R * (1.1f + .5f * ph), x0 = x - R * 1.1f - ph * R * .5f;
            stroke.setColor(alpha(shade(col, lit), (.15f + .45f * lit) * (1 - ph))); stroke.setStrokeWidth(2.2f * dp);
            c.drawLine(x0, yy, x0 - len, yy, stroke);
        }
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        stroke.setColor(alpha(0xFFFFFFFF, .1f + .3f * lit)); stroke.setStrokeWidth(R * .16f);
        for (float k : new float[]{-.7f, -.1f, .5f}) c.drawLine(x - R, y + R * (k + .5f), x + R, y + R * (k - .5f), stroke);
        c.restore(); rim(c, x, y, R, lit);
    }

    private void range(Canvas c, float x, float y, float R, int col, float lit, float t) {
        float r = R * .8f;   // thân nhỏ lại, vòng nét đứt là tầm bay ngắn
        body(c, x, y, r, col, lit); clipBody(c, x, y, r);
        stroke.setColor(alpha(0xFFFFFFFF, .08f + .22f * lit)); stroke.setStrokeWidth(R * .12f);
        path.reset(); path.moveTo(x - R, y - R * .1f); path.quadTo(x, y + R * .2f, x + R, y - R * .1f);
        c.drawPath(path, stroke);
        c.restore(); rim(c, x, y, r, lit);
        stroke.setPathEffect(new DashPathEffect(new float[]{5 * dp, 6 * dp}, t * 14 * dp));
        stroke.setStrokeWidth(2 * dp); stroke.setColor(alpha(shade(col, lit), .35f + .5f * lit));
        c.drawCircle(x, y, R * 1.45f, stroke);
        stroke.setPathEffect(null);
    }

    /** Viễn chinh: hành tinh nhỏ, đoàn đá đi chậm trên đường chấm dài tới một hành tinh xa. */
    private void voyage(Canvas c, float x, float y, float R, int col, float lit, float t) {
        float far = R * 2.3f, ang = -.62f, ca = cos(ang), sa = sin(ang), fx = x + ca * far, fy = y + sa * far, ph = (t * .16f) % 1;
        int sc = shade(col, lit);
        stroke.setPathEffect(new DashPathEffect(new float[]{2 * dp, 6 * dp}, 0));
        stroke.setStrokeWidth(1.6f * dp); stroke.setColor(alpha(sc, .3f + .4f * lit));
        c.drawLine(x + ca * R * 1.15f, y + sa * R * 1.15f, fx - ca * R * .3f, fy - sa * R * .3f, stroke);
        stroke.setPathEffect(null);
        fill.setColor(alpha(shade(0xFFFFFFFF, lit), .3f + .6f * lit));
        c.drawCircle(x + (fx - x) * (.18f + .64f * ph), y + (fy - y) * (.18f + .64f * ph), 2 * dp, fill);   // đoàn đá chậm
        fill.setColor(alpha(sc, .45f + .5f * lit));
        c.drawCircle(fx, fy, R * .26f, fill);                                                           // hành tinh đích ở xa
        float r = R * .9f;
        body(c, x, y, r, col, lit); clipBody(c, x, y, r);
        stroke.setColor(alpha(0xFFFFFFFF, .1f + .25f * lit)); stroke.setStrokeWidth(R * .1f);
        path.reset(); path.moveTo(x - R, y + R * .2f); path.quadTo(x, y - R * .1f, x + R, y + R * .25f);
        c.drawPath(path, stroke);
        c.restore(); rim(c, x, y, r, lit);
    }

    private void fog(Canvas c, float x, float y, float R, int col, float lit, float t) {
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        for (int k = 0; k < 4; k++) {
            float dx = sin(t * .5f + k * 1.7f) * R * .35f;
            blob(c, x + dx, y + (k - 1.5f) * R * .5f, R * (.9f - k * .05f), R * .17f, 0, alpha(0xFFFFFFFF, (.12f + .22f * lit) * (k % 2 == 1 ? 1 : .7f)));
        }
        c.restore(); rim(c, x, y, R, lit);
        fill.setColor(0xFFFFFFFF);
        fill.setShader(new RadialGradient(x, y, R * 1.6f, new int[]{alpha(0xFFD2E1FF, .18f * (.3f + lit)), alpha(0xFFD2E1FF, .18f * (.3f + lit)), alpha(0xFFD2E1FF, 0)},
                new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R * 1.6f, fill);
        fill.setShader(null);
    }

    private void beltRocks(Canvas c, float x, float y, float R, float t, boolean back, int rock) {
        float cs = cos(-.3f), sn = sin(-.3f);
        fill.setColor(rock);
        for (int k = 0; k < BELT_ROCKS; k++) {
            float a = BELT_ANGLE[k] + t * .35f, px = cos(a) * R * BELT_DIST[k], py = sin(a) * R * BELT_DIST[k] * .32f;
            if ((py < 0) == back) c.drawCircle(x + px * cs - py * sn, y + px * sn + py * cs, BELT_SIZE[k] * dp, fill);
        }
    }

    private void belt(Canvas c, float x, float y, float R, int col, float lit, float t) {
        int rock = alpha(shade(0xFFB89A80, lit), .5f + .45f * lit);
        beltRocks(c, x, y, R, t, true, rock);
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        float[][] craters = {{-.3f, -.25f, .22f}, {.35f, .1f, .16f}, {-.05f, .5f, .13f}};
        for (float[] k : craters) {
            blob(c, x + R * k[0], y + R * k[1], R * k[2], R * k[2], 0, alpha(mix(shade(col, lit), 0xFF000000, .5f), .7f));
            stroke.setColor(alpha(0xFFFFFFFF, .08f + .15f * lit)); stroke.setStrokeWidth(dp);
            c.drawCircle(x + R * k[0], y + R * k[1], R * k[2], stroke);
        }
        c.restore(); rim(c, x, y, R, lit);
        beltRocks(c, x, y, R, t, false, rock);
    }

    private void reload(Canvas c, float x, float y, float R, int col, float lit, float t) {
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        stroke.setColor(alpha(0xFFFFFFFF, .1f + .25f * lit)); stroke.setStrokeWidth(R * .1f);
        wave(c, x, y, R, .15f, .3f);
        c.restore(); rim(c, x, y, R, lit);
        final int n = 8;
        final float gap = .22f, ph = (t * .5f) % 1;
        stroke.setStrokeWidth(3 * dp);
        tmp.set(x - R * 1.38f, y - R * 1.38f, x + R * 1.38f, y + R * 1.38f);
        for (int k = 0; k < n; k++) {
            float a0 = -(float) Math.PI / 2 + k * TAU / n + gap / 2, on = ((float) k / n + ph) % 1 < .5f ? 1 : .35f;
            stroke.setColor(alpha(shade(col, lit), (.3f + .6f * lit) * on));
            c.drawArc(tmp, (float) Math.toDegrees(a0), (float) Math.toDegrees(TAU / n - gap), false, stroke);
        }
    }

    private void surge(Canvas c, float x, float y, float R, int col, float lit, float t) {
        for (int k = 0; k < 2; k++) {   // sóng xung toả ra
            float ph = (t * .7f + k * .5f) % 1;
            stroke.setColor(alpha(shade(col, lit), (1 - ph) * (.1f + .5f * lit))); stroke.setStrokeWidth(2 * dp);
            c.drawCircle(x, y, R * (1.05f + ph * .75f), stroke);
        }
        body(c, x, y, R, col, lit); clipBody(c, x, y, R);
        stroke.setColor(alpha(0xFFFFFFFF, .15f + .6f * lit)); stroke.setStrokeWidth(R * .17f);
        for (float dx : new float[]{-.28f, .18f}) {
            path.reset(); path.moveTo(x + R * (dx - .2f), y - R * .42f); path.lineTo(x + R * (dx + .15f), y); path.lineTo(x + R * (dx - .2f), y + R * .42f);
            c.drawPath(path, stroke);
        }
        c.restore(); rim(c, x, y, R, lit);
    }

    private int diskColor(float u, float lit, float a) {
        return alpha(mix(mix(0xFFFFB347, 0xFFFFFFFF, u * .6f), DARK_BASE, DARK_AMOUNT * (1 - lit)), a);
    }

    private void blackHole(Canvas c, float x, float y, float R, float lit, float t) {
        final float core = R * .5f, tilt = -.35f, rx = R * 1.75f, ry = R * .5f;
        if (lit > .02f) {
            fill.setColor(0xFFFFFFFF);
            fill.setShader(new RadialGradient(x, y, R * 2.4f, new int[]{alpha(0xFFFF963C, .28f * lit), alpha(0xFFFF963C, 0)}, null, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, R * 2.4f, fill);
            fill.setShader(null);
        }
        for (int k = 0; k < 4; k++) ring(c, x, y, rx - k * R * .1f, ry - k * R * .03f, tilt, true, diskColor(k / 3f, lit, (.25f + .5f * lit) * (1 - k * .15f)), (3.4f - k * .6f) * dp);
        fill.setColor(0xFF000000); c.drawCircle(x, y, core, fill);
        stroke.setColor(diskColor(1, lit, .35f + .6f * lit)); stroke.setStrokeWidth(2 * dp); c.drawCircle(x, y, core + 1.5f * dp, stroke);   // vòng photon
        stroke.setColor(diskColor(.5f, lit, .18f + .25f * lit)); stroke.setStrokeWidth(1.2f * dp); c.drawCircle(x, y, core * 1.35f, stroke);  // bẻ cong ánh sáng
        for (int k = 0; k < 4; k++) ring(c, x, y, rx - k * R * .1f, ry - k * R * .03f, tilt, false, diskColor(k / 3f, lit, (.3f + .65f * lit) * (1 - k * .15f)), (3.4f - k * .6f) * dp);
        fill.setColor(diskColor(1, lit, .3f + .6f * lit));
        float cs = cos(tilt), sn = sin(tilt);
        for (int k = 0; k < 10; k++) {   // hạt xoáy quanh đĩa
            float a = t * (.9f + (k % 3) * .25f) + k * .63f, s = .85f + (k % 4) * .05f, px = cos(a) * rx * s, py = sin(a) * ry * s;
            c.drawCircle(x + px * cs - py * sn, y + px * sn + py * cs, 1.4f * dp, fill);
        }
    }
}
