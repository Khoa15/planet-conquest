package com.planetconquest.game.engine.model;

/** Vật thể tròn có thể va chạm: đá của các phe (o >= 0) hoặc thiên thạch trung lập (o = -1). */
public class Body {
    public float x, y, vx, vy, rad;
    public int owner;
    public boolean dead;
}
