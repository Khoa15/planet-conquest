package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.model.Planet;

/** Mỗi hành tinh chỉ chứa tối đa một số đá, dù cấp nào. */
public final class CapacityCapRule extends LevelRule {
    private final int cap;

    public CapacityCapRule(int cap) { this.cap = cap; }

    @Override public int capacity(Planet p, int base) { return Math.min(base, cap); }
}
