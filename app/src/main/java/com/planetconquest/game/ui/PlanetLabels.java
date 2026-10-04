package com.planetconquest.game.ui;

import com.planetconquest.game.text.Texts;

/** Bộ nhớ đệm chuỗi nhãn hành tinh (số đá, cấp, máu) để khỏi nối/đổi chuỗi mới mỗi khung hình. */
final class PlanetLabels {
    private static final int MAX_NUM = 1000, MAX_LEVEL = 8;

    private final Texts tx;
    private final String[] nums = new String[MAX_NUM];
    private final String[][] levels = new String[MAX_LEVEL + 1][MAX_LEVEL + 1];
    private final String[] hps = new String[MAX_NUM];
    private String levelPrefix, hpPrefix;

    PlanetLabels(Texts tx) { this.tx = tx; }

    String num(int n) {
        if (n < 0 || n >= MAX_NUM) return String.valueOf(n);
        String s = nums[n];
        return s != null ? s : (nums[n] = String.valueOf(n));
    }

    String level(int lv, int max) {
        syncLanguage();
        if (lv < 0 || lv > MAX_LEVEL || max < 0 || max > MAX_LEVEL) return tx.levelPrefix + lv + "/" + max;
        String s = levels[lv][max];
        return s != null ? s : (levels[lv][max] = tx.levelPrefix + lv + "/" + max);
    }

    String hp(int hp) {
        syncLanguage();
        if (hp < 0 || hp >= MAX_NUM) return tx.hpPrefix + hp;
        String s = hps[hp];
        return s != null ? s : (hps[hp] = tx.hpPrefix + hp);
    }

    /** Đổi ngôn ngữ thì tiền tố đổi theo: xoá các chuỗi đã ghép. */
    private void syncLanguage() {
        if (tx.levelPrefix == levelPrefix && tx.hpPrefix == hpPrefix) return;
        levelPrefix = tx.levelPrefix;
        hpPrefix = tx.hpPrefix;
        for (String[] row : levels) java.util.Arrays.fill(row, null);
        java.util.Arrays.fill(hps, null);
    }
}
