package com.planetconquest.game.engine.ai;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.model.Planet;

/** Đối thủ đứng yên hoàn toàn (màn Hướng dẫn). */
public final class PassiveAi implements AiStrategy {
    @Override public void think(Engine eng, Planet p) { }
    @Override public void onLull(Engine eng) { }
}
