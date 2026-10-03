package com.planetconquest.game.engine;

import java.util.ArrayList;
import java.util.Arrays;

/** Nhóm đá được chọn bằng vòng khoanh: một phần đá quỹ đạo của các hành tinh + các viên đá rời. */
public final class Selection {
    public final ArrayList<Planet> gp = new ArrayList<Planet>();
    public int[] gc = new int[16];
    public final ArrayList<Rock> loose = new ArrayList<Rock>();
    public float[] xs, ys;
    public int n;
    public float cx, cy;

    void addGroup(Planet p, int c) {
        if (gp.size() == gc.length) gc = Arrays.copyOf(gc, gc.length * 2);
        gc[gp.size()] = c;
        gp.add(p);
    }

    public int countFor(Planet p) {
        int i = gp.indexOf(p);
        return i < 0 ? 0 : gc[i];
    }

    public int total() {
        int t = 0;
        for (int i = 0; i < gp.size(); i++) {
            Planet p = gp.get(i);
            if (p.owner == 0) t += Math.min(gc[i], p.n);
        }
        for (Rock r : loose) if (!r.dead && r.o == 0) t++;
        return t;
    }
}
