package com.planetconquest.game.engine.model;

/**
 * Một hành tinh: chủ sở hữu, đá canh gác, giáp, cấp, vị trí. Máu = số đá + giáp; đòn đánh trừ giáp trước.
 * Trạng thái chỉ đổi qua các phương thức bên dưới để luật (bị chiếm về cấp 1, lên cấp được thêm giáp...) nằm một chỗ.
 */
public final class Planet {
    private final int id;
    private int owner, rocks, level = 1, upgradeProgress, armor;
    private final float nx, ny, size;           // vị trí chuẩn hóa và cỡ (theo unit)
    private float x, y, base;                   // vị trí và bán kính gốc theo pixel
    private float productionAcc, cooldown, reveal;
    private boolean captured;                   // đã từng bị chiếm: tối đa cấp 4
    public final AiState ai = new AiState();
    public final PlanetVisual visual = new PlanetVisual();

    public Planet(int id, int owner, int rocks, int armor, float nx, float ny, float size) {
        this.id = id; this.owner = owner; this.rocks = rocks; this.armor = armor;
        this.nx = nx; this.ny = ny; this.size = size;
    }

    // ---- Truy vấn ----
    public int id() { return id; }
    public int owner() { return owner; }
    public int rocks() { return rocks; }
    public int level() { return level; }
    public int upgradeProgress() { return upgradeProgress; }
    public int armor() { return armor; }
    public boolean captured() { return captured; }
    public float nx() { return nx; }
    public float ny() { return ny; }
    public float size() { return size; }
    public float x() { return x; }
    public float y() { return y; }
    public float base() { return base; }
    public float productionAcc() { return productionAcc; }
    public float cooldown() { return cooldown; }
    public float reveal() { return reveal; }

    /** Bán kính hiện tại: lớn dần theo cấp. */
    public float radius() { return base * (1 + 0.08f * (level - 1)); }

    /** Máu = số đá + giáp. */
    public int hp() { return rocks + armor; }

    // ---- Bố cục ----
    /** Đặt vị trí và bán kính gốc theo pixel (gọi lại khi đổi kích thước màn hình). */
    public void place(float x, float y, float base) { this.x = x; this.y = y; this.base = base; }

    // ---- Đá, giáp, cấp ----
    public void setRocks(int n) { rocks = n; }
    public void addRocks(int delta) { rocks += delta; }
    public void setArmor(int n) { armor = n; }
    public void setLevel(int n) { level = n; }
    public void setOwner(int owner) { this.owner = owner; }

    /** Nhận một đòn: trừ giáp trước, hết giáp mới mất đá. Trả về false nếu không còn gì để trừ. */
    public boolean takeHit() {
        if (armor > 0) armor--;
        else if (rocks > 0) rocks--;
        else return false;
        return true;
    }

    /** Thêm một đơn vị tiến độ nâng cấp; trả về tiến độ mới. */
    public int addUpgradeProgress() { return ++upgradeProgress; }

    /** Lên một cấp: xóa tiến độ, thêm giáp. */
    public void levelUp(int armorBonus) { upgradeProgress = 0; level++; armor += armorBonus; }

    /** Bị phe khác chiếm: về cấp 1, mất hết đá và giáp; từ nay có giới hạn cấp thấp hơn. */
    public void capturedBy(int newOwner) {
        owner = newOwner; upgradeProgress = 0; productionAcc = 0; armor = 0; rocks = 0;
        level = 1; captured = true;
    }

    // ---- Sinh đá, nạp lại, lộ diện ----
    /** Tích lũy sinh đá theo tốc độ rate trong dt giây, tối đa cap đá. */
    public void produce(float rate, float dt, int cap) {
        if (rocks < cap && rate > 0) {
            productionAcc += rate * dt;
            while (productionAcc >= 1) {
                productionAcc -= 1; rocks++;
                if (rocks >= cap) { productionAcc = 0; break; }
            }
        } else productionAcc = 0;
    }

    public void setProductionAcc(float v) { productionAcc = v; }

    public void startCooldown(float seconds) { cooldown = seconds; }
    public void tickCooldown(float dt) { if (cooldown > 0) cooldown = Math.max(0, cooldown - dt); }

    public void revealFor(float seconds) { reveal = seconds; }
    public void tickReveal(float dt) { if (reveal > 0) reveal = Math.max(0, reveal - dt); }
}
