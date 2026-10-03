package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.Planet;

/** Không thấy số đá và máu của đối thủ; bị người chơi đánh thì lộ ra trong 3 giây. */
public final class FogRule extends LevelRule {
    private static final float REVEAL_SECONDS = 3f;

    @Override public boolean hidesInfo(Planet p) { return !Faction.isPlayer(p.owner) && p.reveal <= 0; }

    @Override
    public void onHit(Planet target, int attackerOwner) {
        if (Faction.isPlayer(attackerOwner)) target.reveal = REVEAL_SECONDS;
    }
}
