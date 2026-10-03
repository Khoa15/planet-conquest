package com.planetconquest.game.engine.physics;

import com.planetconquest.game.engine.model.Asteroid;
import com.planetconquest.game.engine.model.Body;
import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.Rock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.planetconquest.game.engine.util.MathUtil.clamp;

/** Va chạm giữa các vật tròn bằng lưới ô vuông. Hai vật khác phe chạm nhau thì cùng vỡ; vật cùng phe đi xuyên qua nhau. */
public final class CollisionGrid {
    public interface Listener {
        /** Gọi sau khi cả hai vật đã bị đánh dấu dead. */
        void onCollide(Body a, Body b);
    }

    private final ArrayList<Body> ents = new ArrayList<Body>();
    private int[] head = new int[0], next = new int[64], cellX = new int[64], cellY = new int[64];

    /** Hai vật khác phe chạm nhau: cùng vỡ. Lưới ô vuông để chỉ so vật lân cận. */
    public void resolve(List<Rock> rocks, List<Asteroid> neutrals, float W, float H, float dp, Listener listener) {
        ents.clear();
        for (Rock r : rocks) if (!r.dead) ents.add(r);
        for (Asteroid a : neutrals) if (!a.dead) ents.add(a);
        int count = ents.size();
        float cs = 16 * dp, off = 100 * dp;
        int cols = (int) ((W + 2 * off) / cs) + 2, rows = (int) ((H + 2 * off) / cs) + 2;
        if (head.length < cols * rows) head = new int[cols * rows];
        Arrays.fill(head, 0, cols * rows, -1);
        if (next.length < count) { next = new int[count * 2]; cellX = new int[count * 2]; cellY = new int[count * 2]; }
        for (int i = 0; i < count; i++) {
            Body e = ents.get(i);
            int cx = (int) clamp((e.x + off) / cs, 0, cols - 1), cy = (int) clamp((e.y + off) / cs, 0, rows - 1);
            cellX[i] = cx; cellY[i] = cy;
            int idx = cy * cols + cx;
            next[i] = head[idx]; head[idx] = i;
        }
        for (int i = 0; i < count; i++) {
            Body e = ents.get(i);
            if (e.dead) continue;
            for (int ox = -1; ox <= 1; ox++) {
                int cx = cellX[i] + ox;
                if (cx < 0 || cx >= cols) continue;
                for (int oy = -1; oy <= 1; oy++) {
                    int cy = cellY[i] + oy;
                    if (cy < 0 || cy >= rows) continue;
                    for (int j = head[cy * cols + cx]; j != -1; j = next[j]) {
                        if (j <= i || e.dead) continue;
                        Body f = ents.get(j);
                        if (f.dead) continue;
                        if (e.owner == f.owner && e.owner != Faction.NEUTRAL) continue;
                        float dx = e.x - f.x, dy = e.y - f.y, rr = e.rad + f.rad;
                        if (dx * dx + dy * dy < rr * rr) {
                            e.dead = true; f.dead = true;
                                                        listener.onCollide(e, f);
                        }
                    }
                }
            }
        }
    }

}
