package com.planetconquest.game.session;

import com.planetconquest.game.data.ProgressStore;
import com.planetconquest.game.engine.EndReason;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.GameEvent;
import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.level.Levels;

import java.util.Random;

/**
 * Trạng thái của phiên chơi hiện tại, tách khỏi giao diện: đang ở màn nào (chiến dịch hay Endless), bước của màn Hướng dẫn,
 * khởi động/chơi lại màn, và soạn nội dung màn kết thúc. Các màn hình dùng chung một GameSession.
 */
public final class GameSession {
    private static final GameEvent[] INTRO_EV = {GameEvent.ATTACK, GameEvent.LASSO, GameEvent.POINT, GameEvent.UPGRADE, GameEvent.CAPTURE};

    /** Số bước của màn Hướng dẫn. */
    public static final int INTRO_STEP_COUNT = INTRO_EV.length;

    public enum EndKind { ENDLESS_WIN, ENDLESS_LOSE, INTRO_WIN, INTRO_LOSE, WIN, LOSE }

    /** Kết quả một màn dưới dạng dữ liệu; chữ hiển thị do Texts dựng. */
    public static final class EndInfo {
        public final EndKind kind;
        public final EndReason reason;
        public final int planets, map, cleared, best, level;
        public final String time;
        public final boolean lastLevel;

        EndInfo(EndKind kind, EndReason reason, int planets, String time, int map, int cleared, int best, int level, boolean lastLevel) {
            this.kind = kind; this.reason = reason; this.planets = planets; this.time = time;
            this.map = map; this.cleared = cleared; this.best = best; this.level = level; this.lastLevel = lastLevel;
        }

        public boolean win() { return kind == EndKind.ENDLESS_WIN || kind == EndKind.INTRO_WIN || kind == EndKind.WIN; }
        public boolean hasNext() { return win(); }
        /** Endless thắng thì chỉ có "bản đồ tiếp theo", không có "chơi lại". */
        public boolean hasAgain() { return kind != EndKind.ENDLESS_WIN; }
    }

    private final Engine eng;
    private final ProgressStore progress;
    private final Random rnd = new Random();
    private int curLevel = 0;                   // chỉ số trong Levels.ALL; -1 = Endless
    private Level endlessLevel;
    private int endlessMap = 1, endlessCleared = 0, introStep = 0;

    public GameSession(Engine eng, ProgressStore progress) {
        this.eng = eng;
        this.progress = progress;
    }

    public int curLevel() { return curLevel; }
    public boolean isEndless() { return curLevel < 0; }
    public int endlessMap() { return endlessMap; }
    public int introStep() { return introStep; }

    /** Màn nên chơi tiếp theo tiến độ: Hướng dẫn nếu chưa học, rồi màn chiến dịch đầu tiên chưa qua; -1 (Endless) khi đã qua hết. */
    public int nextLevel() {
        if (!progress.isIntroDone()) return 0;
        for (int i = 1; i < Levels.ALL.length; i++) if (!progress.isLevelDone(i)) return i;
        return -1;
    }

    /** Bắt đầu ngay màn nextLevel(). */
    public void startNext() {
        int next = nextLevel();
        if (next < 0) newEndlessRun(); else startLevel(next);
    }

    public void startLevel(int idx) {
        curLevel = idx; introStep = 0;
        eng.start(Levels.ALL[idx]);
    }

    /** Bắt đầu một hành trình Endless mới từ bản đồ 1. */
    public void newEndlessRun() {
        endlessMap = 1; endlessCleared = 0;
        startEndless();
    }

    /** Sang bản đồ Endless kế tiếp. */
    public void nextEndlessMap() {
        endlessMap++;
        startEndless();
    }

    private void startEndless() {
        curLevel = -1; introStep = 0;
        endlessLevel = Levels.endless(endlessMap, rnd);
        eng.start(endlessLevel);
    }

    /** Chơi lại đúng màn đang chơi, dùng cho cả Tạm dừng và màn thua (Endless: giữ nguyên bản đồ và số thứ tự bản đồ hiện tại). */
    public void restart() {
        if (isEndless()) { introStep = 0; eng.start(endlessLevel); }
        else startLevel(curLevel);
    }

    /** Chuyển sự kiện engine vào tiến trình Hướng dẫn. Trả về số bước mới (từ 2) nếu vừa sang bước kế tiếp, ngược lại 0. */
    public int onEvent(GameEvent ev) {
        if (!eng.level().intro || introStep >= INTRO_EV.length || ev != INTRO_EV[introStep]) return 0;
        introStep++;
        return introStep < INTRO_EV.length ? introStep + 1 : 0;
    }

    /** Ghi nhận kết quả vào tiến độ và trả về dữ liệu màn kết thúc. */
    public EndInfo finish(boolean win, EndReason reason) {
        Level L = eng.level();
        String time = fmtTime(eng.time());
        int planets = eng.planets().size();
        EndInfo info;
        if (L.endless) {
            if (win) {
                endlessCleared = endlessMap;
                progress.recordEndless(endlessCleared);
            }
            info = new EndInfo(win ? EndKind.ENDLESS_WIN : EndKind.ENDLESS_LOSE, reason, planets, time,
                    endlessMap, endlessCleared, progress.bestEndless(), curLevel, false);
        } else if (L.intro) {
            if (win) progress.markIntroDone();
            info = new EndInfo(win ? EndKind.INTRO_WIN : EndKind.INTRO_LOSE, reason, planets, time, 0, 0, 0, curLevel, false);
        } else {
            if (win) progress.markLevelDone(curLevel);
            info = new EndInfo(win ? EndKind.WIN : EndKind.LOSE, reason, planets, time, 0, 0, 0, curLevel, curLevel >= Levels.CAMPAIGN_LAST);
        }
        progress.save();
        return info;
    }

    public static String fmtTime(float s) {
        int m = (int) (s / 60), sec = (int) (s % 60);
        return m + ":" + (sec < 10 ? "0" : "") + sec;
    }
}
