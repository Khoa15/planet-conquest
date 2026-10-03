package com.planetconquest.game.engine.rules;

/** Thiên thạch trôi nổi đâm vào hành tinh, trừ giáp rồi đến đá canh gác. */
public final class AsteroidImpactRule extends LevelRule {
    @Override public boolean asteroidsHitPlanets() { return true; }
}
