package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Path;
import android.graphics.Paint;

import com.planetconquest.game.R;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.input.Pointer;
import com.planetconquest.game.engine.input.GestureMode;
import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.model.Asteroid;
import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.FloatText;
import com.planetconquest.game.engine.model.Particle;
import com.planetconquest.game.engine.model.Planet;
import com.planetconquest.game.engine.model.Rock;
import com.planetconquest.game.engine.model.Selection;
import com.planetconquest.game.session.GameSession;
import com.planetconquest.game.text.Texts;

import java.util.ArrayList;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.MathUtil.TAU;
import static com.planetconquest.game.engine.util.MathUtil.cos;
import static com.planetconquest.game.engine.util.MathUtil.sin;
import static com.planetconquest.game.ui.Palette.*;

/** Vẽ thế giới game: hành tinh, quỹ đạo, thiên thạch, đá, hạt, vòng khoanh, đường kéo và gợi ý của màn Hướng dẫn. */
public final class WorldRenderer extends Painter {
    private final Engine eng;
    private final GameSession session;
    private final Texts tx;
    private final float[] dotX = new float[64], dotY = new float[64], rings = new float[16];
    private final int[] FC = Faction.COLORS, FL = Faction.LIGHT, FD = Faction.DARK;
    private final ArrayList<Planet> tmpPlanets = new ArrayList<Planet>();

    public WorldRenderer(DrawKit kit, Engine eng, GameSession session, Texts tx) {
        super(kit);
        this.tx = tx;
        this.eng = eng;
        this.session = session;
    }

    private void drawIntroCue(Canvas c) {
        if (!eng.lvl.intro || session.introStep() >= GameSession.INTRO_STEP_COUNT || eng.pointer() != null || eng.planets.size() < 2) return;
        Planet me = eng.planets.get(0), en = eng.planets.get(1);
        float pulse = .5f + .5f * sin(eng.clock * 4);
        stroke.setColor(alpha(C_GOLD, .55f + .35f * pulse));
        stroke.setStrokeWidth(2.5f * dp);
        switch (session.introStep()) {
            case 0: case 4: {
                if (session.introStep() == 4 && eng.selection() != null) break;
                dashed(true, eng.clock);
                float r1 = eng.radiusOf(me) + 12 * dp, r2 = eng.radiusOf(en) + 14 * dp, d = (float) Math.hypot(en.x - me.x, en.y - me.y);
                float ux = (en.x - me.x) / d, uy = (en.y - me.y) / d;
                c.drawLine(me.x + ux * r1, me.y + uy * r1, en.x - ux * r2, en.y - uy * r2, stroke);
                dashed(false, 0);
                break;
            }
            case 1: {
                if (eng.selection() != null) break;
                int nr = eng.orbitRings(me, rings);
                float rad = (nr > 0 ? rings[nr - 1] : eng.radiusOf(me)) + 16 * dp;
                dashed(true, eng.clock);
                c.drawCircle(me.x, me.y, rad, stroke);
                dashed(false, 0);
                break;
            }
            case 2: {
                float px = W() / 2, py = H() * .55f;
                c.drawCircle(px, py, (18 + 6 * pulse) * dp, stroke);
                fill.setColor(alpha(C_GOLD, .25f)); c.drawCircle(px, py, 10 * dp, fill);
                text(c, tx.s(R.string.touch_here), px, py + 38 * dp, 12.5f * dp, C_GOLD, tfBold, Paint.Align.CENTER);
                break;
            }
            case 3:
                c.drawCircle(me.x, me.y, eng.radiusOf(me) + (8 + 6 * pulse) * dp, stroke);
                break;
        }
    }

    // ================= Thế giới game =================
    public void draw(Canvas c, boolean playing) {
        eng.updateLive();
        Level L = eng.lvl;
        if (!Float.isInfinite(eng.rangePx())) { tmpPlanets.clear(); for (Planet p : eng.planets) if (Faction.isPlayer(p.owner)) tmpPlanets.add(p); drawRange(c, tmpPlanets, .16f); }
        for (Planet p : eng.planets) drawPlanet(c, p);
        for (Planet p : eng.planets) drawOrbit(c, p);
        if (playing && !L.intro && eng.time < 10 && eng.pointer() == null && eng.selection() == null && !eng.planets.isEmpty()) {
            Planet p = eng.planets.get(0);
            stroke.setColor(alpha(C_YOU, .8f)); stroke.setStrokeWidth(2 * dp);
            dashed(true, eng.clock * .3f);
            c.drawCircle(p.x, p.y, eng.radiusOf(p) + (14 + 3 * sin(eng.clock * 4)) * dp, stroke);
            dashed(false, 0);
        }
        if (playing) drawIntroCue(c);
        drawNeutrals(c);
        drawRocks(c);
        for (Particle q : eng.effects.parts) {
            fill.setColor(alpha(q.color, Math.max(0, q.life / q.max)));
            c.drawCircle(q.x, q.y, q.size, fill);
        }
        if (playing) { drawDrag(c); drawSel(c); }
        for (FloatText t : eng.effects.texts) {
            if (t.text == null) t.text = tx.notice(t.notice);
            float a = Math.min(1, t.life);
            text(c, t.text, t.x + dp, t.y + dp, 14 * dp, alpha(0xFF000000, .6f * a), tfBold, Paint.Align.CENTER);
            text(c, t.text, t.x, t.y, 14 * dp, alpha(t.color, a), tfBold, Paint.Align.CENTER);
        }
    }

    private void drawRange(Canvas c, ArrayList<Planet> srcs, float a) {
        float rp = eng.rangePx();
        if (Float.isInfinite(rp)) return;
        stroke.setPathEffect(new DashPathEffect(new float[]{3 * dp, 8 * dp}, 0));
        stroke.setStrokeWidth(1.5f * dp);
        stroke.setColor(alpha(C_MUTED, a));
        for (Planet s : srcs) c.drawCircle(s.x, s.y, rp, stroke);
        stroke.setPathEffect(null);
    }

    private void drawPlanetBody(Canvas c, float x, float y, float R, int owner, float seed) {
        int col = FC[owner];
        fill.setShader(new RadialGradient(x, y, R * 2.1f, new int[]{alpha(col, .34f), alpha(col, .34f), alpha(col, 0)}, new float[]{0, .43f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R * 2.1f, fill);
        fill.setShader(new RadialGradient(x - R * .35f, y - R * .4f, R * 1.45f, new int[]{FL[owner], col, FD[owner]}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R, fill);
        fill.setShader(null);
        c.save();
        path.reset();
        path.addCircle(x, y, R, Path.Direction.CW);
        c.clipPath(path);
        stroke.setColor(0x24000000);
        stroke.setStrokeWidth(R * .13f);
        float[] bands = {-.4f, .05f, .5f};
        for (float k : bands) {
            path.reset();
            path.moveTo(x - R, y + R * k);
            path.quadTo(x, y + R * k + R * .2f * sin(seed + k * 5), x + R, y + R * k);
            c.drawPath(path, stroke);
        }
        c.restore();
        stroke.setColor(0x47FFFFFF); stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(x, y, R, stroke);
    }

    private void drawPlanet(Canvas c, Planet p) {
        float R = eng.radiusOf(p);
        boolean fg = eng.fogged(p);
        drawPlanetBody(c, p.x, p.y, R, p.owner, p.seed);
        if (p.level < eng.maxLvl(p) && !fg) {
            stroke.setStrokeWidth(3 * dp);
            stroke.setColor(alpha(C_GOLD, .16f)); c.drawCircle(p.x, p.y, R + 4 * dp, stroke);
            if (p.upgradeProgress > 0) { stroke.setColor(C_GOLD); arc(c, p.x, p.y, R + 4 * dp, p.upgradeProgress / (float) Engine.upgradeCost(p.level)); }
        }
        if (eng.rules().cooldownSeconds() > 0 && p.cooldown > 0) {
            stroke.setStrokeWidth(2.5f * dp); stroke.setColor(0xD98FC8FF);
            arc(c, p.x, p.y, R + 8 * dp, p.cooldown / eng.rules().cooldownSeconds());
        }
        if (p.flash > 0) {
            stroke.setStrokeWidth(3 * dp); stroke.setColor(alpha(0xFFFFFFFF, p.flash));
            c.drawCircle(p.x, p.y, R * (1 + (1 - p.flash) * 1.3f), stroke);
        }
        String n = fg ? "?" : String.valueOf(p.rocks);
        float big = Math.max(13 * dp, R * .6f), small = Math.max(8 * dp, R * .27f);
        text(c, n, p.x + dp, p.y - R * .24f + dp, big, 0x73000000, tfBold, Paint.Align.CENTER);
        text(c, n, p.x, p.y - R * .24f, big, 0xFFFFFFFF, tfBold, Paint.Align.CENTER);
        text(c, fg ? tx.levelPrefix + "?" : tx.levelPrefix + p.level + "/" + eng.maxLvl(p), p.x, p.y + R * .28f, small, 0xD9FFFFFF, tfBold, Paint.Align.CENTER);
        text(c, tx.hpPrefix + (fg ? "?" : String.valueOf(eng.hpOf(p))), p.x, p.y + R * .6f, small, 0xB3FFFFFF, tfBold, Paint.Align.CENTER);
    }

    private void drawOrbit(Canvas c, Planet p) {
        if (eng.fogged(p)) return;
        int nr = eng.orbitRings(p, rings), dc = eng.orbitDots(p, dotX, dotY), hl = eng.highlightCount(p);
        stroke.setStrokeWidth(dp);
        stroke.setColor(alpha(FC[p.owner], .12f));
        for (int i = 0; i < nr; i++) c.drawCircle(p.x, p.y, rings[i], stroke);
        float dot = Math.max(1.8f * dp, eng.unit * .0055f);
        int normal = alpha(FC[p.owner], .92f);
        for (int i = 0; i < dc; i++) {
            boolean on = i < hl;
            fill.setColor(on ? C_GOLD : normal);
            c.drawCircle(dotX[i], dotY[i], on ? dot * 1.3f : dot, fill);
        }
    }

    private void drawNeutrals(Canvas c) {
        boolean hot = eng.rules().asteroidsHitPlanets();
        for (Asteroid a : eng.neutrals) {
            path.reset();
            for (int i = 0; i < 8; i++) {
                float ang = a.rot + i / 8f * TAU, r = a.rad * a.verts[i];
                float px = a.x + cos(ang) * r, py = a.y + sin(ang) * r;
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            path.close();
            fill.setColor(hot ? 0xFF7D6A5C : 0xFF7B7168);
            c.drawPath(path, fill);
            stroke.setColor(hot ? 0xBFFFAA6E : 0x8CE1D7CD);
            stroke.setStrokeWidth((hot ? 1.6f : 1.2f) * dp);
            c.drawPath(path, stroke);
            fill.setColor(0x38000000);
            c.drawCircle(a.x + cos(a.rot + .3f) * a.rad * .3f, a.y + sin(a.rot + .3f) * a.rad * .3f, a.rad * .28f, fill);
        }
    }

    private void drawRocks(Canvas c) {
        for (Rock r : eng.rocks) {
            int o = r.owner;
            if (r.idle) {
                float bx = sin(eng.clock * 1.4f + r.ph) * 1.4f * dp, by = cos(eng.clock * 1.1f + r.ph) * 1.4f * dp;
                stroke.setColor(alpha(FC[o], .5f)); stroke.setStrokeWidth(dp);
                c.drawCircle(r.x + bx, r.y + by, r.rad + 2.5f * dp, stroke);
                fill.setColor(FL[o]);
                c.drawCircle(r.x + bx, r.y + by, r.rad, fill);
                continue;
            }
            stroke.setColor(alpha(FC[o], .5f)); stroke.setStrokeWidth(r.rad * 1.1f);
            c.drawLine(r.x - r.vx * .07f, r.y - r.vy * .07f, r.x, r.y, stroke);
            fill.setColor(FL[o]);
            c.drawCircle(r.x, r.y, r.rad, fill);
        }
    }

    // Kéo thẳng từ hành tinh (gửi nửa số đá)
    private void drawDrag(Canvas c) {
        Pointer pt = eng.pointer();
        if (pt == null || pt.mode != GestureMode.QUICK) return;
        ArrayList<Planet> sel = pt.qsel;
        Planet h = pt.hover;
        boolean cancel = h != null && sel.size() == 1 && h == sel.get(0), atk = h != null && !Faction.isPlayer(h.owner);
        float dx = h != null ? h.x : pt.x, dy = h != null ? h.y : pt.y;
        boolean far = !cancel && eng.farFrom(sel, dx, dy);
        int col = cancel ? C_MUTED : far ? C_FAR : atk ? C_DANGER : C_YOU;
        drawRange(c, sel, .5f);
        stroke.setStrokeWidth(2 * dp);
        dashed(true, eng.clock);
        for (Planet s : sel) {
            float R = eng.radiusOf(s);
            stroke.setColor(C_YOU);
            c.drawCircle(s.x, s.y, R + 9 * dp, stroke);
            if (cancel || s == h) continue;
            float ex = pt.x, ey = pt.y;
            if (h != null) { float d = (float) Math.hypot(h.x - s.x, h.y - s.y), q = eng.radiusOf(h) + 6 * dp; if (d > 0) { ex = h.x - (h.x - s.x) / d * q; ey = h.y - (h.y - s.y) / d * q; } }
            float d2 = (float) Math.hypot(ex - s.x, ey - s.y);
            if (d2 < 1) continue;
            stroke.setColor(col);
            c.drawLine(s.x + (ex - s.x) / d2 * R, s.y + (ey - s.y) / d2 * R, ex, ey, stroke);
        }
        dashed(false, 0);
        if (h != null && !cancel) { stroke.setColor(col); stroke.setStrokeWidth(3 * dp); c.drawCircle(h.x, h.y, eng.radiusOf(h) + 11 * dp, stroke); }
        int tot = 0;
        for (Planet s : sel) { if (s == h && sel.size() > 1) continue; tot += eng.quickCount(s); }
        float lx = h != null ? h.x : pt.x, ly = h != null ? h.y - eng.radiusOf(h) - 34 * dp : pt.y - 48 * dp;
        if (cancel) pill(c, lx, ly, tx.s(R.string.pill_cancel), col);
        else if (far) pill(c, lx, ly, tx.s(R.string.msg_out_of_range), col);
        else if (h != null) pill(c, lx, ly, atk ? tx.s(R.string.pill_attack, tot, eng.fogged(h) ? "?" : String.valueOf(eng.hpOf(h))) : tx.s(R.string.pill_move, tot), col);
        else pill(c, lx, ly, tx.s(R.string.pill_point, tot), col);
    }

    // Vòng khoanh, vùng chọn đã khóa và đường kéo tới đích
    private void drawSel(Canvas c) {
        Pointer pt = eng.pointer();
        boolean drawing = pt != null && pt.mode == GestureMode.LASSO;
        if (drawing && pt.pn > 1) {
            path.reset();
            path.moveTo(pt.px[0], pt.py[0]);
            for (int i = 1; i < pt.pn; i++) path.lineTo(pt.px[i], pt.py[i]);
            fill.setColor(0x144FF0B4);
            path.close(); c.drawPath(path, fill);
            path.reset();
            path.moveTo(pt.px[0], pt.py[0]);
            for (int i = 1; i < pt.pn; i++) path.lineTo(pt.px[i], pt.py[i]);
            stroke.setColor(0xF24FF0B4); stroke.setStrokeWidth(3 * dp);
            c.drawPath(path, stroke);
            Selection lv = pt.live;
            if (lv != null) {
                stroke.setColor(C_GOLD); stroke.setStrokeWidth(1.5f * dp);
                for (Rock r : lv.loose) c.drawCircle(r.x, r.y, r.rad + 3 * dp, stroke);
                int tot = lv.total();
                if (tot > 0) pill(c, pt.x, pt.y - 44 * dp, tx.s(R.string.pill_rocks, tot), C_YOU);
            }
        }
        Selection sel = eng.selection();
        if (sel == null || drawing || sel.n < 3) return;
        path.reset();
        path.moveTo(sel.xs[0], sel.ys[0]);
        for (int i = 1; i < sel.n; i++) path.lineTo(sel.xs[i], sel.ys[i]);
        path.close();
        fill.setColor(0x0F4FF0B4); c.drawPath(path, fill);
        stroke.setColor(0xD94FF0B4); stroke.setStrokeWidth(2 * dp);
        dashed(true, eng.clock * .6f); c.drawPath(path, stroke); dashed(false, 0);
        stroke.setColor(C_GOLD); stroke.setStrokeWidth(1.5f * dp);
        for (Rock r : sel.loose) if (!r.dead) c.drawCircle(r.x, r.y, r.rad + 3 * dp, stroke);
        int tot = sel.total();
        if (pt != null && pt.mode == GestureMode.CARRY) {
            Planet h = pt.hover;
            boolean atk = h != null && !Faction.isPlayer(h.owner);
            tmpPlanets.clear();
            for (Planet p : sel.planets) if (p != h) tmpPlanets.add(p);
            float dx = h != null ? h.x : pt.x, dy = h != null ? h.y : pt.y;
            boolean far = eng.farFrom(tmpPlanets, dx, dy);
            int col = far ? C_FAR : atk ? C_DANGER : C_YOU;
            drawRange(c, tmpPlanets, .5f);
            float ex = pt.x, ey = pt.y;
            if (h != null) { float d = (float) Math.hypot(h.x - sel.cx, h.y - sel.cy), q = eng.radiusOf(h) + 6 * dp; if (d > 0) { ex = h.x - (h.x - sel.cx) / d * q; ey = h.y - (h.y - sel.cy) / d * q; } }
            stroke.setColor(col); stroke.setStrokeWidth(2 * dp);
            dashed(true, eng.clock); c.drawLine(sel.cx, sel.cy, ex, ey, stroke); dashed(false, 0);
            if (h != null) { stroke.setStrokeWidth(3 * dp); c.drawCircle(h.x, h.y, eng.radiusOf(h) + 11 * dp, stroke); }
            float lx = h != null ? h.x : pt.x, ly = h != null ? h.y - eng.radiusOf(h) - 34 * dp : pt.y - 48 * dp;
            if (far) pill(c, lx, ly, tx.s(R.string.msg_out_of_range), col);
            else if (h != null) {
                boolean only = sel.planets.size() == 1 && sel.planets.get(0) == h && sel.loose.isEmpty();
                pill(c, lx, ly, atk ? tx.s(R.string.pill_attack, tot, eng.fogged(h) ? "?" : String.valueOf(eng.hpOf(h))) : only ? tx.s(R.string.pill_upgrade, tot) : tx.s(R.string.pill_move, tot), col);
            } else pill(c, lx, ly, tx.s(R.string.pill_point, tot), col);
        } else pill(c, sel.cx, sel.cy, tx.s(R.string.pill_rocks, tot), C_YOU);
    }

}
