package com.planetconquest.game.engine;

import com.planetconquest.game.engine.level.Levels;
import com.planetconquest.game.engine.model.Planet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Vòng khoanh được làm tròn thành hình tròn: vẽ méo hoặc lệch vẫn chọn được đá bên trong. */
public class CircleSelectionTest extends EngineTestBase {

    @Test
    public void sloppyLoopSelectsRocksInsideFittedCircle() {
        start(Levels.ALL[0]);
        Planet p = me();
        float r = 75 * e.dp();
        e.down(p.x() + r, p.y());
        for (int i = 1; i <= 40; i++) {                       // elip méo, nét vẽ rung nhẹ, khép vòng ở điểm đầu
            double a = i / 40.0 * Math.PI * 2;
            float wobble = 1 + .08f * (float) Math.sin(a * 5);
            e.move(p.x() + (float) Math.cos(a) * r * 1.15f * wobble, p.y() + (float) Math.sin(a) * r * .9f * wobble);
        }
        assertNotNull("khép vòng là khóa vùng chọn", e.selection());
        assertTrue(e.selection().total() > 0);
        assertEquals("vùng chọn là đa giác hình tròn", 40, e.selection().n);
    }

    @Test
    public void loopAroundEmptySpaceSelectsNothing() {
        start(Levels.ALL[0]);
        Planet enemy = e.planets.get(1);
        circle(enemy.x(), enemy.y() + 200 * e.dp(), 50 * e.dp()); e.up(enemy.x(), enemy.y());
        assertEquals(null, e.selection());
    }
}
