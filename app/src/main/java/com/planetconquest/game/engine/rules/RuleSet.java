package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Planet;

import java.util.ArrayList;

/** Gộp nhiều LevelRule thành một (Composite): giá trị số đi qua từng luật theo thứ tự, thông báo lấy từ luật đầu tiên có. */
public final class RuleSet extends LevelRule {
    private final ArrayList<LevelRule> rules = new ArrayList<LevelRule>();

    public RuleSet add(LevelRule r) { rules.add(r); return this; }

    @Override public int maxLevel(Planet p, int base) { for (LevelRule r : rules) base = r.maxLevel(p, base); return base; }
    @Override public int capacity(Planet p, int base) { for (LevelRule r : rules) base = r.capacity(p, base); return base; }
    @Override public float productionRate(Planet p, float base) { for (LevelRule r : rules) base = r.productionRate(p, base); return base; }

    @Override public Notice upgradeBlocked() {
        for (LevelRule r : rules) { Notice n = r.upgradeBlocked(); if (n != null) return n; }
        return null;
    }

    @Override public Notice canLaunch(Engine eng, Planet src, float dx, float dy) {
        for (LevelRule r : rules) { Notice n = r.canLaunch(eng, src, dx, dy); if (n != null) return n; }
        return null;
    }

    @Override public void onLaunch(Planet src) { for (LevelRule r : rules) r.onLaunch(src); }
    @Override public void onHit(Planet target, int attackerOwner) { for (LevelRule r : rules) r.onHit(target, attackerOwner); }

    @Override public boolean hidesInfo(Planet p) { for (LevelRule r : rules) if (r.hidesInfo(p)) return true; return false; }
    @Override public boolean asteroidsHitPlanets() { for (LevelRule r : rules) if (r.asteroidsHitPlanets()) return true; return false; }
    @Override public boolean finiteRocks() { for (LevelRule r : rules) if (r.finiteRocks()) return true; return false; }

    @Override public float rangeFraction() { float v = 0; for (LevelRule r : rules) v = Math.max(v, r.rangeFraction()); return v; }
    @Override public int timeLimit() { int v = 0; for (LevelRule r : rules) v = Math.max(v, r.timeLimit()); return v; }
    @Override public float cooldownSeconds() { float v = 0; for (LevelRule r : rules) v = Math.max(v, r.cooldownSeconds()); return v; }
}
