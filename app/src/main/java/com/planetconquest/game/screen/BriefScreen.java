package com.planetconquest.game.screen;

import com.planetconquest.game.R;
import android.graphics.Canvas;

import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.level.Levels;
import com.planetconquest.game.ui.Dialog;
import com.planetconquest.game.ui.UiButton;

import static com.planetconquest.game.ui.Palette.*;

/** Màn mô tả một màn chơi (hạn chế và cách vượt qua) trước khi bắt đầu. index &lt; 0 là Endless. */
final class BriefScreen extends BaseScreen {
    private final int index;
    private final Dialog dialog;

    BriefScreen(ScreenHost host, int index) {
        super(host);
        this.index = index;
        dialog = new Dialog(kit);
    }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        dialog.begin(index < 0 ? tx.levelName(-1) : tx.s(R.string.level_label, index, tx.levelName(index)),
                index < 0 ? C_GOLD : C_INK,
                index < 0 ? tx.s(R.string.brief_endless_sub) : tx.s(R.string.brief_sub, Levels.ALL[index].planets));
        dialog.section(tx.s(R.string.section_limit), tx.levelLimit(index), C_LIMIT);
        dialog.section(tx.s(index < 0 ? R.string.section_note : R.string.section_howto), tx.levelTip(index), C_INK);
        dialog.button(tx.s(R.string.btn_start), UiButton.Style.PRIMARY, new Runnable() {
            @Override public void run() {
                if (index < 0) host.session().newEndlessRun();
                else host.session().startLevel(index);
                host.go(host.screens().play());
            }
        });
        dialog.button(tx.s(R.string.btn_back), UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { host.go(host.screens().levels()); }
        });
        dialog.layout();
        buttons.addAll(dialog.buttons());
    }

    @Override public void draw(Canvas c) { dialog.draw(c); }
    @Override public boolean onBack() { host.go(host.screens().levels()); return true; }
}
