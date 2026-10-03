package com.planetconquest.game.engine.rules;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Planet;

/**
 * Một hạn chế (hoặc biến thể luật) của màn chơi. Mặc định không làm gì; mỗi lớp con ghi đè đúng điểm nó muốn thay đổi.
 * Thêm một hạn chế mới = thêm một lớp con, không phải sửa Engine (OCP).
 */
public abstract class LevelRule {
    /** Cấp tối đa của hành tinh. */
    public int maxLevel(Planet p, int base) { return base; }
    /** Sức chứa đá của hành tinh. */
    public int capacity(Planet p, int base) { return base; }
    /** Tốc độ sinh đá mỗi giây. */
    public float productionRate(Planet p, float base) { return base; }
    /** Lý do không cho nâng cấp ở màn này, hoặc null. */
    public Notice upgradeBlocked() { return null; }
    /** Lý do không cho gửi quân từ src tới (dx, dy), hoặc null. */
    public Notice canLaunch(Engine eng, Planet src, float dx, float dy) { return null; }
    /** Sau khi một đợt quân rời src. */
    public void onLaunch(Planet src) { }
    /** Đá của phe attacker vừa va vào hành tinh target thuộc phe khác. */
    public void onHit(Planet target, int attackerOwner) { }
    /** Che số đá và máu của hành tinh. */
    public boolean hidesInfo(Planet p) { return false; }
    /** Tầm bay tối đa theo đơn vị unit của màn hình; 0 = không giới hạn. */
    public float rangeFraction() { return 0; }
    /** Thiên thạch đâm vào hành tinh. */
    public boolean asteroidsHitPlanets() { return false; }
    /** Giới hạn thời gian (giây); 0 = không giới hạn. */
    public int timeLimit() { return 0; }
    /** Số đá là hữu hạn (không sinh thêm): hết đá là thua và AI giữ lại ít đá hơn. */
    public boolean finiteRocks() { return false; }
    /** Thời gian nạp lại giữa hai lần gửi (giây); 0 = không có. */
    public float cooldownSeconds() { return 0; }
}
