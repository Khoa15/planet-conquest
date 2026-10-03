package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.Msg;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Planet;
import com.planetconquest.game.engine.util.MathUtil;

/** Đá chỉ bay được một quãng ngắn (tỉ lệ so với unit của màn hình). */
public final class RangeRule extends LevelRule {
    private final float fraction;

    public RangeRule(float fraction) { this.fraction = fraction; }

    @Override public float rangeFraction() { return fraction; }

    @Override
    public Notice canLaunch(Engine eng, Planet src, float dx, float dy) {
        return MathUtil.hyp(dx - src.x(), dy - src.y()) > eng.rangePx() ? new Notice(Msg.OUT_OF_RANGE) : null;
    }
}
