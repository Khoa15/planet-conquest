package com.planetconquest.game.engine.model;

import com.planetconquest.game.engine.util.ColorUtil;

/** Phe: chỉ số 0 là người chơi, 1..9 là đối thủ AI, -1 là trung lập (thiên thạch). Mỗi phe có một màu. */
public final class Faction {
    private Faction() {}

    public static final int PLAYER = 0, NEUTRAL = -1;

    public static final int[] COLORS = {
            0xFF4FF0B4, 0xFFFF6B7D, 0xFFFFB347, 0xFFA46BFF, 0xFF5CC8FF,
            0xFFFF8FD8, 0xFFF2E86D, 0xFF9BE564, 0xFFC9B79C, 0xFF6F8BFF};
    public static final int[] LIGHT = new int[COLORS.length];
    public static final int[] DARK = new int[COLORS.length];
    static {
        for (int i = 0; i < COLORS.length; i++) {
            LIGHT[i] = ColorUtil.mix(COLORS[i], 0xFFFFFFFF, .5f);
            DARK[i] = ColorUtil.mix(COLORS[i], 0xFF000000, .62f);
        }
    }

    public static boolean isPlayer(int owner) { return owner == PLAYER; }
}
