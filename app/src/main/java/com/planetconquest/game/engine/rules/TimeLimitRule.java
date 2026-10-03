package com.planetconquest.game.engine.rules;

/** Giới hạn thời gian để chiếm hết hành tinh. */
public final class TimeLimitRule extends LevelRule {
    private final int seconds;

    public TimeLimitRule(int seconds) { this.seconds = seconds; }

    @Override public int timeLimit() { return seconds; }
}
