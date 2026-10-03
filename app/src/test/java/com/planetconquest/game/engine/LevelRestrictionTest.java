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
        start(Levels.ALL[1]); tick(60); tap(me().x, me().y); tick(5);
        assertEquals(1, me().level);
        assertEquals(Msg.NO_UPGRADE_IN_LEVEL, lastNotice);
    }

    @Test
    public void level2_capacityCapped() {
        start(Levels.ALL[2]); freezeAi(); me().rocks = 29; secs(3);
        int capNow = e.capOf(me()); me().level = 3;
        assertEquals(30, me().rocks);
        assertEquals(30, capNow);
        assertEquals(30, e.capOf(me()));
    }

    @Test
    public void level3_noProductionAndLoseWhenOutOfRocks() {
        start(Levels.ALL[3]); freezeAi(); int n0 = me().rocks; secs(10);
        assertEquals(n0, me().rocks);
        me().rocks = 0; e.rocks.clear(); tick(2);
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
            float d = (float) Math.hypot(p.x - me().x, p.y - me().y);
            if (d > fd) { fd = d; far = p; }
            if (d < nd) { nd = d; near = p; }
        }
        int b0 = me().rocks; drag(me(), far.x, far.y);
        assertEquals(b0, me().rocks);
        assertEquals(Msg.OUT_OF_RANGE, lastNotice);
        int b1 = me().rocks; drag(me(), near.x, near.y);
        assertTrue(b1 - me().rocks > 0);
    }

    @Test
    public void level6_fogRevealedBriefly() {
        start(Levels.ALL[6]); freezeAi();
        Planet e1 = e.planets.get(1);
        assertTrue(e.fogged(e1) && !e.fogged(me()));
        Rock probe = new Rock(); probe.x = e1.x + e1.base; probe.y = e1.y; probe.s = 600; probe.owner = 0; probe.t = e1; probe.rad = e.rockRadius;
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
        int a1 = me().rocks; drag(me(), tg.x, tg.y); int a2 = me().rocks; drag(me(), tg.x, tg.y); int a3 = me().rocks;
        Msg cdToast = lastNotice;
        secs(4.2f); int a4 = me().rocks; drag(me(), tg.x, tg.y);
        assertTrue(a1 > a2);
        assertEquals(a2, a3);
        assertEquals(Msg.COOLDOWN, cdToast);
        assertTrue(a4 > me().rocks);
    }

    @Test
    public void level9_enemyProductionDoubled() {
        start(Levels.ALL[9]);
        float rp0 = e.rateOf(me()), re0 = e.rateOf(e.planets.get(1));
        e.planets.get(1).owner = 0;
        assertEquals(1f, rp0, 0);
        assertEquals(2f, re0, 0);
        assertEquals(1f, e.rateOf(e.planets.get(1)), 0);
    }
}
