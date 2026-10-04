package com.planetconquest.game.engine.level;

import java.util.Random;

import static com.planetconquest.game.engine.util.MathUtil.TAU;
import static com.planetconquest.game.engine.util.MathUtil.clamp;
import static com.planetconquest.game.engine.util.MathUtil.cos;
import static com.planetconquest.game.engine.util.MathUtil.hyp;
import static com.planetconquest.game.engine.util.MathUtil.sin;

/** Sinh bản đồ: vị trí và cỡ hành tinh theo kích thước màn hình. */
public final class MapGenerator {
    private final float W, H, dp, unit;

    public MapGenerator(float w, float h, float dp, float unit) {
        this.W = w; this.H = h; this.dp = dp; this.unit = unit;
    }

    public float top() { return 100 * dp; }
    public float bottom() { return Math.max(top() + 200 * dp, H - 100 * dp); }
    /** Đổi tọa độ chuẩn hóa ny (0..1) sang pixel theo chiều dọc. */
    public float y(float ny) { return top() + (bottom() - top()) * ny; }

    /** Sinh vị trí {nx, ny, size}. Người chơi luôn ở đáy giữa. rangeP > 0: mỗi hành tinh nằm trong tầm của một hành tinh trước đó. */
    public float[][] layout(Random r, int n, float rangeP) { return layout(r, n, rangeP, 0, 0); }

    /** Như trên; edgeMarginDp > 0 đặt lề ngang tối thiểu (dp) và minGapUnit > 0 đặt khoảng cách tối thiểu giữa hai hành tinh (theo unit, tự hạ khi bản đồ đông). */
    public float[][] layout(Random r, int n, float rangeP, float edgeMarginDp, float minGapUnit) {
        float top = top(), bottom = bottom();
        float padX = edgeMarginDp > 0 ? edgeMarginDp * dp : Math.max(48 * dp, unit * .13f), y0 = top + 22 * dp, y1 = bottom - 6 * dp;
        float area = (W - 2 * padX) * (y1 - y0);
        float minD = clamp((float) Math.sqrt(area / n) * .85f, 92 * dp, 170 * dp);
        if (rangeP > 0) minD = Math.min(minD, rangeP * .55f);
        if (minGapUnit > 0) minD = Math.min(minGapUnit * unit, (float) Math.sqrt(area / n) * 1.1f);   // bản đồ đông thì tự hạ để vẫn nhét vừa
        float base = n >= 9 ? .056f : n >= 7 ? .062f : .07f;
        float[] xs = new float[n], ys = new float[n];
        for (int attempt = 0; attempt < 80; attempt++) {
            xs[0] = W / 2; ys[0] = y1 - 20 * dp;
            int cnt = 1;
            boolean ok = true;
            for (int i = 1; i < n && ok; i++) {
                boolean placed = false;
                for (int t = 0; t < 250 && !placed; t++) {
                    float x, y;
                    if (rangeP > 0) {
                        int b = r.nextInt(cnt);
                        float a = r.nextFloat() * TAU, d = rangeP * (.6f + r.nextFloat() * .32f);
                        x = xs[b] + cos(a) * d; y = ys[b] + sin(a) * d;
                    } else {
                        x = padX + r.nextFloat() * (W - 2 * padX); y = y0 + r.nextFloat() * (y1 - y0);
                    }
                    if (x < padX || x > W - padX || y < y0 || y > y1) continue;
                    boolean far = true;
                    for (int k = 0; k < cnt; k++) if (hyp(xs[k] - x, ys[k] - y) < minD) { far = false; break; }
                    if (far) { xs[cnt] = x; ys[cnt] = y; cnt++; placed = true; }
                }
                if (!placed) ok = false;
            }
            if (ok && rangeP > 0) {
                boolean any = false;
                for (int k = 1; k < n; k++) if (hyp(xs[k] - xs[0], ys[k] - ys[0]) > rangeP * 1.15f) { any = true; break; }
                if (!any) ok = false;
            }
            if (ok) return pack(r, xs, ys, n, base, top, bottom);
            minD *= .95f;
        }
        int cols = n > 6 ? 3 : 2, rows = Math.max(1, (int) Math.ceil((n - 1) / (double) cols));
        xs[0] = W / 2; ys[0] = y1 - 20 * dp;
        for (int i = 1; i < n; i++) {
            int c = (i - 1) % cols, rr = (i - 1) / cols;
            xs[i] = padX + (c + .5f) / cols * (W - 2 * padX);
            ys[i] = y0 + (rr + .5f) / rows * (y1 - y0 - 120 * dp);
        }
        return pack(r, xs, ys, n, base, top, bottom);
    }

    private float[][] pack(Random r, float[] xs, float[] ys, int n, float base, float top, float bottom) {
        float[][] out = new float[n][3];
        for (int i = 0; i < n; i++) {
            out[i][0] = xs[i] / W;
            out[i][1] = (ys[i] - top) / (bottom - top);
            out[i][2] = i == 0 ? base + .012f : base + r.nextFloat() * .012f;
        }
        return out;
    }

}
