package com.planetconquest.game.engine.ai;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.model.Planet;

/** Cách các hành tinh đối thủ ra quyết định. Thay thế được (OCP) mà không sửa Engine. */
public interface AiStrategy {
    /** Một hành tinh đối thủ đến lượt suy nghĩ. */
    void think(Engine eng, Planet p);

    /** Chống bế tắc: lâu rồi không ai tấn công. */
    void onLull(Engine eng);
}
