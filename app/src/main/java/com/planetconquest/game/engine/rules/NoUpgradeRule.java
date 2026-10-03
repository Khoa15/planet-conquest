package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.Msg;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Planet;

/** Không thể nâng cấp hành tinh. */
public final class NoUpgradeRule extends LevelRule {
    @Override public int maxLevel(Planet p, int base) { return 1; }
    @Override public Notice upgradeBlocked() { return new Notice(Msg.NO_UPGRADE_IN_LEVEL); }
}
