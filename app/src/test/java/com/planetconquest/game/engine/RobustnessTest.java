package com.planetconquest.game.engine;

import com.planetconquest.game.engine.model.*;

import org.junit.Test;

import java.util.Random;

/** Mô phỏng AI và bấm ngẫu nhiên để bắt lỗi runtime (không ném ngoại lệ là đạt). */
public class RobustnessTest extends EngineTestBase {

    @Test
    public void idlePlayerSimulationRunsForEveryLevel() {
        for (int i = 1; i < Levels.ALL.length; i++) {
            start(Levels.ALL[i]);
            for (int sec = 0; sec < 150 && finishWin == null; sec++) secs(1);
        }
    }

    @Test
    public void randomInputDoesNotThrow() {
        Random r = new Random(7);
        for (int round = 0; round < 3; round++) {
            for (Level L : Levels.ALL) fuzz(L, r);
            for (int k = 0; k < 4; k++) fuzz(Levels.endless(1 + k, r), r);
        }
    }

    private void fuzz(Level L, Random r) {
        start(L);
        for (int s = 0; s < 60 && finishWin == null; s++) {
            for (int a = 0; a < 2; a++) {
                float k = r.nextFloat();
                Planet p = e.planets.get(r.nextInt(e.planets.size())), q = e.planets.get(r.nextInt(e.planets.size()));
                if (k < .35f) drag(p, q.x, q.y);
                else if (k < .55f) tap(p.x, p.y);
                else if (k < .8f) {
                    circle(r.nextFloat() * e.W, e.H * (.2f + r.nextFloat() * .6f), (40 + r.nextFloat() * 90) * e.dp);
                    float tx = r.nextFloat() * e.W, ty = e.H * (.2f + r.nextFloat() * .6f);
                    e.move(tx, ty); e.up(tx, ty);
                } else tap(r.nextFloat() * e.W, e.H * (.15f + r.nextFloat() * .7f));
            }
            e.updateLive();
            secs(1);
        }
    }
}
