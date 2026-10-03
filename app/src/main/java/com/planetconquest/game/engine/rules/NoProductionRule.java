package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.model.Planet;

/** Hành tinh không sinh thêm đá: số đá là hữu hạn. */
public final class NoProductionRule extends LevelRule {
    @Override public float productionRate(Planet p, float base) { return 0; }
    @Override public boolean finiteRocks() { return true; }
}
