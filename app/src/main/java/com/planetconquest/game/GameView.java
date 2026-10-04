package com.planetconquest.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.planetconquest.game.audio.Sfx;
import com.planetconquest.game.audio.Sound;
import com.planetconquest.game.data.LanguageStore;
import com.planetconquest.game.data.ProgressStore;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.input.Pointer;
import com.planetconquest.game.R;
import com.planetconquest.game.engine.EndReason;
import com.planetconquest.game.engine.GameEvent;
import com.planetconquest.game.engine.Haptic;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.screen.Screen;
import com.planetconquest.game.screen.ScreenHost;
import com.planetconquest.game.screen.Screens;
import com.planetconquest.game.session.GameSession;
import com.planetconquest.game.text.Language;
import com.planetconquest.game.text.Texts;
import com.planetconquest.game.ui.DrawKit;
import com.planetconquest.game.ui.StarField;
import com.planetconquest.game.ui.UiButton;

import java.util.List;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.ui.Palette.C_INK;

/**
 * View duy nhất của game: vòng đời, vòng lặp khung hình, chuyển cảm ứng/vẽ cho Screen đang hiện, thông báo nổi.
 * Mọi nội dung từng màn hình nằm trong package screen. Vòng lặp chỉ chạy khi màn hiện tại cần (Welcome, đang chơi).
 * Tạm dừng = gỡ hẳn callback khung hình: không cập nhật logic, không đếm giờ, không vẽ lại (trừ khi bấm nút).
 */
public final class GameView extends View implements Choreographer.FrameCallback, Engine.Listener, ScreenHost {
    private final float dp;
    private final DrawKit kit;
    private final StarField stars;
    private final Engine eng = new Engine();
    private final Sfx sfx;
    private final ProgressStore progress;
    private final LanguageStore languages;
    private final Texts texts;
    private final GameSession session;
    private final Screens screens;
    private Screen current;

    private UiButton pressed;
    private boolean loopOn, hostResumed;
    private long lastNanos;
    private String toastMsg;
    private long toastUntil;

    public GameView(Context ctx, Sfx sfx, ProgressStore progress, LanguageStore languages) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        this.sfx = sfx;
        this.progress = progress;
        this.languages = languages;
        texts = new Texts(ctx, languages.get());
        kit = new DrawKit(dp);
        stars = new StarField(kit);
        session = new GameSession(eng, progress);
        screens = new Screens(this);
        current = screens.welcome();
        eng.setListener(this);
        setHapticFeedbackEnabled(true);
        setKeepScreenOn(true);
    }

    // ================= ScreenHost =================
    @Override public DrawKit kit() { return kit; }
    @Override public Engine engine() { return eng; }
    @Override public GameSession session() { return session; }
    @Override public ProgressStore progress() { return progress; }
    @Override public Sfx sfx() { return sfx; }
    @Override public Texts texts() { return texts; }
    @Override public Screens screens() { return screens; }
    @Override public Language language() { return texts.language(); }

    @Override
    public void cycleLanguage() {
        Language next = texts.language().next();
        languages.set(next);
        texts.setLanguage(next);
        current.layout();
        invalidate();
    }

    @Override
    public void go(Screen next) {
        if (pressed != null) { pressed.pressed = false; pressed = null; }
        current = next;
        current.enter();
        updateLoop();
        syncMusic();
    }

    @Override
    public void toast(String m) {
        toastMsg = m;
        toastUntil = SystemClock.uptimeMillis() + 1600;
        if (!loopOn) { invalidate(); postInvalidateDelayed(1700); }
    }

    // ================= Vòng đời & vòng lặp =================
    public void onHostPause() {
        hostResumed = false;
        sfx.onHostPause();
        eng.cancelPointer();
        current.onHostPause();                  // đang chơi thì tự tạm dừng
        updateLoop();
    }

    public void onHostResume() {
        hostResumed = true;
        sfx.onHostResume();
        syncMusic();
        lastNanos = 0;
        updateLoop();
    }

    private void updateLoop() {
        boolean want = hostResumed && current.needsLoop();
        Choreographer ch = Choreographer.getInstance();
        if (want && !loopOn) { loopOn = true; lastNanos = 0; ch.postFrameCallback(this); }
        else if (!want && loopOn) { loopOn = false; ch.removeFrameCallback(this); }
        invalidate();
    }

    @Override
    public void doFrame(long now) {
        if (!loopOn) return;
        float dt = lastNanos == 0 ? 1 / 60f : (now - lastNanos) / 1e9f;
        lastNanos = now;
        if (dt <= 0) dt = 1 / 60f;
        if (dt > .05f) dt = .05f;
        current.update(dt);
        invalidate();
        if (loopOn) Choreographer.getInstance().postFrameCallback(this);
    }

    /** Chọn nhạc nền theo màn hình hiện tại (cũng gọi lúc mở app, vì màn Welcome không đi qua go()). */
    private void syncMusic() {
        sfx.music(current.musicTrack());
        sfx.duck(current.duckMusic());
    }

    /** Nút Back của Android. Trả về false để thoát app. */
    public boolean onBack() { return current.onBack(); }

    // ================= Sự kiện từ engine =================
    @Override public void onNotice(Notice n) { toast(texts.notice(n)); }

    @Override
    public void onHaptic(Haptic kind) {
        performHapticFeedback(kind == Haptic.HEAVY ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.VIRTUAL_KEY);
    }

    @Override
    public void onEvent(GameEvent ev) {
        sfx.onEngineEvent(ev);
        int step = session.onEvent(ev);
        if (step > 0) toast(texts.s(R.string.intro_next, step));
    }

    @Override
    public void onFinish(boolean win, EndReason reason) {
        sfx.play(win ? Sound.WIN : Sound.LOSE);
        GameSession.EndInfo info = session.finish(win, reason);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        go(screens.end(info));
    }

    // ================= Kích thước, cảm ứng, vẽ =================
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        kit.setSize(w, h);
        eng.setSize(w, h, dp);
        stars.build();
        current.layout();
    }

    private UiButton findButton(float x, float y) {
        List<UiButton> buttons = current.buttons();
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UiButton b = buttons.get(i);
            if (b.contains(x, y, b.style == UiButton.Style.ICON ? 8 * dp : 0)) return b;
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        if (a == MotionEvent.ACTION_POINTER_DOWN || a == MotionEvent.ACTION_POINTER_UP) return true;   // chỉ dùng một ngón
        float x = ev.getX(0), y = ev.getY(0);
        switch (a) {
            case MotionEvent.ACTION_DOWN:
                pressed = findButton(x, y);
                if (pressed != null) { pressed.pressed = true; invalidate(); return true; }
                current.onDown(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed != null) {
                    boolean in = pressed.contains(x, y, 12 * dp);
                    if (in != pressed.pressed) { pressed.pressed = in; invalidate(); }
                    return true;
                }
                current.onMove(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed != null) {
                    UiButton b = pressed;
                    pressed = null;
                    boolean fire = b.pressed;
                    b.pressed = false;
                    invalidate();
                    if (fire) { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); sfx.play(Sound.CLICK); b.click(); }
                    return true;
                }
                current.onUp(x, y);
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (pressed != null) { pressed.pressed = false; pressed = null; }
                current.onCancel();
                invalidate();
                return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        stars.draw(c, current.animTime());
        current.draw(c);
        for (UiButton b : current.buttons()) kit.drawButton(c, b);
        drawToast(c);
    }

    private void drawToast(Canvas c) {
        long now = SystemClock.uptimeMillis();
        if (toastMsg == null || now > toastUntil) return;
        float a = Math.min(1, (toastUntil - now) / 250f);
        kit.txt.setTextSize(13.5f * dp); kit.txt.setTypeface(kit.reg);
        float w = kit.txt.measureText(toastMsg) + 28 * dp, h = 36 * dp;
        float y = current.toastTop();
        kit.tmp.set((kit.w - w) / 2, y, (kit.w + w) / 2, y + h);
        kit.fill.setColor(alpha(0xFF080A1A, .94f * a)); c.drawRoundRect(kit.tmp, 10 * dp, 10 * dp, kit.fill);
        kit.stroke.setColor(alpha(0xFFA0AFFF, .25f * a)); kit.stroke.setStrokeWidth(dp); c.drawRoundRect(kit.tmp, 10 * dp, 10 * dp, kit.stroke);
        kit.text(c, toastMsg, kit.w / 2, y + h / 2, 13.5f * dp, alpha(C_INK, a), kit.reg, Paint.Align.CENTER);
    }
}
