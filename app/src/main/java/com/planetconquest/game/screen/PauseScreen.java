package com.planetconquest.game.screen;

import android.graphics.Canvas;

import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.ui.Dialog;
import com.planetconquest.game.ui.UiButton;

import static com.planetconquest.game.ui.Palette.*;

/** Hộp thoại Tạm dừng phủ lên màn chơi đang đứng yên; không có vòng lặp khung hình. */
final class PauseScreen extends BaseScreen {
    private final PlayScreen play;
    private final Dialog dialog;

    PauseScreen(ScreenHost host, PlayScreen play) {
        super(host);
        this.play = play;
        dialog = new Dialog(kit);
    }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        dialog.begin("Tạm dừng", C_INK, host.session().levelLabel());
        Level L = eng.lvl;
        if (L.endless) dialog.section("Lưu ý", L.tip, C_INK);
        else if (!L.intro) dialog.section("Hạn chế", L.limit, C_LIMIT);
        dialog.button("Tiếp tục", UiButton.Style.PRIMARY, new Runnable() {
            @Override public void run() { host.go(play); }
        });
        dialog.button("Chơi lại", UiButton.Style.SECONDARY, new Runnable() {
            @Override public void run() { host.session().restart(); host.go(play); }
        });
        dialog.button(soundLabel(), UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { toggleSound(); }
        });
        dialog.button("Thoát ra menu", UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { host.go(host.screens().levels()); }
        });
        dialog.layout();
        buttons.addAll(dialog.buttons());
    }

    @Override
    public void draw(Canvas c) {
        play.drawWorld(c, false);
        play.drawHud(c, false);
        dim(c);
        dialog.draw(c);
    }

    @Override public boolean onBack() { host.go(play); return true; }
    @Override public MusicTrack musicTrack() { return MusicTrack.GAME; }
    @Override public boolean duckMusic() { return true; }
}
