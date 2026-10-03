package com.planetconquest.game.screen;

import android.graphics.Canvas;

import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.level.Levels;
import com.planetconquest.game.ui.Dialog;
import com.planetconquest.game.ui.UiButton;

import java.util.Random;

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
        if (index < 0) {
            dialog.begin("Endless", C_GOLD, "Bản đồ ngẫu nhiên 3–10 hành tinh. Qua một bản đồ thì bản đồ tiếp theo xuất hiện, khó hơn một chút. Thua là kết thúc.");
            Level sample = Levels.endless(1, new Random());
            dialog.section("Hạn chế", sample.limit, C_LIMIT);
            dialog.section("Lưu ý", sample.tip, C_INK);
        } else {
            Level L = Levels.ALL[index];
            dialog.begin("Màn " + index + " · " + L.name, C_INK, L.planets + " hành tinh. Chiếm hết để thắng.");
            dialog.section("Hạn chế", L.limit, C_LIMIT);
            dialog.section("Cách vượt qua", L.tip, C_INK);
        }
        dialog.button("Bắt đầu", UiButton.Style.PRIMARY, new Runnable() {
            @Override public void run() {
                if (index < 0) host.session().newEndlessRun();
                else host.session().startLevel(index);
                host.go(host.screens().play());
            }
        });
        dialog.button("Quay lại", UiButton.Style.GHOST, new Runnable() {
            @Override public void run() { host.go(host.screens().levels()); }
        });
        dialog.layout();
        buttons.addAll(dialog.buttons());
    }

    @Override public void draw(Canvas c) { dialog.draw(c); }
    @Override public boolean onBack() { host.go(host.screens().levels()); return true; }
}
