package com.planetconquest.game.engine.level;

import com.planetconquest.game.engine.model.*;
import com.planetconquest.game.engine.util.MathUtil;

/** Cấu hình một màn chơi. Mỗi màn chiến dịch bật đúng một hạn chế. */
public final class Level {
    public String name = "", limit = "", tip = "";
    public int planets = 3, playerN = 30, enemyMin = 18, enemyMax = 24, enemyArmor = 0;
    public int neutrals = -1;            // -1: dùng mặc định
    public long seed = 1;
    public float aiGrace = -1;           // -1: dùng mặc định
    public float neutralSize = 1f, nrMin = .016f, nrMax = .03f, nsMin = .03f, nsMax = .08f;
    public float boldShift = 0f;
    public float[][] fixed;              // vị trí cố định {nx, ny, size} (màn Hướng dẫn)

    // Các hạn chế
    public boolean noUpgrade, noProduction, fog;
    public int capCap, timeLimit;
    public float range, cooldown, enemyProd;

    // Chế độ đặc biệt
    public boolean intro, endless, passiveAi, asteroidHits;
}
