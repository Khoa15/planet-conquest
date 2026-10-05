package com.planetconquest.game.engine;

import static org.junit.Assert.*;

import com.planetconquest.game.engine.level.Levels;

import org.junit.Test;

public class GameSpeedTest extends EngineTestBase {
    @Test public void cyclesThroughAllStepsThenWraps() {
        GameSpeed s = new GameSpeed();
        float[] expect = {1f, 1.25f, 1.5f, 2f, 1f};
        for (float f : expect) { assertEquals(f, s.factor(), 0f); s.next(); }
    }

    @Test public void resetReturnsToNormal() {
        GameSpeed s = new GameSpeed();
        s.next(); s.next();
        assertTrue(s.isFast());
        s.reset();
        assertEquals(1f, s.factor(), 0f);
        assertEquals("1", s.valueText());
        assertFalse(s.isFast());
    }

    @Test public void doubleSpeedAdvancesWholeSimulationTwiceAsFast() {
        start(Levels.ALL[2]);
        float t0 = e.time();
        for (int i = 0; i < 60; i++) e.step(1 / 60f * 2f);
        assertEquals(2f, e.time() - t0, .05f);
    }
}
