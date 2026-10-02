package com.planetconquest.game.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Toàn bộ luật chơi, AI, va chạm và xử lý cử chỉ.
 * Java thuần (không import Android) để chạy được cả trên JVM khi kiểm thử (xem tools/EngineSim.java).
 * Mọi khoảng cách tính theo pixel; hằng số giao diện nhân với dp.
 */
public final class Engine {

    public interface Listener {
        void onToast(String msg);
        void onHaptic(int kind);
        void onEvent(int ev);
        void onFinish(boolean win, String reason);
    }

    // Sự kiện cho màn Hướng dẫn
    public static final int EV_ATTACK = 1, EV_MOVE = 2, EV_POINT = 3, EV_LASSO = 4, EV_UPGRADE = 5, EV_CAPTURE = 6, EV_LEVELUP = 7;
    public static final int H_LIGHT = 0, H_HEAVY = 1;

    // ---------- Cân bằng ----------
    public static final float PRODUCE_BASE = 1f, PRODUCE_STEP = .5f;
    public static final int MAX_LEVEL = 5, CAPTURED_MAX_LEVEL = 4, ARMOR_PER_LEVEL = 10;
    public static final float ROCK_SPEED = .55f, QUICK_SEND = .5f;
    public static final float AI_GRACE = 10f, AI_THINK_MIN = 3f, AI_THINK_MAX = 5.5f, LULL_SECONDS = 14f;
    public static final int NEUTRALS = 6;
    public static final float ASTEROID_DMG_K = 11000f;

    public static int upgradeCost(int level) { return 10 + 8 * level; }
    public static int capacity(int level) { return 40 + 20 * level; }

    public static final float TAU = (float) (Math.PI * 2);
    public static final int[] FACTION_COLORS = {
            0xFF4FF0B4, 0xFFFF6B7D, 0xFFFFB347, 0xFFA46BFF, 0xFF5CC8FF,
            0xFFFF8FD8, 0xFFF2E86D, 0xFF9BE564, 0xFFC9B79C, 0xFF6F8BFF};
    public static final int[] FACTION_LIGHT = new int[FACTION_COLORS.length];
    static {
        for (int i = 0; i < FACTION_COLORS.length; i++) FACTION_LIGHT[i] = mixColor(FACTION_COLORS[i], 0xFFFFFFFF, .5f);
    }

    public static int mixColor(int c, int t, float a) {
        int r = (int) (((c >> 16) & 255) + ((((t >> 16) & 255) - ((c >> 16) & 255)) * a));
        int g = (int) (((c >> 8) & 255) + ((((t >> 8) & 255) - ((c >> 8) & 255)) * a));
        int b = (int) ((c & 255) + (((t & 255) - (c & 255)) * a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ---------- Trạng thái ----------
    public float W = 360, H = 640, dp = 1, unit = 360, RR = 2.8f;
    public Level lvl = Levels.ALL[0];
    public final ArrayList<Planet> planets = new ArrayList<Planet>();
    public final ArrayList<Rock> rocks = new ArrayList<Rock>();
    public final ArrayList<Asteroid> neutrals = new ArrayList<Asteroid>();
    public final ArrayList<Particle> parts = new ArrayList<Particle>();
    public final ArrayList<FloatText> texts = new ArrayList<FloatText>();
    private final ArrayList<Float> neutTimers = new ArrayList<Float>();
    public float time, clock;
    public boolean over;
    private float lastAttack;
    public Pointer ptr;
    public Selection selection;

    private final Random rnd = new Random();
    private Listener listener;
    private final float[] dotX = new float[64], dotY = new float[64];
    private final ArrayList<Body> ents = new ArrayList<Body>();
    private int[] head = new int[0], next = new int[64], cellX = new int[64], cellY = new int[64];

    public void setListener(Listener l) { listener = l; }
    private void toast(String s) { if (listener != null) listener.onToast(s); }
    private void haptic(int k) { if (listener != null) listener.onHaptic(k); }
    private void event(int e) { if (listener != null) listener.onEvent(e); }

    float rf(float a, float b) { return a + rnd.nextFloat() * (b - a); }
    static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
    static float hyp(float x, float y) { return (float) Math.sqrt(x * x + y * y); }
    static float cos(float a) { return (float) Math.cos(a); }
    static float sin(float a) { return (float) Math.sin(a); }

    // ---------- Luật theo màn ----------
    public float radiusOf(Planet p) { return p.base * (1 + 0.08f * (p.level - 1)); }
    public int maxLvl(Planet p) { return lvl.noUpgrade ? 1 : (p.captured ? CAPTURED_MAX_LEVEL : MAX_LEVEL); }
    public int capOf(Planet p) { int c = capacity(p.level); return lvl.capCap > 0 ? Math.min(c, lvl.capCap) : c; }
    public int hpOf(Planet p) { return p.n + p.armor; }                  // máu = số đá + giáp
    public boolean fogged(Planet p) { return lvl.fog && p.owner != 0 && p.reveal <= 0; }
    public float rangePx() { return lvl.range > 0 ? unit * lvl.range : Float.POSITIVE_INFINITY; }
    public float aiGrace() { return lvl.passiveAi ? 1e9f : (lvl.aiGrace >= 0 ? lvl.aiGrace : AI_GRACE); }
    public float rateOf(Planet p) {
        if (lvl.noProduction) return 0;
        float r = PRODUCE_BASE + PRODUCE_STEP * (p.level - 1);
        if (p.owner != 0 && lvl.enemyProd > 0) r *= lvl.enemyProd;
        return r;
    }
    public int asteroidDamage(Asteroid a) { float k = a.rad / unit; return Math.max(2, Math.round(k * k * ASTEROID_DMG_K)); }
    public float mapTop() { return 100 * dp; }
    public float mapBottom() { return Math.max(mapTop() + 200 * dp, H - 100 * dp); }
    private float mapY(float ny) { return mapTop() + (mapBottom() - mapTop()) * ny; }
    public String capMsg(Planet p) {
        if (lvl.noUpgrade) return "Màn này không cho nâng cấp";
        return p.captured ? "Hành tinh bị chiếm chỉ lên tối đa cấp " + CAPTURED_MAX_LEVEL : "Đã đạt cấp tối đa";
    }
    static String fmt1(float v) {
        float r = Math.round(v * 10) / 10f;
        return r == (int) r ? String.valueOf((int) r) : String.valueOf(r);
    }

    // ---------- Kích thước & bản đồ ----------
    public void setSize(float w, float h, float density) {
        W = w; H = h; dp = density;
        unit = Math.min(W, H * 0.6f);
        RR = Math.max(2.4f * dp, unit * 0.0075f);
        placePlanets();
    }

    private void placePlanets() {
        for (Planet p : planets) { p.x = p.nx * W; p.y = mapY(p.ny); p.base = unit * p.size; }
    }

    /** Sinh vị trí {nx, ny, size}. Người chơi luôn ở đáy giữa. rangeP > 0: mỗi hành tinh nằm trong tầm của một hành tinh trước đó. */
    float[][] genLayout(Random r, int n, float rangeP) {
        float top = mapTop(), bottom = mapBottom();
        float padX = Math.max(48 * dp, unit * .13f), y0 = top + 22 * dp, y1 = bottom - 6 * dp;
        float area = (W - 2 * padX) * (y1 - y0);
        float minD = clamp((float) Math.sqrt(area / n) * .85f, 92 * dp, 170 * dp);
        if (rangeP > 0) minD = Math.min(minD, rangeP * .55f);
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

    // ---------- Khởi tạo màn ----------
    public void start(Level L) {
        lvl = L;
        Random r = new Random(L.seed);
        float[][] pts = L.fixed != null ? L.fixed : genLayout(r, L.planets, L.range > 0 ? unit * L.range : 0);
        planets.clear();
        for (int i = 0; i < pts.length; i++) {
            Planet p = new Planet();
            p.id = i; p.nx = pts[i][0]; p.ny = pts[i][1]; p.size = pts[i][2]; p.owner = i;
            p.n = i == 0 ? L.playerN : Math.round(L.enemyMin + r.nextFloat() * (L.enemyMax - L.enemyMin));
            if (i > 0) p.armor = L.enemyArmor;
            p.bold = i == 0 ? 0 : clamp(1f + r.nextFloat() * .4f + L.boldShift, .85f, 1.5f);
            p.think = aiGrace() + rf(0, 3);
            p.seed = rf(0, TAU);
            planets.add(p);
        }
        rocks.clear(); neutrals.clear(); neutTimers.clear(); parts.clear(); texts.clear();
        time = 0; over = false; ptr = null; selection = null;
        lastAttack = aiGrace() + 8;
        placePlanets();
        int nn = L.neutrals >= 0 ? L.neutrals : NEUTRALS;
        for (int i = 0; i < nn; i++) spawnNeutral(false);
    }

    private void spawnNeutral(boolean fromEdge) {
        float rad = unit * rf(lvl.nrMin, lvl.nrMax) * lvl.neutralSize;
        float x = 0, y = 0, ang;
        if (fromEdge) {
            int side = rnd.nextInt(4);
            float yMin = 110 * dp, yMax = H - 130 * dp;
            if (side == 0) { x = -30 * dp; y = rf(yMin, yMax); }
            else if (side == 1) { x = W + 30 * dp; y = rf(yMin, yMax); }
            else if (side == 2) { x = rf(20 * dp, W - 20 * dp); y = -30 * dp; }
            else { x = rf(20 * dp, W - 20 * dp); y = H + 30 * dp; }
            Planet tgt = lvl.asteroidHits && !planets.isEmpty() && rnd.nextFloat() < .6f ? planets.get(rnd.nextInt(planets.size())) : null;
            ang = tgt != null ? (float) Math.atan2(tgt.y - y, tgt.x - x) + rf(-.25f, .25f)
                              : (float) Math.atan2(H / 2 - y, W / 2 - x) + rf(-.9f, .9f);
        } else {
            for (int t = 0; t < 30; t++) {
                x = rf(24 * dp, W - 24 * dp); y = rf(110 * dp, H - 130 * dp);
                boolean ok = true;
                for (Planet p : planets) if (hyp(p.x - x, p.y - y) <= radiusOf(p) + 46 * dp) { ok = false; break; }
                if (ok) break;
            }
            ang = rf(0, TAU);
        }
        float sp = unit * rf(lvl.nsMin, lvl.nsMax);
        Asteroid a = new Asteroid();
        a.x = x; a.y = y; a.vx = cos(ang) * sp; a.vy = sin(ang) * sp; a.rad = rad;
        a.rot = rf(0, TAU); a.vr = rf(-.8f, .8f); a.o = -1;
        for (int i = 0; i < 8; i++) a.verts[i] = rf(.72f, 1.15f);
        neutrals.add(a);
    }

    // ---------- Điều quân ----------
    private void spreadPoint(float cx, float cy, int i, Rock out) {
        float sp = RR * 2.8f, rad = sp * (float) Math.sqrt(i + .5f), a = i * 2.39996f;
        out.ptx = clamp(cx + cos(a) * rad, 12 * dp, W - 12 * dp);
        out.pty = clamp(cy + sin(a) * rad, 70 * dp, H - 70 * dp);
        out.hasPt = true;
    }

    /** Lý do không gửi được theo hạn chế của màn, hoặc null. */
    public String blockReason(Planet src, float dx, float dy, boolean feed) {
        if (feed) return null;
        if (lvl.cooldown > 0 && src.cd > 0) return "Đang nạp đạn, chờ " + (int) Math.ceil(src.cd) + "s";
        if (hyp(dx - src.x, dy - src.y) > rangePx()) return "Ngoài tầm bay";
        return null;
    }

    public boolean farFrom(ArrayList<Planet> srcs, float dx, float dy) {
        float rp = rangePx();
        if (Float.isInfinite(rp)) return false;
        for (Planet s : srcs) if (hyp(dx - s.x, dy - s.y) > rp) return true;
        return false;
    }

    /** dest == null: bay tới vị trí trống (dx, dy) rồi chờ. Trả về số đá thực sự gửi. */
    int launch(Planet src, Planet dest, float dx, float dy, int count, boolean feed) {
        if (dest != null) { dx = dest.x; dy = dest.y; }
        count = Math.min(count, src.n);
        if (count <= 0) return 0;
        if (!feed) {
            if (blockReason(src, dx, dy, false) != null) return 0;
            if (lvl.cooldown > 0) src.cd = lvl.cooldown;
        }
        src.n -= count;
        float sr = radiusOf(src), aim = (float) Math.atan2(dy - src.y, dx - src.x), spd = unit * ROCK_SPEED;
        if (dest != null && !feed && dest.owner != src.owner) lastAttack = time;
        for (int i = 0; i < count; i++) {
            float a = feed ? rf(0, TAU) : aim + rf(-.8f, .8f);
            float s = spd * rf(.9f, 1.15f), k = feed ? .55f : 1f;
            Rock r = new Rock();
            r.x = src.x + cos(a) * (sr + 8 * dp); r.y = src.y + sin(a) * (sr + 8 * dp);
            r.vx = cos(a) * s * k; r.vy = sin(a) * s * k; r.s = s; r.o = src.owner;
            r.t = dest;
            if (dest == null) spreadPoint(dx, dy, i, r);
            r.feed = feed; r.rad = RR; r.ph = rf(0, TAU);
            rocks.add(r);
        }
        return count;
    }

    private void redirect(Rock r, Planet dest, float dx, float dy, int i) {
        r.idle = false; r.feed = false; r.dist = 0;
        if (dest != null) { r.t = dest; r.hasPt = false; if (dest.owner != r.o) lastAttack = time; }
        else { r.t = null; spreadPoint(dx, dy, i, r); }
    }

    private void addUpg(Planet p) {
        if (p.level >= maxLvl(p)) { p.n++; return; }
        p.upg++;
        if (p.upg >= upgradeCost(p.level)) {
            p.upg = 0; p.level++; p.flash = .8f; p.armor += ARMOR_PER_LEVEL;
            if (p.owner == 0) {
                popText(p.x, p.y - radiusOf(p) - 14 * dp, "Cấp " + p.level + ": +" + fmt1(rateOf(p)) + "/s, chứa " + capOf(p) + ", +" + ARMOR_PER_LEVEL + " máu", 0xFFFFD166);
                haptic(H_LIGHT); event(EV_LEVELUP);
            }
        }
    }

    private void capture(Planet p, int newO) {
        int old = p.owner;
        p.owner = newO; p.upg = 0; p.acc = 0; p.armor = 0; p.n = 0; p.flash = 1;
        p.level = 1; p.captured = true;                 // bị chiếm: về cấp 1, từ nay tối đa cấp 4
        if (newO == 0 || old == 0) popText(p.x, p.y - radiusOf(p) - 14 * dp, newO == 0 ? "Chiếm được: về cấp 1" : "Bị chiếm", FACTION_COLORS[newO]);
        haptic(newO == 0 ? H_LIGHT : H_HEAVY);
        if (newO == 0) event(EV_CAPTURE);
        boolean alive = false;
        for (Planet q : planets) if (q.owner == old) { alive = true; break; }
        if (!alive) for (Rock r : rocks) if (r.o == old) r.o = newO;   // đá của phe bị diệt chuyển cho bên chiếm
    }

    private void arrive(Rock r) {
        Planet p = r.t;
        r.dead = true;
        if (r.o == p.owner) { if (r.feed) addUpg(p); else p.n++; return; }
        if (r.o == 0 && lvl.fog) p.reveal = 3;
        if (p.armor > 0 || p.n > 0) {                   // trừ giáp trước, hết giáp mới mất đá canh gác
            if (p.armor > 0) p.armor--; else p.n--;
            burst(r.x, r.y, FACTION_LIGHT[r.o], 5); burst(r.x, r.y, FACTION_LIGHT[p.owner], 3);
        } else {
            capture(p, r.o); p.n = 1;
        }
    }

    private void asteroidHit(Asteroid a, Planet p) {
        a.dead = true; neutTimers.add(rf(3, 6));
        int dmg = asteroidDamage(a), left = dmg;
        while (left > 0 && (p.armor > 0 || p.n > 0)) { if (p.armor > 0) p.armor--; else p.n--; left--; }
        burst(a.x, a.y, 0xFFFFB36B, 8 + Math.round(a.rad / dp)); p.flash = .5f;
        popText(p.x, p.y - radiusOf(p) - 10 * dp, "-" + dmg, 0xFFFF9B6B);
        if (p.owner == 0) haptic(H_HEAVY);
    }

    // ---------- AI ----------
    private int incoming(Planet p) {
        int c = 0;
        for (Rock r : rocks) if (!r.dead && r.t == p && r.o != p.owner) c++;
        return c;
    }

    private void aiThink(Planet p) {
        float rp = rangePx();
        int cap = capOf(p);
        float full = (float) p.n / cap;
        float pressure = clamp((full - .55f) / .4f, 0, 1);       // kho càng đầy càng liều
        float bold = p.bold + (.9f - p.bold) * pressure;
        int threat = incoming(p);
        int keep = lvl.noProduction ? 3 : 10;
        int reserve = Math.min(p.n, Math.max(0, (int) Math.ceil(threat * 1.15f) - p.armor) + keep);
        int avail = p.n - reserve;
        for (Planet q : planets) {                                // chi viện đồng minh đang bị đánh
            if (q == p || q.owner != p.owner) continue;
            if (hyp(q.x - p.x, q.y - p.y) > rp) continue;
            int t = incoming(q);
            if (t > hpOf(q) * .7f && avail >= 8) avail -= launch(p, q, 0, 0, Math.min(avail, (int) Math.ceil(t * .8f)), false);
        }
        if (avail < (lvl.noProduction ? 5 : 8)) return;
        boolean canUp = p.level < maxLvl(p) && threat == 0;
        int want = upgradeCost(p.level) - p.upg;
        boolean mustAttack = full >= .85f;
        if (!mustAttack && canUp && avail >= want + 6 && rnd.nextFloat() < (full >= .65f ? .6f : .3f)) { launch(p, p, 0, 0, want, true); return; }
        Planet best = null, weak = null;
        float bs = 0;
        int bestNeed = 0, weakNeed = Integer.MAX_VALUE;
        for (Planet q : planets) {                                // mọi hành tinh khác phe đều là đối thủ
            if (q.owner == p.owner) continue;
            float d = hyp(q.x - p.x, q.y - p.y);
            if (d > rp) continue;
            float eta = d / (unit * ROCK_SPEED);
            int need = (int) Math.ceil((hpOf(q) + rateOf(q) * eta) * bold + 3);
            if (need < weakNeed) { weakNeed = need; weak = q; }
            if (avail < need) continue;
            float s = (1f / (need + 8)) * (1f / (.4f + d / unit));
            if (s > bs) { bs = s; best = q; bestNeed = need; }
        }
        if (best != null) { launch(p, best, 0, 0, Math.min(avail, bestNeed + (int) Math.ceil(bestNeed * .1f)), false); return; }
        if (canUp && avail >= want + 6) { launch(p, p, 0, 0, want, true); return; }
        if (mustAttack && weak != null) launch(p, weak, 0, 0, avail, false);
    }

    /** Chống bế tắc: lâu không ai tấn công thì hành tinh AI nhiều đá dư nhất đánh mục tiêu yếu nhất trong tầm. */
    private void forceAttack() {
        float rp = rangePx();
        int keep = lvl.noProduction ? 3 : 10, minA = lvl.noProduction ? 6 : 12;
        ArrayList<Planet> src = new ArrayList<Planet>();
        for (Planet p : planets) if (p.owner != 0 && p.n - Math.min(p.n, keep) >= minA) src.add(p);
        for (int i = 1; i < src.size(); i++)
            for (int j = i; j > 0 && src.get(j).n > src.get(j - 1).n; j--) { Planet t = src.get(j); src.set(j, src.get(j - 1)); src.set(j - 1, t); }
        for (Planet s : src) {
            int a = s.n - Math.min(s.n, keep);
            Planet tgt = null;
            int th = Integer.MAX_VALUE;
            for (Planet q : planets) {
                if (q.owner == s.owner || hyp(q.x - s.x, q.y - s.y) > rp) continue;
                int h = hpOf(q);
                if (h < th) { th = h; tgt = q; }
            }
            if (tgt != null && launch(s, tgt, 0, 0, (int) Math.floor(a * .6f), false) > 0) return;
        }
        lastAttack = time - LULL_SECONDS + 2;
    }

    // ---------- Mô phỏng ----------
    public void step(float dt) {
        if (over) return;
        time += dt;
        float grace = aiGrace();
        for (int i = 0; i < planets.size(); i++) {
            Planet p = planets.get(i);
            int cap = capOf(p);
            float rate = rateOf(p);
            if (p.cd > 0) p.cd = Math.max(0, p.cd - dt);
            if (p.n < cap && rate > 0) {
                p.acc += rate * dt;
                while (p.acc >= 1) { p.acc -= 1; p.n++; if (p.n >= cap) { p.acc = 0; break; } }
            } else p.acc = 0;
            if (p.owner != 0 && time >= grace) {
                p.think -= dt;
                if (p.think <= 0) { aiThink(p); p.think = rf(AI_THINK_MIN, AI_THINK_MAX); }
            }
        }
        if (time >= grace && time - lastAttack > LULL_SECONDS) forceAttack();

        float rlim = rangePx() * 1.4f;
        for (int i = 0; i < rocks.size(); i++) {
            Rock r = rocks.get(i);
            if (r.dead || r.idle) continue;
            float tx = r.t != null ? r.t.x : r.ptx, ty = r.t != null ? r.t.y : r.pty;
            float dx = tx - r.x, dy = ty - r.y, d = hyp(dx, dy);
            if (d == 0) d = 1;
            float sp = r.t != null ? r.s : Math.min(r.s, d * 5);       // tới gần vị trí trống thì giảm tốc
            float k = Math.min(1, (r.feed ? 2.4f : 5f) * dt);
            r.vx += (dx / d * sp - r.vx) * k; r.vy += (dy / d * sp - r.vy) * k;
            r.x += r.vx * dt; r.y += r.vy * dt;
            r.dist += hyp(r.vx, r.vy) * dt;
            if (!r.feed && r.dist > rlim) { r.dead = true; burst(r.x, r.y, FACTION_LIGHT[r.o], 3); continue; }
            if (r.t != null) { if (d < radiusOf(r.t) + 3 * dp) arrive(r); }
            else if (d < 3 * dp) { r.idle = true; r.vx = r.vy = 0; }
        }
        float m = 40 * dp;
        for (Asteroid a : neutrals) {
            a.x += a.vx * dt; a.y += a.vy * dt; a.rot += a.vr * dt;
            if (a.x < -m) a.x = W + m; else if (a.x > W + m) a.x = -m;
            if (a.y < -m) a.y = H + m; else if (a.y > H + m) a.y = -m;
        }
        if (lvl.asteroidHits) {
            for (Asteroid a : neutrals) {
                if (a.dead) continue;
                for (Planet p : planets) if (hyp(a.x - p.x, a.y - p.y) < radiusOf(p) + a.rad * .6f) { asteroidHit(a, p); break; }
            }
        }
        collide();
        sweep(rocks);
        sweep(neutrals);
        for (int i = neutTimers.size() - 1; i >= 0; i--) {
            float t = neutTimers.get(i) - dt;
            if (t <= 0) { neutTimers.remove(i); spawnNeutral(true); } else neutTimers.set(i, t);
        }
        if (selection != null && selection.total() == 0) selection = null;
        checkEnd();
    }

    private static <T extends Body> void sweep(ArrayList<T> list) {
        int w = 0;
        for (int i = 0; i < list.size(); i++) { T b = list.get(i); if (!b.dead) list.set(w++, b); }
        while (list.size() > w) list.remove(list.size() - 1);
    }

    /** Hai vật khác phe chạm nhau: cùng vỡ. Đá cùng phe đi xuyên qua nhau. Lưới ô vuông để chỉ so vật lân cận. */
    private void collide() {
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
            e.gi = i;
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
                        if (e.o == f.o && e.o != -1) continue;
                        float dx = e.x - f.x, dy = e.y - f.y, rr = e.rad + f.rad;
                        if (dx * dx + dy * dy < rr * rr) {
                            e.dead = true; f.dead = true;
                            float mx = (e.x + f.x) / 2, my = (e.y + f.y) / 2;
                            burst(mx, my, 0xFFFFFFFF, 4);
                            if (e.o >= 0) burst(mx, my, FACTION_LIGHT[e.o], 3);
                            if (f.o >= 0) burst(mx, my, FACTION_LIGHT[f.o], 3);
                            if (e.o == -1) neutTimers.add(rf(4, 8));
                            if (f.o == -1) neutTimers.add(rf(4, 8));
                        }
                    }
                }
            }
        }
    }

    private void checkEnd() {
        int mine = 0;
        for (Planet p : planets) if (p.owner == 0) mine++;
        if (mine == 0) { finish(false, "Hành tinh cuối cùng của bạn đã bị chiếm."); return; }
        if (mine == planets.size()) { finish(true, null); return; }
        if (lvl.timeLimit > 0 && time >= lvl.timeLimit) { finish(false, "Hết giờ trước khi chiếm đủ hành tinh."); return; }
        if (lvl.noProduction) {
            int t = 0;
            for (Planet p : planets) if (p.owner == 0) t += p.n;
            for (Rock r : rocks) if (r.o == 0) t++;
            if (t == 0) finish(false, "Bạn đã hết đá và không còn cách chiếm tiếp.");
        }
    }

    private void finish(boolean win, String reason) {
        over = true; ptr = null; selection = null;
        if (listener != null) listener.onFinish(win, reason);
    }

    // ---------- Hiệu ứng ----------
    private void burst(float x, float y, int color, int n) {
        if (parts.size() > 420) return;
        for (int i = 0; i < n; i++) {
            float a = rf(0, TAU), s = rf(20, 90) * dp;
            Particle q = new Particle();
            q.x = x; q.y = y; q.vx = cos(a) * s; q.vy = sin(a) * s;
            q.life = rf(.25f, .55f); q.max = .55f; q.color = color; q.size = rf(1, 2.2f) * dp;
            parts.add(q);
        }
    }

    private void popText(float x, float y, String s, int color) {
        FloatText t = new FloatText();
        t.x = x; t.y = y; t.text = s; t.color = color; t.life = 1.6f;
        texts.add(t);
    }

    /** Cập nhật hiệu ứng và đồng hồ hình ảnh. Không gọi khi tạm dừng: mọi thứ đứng yên thật sự. */
    public void fx(float dt) {
        clock += dt;
        for (int i = parts.size() - 1; i >= 0; i--) {
            Particle q = parts.get(i);
            q.life -= dt;
            if (q.life <= 0) { parts.remove(i); continue; }
            q.x += q.vx * dt; q.y += q.vy * dt; q.vx *= .94f; q.vy *= .94f;
        }
        for (int i = texts.size() - 1; i >= 0; i--) {
            FloatText t = texts.get(i);
            t.life -= dt;
            if (t.life <= 0) { texts.remove(i); continue; }
            t.y -= 22 * dp * dt;
        }
        for (Planet p : planets) {
            p.flash = Math.max(0, p.flash - dt * 1.4f);
            if (p.reveal > 0) p.reveal = Math.max(0, p.reveal - dt);
        }
    }

    // ---------- Quỹ đạo ----------
    /** Vị trí các viên đá đang quay quanh hành tinh (tối đa 60). Dùng chung cho vẽ và cho vòng khoanh. */
    public int orbitDots(Planet p, float[] ox, float[] oy) {
        float R = radiusOf(p), gap = Math.max(6 * dp, unit * .017f), first = Math.max(9 * dp, unit * .026f);
        int dc = Math.min(p.n, 60), idx = 0, ring = 0;
        while (idx < dc) {
            int m = Math.min(10 + ring * 6, dc - idx);
            float rad = R + first + ring * gap, sp = (ring % 2 == 1 ? -1 : 1) * (.9f / (1 + ring * .45f));
            for (int k = 0; k < m; k++) {
                float a = p.seed + clock * sp + k * TAU / m;
                ox[idx + k] = p.x + cos(a) * rad; oy[idx + k] = p.y + sin(a) * rad;
            }
            idx += m; ring++;
        }
        return dc;
    }

    public int orbitRings(Planet p, float[] radii) {
        float R = radiusOf(p), gap = Math.max(6 * dp, unit * .017f), first = Math.max(9 * dp, unit * .026f);
        int dc = Math.min(p.n, 60), idx = 0, ring = 0;
        while (idx < dc && ring < radii.length) { radii[ring] = R + first + ring * gap; idx += Math.min(10 + ring * 6, dc - idx); ring++; }
        return ring;
    }

    // ---------- Vòng khoanh ----------
    static boolean inPoly(float x, float y, float[] xs, float[] ys, int n) {
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((ys[i] > y) != (ys[j] > y) && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) inside = !inside;
        }
        return inside;
    }

    static float bboxMin(float[] xs, float[] ys, int n) {
        float x0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) { x0 = Math.min(x0, xs[i]); x1 = Math.max(x1, xs[i]); y0 = Math.min(y0, ys[i]); y1 = Math.max(y1, ys[i]); }
        return Math.min(x1 - x0, y1 - y0);
    }

    public Selection computeSelection(float[] xs, float[] ys, int n) {
        Selection s = new Selection();
        if (n >= 3) {
            for (Planet p : planets) {
                if (p.owner != 0 || p.n <= 0) continue;
                int dc = orbitDots(p, dotX, dotY), ic = 0;
                for (int i = 0; i < dc; i++) if (inPoly(dotX[i], dotY[i], xs, ys, n)) ic++;
                if (ic > 0) s.addGroup(p, Math.min(p.n, ic >= dc ? p.n : Math.max(1, Math.round((float) ic / dc * p.n))));
            }
            for (Rock r : rocks) if (!r.dead && r.o == 0 && !r.feed && inPoly(r.x, r.y, xs, ys, n)) s.loose.add(r);
        }
        s.xs = Arrays.copyOf(xs, n); s.ys = Arrays.copyOf(ys, n); s.n = n;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { cx += xs[i]; cy += ys[i]; }
        s.cx = n > 0 ? cx / n : 0; s.cy = n > 0 ? cy / n : 0;
        return s;
    }

    /** Số viên đá quỹ đạo của p cần sáng lên vì đang được chọn. */
    public int highlightCount(Planet p) {
        Selection s = ptr != null && ptr.mode == M_LASSO ? ptr.live : selection;
        if (s == null || p.n <= 0) return 0;
        int c = s.countFor(p);
        if (c == 0) return 0;
        return Math.round(Math.min(c, p.n) / (float) p.n * Math.min(p.n, 60));
    }

    // ---------- Cử chỉ một ngón tay ----------
    public static final int M_UNDECIDED = 0, M_QUICK = 1, M_LASSO = 2, M_CARRY = 3, M_IGNORE = 4;

    public static final class Pointer {
        public float sx, sy, x, y, len, lockX, lockY;
        public Planet startPlanet, hover;
        public int mode;
        public final ArrayList<Planet> qsel = new ArrayList<Planet>();
        public float[] px = new float[256], py = new float[256];
        public int pn;
        public Selection live;

        void add(float x, float y) {
            if (pn == px.length) { px = Arrays.copyOf(px, pn * 2); py = Arrays.copyOf(py, pn * 2); }
            px[pn] = x; py[pn] = y; pn++;
        }
    }

    public Planet hit(float x, float y) {
        Planet best = null;
        float bd = Float.MAX_VALUE, slop = Math.max(22 * dp, unit * .05f);
        for (Planet p : planets) {
            float d = hyp(p.x - x, p.y - y);
            if (d <= radiusOf(p) + slop && d < bd) { best = p; bd = d; }
        }
        return best;
    }

    public int quickCount(Planet s) { return s.n >= 1 ? Math.max(1, (int) Math.floor(s.n * QUICK_SEND)) : 0; }

    public void down(float x, float y) {
        if (over) return;
        ptr = new Pointer();
        ptr.sx = ptr.x = x; ptr.sy = ptr.y = y;
        ptr.startPlanet = ptr.hover = hit(x, y);
        ptr.add(x, y);
    }

    public void move(float x, float y) {
        if (ptr == null) return;
        ptr.x = x; ptr.y = y; ptr.hover = hit(x, y);
        if (ptr.mode == M_UNDECIDED && hyp(x - ptr.sx, y - ptr.sy) > 12 * dp) {
            if (ptr.startPlanet != null && ptr.startPlanet.owner == 0) { ptr.mode = M_QUICK; ptr.qsel.add(ptr.startPlanet); selection = null; }
            else if (ptr.startPlanet == null) ptr.mode = M_LASSO;
            else ptr.mode = M_IGNORE;
        }
        if (ptr.mode == M_QUICK) {
            Planet h = ptr.hover;
            if (h != null && h.owner == 0 && !ptr.qsel.contains(h)) ptr.qsel.add(h);   // kéo qua nhiều hành tinh = chọn thêm
        } else if (ptr.mode == M_LASSO) {
            float d = hyp(x - ptr.px[ptr.pn - 1], y - ptr.py[ptr.pn - 1]);
            if (d > 5 * dp) { ptr.add(x, y); ptr.len += d; }
            if (lassoClosed(ptr)) tryLock();                                        // khép vòng: khóa vùng chọn, tay tiếp tục kéo tới đích
        }
    }

    public void up(float x, float y) {
        if (ptr == null) return;
        Pointer p = ptr;
        ptr = null;
        if (over) return;
        Planet h = hit(x, y);
        if (p.mode == M_UNDECIDED) {
            if (selection != null) dispatch(selection, h, x, y);
            else if (h != null && h.owner == 0) upgradeTap(h);
        } else if (p.mode == M_QUICK) quickSend(p.qsel, h, x, y);
        else if (p.mode == M_LASSO) finishLasso(p);
        else if (p.mode == M_CARRY) { if (hyp(x - p.lockX, y - p.lockY) > 36 * dp) dispatch(selection, h, x, y); }
    }

    public void cancelPointer() { ptr = null; }
    public void cancelSelection() { selection = null; }

    /** Gọi trước khi vẽ để có vùng chọn tạm thời khi đang khoanh. */
    public void updateLive() {
        if (ptr != null && ptr.mode == M_LASSO) ptr.live = ptr.pn >= 3 ? computeSelection(ptr.px, ptr.py, ptr.pn) : null;
    }

    private boolean lassoClosed(Pointer p) {
        if (p.pn < 10 || p.len < 110 * dp || bboxMin(p.px, p.py, p.pn) < 36 * dp) return false;
        return hyp(p.x - p.px[0], p.y - p.py[0]) < Math.max(26 * dp, p.len * .1f);
    }

    private void tryLock() {
        Selection s = computeSelection(ptr.px, ptr.py, ptr.pn);
        if (s.total() > 0) {
            selection = s; haptic(H_LIGHT); event(EV_LASSO);
            ptr.mode = M_CARRY; ptr.lockX = ptr.x; ptr.lockY = ptr.y;
        } else {
            toast("Vòng khoanh chưa có đá");
            ptr.pn = 0; ptr.add(ptr.x, ptr.y); ptr.len = 0;
        }
    }

    private void finishLasso(Pointer p) {
        if (p.len < 90 * dp || bboxMin(p.px, p.py, p.pn) < 28 * dp) { toast("Khoanh vòng quanh đá để chọn"); return; }
        Selection s = computeSelection(p.px, p.py, p.pn);
        if (s.total() > 0) { selection = s; haptic(H_LIGHT); event(EV_LASSO); }
        else toast("Vòng khoanh chưa có đá");
    }

    private void upgradeTap(Planet p) {
        if (p.level >= maxLvl(p)) { toast(capMsg(p)); return; }
        int c = Math.min(upgradeCost(p.level) - p.upg, p.n);
        if (c < 1) { toast("Hành tinh hết đá"); return; }
        if (launch(p, p, 0, 0, c, true) > 0) event(EV_UPGRADE);
    }

    private void quickSend(ArrayList<Planet> qsel, Planet h, float x, float y) {
        selection = null;
        if (h != null && qsel.size() == 1 && h == qsel.get(0)) return;           // kéo về chính nó: hủy
        float dx = h != null ? h.x : clamp(x, 14 * dp, W - 14 * dp), dy = h != null ? h.y : clamp(y, 70 * dp, H - 70 * dp);
        int any = 0;
        String reason = null;
        for (Planet s : qsel) {
            if (s == h) continue;
            String why = blockReason(s, dx, dy, false);
            if (why != null) { reason = why; continue; }
            int c = quickCount(s);
            if (c > 0 && launch(s, h, dx, dy, c, false) > 0) any++;
        }
        if (any == 0) toast(reason != null ? reason : "Hết đá để gửi");
        else event(h == null ? EV_POINT : (h.owner == 0 ? EV_MOVE : EV_ATTACK));
    }

    private void dispatch(Selection sel, Planet h, float x, float y) {
        selection = null;
        float dx = h != null ? h.x : clamp(x, 14 * dp, W - 14 * dp), dy = h != null ? h.y : clamp(y, 70 * dp, H - 70 * dp);
        String reason = null;
        boolean sent = false, fed = false;
        for (int i = 0; i < sel.gp.size(); i++) {
            Planet p = sel.gp.get(i);
            if (p.owner != 0 || p.n <= 0) continue;
            int c = Math.min(sel.gc[i], p.n);
            if (c <= 0) continue;
            if (h != null && h == p) {                                           // thả lại chính hành tinh nguồn: nạp nâng cấp
                if (p.level >= maxLvl(p)) { toast(capMsg(p)); continue; }
                if (launch(p, p, 0, 0, c, true) > 0) fed = true;
            } else {
                String why = blockReason(p, dx, dy, false);
                if (why != null) { reason = why; continue; }
                if (launch(p, h, dx, dy, c, false) > 0) sent = true;
            }
        }
        int li = 0;
        for (Rock r : sel.loose) if (!r.dead && r.o == 0) { redirect(r, h, dx, dy, li++); sent = true; }
        if (reason != null) toast(reason);
        if (fed) event(EV_UPGRADE);
        if (sent) event(h == null ? EV_POINT : (h.owner == 0 ? EV_MOVE : EV_ATTACK));
    }

    // ---------- Truy vấn cho HUD ----------
    public int playerPlanets() { int c = 0; for (Planet p : planets) if (p.owner == 0) c++; return c; }
    public int playerRocks() {
        int t = 0;
        for (Planet p : planets) if (p.owner == 0) t += p.n;
        for (Rock r : rocks) if (r.o == 0) t++;
        return t;
    }
}
