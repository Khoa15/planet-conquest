package com.planetconquest.game.engine.fx;

import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.FloatText;
import com.planetconquest.game.engine.model.Particle;

import java.util.ArrayList;
import java.util.Random;

import static com.planetconquest.game.engine.util.MathUtil.TAU;
import static com.planetconquest.game.engine.util.MathUtil.cos;
import static com.planetconquest.game.engine.util.MathUtil.sin;

/** Hiệu ứng thuần hình ảnh: hạt vỡ và chữ bay lên. Không ảnh hưởng luật chơi. */
public final class Effects {
    public final ArrayList<Particle> parts = new ArrayList<Particle>();
    public final ArrayList<FloatText> texts = new ArrayList<FloatText>();
    private final Random rnd;
    private float dp = 1;

    public Effects(Random rnd) { this.rnd = rnd; }

    public void setDensity(float dp) { this.dp = dp; }

    public void clear() { parts.clear(); texts.clear(); }

    private float randRange(float a, float b) { return a + rnd.nextFloat() * (b - a); }

    // ---------- Hiệu ứng ----------
    public void burst(float x, float y, int color, int n) {
        if (parts.size() > 420) return;
        for (int i = 0; i < n; i++) {
            float a = randRange(0, TAU), s = randRange(20, 90) * dp;
            Particle q = new Particle();
            q.x = x; q.y = y; q.vx = cos(a) * s; q.vy = sin(a) * s;
            q.life = randRange(.25f, .55f); q.max = .55f; q.color = color; q.size = randRange(1, 2.2f) * dp;
            parts.add(q);
        }
    }

    public void popText(float x, float y, String s, int color) {
        FloatText t = new FloatText();
        t.x = x; t.y = y; t.text = s; t.color = color; t.life = 1.6f;
        texts.add(t);
    }

    /** Cập nhật hạt và chữ bay. */
    public void update(float dt) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            Particle q = parts.get(i);
            q.life -= dt;
            if (q.life <= 0) { parts.remove(i); continue; }
            q.x += q.vx * dt; q.y += q.vy * dt; q.vx *= .94f; q.vy *= .94f;
        }
        for (int i = texts.size() - 1; i >= 0; i--) {
            FloatText t = texts.get(i);
            t.life -= dt;
            if (t.life <= 0) { texts.remove(i); continue; }
            t.y -= 22 * dp * dt;
        }
    }
}
