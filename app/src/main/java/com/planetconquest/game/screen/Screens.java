package com.planetconquest.game.screen;

import com.planetconquest.game.session.GameSession;

/** Nơi tạo và giữ các màn hình dùng lại được (Welcome, Chọn màn, Chơi); các màn còn lại tạo theo yêu cầu. */
public final class Screens {
    private final WelcomeScreen welcome;
    private final LevelSelectScreen levels;
    private final PlayScreen play;
    private final ScreenHost host;

    public Screens(ScreenHost host) {
        this.host = host;
        welcome = new WelcomeScreen(host);
        levels = new LevelSelectScreen(host);
        play = new PlayScreen(host);
    }

    public Screen welcome() { return welcome; }
    public Screen levels() { return levels; }
    public PlayScreen play() { return play; }
    /** Màn mô tả: index &lt; 0 là Endless. */
    public Screen brief(int index) { return new BriefScreen(host, index); }
    public Screen pause() { return new PauseScreen(host, play); }
    public Screen end(GameSession.EndInfo info) { return new EndScreen(host, play, info); }
}
