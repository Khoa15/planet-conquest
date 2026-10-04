package com.planetconquest.game.engine;

import static org.junit.Assert.*;

import com.planetconquest.game.engine.level.*;
import com.planetconquest.game.engine.model.*;

import org.junit.Test;

public class OrbitPatternTest extends EngineTestBase {
    @Test public void dotsStayInsideBandAroundPlanet() {
        start(Levels.ALL[2]);
        Planet p = me();
        p.addRocks(60);
        float[] x = new float[64], y = new float[64];
        float outer = e.orbitOuterRadius(p);
        for (int f = 0; f < 600; f++) {
            e.step(1 / 60f);
            int n = e.orbitDots(p, x, y);
            assertEquals(Math.min(p.rocks(), OrbitPattern.MAX_DOTS), n);
            for (int i = 0; i < n; i++) {
                float d = (float) Math.hypot(x[i] - p.x(), y[i] - p.y());
                assertTrue(d > p.radius() && d <= outer + .01f);
            }
        }
    }

    @Test public void motionIsNotUniformRings() {
        start(Levels.ALL[2]);
        Planet p = me();
        p.addRocks(30);
        float[] x = new float[64], y = new float[64];
        e.orbitDots(p, x, y);
        java.util.HashSet<Integer> radii = new java.util.HashSet<Integer>();
        for (int i = 0; i < 30; i++) radii.add(Math.round((float) Math.hypot(x[i] - p.x(), y[i] - p.y())));
        assertTrue("bán kính phải đa dạng, không dồn vào vài vòng", radii.size() > 10);
    }
}
