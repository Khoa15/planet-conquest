package com.planetconquest.game.engine.level;

import com.planetconquest.game.engine.model.*;
import com.planetconquest.game.engine.util.MathUtil;

import java.util.Random;

/** Danh sách màn: [0] Hướng dẫn, [1..9] chiến dịch, cộng chế độ Endless sinh ngẫu nhiên. */
public final class Levels {
    private Levels() {}

    public static final Level[] ALL = build();
    public static final int CAMPAIGN_LAST = ALL.length - 1;

    private static Level make(int planets, long seed, int playerN, int eMin, int eMax, int neutrals) {
        Level L = new Level();
        L.planets = planets; L.seed = seed; L.playerN = playerN;
        L.enemyMin = eMin; L.enemyMax = eMax; L.neutrals = neutrals;
        return L;
    }

    private static Level[] build() {
        Level intro = make(2, 7L, 60, 20, 20, 2);
        intro.intro = true; intro.passiveAi = true; intro.enemyArmor = 30; intro.enemyProd = .3f;
        intro.fixed = new float[][]{{.5f, .9f, .09f}, {.5f, .28f, .075f}};

        Level l1 = make(3, 1101L, 30, 16, 20, 4);
        l1.noUpgrade = true; l1.aiGrace = 14;

        Level l2 = make(4, 2203L, 28, 18, 24, 5);
        l2.capCap = 30;

        Level l3 = make(4, 3307L, 75, 16, 22, 4);
        l3.noProduction = true; l3.aiGrace = 8;

        Level l4 = make(5, 4409L, 36, 16, 20, 5);
        l4.timeLimit = 100; l4.aiGrace = 20;

        Level l5 = make(6, 5521L, 32, 18, 24, 5);
        l5.range = .62f;

        Level l6 = make(7, 6613L, 30, 18, 26, 5);
        l6.fog = true;

        Level l7 = make(8, 7717L, 34, 20, 26, 22);
        l7.neutralSize = 1.25f;

        Level l8 = make(9, 8821L, 34, 20, 28, 6);
        l8.cooldown = 4;

        Level l9 = make(10, 9931L, 38, 16, 22, 6);
        l9.enemyProd = 2;

        Level[] all = {intro, l1, l2, l3, l4, l5, l6, l7, l8, l9};
        for (int i = 1; i < all.length; i++) all[i].boldShift = -0.02f * (i - 1);
        return all;
    }

    /** Endless: bản đồ ngẫu nhiên 3-10 hành tinh, không hạn chế, thiên thạch đâm vào hành tinh. */
    public static Level endless(int map, Random r) {
        Level L = new Level();
        L.endless = true;
        L.planets = 3 + r.nextInt(8);
        L.seed = r.nextLong();
        int d = Math.min(map - 1, 8);
        L.playerN = 30; L.enemyMin = 18 + d; L.enemyMax = 26 + d;
        L.neutrals = 7 + L.planets / 2; L.aiGrace = 12;
        L.nrMin = .014f; L.nrMax = .05f; L.nsMin = .05f; L.nsMax = .11f;
        L.boldShift = -0.015f * (map - 1);
        L.asteroidHits = true;
        return L;
    }
}
