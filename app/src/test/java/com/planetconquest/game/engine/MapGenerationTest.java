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
            for (int li = 0; li < Levels.ALL.length; li++) {
                Level L = Levels.ALL[li];
                start(L);
                String where = "level " + li + " @" + (int) s[0] + "x" + (int) s[1];
                assertEquals(where, L.planets, e.planets.size());
                assertTrue("trong khung " + where, inBounds());
                assertTrue("không chồng " + where, minDist() > 80 * e.dp());
                if (!Float.isInfinite(e.rangePx())) assertRangeConnected(where);
            }
        }
    }

    private void assertRangeConnected(String where) {
        float rp = e.rangePx(), far = 0;
        for (Planet p : e.planets) far = Math.max(far, (float) Math.hypot(p.x() - me().x(), p.y() - me().y()));
        assertTrue("có hành tinh ngoài tầm " + where, far > rp * 1.1f);
        for (Planet p : e.planets) {
            if (p == me()) continue;
            boolean any = false;
            for (Planet q : e.planets) if (q != p && Math.hypot(p.x() - q.x(), p.y() - q.y()) <= rp) any = true;
            assertTrue("hành tinh cô lập " + where, any);
        }
    }

    @Test
    public void endlessMapsHaveFourToTenPlanets() {
        for (float[] s : SIZES) {
            e.setSize(s[0], s[1], s[2]);
            int[] counts = new int[11];
            Random r = new Random(42);
            for (int k = 0; k < 300; k++) {
                start(Levels.endless(1 + k % 10, r));
                int n = e.planets.size();
                assertTrue(n >= 4 && n <= 10);
                assertTrue(inBounds());
                assertTrue(minDist() >= 70 * e.dp());
                counts[n]++;
            }
            assertEquals(0, counts[3]);
            assertTrue(counts[4] > 0 && counts[10] > 0);
        }
    }

    @Test
    public void endlessPlayerPlanetPositionIsRandomAndInBounds() {
        for (float[] s : SIZES) {
            e.setSize(s[0], s[1], s[2]);
            Random r = new Random(7);
            java.util.HashSet<Integer> cellsX = new java.util.HashSet<>(), cellsY = new java.util.HashSet<>();
            for (int k = 0; k < 200; k++) {
                start(Levels.endless(1 + k % 10, r));
                assertTrue(inBounds());
                cellsX.add((int) (me().x() / (e.dp() * 60)));
                cellsY.add((int) (me().y() / (e.dp() * 60)));
            }
            assertTrue("vị trí người chơi phải đa dạng theo cả hai trục", cellsX.size() >= 3 && cellsY.size() >= 3);
        }
    }

    @Test
    public void campaignPlayerStaysBottomCenter() {
        e.setSize(1080, 2340, 2.75f);
        for (Level L : Levels.ALL) {
            if (L.fixed != null) continue;
            start(L);
            assertEquals(540f, me().x(), 1f);
        }
    }

    @Test
    public void restartingEndlessLevelKeepsSameMap() {
        for (float[] s : SIZES) {
            e.setSize(s[0], s[1], s[2]);
            Random r = new Random(11);
            for (int k = 0; k < 20; k++) {
                Level L = Levels.endless(1 + k % 10, r);
                start(L);
                float[][] first = positions();
                start(L);                                   // chơi lại cùng đối tượng Level, như GameSession.restart()
                float[][] again = positions();
                assertEquals(first.length, again.length);
                for (int i = 0; i < first.length; i++) {
                    assertEquals(first[i][0], again[i][0], 0.01f);
                    assertEquals(first[i][1], again[i][1], 0.01f);
                }
            }
        }
    }

    private float[][] positions() {
        float[][] out = new float[e.planets.size()][2];
        for (int i = 0; i < out.length; i++) { out[i][0] = e.planets.get(i).x(); out[i][1] = e.planets.get(i).y(); }
        return out;
    }
}
