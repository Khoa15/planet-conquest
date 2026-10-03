package com.planetconquest.game.engine.model;

/** Một viên đá đang bay (tới hành tinh hoặc tới một vị trí trống) hoặc đang chờ ở vị trí trống. */
public final class Rock extends Body {
    public float s;          // tốc độ danh định
    public float ph;         // pha dao động khi đang chờ
    public float dist;       // quãng đường đã bay (dùng cho giới hạn tầm bay)
    private Planet planetTarget;   // đích là hành tinh (null nếu đích là vị trí trống)
    private float ptx, pty;        // đích là vị trí trống
    public boolean feed, idle;

    /** Hành tinh đích, hoặc null nếu đang bay tới một vị trí trống. */
    public Planet targetPlanet() { return planetTarget; }
    public void setTarget(Planet p) { planetTarget = p; }
    public void setTargetPoint(float x, float y) { planetTarget = null; ptx = x; pty = y; }

    /** Tọa độ đích hiện tại: hành tinh nếu có, ngược lại vị trí trống. */
    public float targetX() { return planetTarget != null ? planetTarget.x() : ptx; }
    public float targetY() { return planetTarget != null ? planetTarget.y() : pty; }
}
