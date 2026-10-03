package com.planetconquest.game.engine.ai;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.Planet;
import com.planetconquest.game.engine.model.Rock;

import java.util.ArrayList;

import static com.planetconquest.game.engine.util.MathUtil.clamp;
import static com.planetconquest.game.engine.util.MathUtil.hyp;

/** AI mặc định: mỗi hành tinh đối thủ tự cân nhắc chi viện, nâng cấp hoặc tấn công mục tiêu hợp lý nhất trong tầm. */
public final class DefaultAi implements AiStrategy {
    // ---------- AI ----------
    private static int incoming(Engine eng, Planet p) {
        int c = 0;
        for (Rock r : eng.rocks()) if (!r.dead && r.t == p && r.owner != p.owner) c++;
        return c;
    }

    @Override
    public void think(Engine eng, Planet p) {
        float rp = eng.rangePx();
        int cap = eng.capOf(p);
        float full = (float) p.rocks / cap;
        float pressure = clamp((full - .55f) / .4f, 0, 1);       // kho càng đầy càng liều
        float bold = p.ai.bold + (.9f - p.ai.bold) * pressure;
        int threat = incoming(eng, p);
        int keep = eng.rules().finiteRocks() ? 3 : 10;
        int reserve = Math.min(p.rocks, Math.max(0, (int) Math.ceil(threat * 1.15f) - p.armor) + keep);
        int avail = p.rocks - reserve;
        for (Planet q : eng.planets()) {                                // chi viện đồng minh đang bị đánh
            if (q == p || q.owner != p.owner) continue;
            if (hyp(q.x - p.x, q.y - p.y) > rp) continue;
            int t = incoming(eng, q);
            if (t > q.hp() * .7f && avail >= 8) avail -= eng.launch(p, q, 0, 0, Math.min(avail, (int) Math.ceil(t * .8f)), false);
        }
        if (avail < (eng.rules().finiteRocks() ? 5 : 8)) return;
        boolean canUp = p.level < eng.maxLvl(p) && threat == 0;
        int want = Engine.upgradeCost(p.level) - p.upgradeProgress;
        boolean mustAttack = full >= .85f;
        if (!mustAttack && canUp && avail >= want + 6 && eng.randFloat() < (full >= .65f ? .6f : .3f)) { eng.launch(p, p, 0, 0, want, true); return; }
        Planet best = null, weak = null;
        float bs = 0;
        int bestNeed = 0, weakNeed = Integer.MAX_VALUE;
        for (Planet q : eng.planets()) {                                // mọi hành tinh khác phe đều là đối thủ
            if (q.owner == p.owner) continue;
            float d = hyp(q.x - p.x, q.y - p.y);
            if (d > rp) continue;
            float eta = d / (eng.unit * Engine.ROCK_SPEED);
            int need = (int) Math.ceil((q.hp() + eng.rateOf(q) * eta) * bold + 3);
            if (need < weakNeed) { weakNeed = need; weak = q; }
            if (avail < need) continue;
            float s = (1f / (need + 8)) * (1f / (.4f + d / eng.unit));
            if (s > bs) { bs = s; best = q; bestNeed = need; }
        }
        if (best != null) { eng.launch(p, best, 0, 0, Math.min(avail, bestNeed + (int) Math.ceil(bestNeed * .1f)), false); return; }
        if (canUp && avail >= want + 6) { eng.launch(p, p, 0, 0, want, true); return; }
        if (mustAttack && weak != null) eng.launch(p, weak, 0, 0, avail, false);
    }

    /** Chống bế tắc: lâu không ai tấn công thì hành tinh AI nhiều đá dư nhất đánh mục tiêu yếu nhất trong tầm. */
    @Override
    public void onLull(Engine eng) {
        float rp = eng.rangePx();
        int keep = eng.rules().finiteRocks() ? 3 : 10, minA = eng.rules().finiteRocks() ? 6 : 12;
        ArrayList<Planet> src = new ArrayList<Planet>();
        for (Planet p : eng.planets()) if (!Faction.isPlayer(p.owner) && p.rocks - Math.min(p.rocks, keep) >= minA) src.add(p);
        for (int i = 1; i < src.size(); i++)
            for (int j = i; j > 0 && src.get(j).rocks > src.get(j - 1).rocks; j--) { Planet t = src.get(j); src.set(j, src.get(j - 1)); src.set(j - 1, t); }
        for (Planet s : src) {
            int a = s.rocks - Math.min(s.rocks, keep);
            Planet tgt = null;
            int th = Integer.MAX_VALUE;
            for (Planet q : eng.planets()) {
                if (q.owner == s.owner || hyp(q.x - s.x, q.y - s.y) > rp) continue;
                int h = q.hp();
                if (h < th) { th = h; tgt = q; }
            }
            if (tgt != null && eng.launch(s, tgt, 0, 0, (int) Math.floor(a * .6f), false) > 0) return;
        }
        eng.postponeLull();
    }

}
