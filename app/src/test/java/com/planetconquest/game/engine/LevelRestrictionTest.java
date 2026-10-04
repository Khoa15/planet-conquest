package com.planetconquest.game.engine;

import com.planetconquest.game.engine.level.*;
import com.planetconquest.game.engine.model.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LevelRestrictionTest extends EngineTestBase {

    @Test
    public void level1_noUpgrade() {
        start(Levels.ALL[1]); tick(60); tap(me().x(), me().y()); tick(5);
        assertEquals(1, me().level());
        assertEquals(Msg.NO_UPGRADE_IN_LEVEL, lastNotice);
    }

    @Test
    public void level2_capacityCapped() {
        start(Levels.ALL[2]); freezeAi(); me().setRocks(29); secs(3);
        int capNow = e.capOf(me()); me().setLevel(3);
        assertEquals(30, me().rocks());
        assertEquals(30, capNow);
        assertEquals(30, e.capOf(me()));
    }

    @Test
    public void level3_noProductionAndLoseWhenOutOfRocks() {
        start(Levels.ALL[3]); freezeAi(); int n0 = me().rocks(); secs(10);
        assertEquals(n0, me().rocks());
        me().setRocks(0); e.rocks.clear(); tick(2);
        assertEquals(Boolean.FALSE, finishWin);
        assertEquals(EndReason.OUT_OF_ROCKS, finishReason);
    }

    @Test
    public void level4_timeLimit() {
        start(Levels.ALL[4]); freezeAi(); secs(101);
        assertEquals(Boolean.FALSE, finishWin);
        assertEquals(EndReason.TIME_UP, finishReason);
    }

    @Test
    public void level5_shortRange() {
        start(Levels.ALL[5]); freezeAi();
        Planet far = null, near = null;
        float fd = 0, nd = Float.MAX_VALUE;
        for (Planet p : e.planets) {
            if (p == me()) continue;
            float d = (float) Math.hypot(p.x() - me().x(), p.y() - me().y());
            if (d > fd) { fd = d; far = p; }
            if (d < nd) { nd = d; near = p; }
        }
        int b0 = me().rocks(); drag(me(), far.x(), far.y());
        assertEquals(b0, me().rocks());
        assertEquals(Msg.OUT_OF_RANGE, lastNotice);
        int b1 = me().rocks(); drag(me(), near.x(), near.y());
        assertTrue(b1 - me().rocks() > 0);
    }

    @Test
    public void level6_fogRevealedBriefly() {
        start(Levels.ALL[6]); freezeAi();
        Planet e1 = e.planets.get(1);
        assertTrue(e.fogged(e1) && !e.fogged(me()));
        Rock probe = new Rock(); probe.x = e1.x() + e1.base(); probe.y = e1.y(); probe.s = 600; probe.owner = 0; probe.setTarget(e1); probe.rad = e.rockRadius();
        e.rocks.add(probe); tick(3);
        assertFalse(e.fogged(e1));
        secs(3.5f);
        assertTrue(e.fogged(e1));
    }

    @Test
    public void level7_asteroidField() {
        start(Levels.ALL[7]);
        assertEquals(22, e.neutrals.size());
    }

    @Test
    public void level8_cooldownBetweenLaunches() {
        start(Levels.ALL[8]); freezeAi(); Planet tg = e.planets.get(1);
        int a1 = me().rocks(); drag(me(), tg.x(), tg.y()); int a2 = me().rocks(); drag(me(), tg.x(), tg.y()); int a3 = me().rocks();
        Msg cdToast = lastNotice;
        secs(4.2f); int a4 = me().rocks(); drag(me(), tg.x(), tg.y());
        assertTrue(a1 > a2);
        assertEquals(a2, a3);
        assertEquals(Msg.COOLDOWN, cdToast);
        assertTrue(a4 > me().rocks());
    }

    @Test
    public void level9_enemyProductionDoubled() {
        start(Levels.ALL[9]);
        float rp0 = e.rateOf(me()), re0 = e.rateOf(e.planets.get(1));
        e.planets.get(1).setOwner(0);
        assertEquals(1f, rp0, 0);
        assertEquals(2f, re0, 0);
        assertEquals(1f, e.rateOf(e.planets.get(1)), 0);
    }

    @Test
    public void level10_rocksFlySlowly() {
        start(Levels.ALL[10]); freezeAi(); Planet near = nearest();
        int n0 = me().rocks(); drag(me(), near.x(), near.y());
        assertTrue(n0 > me().rocks());
        tick(1);
        Rock r = e.rocks.get(0);
        assertTrue("đá phải bay chậm", Math.hypot(r.vx, r.vy) <= e.unit() * Engine.ROCK_SPEED * .25f * 1.3f);
        secs(1);
        assertTrue("sau 1 giây đá vẫn đang bay", e.rocks.size() > 0);
    }

    @Test
    public void endless_smallBodiesFarApartAndSlowRocks() {
        java.util.Random r = new java.util.Random(7);
        for (int k = 0; k < 60; k++) {
            Level L = Levels.endless(1 + k % 10, r);
            assertEquals(.4f, L.bodyScale, 0);
            assertEquals(.25f, L.rules.rockSpeedScale(), 0);
            start(L);
            assertTrue(Float.isInfinite(e.rangePx()));
            assertTrue("khoảng cách đủ xa với " + e.planets.size() + " hành tinh", minDist() >= 90 * e.dp());
            assertEquals(0, L.rules.thinGuardBelow());
        }
    }

    @Test
    public void endless_asteroidsShrinkWithBodiesButKeepDamage() {
        start(Levels.endless(1, new java.util.Random(5)));
        float maxRad = e.unit() * .05f * .4f;
        for (Asteroid ast : e.neutrals) assertTrue("thiên thạch phải nhỏ theo", ast.rad <= maxRad + .01f);
        Asteroid ref = new Asteroid(); ref.rad = e.unit() * .05f * .4f;
        assertEquals(e.asteroidDamage(ref), Math.max(2, Math.round(.05f * .05f * Engine.ASTEROID_DMG_K)));
    }

    @Test
    public void level10_noRangeLimit() {
        start(Levels.ALL[10]); freezeAi();
        assertTrue(Float.isInfinite(e.rangePx()));
        Planet far = null; float fd = 0;
        for (Planet p : e.planets) { float d = (float) Math.hypot(p.x() - me().x(), p.y() - me().y()); if (d > fd) { fd = d; far = p; } }
        int n0 = me().rocks(); drag(me(), far.x(), far.y());
        assertTrue("gửi được tới hành tinh xa nhất", n0 > me().rocks());
    }

    @Test
    public void level10_mapKeepsMarginsAndGaps() {
        Level L = Levels.ALL[10];
        float[][] sizes = {{1080, 2340, 2.75f}, {720, 1600, 2f}, {1440, 3120, 3.5f}, {1080, 1920, 3f}};
        for (float[] sz : sizes) {
            e.setSize(sz[0], sz[1], sz[2]); start(L);
            String where = (int) sz[0] + "x" + (int) sz[1];
            assertEquals(.4f, e.bodyScale(), 0);
            for (Planet p : e.planets) {
                assertTrue("lề ngang " + where, p.x() >= 72 * e.dp() - 1 && p.x() <= e.W() - 72 * e.dp() + 1);
                assertTrue("lề dọc " + where, p.y() >= 100 * e.dp() && p.y() <= e.H() - 100 * e.dp());
            }
            assertTrue("khoảng cách tối thiểu " + where, minDist() >= .3f * e.unit());
        }
    }

    @Test
    public void level10_thinGuardWarningThreshold() {
        start(Levels.ALL[10]);
        assertEquals(8, e.rules().thinGuardBelow());
        assertEquals(0, Levels.ALL[5].rules.thinGuardBelow());
    }

    private Planet nearest() {
        Planet best = null; float bd = Float.MAX_VALUE;
        for (Planet p : e.planets) {
            if (p == me()) continue;
            float d = (float) Math.hypot(p.x() - me().x(), p.y() - me().y());
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }
}
