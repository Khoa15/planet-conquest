package com.planetconquest.game.engine;

import com.planetconquest.game.engine.level.*;
import com.planetconquest.game.engine.model.*;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapGenerationTest extends EngineTestBase {
    private static final float[][] SIZES = {{1080, 2340, 2.75f}, {720, 1600, 2f}, {1440, 3120, 3.5f}, {1080, 1920, 3f}};

    @Test
    public void campaignMapsFitEveryScreenSize() {
        for (float[] s : SIZES) {
            e.setSize(s[0], s[1], s[2]);
            for (Level L : Levels.ALL) {
                start(L);
                String where = L.name + " @" + (int) s[0] + "x" + (int) s[1];
                assertEquals(where, L.planets, e.planets.size());
                assertTrue("trong khung " + where, inBounds());
                assertTrue("không chồng " + where, minDist() > 80 * e.dp);
                if (L.range > 0) assertRangeConnected(where);
            }
        }
    }

    private void assertRangeConnected(String where) {
        float rp = e.rangePx(), far = 0;
        for (Planet p : e.planets) far = Math.max(far, (float) Math.hypot(p.x - me().x, p.y - me().y));
        assertTrue("có hành tinh ngoài tầm " + where, far > rp * 1.1f);
        for (Planet p : e.planets) {
            if (p == me()) continue;
            boolean any = false;
            for (Planet q : e.planets) if (q != p && Math.hypot(p.x - q.x, p.y - q.y) <= rp) any = true;
            assertTrue("hành tinh cô lập " + where, any);
        }
    }

    @Test
    public void endlessMapsHaveThreeToTenPlanets() {
        for (float[] s : SIZES) {
            e.setSize(s[0], s[1], s[2]);
            int[] counts = new int[11];
            Random r = new Random(42);
            for (int k = 0; k < 300; k++) {
                start(Levels.endless(1 + k % 10, r));
                int n = e.planets.size();
                assertTrue(n >= 3 && n <= 10);
                assertTrue(inBounds());
                assertTrue(minDist() >= 70 * e.dp);
                counts[n]++;
            }
            assertTrue(counts[3] > 0 && counts[10] > 0);
        }
    }
}
