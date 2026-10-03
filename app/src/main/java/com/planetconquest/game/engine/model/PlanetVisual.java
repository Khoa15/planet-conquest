package com.planetconquest.game.engine.model;

/** Trạng thái thuần hình ảnh của hành tinh: vòng sáng khi có biến cố và pha dải mây. Không ảnh hưởng luật chơi. */
public final class PlanetVisual {
    /** Độ sáng của vòng nháy (1 → 0). */
    public float flash;
    /** Pha ngẫu nhiên của dải mây và quỹ đạo, cố định cho mỗi hành tinh. */
    public float seed;

    /** Vòng nháy mờ dần. */
    public void fade(float dt) { flash = Math.max(0, flash - dt * 1.4f); }
}
