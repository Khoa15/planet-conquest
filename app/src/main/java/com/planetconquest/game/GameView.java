package com.planetconquest.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.planetconquest.game.engine.Asteroid;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.FloatText;
import com.planetconquest.game.engine.Level;
import com.planetconquest.game.engine.Levels;
import com.planetconquest.game.engine.Particle;
import com.planetconquest.game.engine.Planet;
import com.planetconquest.game.engine.Rock;
import com.planetconquest.game.engine.Selection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * View duy nhất của game: vòng lặp khung hình, các màn hình giao diện, vẽ bằng Canvas và nhận cảm ứng.
 * Vòng lặp chỉ chạy ở màn Welcome (hoạt ảnh) và khi đang chơi. Tạm dừng = gỡ hẳn callback khung hình:
 * không cập nhật logic, không đếm giờ, không vẽ lại (trừ khi bấm nút).
 */
public final class GameView extends View implements Choreographer.FrameCallback, Engine.Listener {

    static final String VERSION = "0.1.0";

    static final int S_WELCOME = 0, S_LEVELS = 1, S_BRIEF = 2, S_PLAY = 3, S_PAUSE = 4, S_END = 5;
    static final int B_PLAY = 1, B_TUTORIAL = 2, B_BACK = 3, B_GO = 4, B_PAUSE = 5, B_RESUME = 6, B_RESTART = 7,
            B_QUIT = 8, B_NEXT = 9, B_AGAIN = 10, B_MENU = 11, B_CANCEL_SEL = 12;

    static final int C_BG = 0xFF05070F, C_INK = 0xFFE9EDFF, C_MUTED = 0xFF8F99C8, C_PANEL = 0xE00E122A,
            C_LINE = 0x40A0AFFF, C_YOU = 0xFF4FF0B4, C_GOLD = 0xFFFFD166, C_DANGER = 0xFFFF6B7D, C_LIMIT = 0xFFFFB0B0,
            C_FAR = 0xFFFF9B6B;
    static final float TAU = Engine.TAU;

    static final String[] INTRO_STEPS = {
            "Chạm giữ hành tinh xanh của bạn, kéo sang hành tinh đỏ rồi thả tay. Một nửa số đá sẽ bay đi tấn công.",
            "Khoanh một vòng quanh những viên đá đang quay quanh hành tinh của bạn để chọn chúng.",
            "Chạm vào vòng sáng ở giữa màn hình: đá đã chọn bay tới và chờ ở đó. Đá khác phe va vào nhau sẽ cùng vỡ.",
            "Chạm vào hành tinh của bạn để nâng cấp: sinh đá nhanh hơn, chứa nhiều hơn và được thêm giáp.",
            "Máu = số đá + giáp. Tiếp tục gửi đá vào hành tinh đỏ cho tới khi chiếm được nó."
    };
    static final int[] INTRO_EV = {Engine.EV_ATTACK, Engine.EV_LASSO, Engine.EV_POINT, Engine.EV_UPGRADE, Engine.EV_CAPTURE};

    private final Engine eng = new Engine();
    private final float dp;
    private float W, H;
    private final SharedPreferences prefs;
    private final HashSet<Integer> done = new HashSet<Integer>();
    private int best;
    private boolean introDone;
    private final Random rnd = new Random();

    private int screen = S_WELCOME;
    private int curLevel = 0;                   // chỉ số trong Levels.ALL; -1 = Endless
    private int briefLevel = 1;                 // -1 = Endless
    private Level endlessLevel;
    private int endlessMap = 1, endlessCleared = 0, introStep = 0;
    private String endTitle = "", endText = "", endNext;
    private boolean endWin, endAgain;

    private final ArrayList<UiButton> buttons = new ArrayList<UiButton>();
    private UiButton pressed, cancelSelBtn;

    private float scroll, scrollMax, downY, scrollAtDown;
    private boolean scrolling;
    private int downCard = -1;

    private boolean loopOn, hostResumed;
    private long lastNanos;
    private float welcomeT;

    private String toastMsg;
    private long toastUntil;
    private float hudBottom;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fm = new Paint.FontMetrics();
    private final Path path = new Path();
    private final RectF tmp = new RectF();
    private Bitmap bg;
    private float[] tw = new float[0];
    private final float[] dotX = new float[64], dotY = new float[64], rings = new float[16];
    private final int[] FC = Engine.FACTION_COLORS, FL = Engine.FACTION_LIGHT;
    private final int[] FD;
    private final Typeface tfReg, tfBold, tfTitle;
    private final ArrayList<Planet> tmpPlanets = new ArrayList<Planet>();

    // Hộp thoại dùng chung cho Brief / Tạm dừng / Kết thúc
    private String dTitle, dSub;
    private int dTitleColor;
    private final ArrayList<String> dLabels = new ArrayList<String>(), dTexts = new ArrayList<String>();
    private final ArrayList<Integer> dColors = new ArrayList<Integer>();
    private final ArrayList<UiButton> dBtns = new ArrayList<UiButton>();
    private final ArrayList<String> dSubLines = new ArrayList<String>(), dTitleLines = new ArrayList<String>();
    private final ArrayList<ArrayList<String>> dSecLines = new ArrayList<ArrayList<String>>();
    private final RectF dCard = new RectF();

    public GameView(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        prefs = ctx.getSharedPreferences("planet_conquest", Context.MODE_PRIVATE);
        loadProgress();
        tfReg = Typeface.create("sans-serif", Typeface.NORMAL);
        tfBold = Typeface.create("sans-serif", Typeface.BOLD);
        tfTitle = Typeface.create("sans-serif-black", Typeface.NORMAL);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        FD = new int[FC.length];
        for (int i = 0; i < FC.length; i++) FD[i] = Engine.mixColor(FC[i], 0xFF000000, .62f);
        eng.setListener(this);
        setHapticFeedbackEnabled(true);
        setKeepScreenOn(true);
    }

    // ================= Lưu tiến độ =================
    private void loadProgress() {
        done.clear();
        for (String p : prefs.getString("done", "").split(",")) {
            if (p.length() == 0) continue;
            try { done.add(Integer.parseInt(p)); } catch (NumberFormatException ignored) { }
        }
        best = prefs.getInt("best", 0);
        introDone = prefs.getBoolean("introDone", false);
    }

    private void saveProgress() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : done) { if (sb.length() > 0) sb.append(','); sb.append(i); }
        prefs.edit().putString("done", sb.toString()).putInt("best", best).putBoolean("introDone", introDone).apply();
    }

    // ================= Vòng đời & vòng lặp =================
    public void onHostPause() {
        hostResumed = false;
        eng.cancelPointer();
        if (screen == S_PLAY) setScreen(S_PAUSE);   // rời app khi đang chơi: tự tạm dừng
        else updateLoop();
    }

    public void onHostResume() {
        hostResumed = true;
        lastNanos = 0;
        updateLoop();
    }

    private void updateLoop() {
        boolean want = hostResumed && (screen == S_WELCOME || screen == S_PLAY);
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
        if (screen == S_PLAY) {
            eng.step(dt);                       // có thể kết thúc màn và đổi sang S_END
            if (screen == S_PLAY) eng.fx(dt);
        } else if (screen == S_WELCOME) {
            welcomeT += dt;
        }
        invalidate();
        if (loopOn) Choreographer.getInstance().postFrameCallback(this);
    }

    private void setScreen(int s) {
        screen = s;
        if (pressed != null) { pressed.pressed = false; pressed = null; }
        layoutUi();
        updateLoop();
    }

    /** Nút Back của Android. Trả về false để thoát app ở màn Welcome. */
    public boolean onBack() {
        switch (screen) {
            case S_PLAY: setScreen(S_PAUSE); return true;
            case S_PAUSE: setScreen(S_PLAY); return true;
            case S_BRIEF: case S_END: setScreen(S_LEVELS); return true;
            case S_LEVELS: setScreen(S_WELCOME); return true;
            default: return false;
        }
    }

    // ================= Điều hướng =================
    private void onButton(int id) {
        switch (id) {
            case B_PLAY: setScreen(S_LEVELS); break;
            case B_TUTORIAL: startLevel(0); break;
            case B_BACK: setScreen(screen == S_LEVELS ? S_WELCOME : S_LEVELS); break;
            case B_GO:
                if (briefLevel < 0) { endlessMap = 1; endlessCleared = 0; startEndless(); }
                else startLevel(briefLevel);
                break;
            case B_PAUSE: eng.cancelPointer(); setScreen(S_PAUSE); break;
            case B_RESUME: setScreen(S_PLAY); break;
            case B_RESTART:
                if (curLevel < 0) { introStep = 0; eng.start(endlessLevel); setScreen(S_PLAY); }
                else startLevel(curLevel);
                break;
            case B_QUIT: case B_MENU: setScreen(S_LEVELS); break;
            case B_NEXT:
                if (curLevel < 0) { endlessMap++; startEndless(); }
                else if (curLevel < Levels.CAMPAIGN_LAST) openBrief(curLevel + 1);
                else openBrief(-1);
                break;
            case B_AGAIN:
                if (curLevel < 0) { endlessMap = 1; endlessCleared = 0; startEndless(); }
                else startLevel(curLevel);
                break;
            case B_CANCEL_SEL: eng.cancelSelection(); break;
        }
    }

    private void startLevel(int idx) {
        curLevel = idx; introStep = 0;
        eng.start(Levels.ALL[idx]);
        setScreen(S_PLAY);
    }

    private void startEndless() {
        curLevel = -1; introStep = 0;
        endlessLevel = Levels.endless(endlessMap, rnd);
        eng.start(endlessLevel);
        setScreen(S_PLAY);
    }

    private void openBrief(int idx) {
        briefLevel = idx;
        setScreen(S_BRIEF);
    }

    private void openCard(int i) {
        if (i == 0) startLevel(0);
        else if (i < Levels.ALL.length) openBrief(i);
        else openBrief(-1);
    }

    private String levelLabel() {
        if (curLevel < 0) return "Endless · Bản đồ " + endlessMap;
        if (curLevel == 0) return "Hướng dẫn";
        return "Màn " + curLevel + " · " + Levels.ALL[curLevel].name;
    }

    // ================= Sự kiện từ engine =================
    @Override public void onToast(String m) { toast(m); }

    @Override
    public void onHaptic(int kind) {
        performHapticFeedback(kind == Engine.H_HEAVY ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.VIRTUAL_KEY);
    }

    @Override
    public void onEvent(int ev) {
        if (!eng.lvl.intro || introStep >= INTRO_EV.length || ev != INTRO_EV[introStep]) return;
        introStep++;
        if (introStep < INTRO_EV.length) toast("Tốt lắm! Sang bước " + (introStep + 1));
    }

    @Override
    public void onFinish(boolean win, String reason) {
        Level L = eng.lvl;
        endWin = win; endAgain = true; endNext = null;
        String t = fmtTime(eng.time);
        if (L.endless) {
            if (win) {
                endlessCleared = endlessMap;
                if (endlessCleared > best) best = endlessCleared;
                endTitle = "Qua bản đồ " + endlessMap;
                endText = "Đã chiếm " + eng.planets.size() + " hành tinh sau " + t + ". Bản đồ tiếp theo ngẫu nhiên và khó hơn một chút. Kỷ lục: " + best + " bản đồ.";
                endNext = "Bản đồ tiếp theo"; endAgain = false;
            } else {
                endTitle = "Kết thúc hành trình";
                endText = reason + " Bạn đã vượt " + endlessCleared + " bản đồ. Kỷ lục: " + best + ".";
            }
        } else if (L.intro) {
            if (win) {
                introDone = true;
                endTitle = "Hoàn thành hướng dẫn";
                endText = "Bạn đã nắm đủ cách chơi. Mỗi màn tiếp theo có một hạn chế riêng để vượt qua.";
                endNext = "Vào màn 1";
            } else { endTitle = "Thử lại nhé"; endText = reason; }
        } else if (win) {
            done.add(curLevel);
            endTitle = "Chiến thắng";
            endText = "Đã chiếm " + eng.planets.size() + " hành tinh sau " + t + ", dù hạn chế: " + L.limit.toLowerCase();
            endNext = curLevel < Levels.CAMPAIGN_LAST ? "Màn tiếp theo" : "Thử Endless";
        } else {
            endTitle = "Thất bại"; endText = reason;
        }
        saveProgress();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        setScreen(S_END);
    }

    private void toast(String m) {
        toastMsg = m;
        toastUntil = SystemClock.uptimeMillis() + 1600;
        if (!loopOn) { invalidate(); postInvalidateDelayed(1700); }
    }

    static String fmtTime(float s) {
        int m = (int) (s / 60), sec = (int) (s % 60);
        return m + ":" + (sec < 10 ? "0" : "") + sec;
    }

    // ================= Bố cục giao diện =================
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        W = w; H = h;
        eng.setSize(w, h, dp);
        buildBg();
        layoutUi();
    }

    private UiButton add(UiButton b) { buttons.add(b); return b; }

    private void layoutUi() {
        buttons.clear();
        if (W <= 0) return;
        float bw = Math.min(W - 48 * dp, 420 * dp), bx = (W - bw) / 2;
        switch (screen) {
            case S_WELCOME:
                add(new UiButton(B_PLAY, "Chơi", UiButton.PRIMARY, 0)).at(bx, H - 190 * dp, bx + bw, H - 134 * dp);
                add(new UiButton(B_TUTORIAL, "Hướng dẫn", UiButton.SECONDARY, 0)).at(bx, H - 122 * dp, bx + bw, H - 66 * dp);
                break;
            case S_LEVELS:
                add(new UiButton(B_BACK, null, UiButton.ICON, UiButton.I_BACK)).at(16 * dp, 18 * dp, 60 * dp, 62 * dp);
                scrollMax = Math.max(0, cardTop() + (Levels.ALL.length + 1) * (cardH() + 10 * dp) + 24 * dp - H);
                scroll = Math.max(0, Math.min(scroll, scrollMax));
                break;
            case S_BRIEF: buildBrief(); break;
            case S_PLAY:
                add(new UiButton(B_PAUSE, null, UiButton.ICON, UiButton.I_PAUSE)).at(W - 56 * dp, 9 * dp, W - 12 * dp, 53 * dp);
                cancelSelBtn = add(new UiButton(B_CANCEL_SEL, "Hủy", UiButton.SMALL, 0));
                cancelSelBtn.visible = false;
                break;
            case S_PAUSE: buildPause(); break;
            case S_END: buildEnd(); break;
        }
    }

    private void buildBrief() {
        if (briefLevel < 0) {
            dialogBegin("Endless", C_GOLD, "Bản đồ ngẫu nhiên 3–10 hành tinh. Qua một bản đồ thì bản đồ tiếp theo xuất hiện, khó hơn một chút. Thua là kết thúc.");
            Level sample = Levels.endless(1, rnd);
            dialogSection("Hạn chế", sample.limit, C_LIMIT);
            dialogSection("Lưu ý", sample.tip, C_INK);
        } else {
            Level L = Levels.ALL[briefLevel];
            dialogBegin("Màn " + briefLevel + " · " + L.name, C_INK, L.planets + " hành tinh. Chiếm hết để thắng.");
            dialogSection("Hạn chế", L.limit, C_LIMIT);
            dialogSection("Cách vượt qua", L.tip, C_INK);
        }
        dialogButton(B_GO, "Bắt đầu", UiButton.PRIMARY);
        dialogButton(B_BACK, "Quay lại", UiButton.GHOST);
        dialogLayout();
    }

    private void buildPause() {
        dialogBegin("Tạm dừng", C_INK, levelLabel());
        Level L = eng.lvl;
        if (L.endless) dialogSection("Lưu ý", L.tip, C_INK);
        else if (!L.intro) dialogSection("Hạn chế", L.limit, C_LIMIT);
        dialogButton(B_RESUME, "Tiếp tục", UiButton.PRIMARY);
        dialogButton(B_RESTART, "Chơi lại", UiButton.SECONDARY);
        dialogButton(B_QUIT, "Thoát ra menu", UiButton.GHOST);
        dialogLayout();
    }

    private void buildEnd() {
        dialogBegin(endTitle, endWin ? C_YOU : C_DANGER, endText);
        if (endNext != null) dialogButton(B_NEXT, endNext, UiButton.PRIMARY);
        if (endAgain) dialogButton(B_AGAIN, "Chơi lại", endNext == null ? UiButton.PRIMARY : UiButton.SECONDARY);
        dialogButton(B_MENU, "Chọn màn", UiButton.GHOST);
        dialogLayout();
    }

    private void dialogBegin(String title, int color, String sub) {
        dTitle = title; dTitleColor = color; dSub = sub;
        dLabels.clear(); dTexts.clear(); dColors.clear(); dBtns.clear();
    }

    private void dialogSection(String label, String text, int color) { dLabels.add(label); dTexts.add(text); dColors.add(color); }

    private void dialogButton(int id, String label, int style) { dBtns.add(add(new UiButton(id, label, style, 0))); }

    private void dialogLayout() {
        float cw = Math.min(W - 32 * dp, 420 * dp), pad = 22 * dp, inner = cw - 2 * pad;
        dTitleLines.clear();
        dTitleLines.addAll(wrap(dTitle, 23 * dp, tfTitle, inner));
        dSubLines.clear();
        if (dSub != null) dSubLines.addAll(wrap(dSub, 14.5f * dp, tfReg, inner));
        dSecLines.clear();
        for (String s : dTexts) dSecLines.add(wrap(s, 15.5f * dp, tfReg, inner));
        float h = pad + dTitleLines.size() * 31 * dp;
        if (!dSubLines.isEmpty()) h += 4 * dp + dSubLines.size() * 21 * dp;
        for (ArrayList<String> ls : dSecLines) h += 14 * dp + 18 * dp + ls.size() * 23 * dp;
        int n = dBtns.size();
        float bh = 52 * dp, gap = 10 * dp, bblock = n * bh + (n - 1) * gap;
        h += 22 * dp + bblock + pad;
        float top = Math.max(16 * dp, (H - h) / 2);
        dCard.set((W - cw) / 2, top, (W + cw) / 2, top + h);
        float by = dCard.bottom - pad - bblock;
        for (UiButton b : dBtns) { b.at(dCard.left + pad, by, dCard.right - pad, by + bh); by += bh + gap; }
    }

    // ================= Cảm ứng =================
    private UiButton findButton(float x, float y) {
        for (int i = buttons.size() - 1; i >= 0; i--) {
            UiButton b = buttons.get(i);
            if (b.contains(x, y, b.style == UiButton.ICON ? 8 * dp : 0)) return b;
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
                if (screen == S_PLAY) eng.down(x, y);
                else if (screen == S_LEVELS) { scrolling = false; downY = y; scrollAtDown = scroll; downCard = cardAt(x, y); }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed != null) {
                    boolean in = pressed.contains(x, y, 12 * dp);
                    if (in != pressed.pressed) { pressed.pressed = in; invalidate(); }
                    return true;
                }
                if (screen == S_PLAY) eng.move(x, y);
                else if (screen == S_LEVELS) {
                    if (!scrolling && Math.abs(y - downY) > 8 * dp) scrolling = true;
                    if (scrolling) { scroll = Math.max(0, Math.min(scrollMax, scrollAtDown - (y - downY))); invalidate(); }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed != null) {
                    UiButton b = pressed;
                    pressed = null;
                    boolean fire = b.pressed;
                    b.pressed = false;
                    invalidate();
                    if (fire) { performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onButton(b.id); }
                    return true;
                }
                if (screen == S_PLAY) eng.up(x, y);
                else if (screen == S_LEVELS && !scrolling && downCard >= 0 && cardAt(x, y) == downCard) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    openCard(downCard);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (pressed != null) { pressed.pressed = false; pressed = null; }
                eng.cancelPointer();
                invalidate();
                return true;
        }
        return true;
    }

    // ================= Vẽ: tiện ích =================
    static int alpha(int c, float a) { return (c & 0x00FFFFFF) | (((int) (Math.max(0, Math.min(1, a)) * 255)) << 24); }
    static float sin(float a) { return (float) Math.sin(a); }
    static float cos(float a) { return (float) Math.cos(a); }

    private void text(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align al) {
        txt.setTextSize(size); txt.setColor(color); txt.setTypeface(tf); txt.setTextAlign(al);
        txt.getFontMetrics(fm);
        c.drawText(s, x, y - (fm.ascent + fm.descent) / 2, txt);
    }

    private ArrayList<String> wrap(String s, float size, Typeface tf, float maxW) {
        txt.setTextSize(size); txt.setTypeface(tf);
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String w : s.split(" ")) {
            String cand = line.length() == 0 ? w : line + " " + w;
            if (txt.measureText(cand) <= maxW || line.length() == 0) { line.setLength(0); line.append(cand); }
            else { out.add(line.toString()); line.setLength(0); line.append(w); }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private void panel(Canvas c, RectF r, float rad, int strokeColor) {
        fill.setColor(C_PANEL); c.drawRoundRect(r, rad, rad, fill);
        stroke.setColor(strokeColor); stroke.setStrokeWidth(dp); c.drawRoundRect(r, rad, rad, stroke);
    }

    private void pill(Canvas c, float x, float y, String s, int col) {
        txt.setTextSize(12.5f * dp); txt.setTypeface(tfBold);
        float w = txt.measureText(s) + 20 * dp, h = 26 * dp;
        float px = Math.max(6 * dp, Math.min(x - w / 2, W - w - 6 * dp)), py = Math.max(y - h / 2, 6 * dp);
        tmp.set(px, py, px + w, py + h);
        fill.setColor(0xE6080A1A); c.drawRoundRect(tmp, h / 2, h / 2, fill);
        stroke.setColor(col); stroke.setStrokeWidth(1.5f * dp); c.drawRoundRect(tmp, h / 2, h / 2, stroke);
        text(c, s, px + w / 2, py + h / 2, 12.5f * dp, 0xFFFFFFFF, tfBold, Paint.Align.CENTER);
    }

    private void dashed(boolean on, float phase) {
        stroke.setPathEffect(on ? new DashPathEffect(new float[]{7 * dp, 7 * dp}, 14 * dp - (phase * 50 * dp) % (14 * dp)) : null);
    }

    private void buildBg() {
        if (W <= 0 || H <= 0) return;
        if (bg != null) bg.recycle();
        bg = Bitmap.createBitmap((int) W, (int) H, Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(bg);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, H, 0xFF070A1C, 0xFF04050D, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, W, H, p);
        float[][] neb = {{.2f, .25f, .55f}, {.88f, .62f, .6f}, {.4f, .95f, .5f}};
        int[] nc = {0x5A3CC8, 0x288CC8, 0xC8468C};
        for (int i = 0; i < 3; i++) {
            p.setShader(new RadialGradient(neb[i][0] * W, neb[i][1] * H, neb[i][2] * Math.max(W, H), 0x26000000 | nc[i], nc[i], Shader.TileMode.CLAMP));
            g.drawRect(0, 0, W, H, p);
        }
        p.setShader(null);
        Random r = new Random(11);
        for (int i = 0; i < 170; i++) {
            p.setColor(Color.argb((int) ((.2f + r.nextFloat() * .6f) * 255), 255, 255, 255));
            g.drawCircle(r.nextFloat() * W, r.nextFloat() * H, (r.nextFloat() * 1.1f + .2f) * dp, p);
        }
        tw = new float[28 * 4];
        for (int i = 0; i < 28; i++) { tw[i * 4] = r.nextFloat() * W; tw[i * 4 + 1] = r.nextFloat() * H; tw[i * 4 + 2] = r.nextFloat() * TAU; tw[i * 4 + 3] = .8f + r.nextFloat() * 1.4f; }
    }

    // ================= Vẽ: khung chính =================
    @Override
    protected void onDraw(Canvas c) {
        if (bg != null) c.drawBitmap(bg, 0, 0, null); else c.drawColor(C_BG);
        float t = screen == S_WELCOME ? welcomeT : eng.clock;
        for (int i = 0; i < tw.length; i += 4) {
            fill.setColor(alpha(0xFFFFFFFF, .25f + .35f * sin(t * tw[i + 3] + tw[i + 2])));
            c.drawRect(tw[i], tw[i + 1], tw[i] + 1.6f * dp, tw[i + 1] + 1.6f * dp, fill);
        }
        switch (screen) {
            case S_WELCOME: drawWelcome(c); break;
            case S_LEVELS: drawLevels(c); break;
            case S_BRIEF: drawDialog(c); break;
            case S_PLAY: drawWorld(c); drawPlayHud(c); break;
            case S_PAUSE: drawWorld(c); drawPlayHud(c); dim(c); drawDialog(c); break;
            case S_END: drawWorld(c); dim(c); drawDialog(c); break;
        }
        for (UiButton b : buttons) drawButton(c, b);
        drawToast(c);
    }

    private void dim(Canvas c) { fill.setColor(0xC8030510); c.drawRect(0, 0, W, H, fill); }

    private void drawToast(Canvas c) {
        long now = SystemClock.uptimeMillis();
        if (toastMsg == null || now > toastUntil) return;
        float a = Math.min(1, (toastUntil - now) / 250f);
        txt.setTextSize(13.5f * dp); txt.setTypeface(tfReg);
        float w = txt.measureText(toastMsg) + 28 * dp, h = 36 * dp;
        float y = screen == S_PLAY ? hudBottom + 10 * dp : 90 * dp;
        tmp.set((W - w) / 2, y, (W + w) / 2, y + h);
        fill.setColor(alpha(0xFF080A1A, .94f * a)); c.drawRoundRect(tmp, 10 * dp, 10 * dp, fill);
        stroke.setColor(alpha(0xFFA0AFFF, .25f * a)); stroke.setStrokeWidth(dp); c.drawRoundRect(tmp, 10 * dp, 10 * dp, stroke);
        text(c, toastMsg, W / 2, y + h / 2, 13.5f * dp, alpha(C_INK, a), tfReg, Paint.Align.CENTER);
    }

    // ================= Nút chuẩn =================
    private void drawButton(Canvas c, UiButton b) {
        if (!b.visible) return;
        RectF r = b.r;
        float rad = b.style == UiButton.ICON ? 14 * dp : Math.min(r.height() / 2, 16 * dp);
        c.save();
        if (b.pressed) c.scale(.96f, .96f, r.centerX(), r.centerY());
        int fillC, textC, strokeC = 0;
        switch (b.style) {
            case UiButton.PRIMARY: fillC = b.pressed ? Engine.mixColor(C_YOU, 0xFF000000, .18f) : C_YOU; textC = 0xFF04170F; break;
            case UiButton.SECONDARY: fillC = b.pressed ? 0x2EFFFFFF : 0x14FFFFFF; textC = C_INK; strokeC = 0x55A0AFFF; break;
            case UiButton.GHOST: fillC = b.pressed ? 0x1AFFFFFF : 0; textC = C_MUTED; break;
            case UiButton.SMALL: fillC = b.pressed ? Engine.mixColor(C_GOLD, 0xFF000000, .18f) : C_GOLD; textC = 0xFF241A00; break;
            default: fillC = b.pressed ? 0xF01E2650 : C_PANEL; textC = C_INK; strokeC = C_LINE; break;
        }
        if (fillC != 0) { fill.setColor(fillC); c.drawRoundRect(r, rad, rad, fill); }
        if (strokeC != 0) { stroke.setColor(strokeC); stroke.setStrokeWidth(1.2f * dp); c.drawRoundRect(r, rad, rad, stroke); }
        if (b.style == UiButton.ICON) drawIcon(c, b.icon, r.centerX(), r.centerY(), textC);
        else text(c, b.label, r.centerX(), r.centerY(), (b.style == UiButton.SMALL ? 13.5f : 16.5f) * dp, textC, tfBold, Paint.Align.CENTER);
        c.restore();
    }

    private void drawIcon(Canvas c, int icon, float cx, float cy, int col) {
        if (icon == UiButton.I_PAUSE) {
            fill.setColor(col);
            tmp.set(cx - 7 * dp, cy - 8 * dp, cx - 2.5f * dp, cy + 8 * dp); c.drawRoundRect(tmp, 1.5f * dp, 1.5f * dp, fill);
            tmp.set(cx + 2.5f * dp, cy - 8 * dp, cx + 7 * dp, cy + 8 * dp); c.drawRoundRect(tmp, 1.5f * dp, 1.5f * dp, fill);
        } else if (icon == UiButton.I_BACK) {
            stroke.setColor(col); stroke.setStrokeWidth(2.6f * dp);
            path.reset(); path.moveTo(cx + 4 * dp, cy - 8 * dp); path.lineTo(cx - 4 * dp, cy); path.lineTo(cx + 4 * dp, cy + 8 * dp);
            c.drawPath(path, stroke);
        }
    }

    // ================= Màn Welcome =================
    private void drawWelcome(Canvas c) {
        float t = welcomeT, cx = W / 2, cy = H * .5f, R = Math.min(W, H) * .15f;
        float ex = W * .2f, ey = H * .32f, er = R * .42f;
        drawPlanetBody(c, ex, ey, er, 1, 1.3f);
        drawPlanetBody(c, W * .82f, H * .66f, R * .36f, 2, 2.1f);
        stroke.setStrokeWidth(2.6f * dp);
        for (int i = 0; i < 8; i++) {                       // một dòng đá bay từ hành tinh của bạn sang đối thủ
            float u = (t * .3f + i * .055f) % 1f, mx = (cx + ex) / 2 + 40 * dp, my = (cy + ey) / 2;
            float x = (1 - u) * (1 - u) * cx + 2 * (1 - u) * u * mx + u * u * ex;
            float y = (1 - u) * (1 - u) * cy + 2 * (1 - u) * u * my + u * u * ey;
            fill.setColor(alpha(FL[0], Math.min(1, (1 - u) * 3)));
            c.drawCircle(x, y, 2.6f * dp, fill);
        }
        drawPlanetBody(c, cx, cy, R, 0, .4f);
        int[] cnt = {12, 18, 24};
        float[] spd = {.6f, -.4f, .28f};
        for (int k = 0; k < 3; k++) {
            float rad = R + (14 + 10 * k) * dp;
            stroke.setColor(alpha(C_YOU, .12f)); stroke.setStrokeWidth(dp); c.drawCircle(cx, cy, rad, stroke);
            fill.setColor(alpha(C_YOU, .92f));
            for (int i = 0; i < cnt[k]; i++) {
                float a = t * spd[k] + i * TAU / cnt[k];
                c.drawCircle(cx + cos(a) * rad, cy + sin(a) * rad, 2.1f * dp, fill);
            }
        }
        float ty = Math.max(80 * dp, H * .13f);
        text(c, "Planet", W / 2, ty, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, "Conquest", W / 2, ty + 48 * dp, 44 * dp, C_INK, tfTitle, Paint.Align.CENTER);
        text(c, "Chinh phục thiên hà bằng một ngón tay", W / 2, ty + 90 * dp, 14.5f * dp, C_MUTED, tfReg, Paint.Align.CENTER);
        if (best > 0) text(c, "Kỷ lục Endless: " + best + " bản đồ", W / 2, H - 44 * dp, 12.5f * dp, C_MUTED, tfReg, Paint.Align.CENTER);
        text(c, "Phiên bản " + VERSION, W / 2, H - 24 * dp, 11.5f * dp, alpha(C_MUTED, .7f), tfReg, Paint.Align.CENTER);
    }

    // ================= Màn chọn màn =================
    private float cardTop() { return 84 * dp; }
    private float cardH() { return 82 * dp; }

    private void cardRect(int i, RectF out) {
        float y = cardTop() + i * (cardH() + 10 * dp) - scroll;
        out.set(16 * dp, y, W - 16 * dp, y + cardH());
    }

    private int cardAt(float x, float y) {
        if (y < cardTop() - 4 * dp) return -1;
        RectF r = new RectF();
        for (int i = 0; i <= Levels.ALL.length; i++) { cardRect(i, r); if (r.contains(x, y)) return i; }
        return -1;
    }

    private void drawLevels(Canvas c) {
        RectF r = new RectF();
        c.save();
        c.clipRect(0, cardTop() - 6 * dp, W, H);
        int n = Levels.ALL.length;
        for (int i = 0; i <= n; i++) {
            cardRect(i, r);
            if (r.bottom < cardTop() - 10 * dp || r.top > H) continue;
            boolean endless = i == n, intro = i == 0, isDone = !endless && (intro ? introDone : done.contains(i));
            fill.setColor(0x0FFFFFFF); c.drawRoundRect(r, 16 * dp, 16 * dp, fill);
            stroke.setColor(C_LINE); stroke.setStrokeWidth(dp); c.drawRoundRect(r, 16 * dp, 16 * dp, stroke);
            float ccx = r.left + 34 * dp, ccy = r.centerY();
            fill.setColor(endless || intro ? C_GOLD : (isDone ? 0xFF27325C : C_YOU));
            c.drawCircle(ccx, ccy, 18 * dp, fill);
            String num = endless ? "∞" : intro ? "?" : String.valueOf(i);
            text(c, num, ccx, ccy, (endless ? 19 : 16) * dp, isDone && !intro ? C_YOU : 0xFF141400, tfBold, Paint.Align.CENTER);
            String name = endless ? "Endless" : Levels.ALL[i].name;
            String desc = endless ? "Bản đồ ngẫu nhiên, không hạn chế. Thiên thạch đâm vào hành tinh."
                    : intro ? "Học cách chơi: bạn và một đối thủ, bạn có nhiều đá hơn." : Levels.ALL[i].limit;
            String right = endless ? (best > 0 ? "Kỷ lục " + best : "3–10 hành tinh") : Levels.ALL[i].planets + " hành tinh";
            float tx = r.left + 64 * dp;
            text(c, name, tx, r.top + 24 * dp, 16 * dp, C_INK, tfBold, Paint.Align.LEFT);
            if (isDone) {
                txt.setTextSize(16 * dp); txt.setTypeface(tfBold);
                text(c, intro ? "Đã học" : "Đã qua", tx + txt.measureText(name) + 10 * dp, r.top + 24 * dp, 12 * dp, C_YOU, tfBold, Paint.Align.LEFT);
            }
            text(c, right, r.right - 14 * dp, r.top + 24 * dp, 12 * dp, C_MUTED, tfReg, Paint.Align.RIGHT);
            ArrayList<String> ls = wrap(desc, 12.5f * dp, tfReg, r.right - tx - 14 * dp);
            for (int k = 0; k < Math.min(2, ls.size()); k++) {
                String s = ls.get(k);
                if (k == 1 && ls.size() > 2) s = s + "…";
                text(c, s, tx, r.top + 47 * dp + k * 17 * dp, 12.5f * dp, C_MUTED, tfReg, Paint.Align.LEFT);
            }
        }
        c.restore();
        text(c, "Chọn màn", 74 * dp, 40 * dp, 22 * dp, C_INK, tfBold, Paint.Align.LEFT);
    }

    // ================= Hộp thoại =================
    private void drawDialog(Canvas c) {
        RectF r = dCard;
        fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xF71A2046, 0xF70B0E22, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, 22 * dp, 22 * dp, fill);
        fill.setShader(null);
        stroke.setColor(C_LINE); stroke.setStrokeWidth(dp); c.drawRoundRect(r, 22 * dp, 22 * dp, stroke);
        float pad = 22 * dp, x = r.left + pad, y = r.top + pad;
        for (String s : dTitleLines) { text(c, s, x, y + 14 * dp, 23 * dp, dTitleColor, tfTitle, Paint.Align.LEFT); y += 31 * dp; }
        if (!dSubLines.isEmpty()) {
            y += 4 * dp;
            for (String s : dSubLines) { text(c, s, x, y + 10 * dp, 14.5f * dp, C_MUTED, tfReg, Paint.Align.LEFT); y += 21 * dp; }
        }
        for (int i = 0; i < dLabels.size(); i++) {
            y += 14 * dp;
            text(c, dLabels.get(i), x, y + 8 * dp, 12 * dp, C_MUTED, tfBold, Paint.Align.LEFT);
            y += 18 * dp;
            for (String s : dSecLines.get(i)) { text(c, s, x, y + 11 * dp, 15.5f * dp, dColors.get(i), tfReg, Paint.Align.LEFT); y += 23 * dp; }
        }
    }

    // ================= HUD khi chơi =================
    private float chip(Canvas c, float x, float y, String label, String val, int vcol) {
        float ls = 11.5f * dp, vs = 16.5f * dp;
        txt.setTypeface(tfReg); txt.setTextSize(ls);
        float lw = txt.measureText(label);
        txt.setTypeface(tfBold); txt.setTextSize(vs);
        float vw = txt.measureText(val);
        float w = 10 * dp + lw + 6 * dp + vw + 10 * dp, h = 38 * dp;
        if (x + w > W - 64 * dp && x > 12 * dp) { x = 12 * dp; y += 44 * dp; }
        tmp.set(x, y, x + w, y + h);
        panel(c, tmp, 12 * dp, C_LINE);
        text(c, label, x + 10 * dp, y + h / 2 + dp, ls, C_MUTED, tfReg, Paint.Align.LEFT);
        text(c, val, x + 10 * dp + lw + 6 * dp, y + h / 2, vs, vcol, tfBold, Paint.Align.LEFT);
        hudBottom = Math.max(hudBottom, y + h);
        chipY = y;
        return x + w;
    }
    private float chipY;

    private void drawPlayHud(Canvas c) {
        hudBottom = 0;
        chipY = 12 * dp;
        float x = 12 * dp;
        x = chip(c, x, chipY, "Hành tinh", eng.playerPlanets() + "/" + eng.planets.size(), C_INK) + 6 * dp;
        x = chip(c, x, chipY, "Đá", String.valueOf(eng.playerRocks()), C_INK) + 6 * dp;
        Level L = eng.lvl;
        if (L.timeLimit > 0) {
            int left = Math.max(0, (int) Math.ceil(L.timeLimit - eng.time));
            chip(c, x, chipY, "Còn", left + "s", left <= 15 ? C_DANGER : C_INK);
        } else if (L.endless) chip(c, x, chipY, "Bản đồ", String.valueOf(endlessMap), C_INK);
        hudBottom = Math.max(hudBottom, 53 * dp);
        if (L.intro && introStep < INTRO_STEPS.length) hudBottom = drawCoach(c, hudBottom + 8 * dp);

        // Thanh vùng chọn: số đá đã chọn + nút Hủy (vùng ngón cái)
        Selection s = eng.selection;
        boolean show = screen == S_PLAY && s != null && (eng.ptr == null || eng.ptr.mode != Engine.M_CARRY);
        if (cancelSelBtn != null) cancelSelBtn.visible = show;
        if (show) {
            String msg = "Đã chọn " + s.total() + " đá. Chạm đích để điều động";
            txt.setTextSize(13.5f * dp); txt.setTypeface(tfReg);
            float tw2 = txt.measureText(msg), bw = 64 * dp, h = 46 * dp, w = 18 * dp + tw2 + 12 * dp + bw + 5 * dp;
            if (w > W - 24 * dp) { msg = "Đã chọn " + s.total() + " đá"; tw2 = txt.measureText(msg); w = 18 * dp + tw2 + 12 * dp + bw + 5 * dp; }
            float bx = (W - w) / 2, by = H - 16 * dp - h;
            tmp.set(bx, by, bx + w, by + h);
            panel(c, tmp, h / 2, C_LINE);
            text(c, msg, bx + 18 * dp, by + h / 2, 13.5f * dp, C_INK, tfReg, Paint.Align.LEFT);
            cancelSelBtn.at(bx + w - 5 * dp - bw, by + 5 * dp, bx + w - 5 * dp, by + h - 5 * dp);
        }
    }

    private float drawCoach(Canvas c, float top) {
        float x = 12 * dp, w = W - 24 * dp, pad = 14 * dp;
        ArrayList<String> lines = wrap(INTRO_STEPS[introStep], 14.5f * dp, tfReg, w - 2 * pad);
        float h = pad + 18 * dp + lines.size() * 21 * dp + pad - 2 * dp;
        tmp.set(x, top, x + w, top + h);
        panel(c, tmp, 16 * dp, alpha(C_GOLD, .45f));
        text(c, "Hướng dẫn · bước " + (introStep + 1) + "/" + INTRO_STEPS.length, x + pad, top + pad + 6 * dp, 12 * dp, C_GOLD, tfBold, Paint.Align.LEFT);
        for (int i = 0; i < INTRO_STEPS.length; i++) {
            fill.setColor(i < introStep ? C_GOLD : (i == introStep ? alpha(C_GOLD, .8f) : alpha(C_MUTED, .4f)));
            c.drawCircle(x + w - pad - (INTRO_STEPS.length - 1 - i) * 12 * dp, top + pad + 6 * dp, (i == introStep ? 4 : 3) * dp, fill);
        }
        float y = top + pad + 18 * dp;
        for (String s : lines) { text(c, s, x + pad, y + 10 * dp, 14.5f * dp, C_INK, tfReg, Paint.Align.LEFT); y += 21 * dp; }
        return top + h;
    }

    private void drawIntroCue(Canvas c) {
        if (!eng.lvl.intro || introStep >= INTRO_STEPS.length || eng.ptr != null || eng.planets.size() < 2) return;
        Planet me = eng.planets.get(0), en = eng.planets.get(1);
        float pulse = .5f + .5f * sin(eng.clock * 4);
        stroke.setColor(alpha(C_GOLD, .55f + .35f * pulse));
        stroke.setStrokeWidth(2.5f * dp);
        switch (introStep) {
            case 0: case 4: {
                if (introStep == 4 && eng.selection != null) break;
                dashed(true, eng.clock);
                float r1 = eng.radiusOf(me) + 12 * dp, r2 = eng.radiusOf(en) + 14 * dp, d = (float) Math.hypot(en.x - me.x, en.y - me.y);
                float ux = (en.x - me.x) / d, uy = (en.y - me.y) / d;
                c.drawLine(me.x + ux * r1, me.y + uy * r1, en.x - ux * r2, en.y - uy * r2, stroke);
                dashed(false, 0);
                break;
            }
            case 1: {
                if (eng.selection != null) break;
                int nr = eng.orbitRings(me, rings);
                float rad = (nr > 0 ? rings[nr - 1] : eng.radiusOf(me)) + 16 * dp;
                dashed(true, eng.clock);
                c.drawCircle(me.x, me.y, rad, stroke);
                dashed(false, 0);
                break;
            }
            case 2: {
                float px = W / 2, py = H * .55f;
                c.drawCircle(px, py, (18 + 6 * pulse) * dp, stroke);
                fill.setColor(alpha(C_GOLD, .25f)); c.drawCircle(px, py, 10 * dp, fill);
                text(c, "Chạm vào đây", px, py + 38 * dp, 12.5f * dp, C_GOLD, tfBold, Paint.Align.CENTER);
                break;
            }
            case 3:
                c.drawCircle(me.x, me.y, eng.radiusOf(me) + (8 + 6 * pulse) * dp, stroke);
                break;
        }
    }

    // ================= Thế giới game =================
    private void drawWorld(Canvas c) {
        eng.updateLive();
        Level L = eng.lvl;
        if (L.range > 0) { tmpPlanets.clear(); for (Planet p : eng.planets) if (p.owner == 0) tmpPlanets.add(p); drawRange(c, tmpPlanets, .16f); }
        for (Planet p : eng.planets) drawPlanet(c, p);
        for (Planet p : eng.planets) drawOrbit(c, p);
        if (screen == S_PLAY && !L.intro && eng.time < 10 && eng.ptr == null && eng.selection == null && !eng.planets.isEmpty()) {
            Planet p = eng.planets.get(0);
            stroke.setColor(alpha(C_YOU, .8f)); stroke.setStrokeWidth(2 * dp);
            dashed(true, eng.clock * .3f);
            c.drawCircle(p.x, p.y, eng.radiusOf(p) + (14 + 3 * sin(eng.clock * 4)) * dp, stroke);
            dashed(false, 0);
        }
        if (screen == S_PLAY) drawIntroCue(c);
        drawNeutrals(c);
        drawRocks(c);
        for (Particle q : eng.parts) {
            fill.setColor(alpha(q.color, Math.max(0, q.life / q.max)));
            c.drawCircle(q.x, q.y, q.size, fill);
        }
        if (screen == S_PLAY) { drawDrag(c); drawSel(c); }
        for (FloatText t : eng.texts) {
            float a = Math.min(1, t.life);
            text(c, t.text, t.x + dp, t.y + dp, 14 * dp, alpha(0xFF000000, .6f * a), tfBold, Paint.Align.CENTER);
            text(c, t.text, t.x, t.y, 14 * dp, alpha(t.color, a), tfBold, Paint.Align.CENTER);
        }
    }

    private void drawRange(Canvas c, ArrayList<Planet> srcs, float a) {
        float rp = eng.rangePx();
        if (Float.isInfinite(rp)) return;
        stroke.setPathEffect(new DashPathEffect(new float[]{3 * dp, 8 * dp}, 0));
        stroke.setStrokeWidth(1.5f * dp);
        stroke.setColor(alpha(C_MUTED, a));
        for (Planet s : srcs) c.drawCircle(s.x, s.y, rp, stroke);
        stroke.setPathEffect(null);
    }

    private void drawPlanetBody(Canvas c, float x, float y, float R, int owner, float seed) {
        int col = FC[owner];
        fill.setShader(new RadialGradient(x, y, R * 2.1f, new int[]{alpha(col, .34f), alpha(col, .34f), alpha(col, 0)}, new float[]{0, .43f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R * 2.1f, fill);
        fill.setShader(new RadialGradient(x - R * .35f, y - R * .4f, R * 1.45f, new int[]{FL[owner], col, FD[owner]}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(x, y, R, fill);
        fill.setShader(null);
        c.save();
        path.reset();
        path.addCircle(x, y, R, Path.Direction.CW);
        c.clipPath(path);
        stroke.setColor(0x24000000);
        stroke.setStrokeWidth(R * .13f);
        float[] bands = {-.4f, .05f, .5f};
        for (float k : bands) {
            path.reset();
            path.moveTo(x - R, y + R * k);
            path.quadTo(x, y + R * k + R * .2f * sin(seed + k * 5), x + R, y + R * k);
            c.drawPath(path, stroke);
        }
        c.restore();
        stroke.setColor(0x47FFFFFF); stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(x, y, R, stroke);
    }

    private void arc(Canvas c, float x, float y, float r, float frac) {
        tmp.set(x - r, y - r, x + r, y + r);
        c.drawArc(tmp, -90, 360 * frac, false, stroke);
    }

    private void drawPlanet(Canvas c, Planet p) {
        float R = eng.radiusOf(p);
        boolean fg = eng.fogged(p);
        drawPlanetBody(c, p.x, p.y, R, p.owner, p.seed);
        if (p.level < eng.maxLvl(p) && !fg) {
            stroke.setStrokeWidth(3 * dp);
            stroke.setColor(alpha(C_GOLD, .16f)); c.drawCircle(p.x, p.y, R + 4 * dp, stroke);
            if (p.upg > 0) { stroke.setColor(C_GOLD); arc(c, p.x, p.y, R + 4 * dp, p.upg / (float) Engine.upgradeCost(p.level)); }
        }
        if (eng.lvl.cooldown > 0 && p.cd > 0) {
            stroke.setStrokeWidth(2.5f * dp); stroke.setColor(0xD98FC8FF);
            arc(c, p.x, p.y, R + 8 * dp, p.cd / eng.lvl.cooldown);
        }
        if (p.flash > 0) {
            stroke.setStrokeWidth(3 * dp); stroke.setColor(alpha(0xFFFFFFFF, p.flash));
            c.drawCircle(p.x, p.y, R * (1 + (1 - p.flash) * 1.3f), stroke);
        }
        String n = fg ? "?" : String.valueOf(p.n);
        float big = Math.max(13 * dp, R * .6f), small = Math.max(8 * dp, R * .27f);
        text(c, n, p.x + dp, p.y - R * .24f + dp, big, 0x73000000, tfBold, Paint.Align.CENTER);
        text(c, n, p.x, p.y - R * .24f, big, 0xFFFFFFFF, tfBold, Paint.Align.CENTER);
        text(c, fg ? "Cấp ?" : "Cấp " + p.level + "/" + eng.maxLvl(p), p.x, p.y + R * .28f, small, 0xD9FFFFFF, tfBold, Paint.Align.CENTER);
        text(c, "Máu " + (fg ? "?" : String.valueOf(eng.hpOf(p))), p.x, p.y + R * .6f, small, 0xB3FFFFFF, tfBold, Paint.Align.CENTER);
    }

    private void drawOrbit(Canvas c, Planet p) {
        if (eng.fogged(p)) return;
        int nr = eng.orbitRings(p, rings), dc = eng.orbitDots(p, dotX, dotY), hl = eng.highlightCount(p);
        stroke.setStrokeWidth(dp);
        stroke.setColor(alpha(FC[p.owner], .12f));
        for (int i = 0; i < nr; i++) c.drawCircle(p.x, p.y, rings[i], stroke);
        float dot = Math.max(1.8f * dp, eng.unit * .0055f);
        int normal = alpha(FC[p.owner], .92f);
        for (int i = 0; i < dc; i++) {
            boolean on = i < hl;
            fill.setColor(on ? C_GOLD : normal);
            c.drawCircle(dotX[i], dotY[i], on ? dot * 1.3f : dot, fill);
        }
    }

    private void drawNeutrals(Canvas c) {
        boolean hot = eng.lvl.asteroidHits;
        for (Asteroid a : eng.neutrals) {
            path.reset();
            for (int i = 0; i < 8; i++) {
                float ang = a.rot + i / 8f * TAU, r = a.rad * a.verts[i];
                float px = a.x + cos(ang) * r, py = a.y + sin(ang) * r;
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            path.close();
            fill.setColor(hot ? 0xFF7D6A5C : 0xFF7B7168);
            c.drawPath(path, fill);
            stroke.setColor(hot ? 0xBFFFAA6E : 0x8CE1D7CD);
            stroke.setStrokeWidth((hot ? 1.6f : 1.2f) * dp);
            c.drawPath(path, stroke);
            fill.setColor(0x38000000);
            c.drawCircle(a.x + cos(a.rot + .3f) * a.rad * .3f, a.y + sin(a.rot + .3f) * a.rad * .3f, a.rad * .28f, fill);
        }
    }

    private void drawRocks(Canvas c) {
        for (Rock r : eng.rocks) {
            int o = r.o;
            if (r.idle) {
                float bx = sin(eng.clock * 1.4f + r.ph) * 1.4f * dp, by = cos(eng.clock * 1.1f + r.ph) * 1.4f * dp;
                stroke.setColor(alpha(FC[o], .5f)); stroke.setStrokeWidth(dp);
                c.drawCircle(r.x + bx, r.y + by, r.rad + 2.5f * dp, stroke);
                fill.setColor(FL[o]);
                c.drawCircle(r.x + bx, r.y + by, r.rad, fill);
                continue;
            }
            stroke.setColor(alpha(FC[o], .5f)); stroke.setStrokeWidth(r.rad * 1.1f);
            c.drawLine(r.x - r.vx * .07f, r.y - r.vy * .07f, r.x, r.y, stroke);
            fill.setColor(FL[o]);
            c.drawCircle(r.x, r.y, r.rad, fill);
        }
    }

    // Kéo thẳng từ hành tinh (gửi nửa số đá)
    private void drawDrag(Canvas c) {
        Engine.Pointer pt = eng.ptr;
        if (pt == null || pt.mode != Engine.M_QUICK) return;
        ArrayList<Planet> sel = pt.qsel;
        Planet h = pt.hover;
        boolean cancel = h != null && sel.size() == 1 && h == sel.get(0), atk = h != null && h.owner != 0;
        float dx = h != null ? h.x : pt.x, dy = h != null ? h.y : pt.y;
        boolean far = !cancel && eng.farFrom(sel, dx, dy);
        int col = cancel ? C_MUTED : far ? C_FAR : atk ? C_DANGER : C_YOU;
        drawRange(c, sel, .5f);
        stroke.setStrokeWidth(2 * dp);
        dashed(true, eng.clock);
        for (Planet s : sel) {
            float R = eng.radiusOf(s);
            stroke.setColor(C_YOU);
            c.drawCircle(s.x, s.y, R + 9 * dp, stroke);
            if (cancel || s == h) continue;
            float ex = pt.x, ey = pt.y;
            if (h != null) { float d = (float) Math.hypot(h.x - s.x, h.y - s.y), q = eng.radiusOf(h) + 6 * dp; if (d > 0) { ex = h.x - (h.x - s.x) / d * q; ey = h.y - (h.y - s.y) / d * q; } }
            float d2 = (float) Math.hypot(ex - s.x, ey - s.y);
            if (d2 < 1) continue;
            stroke.setColor(col);
            c.drawLine(s.x + (ex - s.x) / d2 * R, s.y + (ey - s.y) / d2 * R, ex, ey, stroke);
        }
        dashed(false, 0);
        if (h != null && !cancel) { stroke.setColor(col); stroke.setStrokeWidth(3 * dp); c.drawCircle(h.x, h.y, eng.radiusOf(h) + 11 * dp, stroke); }
        int tot = 0;
        for (Planet s : sel) { if (s == h && sel.size() > 1) continue; tot += eng.quickCount(s); }
        float lx = h != null ? h.x : pt.x, ly = h != null ? h.y - eng.radiusOf(h) - 34 * dp : pt.y - 48 * dp;
        if (cancel) pill(c, lx, ly, "Thả để hủy", col);
        else if (far) pill(c, lx, ly, "Ngoài tầm bay", col);
        else if (h != null) pill(c, lx, ly, atk ? "Tấn công · " + tot + " (máu " + (eng.fogged(h) ? "?" : String.valueOf(eng.hpOf(h))) + ")" : "Chuyển quân · " + tot, col);
        else pill(c, lx, ly, "Điều đến đây · " + tot, col);
    }

    // Vòng khoanh, vùng chọn đã khóa và đường kéo tới đích
    private void drawSel(Canvas c) {
        Engine.Pointer pt = eng.ptr;
        boolean drawing = pt != null && pt.mode == Engine.M_LASSO;
        if (drawing && pt.pn > 1) {
            path.reset();
            path.moveTo(pt.px[0], pt.py[0]);
            for (int i = 1; i < pt.pn; i++) path.lineTo(pt.px[i], pt.py[i]);
            fill.setColor(0x144FF0B4);
            path.close(); c.drawPath(path, fill);
            path.reset();
            path.moveTo(pt.px[0], pt.py[0]);
            for (int i = 1; i < pt.pn; i++) path.lineTo(pt.px[i], pt.py[i]);
            stroke.setColor(0xF24FF0B4); stroke.setStrokeWidth(3 * dp);
            c.drawPath(path, stroke);
            Selection lv = pt.live;
            if (lv != null) {
                stroke.setColor(C_GOLD); stroke.setStrokeWidth(1.5f * dp);
                for (Rock r : lv.loose) c.drawCircle(r.x, r.y, r.rad + 3 * dp, stroke);
                int tot = lv.total();
                if (tot > 0) pill(c, pt.x, pt.y - 44 * dp, tot + " đá", C_YOU);
            }
        }
        Selection sel = eng.selection;
        if (sel == null || drawing || sel.n < 3) return;
        path.reset();
        path.moveTo(sel.xs[0], sel.ys[0]);
        for (int i = 1; i < sel.n; i++) path.lineTo(sel.xs[i], sel.ys[i]);
        path.close();
        fill.setColor(0x0F4FF0B4); c.drawPath(path, fill);
        stroke.setColor(0xD94FF0B4); stroke.setStrokeWidth(2 * dp);
        dashed(true, eng.clock * .6f); c.drawPath(path, stroke); dashed(false, 0);
        stroke.setColor(C_GOLD); stroke.setStrokeWidth(1.5f * dp);
        for (Rock r : sel.loose) if (!r.dead) c.drawCircle(r.x, r.y, r.rad + 3 * dp, stroke);
        int tot = sel.total();
        if (pt != null && pt.mode == Engine.M_CARRY) {
            Planet h = pt.hover;
            boolean atk = h != null && h.owner != 0;
            tmpPlanets.clear();
            for (Planet p : sel.gp) if (p != h) tmpPlanets.add(p);
            float dx = h != null ? h.x : pt.x, dy = h != null ? h.y : pt.y;
            boolean far = eng.farFrom(tmpPlanets, dx, dy);
            int col = far ? C_FAR : atk ? C_DANGER : C_YOU;
            drawRange(c, tmpPlanets, .5f);
            float ex = pt.x, ey = pt.y;
            if (h != null) { float d = (float) Math.hypot(h.x - sel.cx, h.y - sel.cy), q = eng.radiusOf(h) + 6 * dp; if (d > 0) { ex = h.x - (h.x - sel.cx) / d * q; ey = h.y - (h.y - sel.cy) / d * q; } }
            stroke.setColor(col); stroke.setStrokeWidth(2 * dp);
            dashed(true, eng.clock); c.drawLine(sel.cx, sel.cy, ex, ey, stroke); dashed(false, 0);
            if (h != null) { stroke.setStrokeWidth(3 * dp); c.drawCircle(h.x, h.y, eng.radiusOf(h) + 11 * dp, stroke); }
            float lx = h != null ? h.x : pt.x, ly = h != null ? h.y - eng.radiusOf(h) - 34 * dp : pt.y - 48 * dp;
            if (far) pill(c, lx, ly, "Ngoài tầm bay", col);
            else if (h != null) {
                boolean only = sel.gp.size() == 1 && sel.gp.get(0) == h && sel.loose.isEmpty();
                pill(c, lx, ly, atk ? "Tấn công · " + tot + " (máu " + (eng.fogged(h) ? "?" : String.valueOf(eng.hpOf(h))) + ")" : only ? "Nâng cấp · " + tot : "Chuyển quân · " + tot, col);
            } else pill(c, lx, ly, "Điều đến đây · " + tot, col);
        } else pill(c, sel.cx, sel.cy, tot + " đá", C_YOU);
    }
}
