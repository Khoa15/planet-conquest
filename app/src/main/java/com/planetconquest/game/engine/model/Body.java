package com.planetconquest.game.engine.model;

/**
 * Vật thể tròn có thể va chạm: đá của các phe (owner >= 0) hoặc thiên thạch trung lập (Faction.NEUTRAL).
 * Cố ý để field công khai: đây là hạt mô phỏng sinh ra hàng trăm lần mỗi giây và chỉ Engine sở hữu, giao diện chỉ đọc
 * qua Engine.rocks()/neutrals(). Các hành tinh thì đã đóng gói đầy đủ (xem Planet).
 */
public class Body {
    public float x, y, vx, vy, rad;
    public int owner;
    public boolean dead;
}
