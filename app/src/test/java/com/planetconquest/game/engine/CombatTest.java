package com.planetconquest.game.engine;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CombatTest extends EngineTestBase {

    @Test
    public void armorAbsorbsFirstThenPlanetIsCaptured() {
        start(Levels.ALL[2]); freezeAi();
        Planet b = e.planets.get(2); b.level = 3; b.armor = 20; b.n = 5; b.acc = -1e9f;
        for (int i = 0; i < 25; i++) shoot(b);
        tick(3);
        assertTrue(b.owner == 2 && b.n == 0);
        shoot(b); tick(3);
        assertEquals(0, b.owner);
        assertEquals(1, b.level);
        assertEquals(0, b.armor);
        assertTrue(b.captured);
        assertEquals(4, e.maxLvl(b));
    }

    @Test
    public void biggerAsteroidsDealMoreDamage() {
        start(Levels.endless(1, new Random(3))); freezeAi();
        Planet tp = me(); tp.n = 60; tp.acc = -1e9f;
        int prev = 0;
        for (float f : new float[]{.014f, .025f, .04f, .05f}) {
            int before = tp.n + tp.armor;
            Asteroid a = new Asteroid(); a.x = tp.x; a.y = tp.y; a.rad = f * e.unit; a.o = -1;
            e.neutrals.add(a); tick(1);
            int d = before - (tp.n + tp.armor);
            assertTrue(d > prev);
            prev = d;
        }
    }
}
