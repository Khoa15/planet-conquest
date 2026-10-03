package com.planetconquest.game.screen;

import com.planetconquest.game.audio.Sfx;
import com.planetconquest.game.data.ProgressStore;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.session.GameSession;
import com.planetconquest.game.text.Texts;
import com.planetconquest.game.ui.DrawKit;

/** Những gì một Screen cần từ nơi chứa nó (GameView). Screen chỉ phụ thuộc interface này, không phụ thuộc View. */
public interface ScreenHost {
    DrawKit kit();
    Engine engine();
    GameSession session();
    ProgressStore progress();
    Sfx sfx();
    Texts texts();
    Screens screens();

    void go(Screen next);
    void toast(String message);
    void invalidate();
}
