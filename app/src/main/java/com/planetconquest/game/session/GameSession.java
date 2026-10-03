package com.planetconquest.game.session;

import com.planetconquest.game.data.ProgressStore;
import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.GameEvent;
import com.planetconquest.game.engine.Level;
import com.planetconquest.game.engine.Levels;

import java.util.Random;

/**
 * Trạng thái của phiên chơi hiện tại, tách khỏi giao diện: đang ở màn nào (chiến dịch hay Endless), bước của màn Hướng dẫn,
 * khởi động/chơi lại màn, và soạn nội dung màn kết thúc. Các màn hình dùng chung một GameSession.
 */
public final class GameSession {
    public static final String[] INTRO_STEPS = {
            "Chạm giữ hành tinh xanh của bạn, kéo sang hành tinh đỏ rồi thả tay. Một nửa số đá sẽ bay đi tấn công.",
            "Khoanh một vòng quanh những viên đá đang quay quanh hành tinh của bạn để chọn chúng.",
            "Chạm vào vòng sáng ở giữa màn hình: đá đã chọn bay tới và chờ ở đó. Đá khác phe va vào nhau sẽ cùng vỡ.",
            "Chạm vào hành tinh của bạn để nâng cấp: sinh đá nhanh hơn, chứa nhiều hơn và được thêm giáp.",
            "Máu = số đá + giáp. Tiếp tục gửi đá vào hành tinh đỏ cho tới khi chiếm được nó."
    };
    private static final GameEvent[] INTRO_EV = {GameEvent.ATTACK, GameEvent.LASSO, GameEvent.POINT, GameEvent.UPGRADE, GameEvent.CAPTURE};

    /** Nội dung màn kết thúc. next == null: không có nút "tiếp theo". */
    public static final class EndInfo {
        public final boolean win, again;
        public final String title, text, next;

        EndInfo(boolean win, String title, String text, String next, boolean again) {
            this.win = win; this.title = title; this.text = text; this.next = next; this.again = again;
        }
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

    /** Chơi lại đúng màn đang chơi (Endless: giữ nguyên bản đồ hiện tại). */
    public void restart() {
        if (isEndless()) { introStep = 0; eng.start(endlessLevel); }
        else startLevel(curLevel);
    }

    /** Chơi lại từ đầu: Endless quay về bản đồ 1. */
    public void again() {
        if (isEndless()) newEndlessRun();
        else startLevel(curLevel);
    }

    public String levelLabel() {
        if (curLevel < 0) return "Endless · Bản đồ " + endlessMap;
        if (curLevel == 0) return "Hướng dẫn";
        return "Màn " + curLevel + " · " + Levels.ALL[curLevel].name;
    }

    /** Chuyển sự kiện engine vào tiến trình Hướng dẫn. Trả về số bước mới (từ 2) nếu vừa sang bước kế tiếp, ngược lại 0. */
    public int onEvent(GameEvent ev) {
        if (!eng.lvl.intro || introStep >= INTRO_EV.length || ev != INTRO_EV[introStep]) return 0;
        introStep++;
        return introStep < INTRO_EV.length ? introStep + 1 : 0;
    }

    /** Ghi nhận kết quả vào tiến độ và soạn nội dung màn kết thúc. */
    public EndInfo finish(boolean win, String reason) {
        Level L = eng.lvl;
        String t = fmtTime(eng.time);
        EndInfo info;
        if (L.endless) {
            if (win) {
                endlessCleared = endlessMap;
                progress.recordEndless(endlessCleared);
                info = new EndInfo(true, "Qua bản đồ " + endlessMap,
                        "Đã chiếm " + eng.planets.size() + " hành tinh sau " + t + ". Bản đồ tiếp theo ngẫu nhiên và khó hơn một chút. Kỷ lục: " + progress.bestEndless() + " bản đồ.",
                        "Bản đồ tiếp theo", false);
            } else {
                info = new EndInfo(false, "Kết thúc hành trình",
                        reason + " Bạn đã vượt " + endlessCleared + " bản đồ. Kỷ lục: " + progress.bestEndless() + ".", null, true);
            }
        } else if (L.intro) {
            if (win) {
                progress.markIntroDone();
                info = new EndInfo(true, "Hoàn thành hướng dẫn",
                        "Bạn đã nắm đủ cách chơi. Mỗi màn tiếp theo có một hạn chế riêng để vượt qua.", "Vào màn 1", true);
            } else info = new EndInfo(false, "Thử lại nhé", reason, null, true);
        } else if (win) {
            progress.markLevelDone(curLevel);
            info = new EndInfo(true, "Chiến thắng",
                    "Đã chiếm " + eng.planets.size() + " hành tinh sau " + t + ", dù hạn chế: " + L.limit.toLowerCase(),
                    curLevel < Levels.CAMPAIGN_LAST ? "Màn tiếp theo" : "Thử Endless", true);
        } else {
            info = new EndInfo(false, "Thất bại", reason, null, true);
        }
        progress.save();
        return info;
    }

    public static String fmtTime(float s) {
        int m = (int) (s / 60), sec = (int) (s % 60);
        return m + ":" + (sec < 10 ? "0" : "") + sec;
    }
}
