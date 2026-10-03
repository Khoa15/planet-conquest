package com.planetconquest.game.engine;

/** Một thông báo: mã {@link Msg} cùng tham số. Bất biến. */
public final class Notice {
    public final Msg msg;
    public final Object[] args;

    public Notice(Msg msg, Object... args) {
        this.msg = msg;
        this.args = args;
    }
}
