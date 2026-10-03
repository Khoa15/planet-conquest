package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.Planet;

/** Hành tinh đối thủ sinh đá nhanh (hoặc chậm) hơn theo một hệ số. */
public final class EnemyProductionRule extends LevelRule {
    private final float multiplier;

    public EnemyProductionRule(float multiplier) { this.multiplier = multiplier; }

    @Override
    public float productionRate(Planet p, float base) {
        return !Faction.isPlayer(p.owner) && multiplier > 0 ? base * multiplier : base;
    }
}
