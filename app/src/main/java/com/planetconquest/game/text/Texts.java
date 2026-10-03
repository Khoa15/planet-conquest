package com.planetconquest.game.text;

import android.content.res.Resources;

import com.planetconquest.game.R;
import com.planetconquest.game.engine.EndReason;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.session.GameSession;

import java.util.Locale;

/**
 * Cổng duy nhất tới chuỗi hiển thị (res/values/strings.xml). Engine và session chỉ phát mã + tham số
 * ({@link Notice}, {@link EndReason}, {@link GameSession.EndInfo}); lớp này đổi chúng ra chữ theo ngôn ngữ máy.
 */
public final class Texts {
    private final Resources res;
    /** Tiền tố nhãn hành tinh, lấy sẵn một lần vì được ghép chuỗi mỗi khung hình. */
    public final String levelPrefix, hpPrefix;

    public Texts(Resources res) {
        this.res = res;
        levelPrefix = res.getString(R.string.planet_level_prefix);
        hpPrefix = res.getString(R.string.planet_hp_prefix);
    }

    public String s(int id, Object... args) { return args.length == 0 ? res.getString(id) : res.getString(id, args); }

    // ---- Màn chơi (index < 0: Endless) ----
    public String levelName(int index) { return index < 0 ? s(R.string.endless_name) : res.getStringArray(R.array.level_names)[index]; }
    public String levelLimit(int index) { return index < 0 ? s(R.string.endless_limit) : res.getStringArray(R.array.level_limits)[index]; }
    public String levelTip(int index) { return index < 0 ? s(R.string.endless_tip) : res.getStringArray(R.array.level_tips)[index]; }
    public String[] introSteps() { return res.getStringArray(R.array.intro_steps); }

    public String levelLabel(int curLevel, int endlessMap) {
        if (curLevel < 0) return s(R.string.endless_label, endlessMap);
        if (curLevel == 0) return levelName(0);
        return s(R.string.level_label, curLevel, levelName(curLevel));
    }

    // ---- Thông báo từ engine ----
    public String notice(Notice n) {
        Object[] a = n.args;
        switch (n.msg) {
            case NO_UPGRADE_IN_LEVEL: return s(R.string.msg_no_upgrade);
            case CAPTURED_MAX_LEVEL: return s(R.string.msg_captured_max, a[0]);
            case MAX_LEVEL_REACHED: return s(R.string.msg_max_level);
            case COOLDOWN: return s(R.string.msg_cooldown, a[0]);
            case OUT_OF_RANGE: return s(R.string.msg_out_of_range);
            case PLANET_LEVEL_UP: return s(R.string.msg_level_up, a[0], fmt1((Float) a[1]), a[2], a[3]);
            case PLANET_CAPTURED: return s(R.string.msg_captured);
            case PLANET_LOST: return s(R.string.msg_lost);
            case ASTEROID_DAMAGE: return s(R.string.msg_damage, a[0]);
            case LASSO_EMPTY: return s(R.string.msg_lasso_empty);
            case LASSO_TOO_SMALL: return s(R.string.msg_lasso_small);
            case PLANET_OUT_OF_ROCKS: return s(R.string.msg_planet_empty);
            case NO_ROCKS_TO_SEND: return s(R.string.msg_no_rocks);
            default: return "";
        }
    }

    public String reason(EndReason r) {
        switch (r) {
            case ALL_PLANETS_LOST: return s(R.string.reason_all_lost);
            case TIME_UP: return s(R.string.reason_time_up);
            case OUT_OF_ROCKS: return s(R.string.reason_out_of_rocks);
            default: return "";
        }
    }

    /** Một chữ số thập phân, bỏ ".0" (1.0 -> "1", 1.5 -> "1.5"). */
    private static String fmt1(float v) {
        float r = Math.round(v * 10) / 10f;
        return r == (int) r ? String.valueOf((int) r) : String.valueOf(r);
    }

    // ---- Màn kết thúc ----
    public String endTitle(GameSession.EndInfo e) {
        switch (e.kind) {
            case ENDLESS_WIN: return s(R.string.end_endless_win_title, e.map);
            case ENDLESS_LOSE: return s(R.string.end_endless_lose_title);
            case INTRO_WIN: return s(R.string.end_intro_win_title);
            case INTRO_LOSE: return s(R.string.end_intro_lose_title);
            case WIN: return s(R.string.end_win_title);
            default: return s(R.string.end_lose_title);
        }
    }

    public String endText(GameSession.EndInfo e) {
        switch (e.kind) {
            case ENDLESS_WIN: return s(R.string.end_endless_win_text, e.planets, e.time, e.best);
            case ENDLESS_LOSE: return s(R.string.end_endless_lose_text, reason(e.reason), e.cleared, e.best);
            case INTRO_WIN: return s(R.string.end_intro_win_text);
            case WIN: return s(R.string.end_win_text, e.planets, e.time, levelLimit(e.level).toLowerCase(Locale.getDefault()));
            default: return reason(e.reason);
        }
    }

    /** Nhãn nút "tiếp theo"; null nếu không có. */
    public String endNext(GameSession.EndInfo e) {
        switch (e.kind) {
            case ENDLESS_WIN: return s(R.string.end_endless_next);
            case INTRO_WIN: return s(R.string.end_intro_next);
            case WIN: return s(e.lastLevel ? R.string.end_try_endless : R.string.end_next_level);
            default: return null;
        }
    }
}
