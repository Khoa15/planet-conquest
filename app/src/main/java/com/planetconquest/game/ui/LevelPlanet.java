package com.planetconquest.game.ui;

/** Hành tinh đại diện cho từng màn trên bản đồ chọn màn: mỗi màn một màu và một đặc điểm riêng (thứ tự = chỉ số màn). */
public enum LevelPlanet {
    SPROUT(0xFF9BE564),     // 0 Hướng dẫn
    OCEAN(0xFF3D6BFF),      // 1 Làm quen
    CARGO(0xFFC98A3C),      // 2 Kho nhỏ: vành đai chứa
    DESERT(0xFFF2E86D),     // 3 Mùa khô
    SPEED(0xFFFF4D5E),      // 4 Chạy đua
    RANGE(0xFFA46BFF),      // 5 Tay ngắn: vòng tầm bay
    FOG(0xFF9FB0C8),        // 6 Sương mù
    BELT(0xFF8A6A55),       // 7 Bãi thiên thạch
    RELOAD(0xFFFF8FD8),     // 8 Nạp đạn
    SURGE(0xFF00E5FF),      // 9 Đối thủ tăng tốc
    VOYAGE(0xFF2EC4B6),     // 10 Viễn chinh: hành tinh nhỏ, đoàn đá đi chậm tới hành tinh xa
    BLACK_HOLE(0xFFFFB347); // Endless: hố đen, màu là màu đĩa bồi tụ

    public final int color;

    LevelPlanet(int color) { this.color = color; }

    /** Hành tinh của màn index; index &lt; 0 (Endless) là hố đen. */
    public static LevelPlanet forLevel(int index) {
        LevelPlanet[] all = values();
        return index < 0 || index >= all.length - 1 ? BLACK_HOLE : all[index];
    }
}
