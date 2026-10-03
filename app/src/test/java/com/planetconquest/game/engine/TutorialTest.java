package com.planetconquest.game.engine;

import com.planetconquest.game.engine.model.*;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TutorialTest extends EngineTestBase {

    @Test
    public void tutorialStepsCompleteInOrder() {
        start(Levels.ALL[0]);
        Planet enemy = e.planets.get(1);
        drag(me(), enemy.x, enemy.y); secs(2);
        assertTrue("bước 1", events.contains(GameEvent.ATTACK) && enemy.owner == 1);
        circle(me().x, me().y, 70 * e.dp); e.up(me().x + 70 * e.dp, me().y);
        assertTrue("bước 2", events.contains(GameEvent.LASSO) && e.selection != null);
        tap(e.W / 2, e.H * .55f); secs(2);
        int idle = 0; for (Rock r : e.rocks) if (r.idle && r.owner == 0) idle++;
        assertTrue("bước 3", events.contains(GameEvent.POINT) && idle > 0);
        secs(5); tap(me().x, me().y); secs(2);
        assertTrue("bước 4", events.contains(GameEvent.UPGRADE));
        int guard = 0;
        while (finishWin == null && guard++ < 40) { secs(3); drag(me(), enemy.x, enemy.y); }
        secs(3);
        assertEquals("bước 5", Boolean.TRUE, finishWin);
        assertTrue(events.contains(GameEvent.CAPTURE));
    }

    @Test
    public void tutorialOpponentNeverAttacks() {
        start(Levels.ALL[0]); secs(120);
        int launched = 0; for (Rock r : e.rocks) if (r.owner != 0) launched++;
        assertEquals(0, launched);
        assertEquals(0, me().owner);
    }
}
