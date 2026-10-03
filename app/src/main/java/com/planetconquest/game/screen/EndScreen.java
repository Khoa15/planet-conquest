package com.planetconquest.game.screen;

import android.graphics.Canvas;

import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.engine.Levels;
import com.planetconquest.game.session.GameSession;
import com.planetconquest.game.ui.Dialog;
import com.planetconquest.game.ui.UiButton;

import static com.planetconquest.game.ui.Palette.*;

/** Hộp thoại kết thúc màn (thắng/thua) phủ lên màn chơi đã dừng. */
final class EndScreen extends BaseScreen {
    private final PlayScreen play;
    private final GameSession.EndInfo info;
    private final Dialog dialog;

    EndScreen(ScreenHost host, PlayScreen play, GameSession.EndInfo info) {
        super(host);
        this.play = play;
        this.info = info;
        dialog = new Dialog(kit);
    }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        dialog.begin(info.title, info.win ? C_YOU : C_DANGER, info.text);
        if (info.next != null) dialog.button(info.next, UiButton.Style.PRIMARY, new Runnable() {
            @Override public void run() { next(); }
        });
        if (info.again) dialog.button("Chơi lại", info.next == null ? UiButton.Style.PRIMARY : UiButton.Style.SECONDARY, new Runnable() {
            @Override public void run() { host.session().again(); host.go(play); }
        });
        dialog.button("Chọn màn", UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { host.go(host.screens().levels()); }
        });
        dialog.layout();
        buttons.addAll(dialog.buttons());
    }

    private void next() {
        GameSession s = host.session();
        if (s.isEndless()) { s.nextEndlessMap(); host.go(play); }
        else if (s.curLevel() < Levels.CAMPAIGN_LAST) host.go(host.screens().brief(s.curLevel() + 1));
        else host.go(host.screens().brief(-1));
    }

    @Override
    public void draw(Canvas c) {
        play.drawWorld(c, false);
        dim(c);
        dialog.draw(c);
    }

    @Override public boolean onBack() { host.go(host.screens().levels()); return true; }
    @Override public MusicTrack musicTrack() { return MusicTrack.NONE; }
}
