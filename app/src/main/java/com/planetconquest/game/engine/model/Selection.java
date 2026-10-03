package com.planetconquest.game.engine.model;

import java.util.ArrayList;
import com.planetconquest.game.engine.model.Faction;
import java.util.Arrays;

/** Nhóm đá được chọn bằng vòng khoanh: một phần đá quỹ đạo của các hành tinh + các viên đá rời. */
public final class Selection {
    public final ArrayList<Planet> planets = new ArrayList<Planet>();
    public int[] counts = new int[16];
    public final ArrayList<Rock> loose = new ArrayList<Rock>();
    public float[] xs, ys;
    public int n;
    public float cx, cy;

    public void addGroup(Planet p, int c) {
        if (planets.size() == counts.length) counts = Arrays.copyOf(counts, counts.length * 2);
        counts[planets.size()] = c;
        planets.add(p);
    }

    public int countFor(Planet p) {
        int i = planets.indexOf(p);
        return i < 0 ? 0 : counts[i];
    }

    public int total() {
        int t = 0;
        for (int i = 0; i < planets.size(); i++) {
            Planet p = planets.get(i);
            if (Faction.isPlayer(p.owner())) t += Math.min(counts[i], p.rocks());
        }
        for (Rock r : loose) if (!r.dead && Faction.isPlayer(r.owner)) t++;
        return t;
    }
}
