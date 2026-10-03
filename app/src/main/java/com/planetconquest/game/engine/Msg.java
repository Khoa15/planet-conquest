package com.planetconquest.game.engine;

/** Mã thông báo engine gửi ra giao diện (toast, chữ bay). Giao diện tra mã này ra chuỗi theo ngôn ngữ. */
public enum Msg {
    NO_UPGRADE_IN_LEVEL,        // Màn này không cho nâng cấp
    CAPTURED_MAX_LEVEL,         // args: cấp tối đa của hành tinh bị chiếm
    MAX_LEVEL_REACHED,
    COOLDOWN,                   // args: số giây còn phải chờ
    OUT_OF_RANGE,
    PLANET_LEVEL_UP,            // args: cấp mới, tốc độ sinh đá/giây, sức chứa, giáp thêm
    PLANET_CAPTURED,
    PLANET_LOST,
    ASTEROID_DAMAGE,            // args: sát thương
    LASSO_EMPTY,
    LASSO_TOO_SMALL,
    PLANET_OUT_OF_ROCKS,
    NO_ROCKS_TO_SEND
}
