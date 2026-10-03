package com.planetconquest.game.screen;

import com.planetconquest.game.audio.MusicTrack;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.ui.Painter;
import com.planetconquest.game.ui.UiButton;

import java.util.ArrayList;
import java.util.List;

/** Mặc định cho các Screen: không cảm ứng riêng, không vòng lặp, nhạc menu. */
abstract class BaseScreen extends Painter implements Screen {
    protected final ScreenHost host;
    protected final Engine eng;
    protected final ArrayList<UiButton> buttons = new ArrayList<UiButton>();

    protected BaseScreen(ScreenHost host) {
        super(host.kit());
        this.host = host;
        this.eng = host.engine();
    }

    @Override public void enter() { layout(); }
    @Override public void update(float dt) { }
    @Override public List<UiButton> buttons() { return buttons; }
    @Override public void onDown(float x, float y) { }
    @Override public void onMove(float x, float y) { }
    @Override public void onUp(float x, float y) { }
    @Override public void onCancel() { }
    @Override public boolean onBack() { return false; }
    @Override public void onHostPause() { }
    @Override public boolean needsLoop() { return false; }
    @Override public MusicTrack musicTrack() { return MusicTrack.MENU; }
    @Override public boolean duckMusic() { return false; }
    @Override public float animTime() { return eng.clock; }
    @Override public float toastTop() { return 90 * dp; }

    protected final UiButton add(UiButton b) { buttons.add(b); return b; }

    protected final String soundLabel() { return host.sfx().isOn() ? "Âm thanh: Bật" : "Âm thanh: Tắt"; }

    protected final void toggleSound() {
        host.sfx().toggle();
        layout();
        host.invalidate();
    }

    protected final void startLevelAndPlay(int idx) {
        host.session().startLevel(idx);
        host.go(host.screens().play());
    }
}
