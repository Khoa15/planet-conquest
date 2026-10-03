package com.planetconquest.game.engine;

/** Vật thể tròn có thể va chạm: đá của các phe (o >= 0) hoặc thiên thạch trung lập (o = -1). */
public class Body {
    public float x, y, vx, vy, rad;
    public int o;
    public boolean dead;
    int gi; // chỉ số tạm trong lưới va chạm
}
