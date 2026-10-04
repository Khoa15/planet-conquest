package com.planetconquest.game.engine.rules;

/** Đá bay chậm: mọi chặng đường mất nhiều thời gian, hành tinh nguồn hở sườn suốt chặng đó; còn ít đá thì bị cảnh báo. */
public final class SlowRocksRule extends LevelRule {
    private final float scale;
    private final int thinGuard;

    public SlowRocksRule(float scale, int thinGuard) { this.scale = scale; this.thinGuard = thinGuard; }

    @Override public float rockSpeedScale() { return scale; }
    @Override public int thinGuardBelow() { return thinGuard; }
}
