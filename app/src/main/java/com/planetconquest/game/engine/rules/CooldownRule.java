package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.Msg;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Planet;

/** Mỗi hành tinh chỉ gửi quân được một lần mỗi vài giây. */
public final class CooldownRule extends LevelRule {
    private final float seconds;

    public CooldownRule(float seconds) { this.seconds = seconds; }

    @Override public float cooldownSeconds() { return seconds; }

    @Override
    public Notice canLaunch(Engine eng, Planet src, float dx, float dy) {
        return src.cooldown() > 0 ? new Notice(Msg.COOLDOWN, (int) Math.ceil(src.cooldown())) : null;
    }

    @Override public void onLaunch(Planet src) { src.startCooldown(seconds); }
}
