package com.planetconquest.game.engine.model;

/** Một viên đá đang bay (tới hành tinh hoặc tới một vị trí trống) hoặc đang chờ ở vị trí trống. */
public final class Rock extends Body {
    public float s;          // tốc độ danh định
    public float ph;         // pha dao động khi đang chờ
    public float dist;       // quãng đường đã bay (dùng cho giới hạn tầm bay)
    public Planet t;         // đích là hành tinh (null nếu đích là vị trí trống)
    public float ptx, pty;   // đích là vị trí trống
    public boolean feed, idle;

    /** Tọa độ đích hiện tại: hành tinh nếu có, ngược lại vị trí trống. */
    public float targetX() { return t != null ? t.x : ptx; }
    public float targetY() { return t != null ? t.y : pty; }
}
