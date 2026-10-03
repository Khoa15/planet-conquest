package com.planetconquest.game.engine.model;

public final class Planet {
    public int id, owner, rocks, level = 1, upgradeProgress, armor;
    public float nx, ny, size;           // vị trí chuẩn hóa và cỡ (theo unit)
    public float x, y, base;             // vị trí và bán kính gốc theo pixel
    public float productionAcc, cooldown, reveal, flash, think, bold, seed;
    public boolean captured;             // đã từng bị chiếm: tối đa cấp 4
}
