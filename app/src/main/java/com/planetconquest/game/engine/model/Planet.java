package com.planetconquest.game.engine.model;

/** Một hành tinh: chủ sở hữu, đá canh gác, giáp, cấp, vị trí. Máu = số đá + giáp; đòn đánh trừ giáp trước. */
public final class Planet {
    public int id, owner, rocks, level = 1, upgradeProgress, armor;
    public float nx, ny, size;           // vị trí chuẩn hóa và cỡ (theo unit)
    public float x, y, base;             // vị trí và bán kính gốc theo pixel
    public float productionAcc, cooldown, reveal, flash, seed;
    public boolean captured;             // đã từng bị chiếm: tối đa cấp 4
    public final AiState ai = new AiState();

    /** Bán kính hiện tại: lớn dần theo cấp. */
    public float radius() { return base * (1 + 0.08f * (level - 1)); }

    /** Máu = số đá + giáp. */
    public int hp() { return rocks + armor; }

    /** Nhận một đòn: trừ giáp trước, hết giáp mới mất đá. Trả về false nếu không còn gì để trừ. */
    public boolean takeHit() {
        if (armor > 0) armor--;
        else if (rocks > 0) rocks--;
        else return false;
        return true;
    }
}
