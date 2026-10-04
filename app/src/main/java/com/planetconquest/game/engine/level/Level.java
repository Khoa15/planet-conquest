package com.planetconquest.game.engine.level;

import com.planetconquest.game.engine.rules.RuleSet;

import com.planetconquest.game.engine.model.*;
import com.planetconquest.game.engine.util.MathUtil;

/** Cấu hình một màn chơi. Mỗi màn chiến dịch bật đúng một hạn chế. */
public final class Level {
    public int planets = 3, playerN = 30, enemyMin = 18, enemyMax = 24, enemyArmor = 0;
    public int neutrals = -1;            // -1: dùng mặc định
    public long seed = 1;
    public float aiGrace = -1;           // -1: dùng mặc định
    public float neutralSize = 1f, nrMin = .016f, nrMax = .03f, nsMin = .03f, nsMax = .08f;
    public float boldShift = 0f;
    public float edgeMarginX = 0;        // dp; > 0: lề ngang tối thiểu của tâm hành tinh (0 = mặc định của MapGenerator)
    public float gap = 0;                // theo unit; > 0: khoảng cách tối thiểu giữa hai hành tinh (0 = mặc định)
    public float bodyScale = 1f;         // thu nhỏ hành tinh, quỹ đạo và đá để cùng khoảng cách trông xa hơn
    public float[][] fixed;              // vị trí cố định {nx, ny, size} (màn Hướng dẫn)

    /** Các hạn chế của màn (mỗi hạn chế là một LevelRule). */
    public final RuleSet rules = new RuleSet();

    // Chế độ đặc biệt
    public boolean intro, endless, passiveAi;
}
