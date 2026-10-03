package com.planetconquest.game.data;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Tiến độ người chơi (màn đã qua, kỷ lục Endless, đã học hướng dẫn), lưu trong SharedPreferences. */
public final class ProgressStore {
    private static final String K_DONE = "done", K_BEST = "best", K_INTRO = "introDone";

    private final SharedPreferences prefs;
    private final Set<Integer> done = new HashSet<Integer>();
    private int best;
    private boolean introDone;

    public ProgressStore(SharedPreferences prefs) {
        this.prefs = prefs;
        for (String p : prefs.getString(K_DONE, "").split(",")) {
            if (p.length() == 0) continue;
            try { done.add(Integer.parseInt(p)); } catch (NumberFormatException ignored) { }
        }
        best = prefs.getInt(K_BEST, 0);
        introDone = prefs.getBoolean(K_INTRO, false);
    }

    public boolean isLevelDone(int level) { return done.contains(level); }
    public boolean isIntroDone() { return introDone; }
    public int bestEndless() { return best; }

    public void markLevelDone(int level) { done.add(level); }
    public void markIntroDone() { introDone = true; }
    public void recordEndless(int mapsCleared) { if (mapsCleared > best) best = mapsCleared; }

    public void save() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : done) { if (sb.length() > 0) sb.append(','); sb.append(i); }
        prefs.edit().putString(K_DONE, sb.toString()).putInt(K_BEST, best).putBoolean(K_INTRO, introDone).apply();
    }
}
