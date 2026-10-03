package com.planetconquest.game.screen;

import com.planetconquest.game.R;
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
        dialog.begin(tx.s(R.string.pause_title), C_INK, tx.levelLabel(host.session().curLevel(), host.session().endlessMap()));
        Level L = eng.level();
        int idx = host.session().curLevel();
        if (L.endless) dialog.section(tx.s(R.string.section_note), tx.levelTip(idx), C_INK);
        else if (!L.intro) dialog.section(tx.s(R.string.section_limit), tx.levelLimit(idx), C_LIMIT);
        dialog.button(tx.s(R.string.btn_resume), UiButton.Style.PRIMARY, new Runnable() {
            @Override public void run() { host.go(play); }
        });
        dialog.button(tx.s(R.string.btn_restart), UiButton.Style.SECONDARY, new Runnable() {
            @Override public void run() { host.session().restart(); host.go(play); }
        });
        dialog.button(soundLabel(), UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { toggleSound(); }
        });
        dialog.button(tx.s(R.string.btn_quit_menu), UiButton.Style.GHOST, new Runnable() {
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
