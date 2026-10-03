package com.planetconquest.game.engine;

import org.junit.Before;

import java.util.ArrayList;

/** Tiện ích chung cho các bài kiểm thử engine trên JVM (không cần thiết bị Android). */
public abstract class EngineTestBase implements Engine.Listener {
    protected final Engine e = new Engine();
    protected final ArrayList<Integer> events = new ArrayList<Integer>();
    protected String lastToast = "", finishReason;
    protected Boolean finishWin;

    @Override public void onToast(String m) { lastToast = m; }
    @Override public void onHaptic(int k) { }
    @Override public void onEvent(int ev) { events.add(ev); }
    @Override public void onFinish(boolean win, String reason) { finishWin = win; finishReason = reason; }

    @Before
    public void setUpEngine() {
        e.setListener(this);
        e.setSize(1080, 2340, 2.75f);
    }

    protected void tick(int frames) { for (int i = 0; i < frames; i++) { e.step(1 / 60f); e.fx(1 / 60f); } }
    protected void secs(float s) { tick(Math.round(s * 60)); }
    protected void start(Level L) { finishWin = null; finishReason = null; events.clear(); e.start(L); }
    protected Planet me() { return e.planets.get(0); }
    protected void freezeAi() { for (Planet p : e.planets) p.think = 1e9f; }

    protected void drag(Planet a, float bx, float by) {
        e.down(a.x, a.y);
        e.move((a.x + bx) / 2, (a.y + by) / 2);
        e.move(bx, by);
        e.up(bx, by);
    }

    protected void tap(float x, float y) { e.down(x, y); e.up(x, y); }

    protected void circle(float cx, float cy, float r) {
        e.down(cx + r, cy);
        for (int i = 1; i <= 40; i++) {
            double a = i / 40.0 * Math.PI * 2;
            e.move(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
    }

    protected float minDist() {
        float m = Float.MAX_VALUE;
        for (int i = 0; i < e.planets.size(); i++) for (int j = i + 1; j < e.planets.size(); j++) {
            Planet a = e.planets.get(i), b = e.planets.get(j);
            m = Math.min(m, (float) Math.hypot(a.x - b.x, a.y - b.y));
        }
        return m;
    }

    protected boolean inBounds() {
        for (Planet p : e.planets) if (p.x < 25 * e.dp || p.x > e.W - 25 * e.dp || p.y < 70 * e.dp || p.y > e.H - 50 * e.dp) return false;
        return true;
    }

    /** Bắn một viên đá của người chơi thẳng vào hành tinh đích. */
    protected void shoot(Planet target) {
        Rock k = new Rock();
        k.x = target.x + target.base; k.y = target.y; k.s = 600; k.o = 0; k.t = target; k.rad = e.RR;
        e.rocks.add(k);
    }
}
