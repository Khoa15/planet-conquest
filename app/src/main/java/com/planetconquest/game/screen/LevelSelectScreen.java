package com.planetconquest.game.screen;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import com.planetconquest.game.R;
import com.planetconquest.game.engine.level.Levels;
import com.planetconquest.game.engine.util.ColorUtil;
import com.planetconquest.game.ui.LevelPath;
import com.planetconquest.game.ui.LevelPlanet;
import com.planetconquest.game.ui.LevelPlanetRenderer;
import com.planetconquest.game.ui.UiButton;

import java.util.Arrays;

import static com.planetconquest.game.ui.Palette.*;

/**
 * Màn chọn màn dạng lộ trình: mỗi màn là một hành tinh nối nhau bằng đường cong vẹo, cuộn dọc.
 * Hướng dẫn ở dưới cùng, Endless (hố đen) ở trên cùng. Hành tinh tối khi chưa qua, sáng dần khi đã qua.
 * Số liệu bố cục lấy từ planet-ui/levels.js.
 */
final class LevelSelectScreen extends BaseScreen {
    private static final float NODE_STEP = 128, TOP_PAD = 110, BOTTOM_PAD = 90;
    private static final float NODE_R = 26, INTRO_R = 22, ENDLESS_R = 36, TAP_SLOP = 8, LIT_SECONDS = .9f, HEADER_H = 62;
    /** Tỉ lệ x của từng nút theo chiều rộng màn hình (0 = Hướng dẫn ... cuối = Endless). */
    private static final float[] ZIGZAG_X = {.5f, .28f, .72f, .3f, .7f, .27f, .73f, .32f, .68f, .3f, .5f};

    private final int endless = Levels.ALL.length;    // chỉ số nút Endless; các nút 0..endless-1 là Hướng dẫn + chiến dịch
    private final int nodes = endless + 1;
    private final float[] nx = new float[nodes], ny = new float[nodes], nr = new float[nodes], lit = new float[nodes];
    private final LevelPath[] paths = new LevelPath[nodes - 1];
    private final LevelPlanetRenderer planets;
    private LinearGradient headerShade;
    private float scroll, scrollMax, downY, scrollAtDown, clock, mapH;
    private boolean scrolling;
    private int downNode = -1;

    LevelSelectScreen(ScreenHost host) {
        super(host);
        planets = new LevelPlanetRenderer(host.kit());
    }

    @Override
    public void enter() {
        super.enter();
        Arrays.fill(lit, 0);                          // mở màn: các hành tinh đã qua sáng dần lên
        scroll = Math.max(0, Math.min(scrollMax, ny[nextNode()] - H() * .6f));
    }

    @Override
    public void layout() {
        buttons.clear();
        if (W() <= 0) return;
        add(new UiButton(null, UiButton.Style.ICON, UiButton.Icon.BACK, new Runnable() {
            @Override public void run() { host.go(host.screens().welcome()); }
        })).at(16 * dp, 18 * dp, 60 * dp, 62 * dp);

        mapH = (TOP_PAD + (nodes - 1) * NODE_STEP + BOTTOM_PAD) * dp;
        for (int i = 0; i < nodes; i++) {
            nx[i] = ZIGZAG_X[i] * W();
            ny[i] = mapH - (BOTTOM_PAD + i * NODE_STEP) * dp;
            nr[i] = (i == 0 ? INTRO_R : i == endless ? ENDLESS_R : NODE_R) * dp;
        }
        for (int i = 0; i < paths.length; i++) paths[i] = new LevelPath(nx[i], ny[i], nr[i], nx[i + 1], ny[i + 1], nr[i + 1], i + 1, dp);
        headerShade = new LinearGradient(0, 0, 0, HEADER_H * dp, 0xEE05070F, 0x0005070F, Shader.TileMode.CLAMP);
        scrollMax = Math.max(0, mapH - H());
        scroll = Math.max(0, Math.min(scroll, scrollMax));
    }

    @Override public boolean onBack() { host.go(host.screens().welcome()); return true; }
    @Override public boolean needsLoop() { return true; }
    @Override public float animTime() { return clock; }

    @Override
    public void update(float dt) {
        clock += dt;
        for (int i = 0; i < nodes; i++) {
            float target = isLit(i) ? 1 : 0, step = dt / LIT_SECONDS;
            lit[i] = lit[i] < target ? Math.min(target, lit[i] + step) : Math.max(target, lit[i] - step);
        }
    }

    /** Hành tinh sáng khi màn đã qua (Endless: đã có kỷ lục). */
    private boolean isLit(int i) {
        if (i == endless) return host.progress().bestEndless() > 0;
        return i == 0 ? host.progress().isIntroDone() : host.progress().isLevelDone(i);
    }

    /** Màn nên chơi tiếp: màn đầu tiên chưa qua (Endless khi đã qua hết chiến dịch). */
    private int nextNode() {
        for (int i = 0; i < endless; i++) if (!isLit(i)) return i;
        return endless;
    }

    // ================= Cảm ứng =================
    @Override
    public void onDown(float x, float y) {
        scrolling = false; downY = y; scrollAtDown = scroll; downNode = nodeAt(x, y);
    }

    @Override
    public void onMove(float x, float y) {
        if (!scrolling && Math.abs(y - downY) > TAP_SLOP * dp) scrolling = true;
        if (scrolling) { scroll = Math.max(0, Math.min(scrollMax, scrollAtDown - (y - downY))); host.invalidate(); }
    }

    @Override
    public void onUp(float x, float y) {
        if (!scrolling && downNode >= 0 && nodeAt(x, y) == downNode) open(downNode);
    }

    private void open(int i) {
        if (i == 0) startLevelAndPlay(0);
        else if (i < endless) host.go(host.screens().brief(i));
        else host.go(host.screens().brief(-1));
    }

    private int nodeAt(float x, float y) {
        float my = y + scroll;
        for (int i = 0; i < nodes; i++) {
            float r = Math.max(nr[i] * 1.4f, 28 * dp), dx = x - nx[i], dy = my - ny[i];
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    // ================= Vẽ =================
    @Override
    public void draw(Canvas c) {
        c.save();
        c.translate(0, -scroll);
        float viewTop = scroll, viewBottom = scroll + H();
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].bottom() < viewTop || paths[i].top() > viewBottom) continue;
            paths[i].draw(c, kit, LevelPlanet.forLevel(i).color, LevelPlanet.forLevel(i + 1).color, Math.min(lit[i], 1), clock);
        }
        int next = nextNode();
        for (int i = 0; i < nodes; i++) {
            if (ny[i] + nr[i] * 3 < viewTop || ny[i] - nr[i] * 3 > viewBottom) continue;
            drawNode(c, i, i == next);
        }
        c.restore();

        fill.setColor(0xFFFFFFFF);                    // paint dùng chung: đặt lại màu đục trước khi vẽ bằng shader
        fill.setShader(headerShade);
        c.drawRect(0, 0, W(), HEADER_H * dp, fill);
        fill.setShader(null);
        text(c, tx.s(R.string.levels_title), 74 * dp, 40 * dp, 22 * dp, C_INK, tfBold, Paint.Align.LEFT);
    }

    private void drawNode(Canvas c, int i, boolean isNext) {
        float x = nx[i], y = ny[i], rad = nr[i], l = lit[i], pulse = (float) (Math.sin(clock * 3) + 1) / 2;
        if (isNext) {   // vòng gợi ý màn nên chơi tiếp
            dashed(true, clock * .4f);
            stroke.setStrokeWidth(2 * dp); stroke.setColor(ColorUtil.alpha(C_GOLD, .35f + .5f * pulse));
            c.drawCircle(x, y, rad * 1.95f + pulse * 3 * dp, stroke);
            dashed(false, 0);
        }
        planets.draw(c, LevelPlanet.forLevel(i == endless ? -1 : i), x, y, rad, l, clock);

        String label = i == endless ? tx.levelName(-1) : i == 0 ? tx.levelName(0) : tx.s(R.string.level_label, i, tx.levelName(i));
        float ly = y + rad * (i == endless ? 1.75f : 1.55f) + 14 * dp;
        outlinedText(c, label, x, ly, 12.5f * dp, ColorUtil.alpha(C_INK, .45f + .55f * l));
        if (i == endless && host.progress().bestEndless() > 0)
            outlinedText(c, tx.s(R.string.best_short, host.progress().bestEndless()), x, ly + 16 * dp, 11 * dp, C_MUTED);
        if (i != endless && isLit(i)) drawTick(c, x + rad, y - rad);
    }

    /** Chữ có viền tối để đọc được khi đè lên đường nối. */
    private void outlinedText(Canvas c, String s, float x, float y, float size, int color) {
        txt.setStyle(Paint.Style.STROKE); txt.setStrokeWidth(4 * dp); txt.setStrokeJoin(Paint.Join.ROUND);
        text(c, s, x, y, size, 0xE605070F, tfBold, Paint.Align.CENTER);
        txt.setStyle(Paint.Style.FILL);
        text(c, s, x, y, size, color, tfBold, Paint.Align.CENTER);
    }

    private void drawTick(Canvas c, float x, float y) {
        path.reset();
        path.moveTo(x - 5 * dp, y); path.lineTo(x - 1.5f * dp, y + 4 * dp); path.lineTo(x + 5 * dp, y - 4 * dp);
        stroke.setStrokeWidth(5.5f * dp); stroke.setColor(0xE605070F); c.drawPath(path, stroke);
        stroke.setStrokeWidth(2.6f * dp); stroke.setColor(C_YOU); c.drawPath(path, stroke);
    }
}
