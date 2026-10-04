package com.planetconquest.game.engine;

import com.planetconquest.game.engine.ai.AiStrategy;
import com.planetconquest.game.engine.ai.DefaultAi;
import com.planetconquest.game.engine.ai.PassiveAi;
import com.planetconquest.game.engine.fx.Effects;
import com.planetconquest.game.engine.input.GestureController;
import com.planetconquest.game.engine.input.Pointer;
import com.planetconquest.game.engine.level.Level;
import com.planetconquest.game.engine.level.Levels;
import com.planetconquest.game.engine.level.MapGenerator;
import com.planetconquest.game.engine.model.*;
import com.planetconquest.game.engine.physics.CollisionGrid;
import com.planetconquest.game.engine.rules.RuleSet;

import static com.planetconquest.game.engine.util.MathUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Luật chơi và vòng mô phỏng; điều phối các thành phần: AiStrategy (đối thủ), GestureController (cử chỉ),
 * CollisionGrid (va chạm), MapGenerator (bản đồ), Effects (hiệu ứng).
 * Java thuần (không import Android) để chạy được cả trên JVM khi kiểm thử (xem app/src/test).
 * Mọi khoảng cách tính theo pixel; hằng số giao diện nhân với dp.
 */
public final class Engine {

    public interface Listener {
        void onNotice(Notice notice);
        void onHaptic(Haptic kind);
        void onEvent(GameEvent ev);
        void onFinish(boolean win, EndReason reason);
    }

    // ---------- Cân bằng ----------
    public static final float PRODUCE_BASE = 1f, PRODUCE_STEP = .5f;
    public static final int MAX_LEVEL = 5, CAPTURED_MAX_LEVEL = 4, ARMOR_PER_LEVEL = 10;
    public static final float ROCK_SPEED = .55f, QUICK_SEND = .5f;
    public static final float AI_GRACE = 10f, AI_THINK_MIN = 3f, AI_THINK_MAX = 5.5f, LULL_SECONDS = 14f;
    public static final int NEUTRALS = 6;
    public static final float ASTEROID_DMG_K = 11000f;

    public static int upgradeCost(int level) { return 10 + 8 * level; }
    public static int capacity(int level) { return 40 + 20 * level; }

    // ---------- Trạng thái ----------
    private float W = 360, H = 640, dp = 1, unit = 360, rockRadius = 2.8f;
    private Level lvl = Levels.ALL[0];
    final ArrayList<Planet> planets = new ArrayList<Planet>();
    final ArrayList<Rock> rocks = new ArrayList<Rock>();
    final ArrayList<Asteroid> neutrals = new ArrayList<Asteroid>();
    private final List<Planet> planetsView = Collections.unmodifiableList(planets);
    private final List<Rock> rocksView = Collections.unmodifiableList(rocks);
    private final List<Asteroid> neutralsView = Collections.unmodifiableList(neutrals);
    private final ArrayList<Float> neutTimers = new ArrayList<Float>();
    private float time, clock;
    private OrbitPattern orbit = new OrbitPattern(1, 1);
    private final float[] orbitTmp = new float[2];
    private boolean over;
    private float lastAttack;

    private final Random rnd = new Random();
    public final Effects effects = new Effects(rnd);
    private final GestureController gestures = new GestureController(this);
    private final CollisionGrid collisions = new CollisionGrid();
    private MapGenerator map = new MapGenerator(W, H, dp, unit);
    private AiStrategy ai = new DefaultAi();
    private Listener listener;

    // Kích thước màn hình (pixel), mật độ dp, đơn vị tỉ lệ và cỡ viên đá; chỉ setSize đổi được.
    public float W() { return W; }
    public float H() { return H; }
    public float dp() { return dp; }
    public float unit() { return unit; }
    public float rockRadius() { return rockRadius; }
    /** Màn đang chơi. */
    public Level level() { return lvl; }
    /** Giây đã chơi (đồng hồ logic) và đồng hồ hình ảnh. */
    public float time() { return time; }
    public float clock() { return clock; }
    public boolean over() { return over; }

    public void setListener(Listener l) { listener = l; }
    public void notice(Msg msg, Object... args) { notice(new Notice(msg, args)); }
    public void notice(Notice n) { if (listener != null) listener.onNotice(n); }
    public void haptic(Haptic k) { if (listener != null) listener.onHaptic(k); }
    public void event(GameEvent e) { if (listener != null) listener.onEvent(e); }

    float randRange(float a, float b) { return a + rnd.nextFloat() * (b - a); }
    public float randFloat() { return rnd.nextFloat(); }
    /** Hoãn cơ chế chống bế tắc thêm một lúc. */
    public void postponeLull() { lastAttack = time - LULL_SECONDS + 2; }

    // Truy cập chỉ đọc cho giao diện, AI và cử chỉ; chỉ Engine (và kiểm thử cùng package) được sửa trạng thái.
    public List<Planet> planets() { return planetsView; }
    public List<Rock> rocks() { return rocksView; }
    public List<Asteroid> neutrals() { return neutralsView; }

    // ---------- Cử chỉ (ủy quyền cho GestureController) ----------
    public Pointer pointer() { return gestures.pointer(); }
    public Selection selection() { return gestures.selection(); }
    public void down(float x, float y) { gestures.down(x, y); }
    public void move(float x, float y) { gestures.move(x, y); }
    public void up(float x, float y) { gestures.up(x, y); }
    public void cancelPointer() { gestures.cancelPointer(); }
    public void cancelSelection() { gestures.cancelSelection(); }
    /** Gọi trước khi vẽ để có vùng chọn tạm thời khi đang khoanh. */
    public void updateLive() { gestures.updateLive(); }
    public int highlightCount(Planet p) { return gestures.highlightCount(p); }
    public int quickCount(Planet s) { return gestures.quickCount(s); }

    // ---------- Luật theo màn ----------
    public int maxLvl(Planet p) { return rules().maxLevel(p, p.captured() ? CAPTURED_MAX_LEVEL : MAX_LEVEL); }
    public int capOf(Planet p) { return rules().capacity(p, capacity(p.level())); }
    public boolean fogged(Planet p) { return rules().hidesInfo(p); }
    public float rangePx() { float f = rules().rangeFraction(); return f > 0 ? unit * f : Float.POSITIVE_INFINITY; }
    public RuleSet rules() { return lvl.rules; }
    public float aiGrace() { return lvl.passiveAi ? 1e9f : (lvl.aiGrace >= 0 ? lvl.aiGrace : AI_GRACE); }
    public float rateOf(Planet p) {
        return rules().productionRate(p, PRODUCE_BASE + PRODUCE_STEP * (p.level() - 1));
    }
    public int asteroidDamage(Asteroid a) { float k = a.rad / unit; return Math.max(2, Math.round(k * k * ASTEROID_DMG_K)); }
    public Notice capMsg(Planet p) {
        Notice blocked = rules().upgradeBlocked();
        if (blocked != null) return blocked;
        return p.captured() ? new Notice(Msg.CAPTURED_MAX_LEVEL, CAPTURED_MAX_LEVEL) : new Notice(Msg.MAX_LEVEL_REACHED);
    }

    // ---------- Kích thước & bản đồ ----------
    public void setSize(float w, float h, float density) {
        W = w; H = h; dp = density;
        unit = Math.min(W, H * 0.6f);
        rockRadius = Math.max(2.4f * dp, unit * 0.0075f);
        map = new MapGenerator(W, H, dp, unit);
        effects.setDensity(dp);
        orbit = new OrbitPattern(dp, unit);
        placePlanets();
    }

    private void placePlanets() {
        for (Planet p : planets) p.place(p.nx() * W, map.y(p.ny()), unit * p.size());
    }

    // ---------- Khởi tạo màn ----------
    public void start(Level L) {
        lvl = L;
        Random r = new Random(L.seed);
        float[][] pts = L.fixed != null ? L.fixed : map.layout(r, L.planets, L.rules.rangeFraction() > 0 ? unit * L.rules.rangeFraction() : 0);
        planets.clear();
        for (int i = 0; i < pts.length; i++) {
            int startRocks = i == 0 ? L.playerN : Math.round(L.enemyMin + r.nextFloat() * (L.enemyMax - L.enemyMin));
            Planet p = new Planet(i, i, startRocks, i > 0 ? L.enemyArmor : 0, pts[i][0], pts[i][1], pts[i][2]);
            p.ai.bold = i == 0 ? 0 : clamp(1f + r.nextFloat() * .4f + L.boldShift, .85f, 1.5f);
            p.ai.think = aiGrace() + randRange(0, 3);
            p.visual.seed = randRange(0, TAU);
            planets.add(p);
        }
        rocks.clear(); neutrals.clear(); neutTimers.clear(); effects.clear();
        ai = L.passiveAi ? new PassiveAi() : new DefaultAi();
        time = 0; over = false; gestures.reset();
        lastAttack = aiGrace() + 8;
        placePlanets();
        int nn = L.neutrals >= 0 ? L.neutrals : NEUTRALS;
        for (int i = 0; i < nn; i++) spawnNeutral(false);
    }

    private void spawnNeutral(boolean fromEdge) {
        float rad = unit * randRange(lvl.nrMin, lvl.nrMax) * lvl.neutralSize;
        float x = 0, y = 0, ang;
        if (fromEdge) {
            int side = rnd.nextInt(4);
            float yMin = 110 * dp, yMax = H - 130 * dp;
            if (side == 0) { x = -30 * dp; y = randRange(yMin, yMax); }
            else if (side == 1) { x = W + 30 * dp; y = randRange(yMin, yMax); }
            else if (side == 2) { x = randRange(20 * dp, W - 20 * dp); y = -30 * dp; }
            else { x = randRange(20 * dp, W - 20 * dp); y = H + 30 * dp; }
            Planet tgt = rules().asteroidsHitPlanets() && !planets.isEmpty() && rnd.nextFloat() < .6f ? planets.get(rnd.nextInt(planets.size())) : null;
            ang = tgt != null ? (float) Math.atan2(tgt.y() - y, tgt.x() - x) + randRange(-.25f, .25f)
                              : (float) Math.atan2(H / 2 - y, W / 2 - x) + randRange(-.9f, .9f);
        } else {
            for (int t = 0; t < 30; t++) {
                x = randRange(24 * dp, W - 24 * dp); y = randRange(110 * dp, H - 130 * dp);
                boolean ok = true;
                for (Planet p : planets) if (hyp(p.x() - x, p.y() - y) <= p.radius() + 46 * dp) { ok = false; break; }
                if (ok) break;
            }
            ang = randRange(0, TAU);
        }
        float sp = unit * randRange(lvl.nsMin, lvl.nsMax);
        Asteroid a = new Asteroid();
        a.x = x; a.y = y; a.vx = cos(ang) * sp; a.vy = sin(ang) * sp; a.rad = rad;
        a.rot = randRange(0, TAU); a.vr = randRange(-.8f, .8f); a.owner = Faction.NEUTRAL;
        for (int i = 0; i < 8; i++) a.verts[i] = randRange(.72f, 1.15f);
        neutrals.add(a);
    }

    // ---------- Điều quân ----------
    private void spreadPoint(float cx, float cy, int i, Rock out) {
        float sp = rockRadius * 2.8f, rad = sp * (float) Math.sqrt(i + .5f), a = i * 2.39996f;
        out.setTargetPoint(clamp(cx + cos(a) * rad, 12 * dp, W - 12 * dp), clamp(cy + sin(a) * rad, 70 * dp, H - 70 * dp));
    }

    /** Lý do không gửi được theo hạn chế của màn, hoặc null. */
    public Notice blockReason(Planet src, float dx, float dy, boolean feed) {
        if (feed) return null;
        return rules().canLaunch(this, src, dx, dy);
    }

    public boolean farFrom(ArrayList<Planet> srcs, float dx, float dy) {
        float rp = rangePx();
        if (Float.isInfinite(rp)) return false;
        for (Planet s : srcs) if (hyp(dx - s.x(), dy - s.y()) > rp) return true;
        return false;
    }

    /** dest == null: bay tới vị trí trống (dx, dy) rồi chờ. Trả về số đá thực sự gửi. */
    public int launch(Planet src, Planet dest, float dx, float dy, int count, boolean feed) {
        if (dest != null) { dx = dest.x(); dy = dest.y(); }
        count = Math.min(count, src.rocks());
        if (count <= 0) return 0;
        if (!feed) {
            if (blockReason(src, dx, dy, false) != null) return 0;
            rules().onLaunch(src);
        }
        src.addRocks(-count);
        float sr = src.radius(), aim = (float) Math.atan2(dy - src.y(), dx - src.x()), spd = unit * ROCK_SPEED;
        if (dest != null && !feed && dest.owner() != src.owner()) lastAttack = time;
        for (int i = 0; i < count; i++) {
            float a = feed ? randRange(0, TAU) : aim + randRange(-.8f, .8f);
            float s = spd * randRange(.9f, 1.15f), k = feed ? .55f : 1f;
            Rock r = new Rock();
            r.x = src.x() + cos(a) * (sr + 8 * dp); r.y = src.y() + sin(a) * (sr + 8 * dp);
            r.vx = cos(a) * s * k; r.vy = sin(a) * s * k; r.s = s; r.owner = src.owner();
            if (dest != null) r.setTarget(dest); else spreadPoint(dx, dy, i, r);
            r.feed = feed; r.rad = rockRadius; r.ph = randRange(0, TAU);
            rocks.add(r);
        }
        return count;
    }

    public void redirect(Rock r, Planet dest, float dx, float dy, int i) {
        r.idle = false; r.feed = false; r.dist = 0;
        if (dest != null) { r.setTarget(dest); if (dest.owner() != r.owner) lastAttack = time; }
        else spreadPoint(dx, dy, i, r);
    }

    private void addUpg(Planet p) {
        if (p.level() >= maxLvl(p)) { p.addRocks(1); return; }
        if (p.addUpgradeProgress() >= upgradeCost(p.level())) {
            p.levelUp(ARMOR_PER_LEVEL); p.visual.flash = .8f;
            if (Faction.isPlayer(p.owner())) {
                effects.popText(p.x(), p.y() - p.radius() - 14 * dp, new Notice(Msg.PLANET_LEVEL_UP, p.level(), rateOf(p), capOf(p), ARMOR_PER_LEVEL), 0xFFFFD166);
                haptic(Haptic.LIGHT); event(GameEvent.LEVELUP);
            }
        }
    }

    private void capture(Planet p, int newO) {
        int old = p.owner();
        p.capturedBy(newO);                             // về cấp 1, từ nay tối đa cấp 4
        p.visual.flash = 1;
        if (Faction.isPlayer(newO) || Faction.isPlayer(old)) effects.popText(p.x(), p.y() - p.radius() - 14 * dp, new Notice(Faction.isPlayer(newO) ? Msg.PLANET_CAPTURED : Msg.PLANET_LOST), Faction.COLORS[newO]);
        haptic(Faction.isPlayer(newO) ? Haptic.LIGHT : Haptic.HEAVY);
        if (Faction.isPlayer(newO)) event(GameEvent.CAPTURE);
        boolean alive = false;
        for (Planet q : planets) if (q.owner() == old) { alive = true; break; }
        if (!alive) for (Rock r : rocks) if (r.owner == old) r.owner = newO;   // đá của phe bị diệt chuyển cho bên chiếm
    }

    private void arrive(Rock r) {
        Planet p = r.targetPlanet();
        r.dead = true;
        if (r.owner == p.owner()) { if (r.feed) addUpg(p); else p.addRocks(1); return; }
        rules().onHit(p, r.owner);
        if (p.takeHit()) {                                  // trừ giáp trước, hết giáp mới mất đá canh gác
            effects.burst(r.x, r.y, Faction.LIGHT[r.owner], 5); effects.burst(r.x, r.y, Faction.LIGHT[p.owner()], 3);
        } else {
            capture(p, r.owner); p.setRocks(1);
        }
    }

    private void asteroidHit(Asteroid a, Planet p) {
        a.dead = true; neutTimers.add(randRange(3, 6));
        int dmg = asteroidDamage(a), left = dmg;
        while (left > 0 && p.takeHit()) left--;
        effects.burst(a.x, a.y, 0xFFFFB36B, 8 + Math.round(a.rad / dp)); p.visual.flash = .5f;
        effects.popText(p.x(), p.y() - p.radius() - 10 * dp, new Notice(Msg.ASTEROID_DAMAGE, dmg), 0xFFFF9B6B);
        if (Faction.isPlayer(p.owner())) haptic(Haptic.HEAVY);
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
            p.tickCooldown(dt);
            p.produce(rate, dt, cap);
            if (!Faction.isPlayer(p.owner()) && time >= grace) {
                p.ai.think -= dt;
                if (p.ai.think <= 0) { ai.think(this, p); p.ai.think = randRange(AI_THINK_MIN, AI_THINK_MAX); }
            }
        }
        if (time >= grace && time - lastAttack > LULL_SECONDS) ai.onLull(this);

        float rlim = rangePx() * 1.4f;
        for (int i = 0; i < rocks.size(); i++) {
            Rock r = rocks.get(i);
            if (r.dead || r.idle) continue;
            float tx = r.targetX(), ty = r.targetY();
            float dx = tx - r.x, dy = ty - r.y, d = hyp(dx, dy);
            if (d == 0) d = 1;
            float sp = r.targetPlanet() != null ? r.s : Math.min(r.s, d * 5);       // tới gần vị trí trống thì giảm tốc
            float k = Math.min(1, (r.feed ? 2.4f : 5f) * dt);
            r.vx += (dx / d * sp - r.vx) * k; r.vy += (dy / d * sp - r.vy) * k;
            r.x += r.vx * dt; r.y += r.vy * dt;
            r.dist += hyp(r.vx, r.vy) * dt;
            if (!r.feed && r.dist > rlim) { r.dead = true; effects.burst(r.x, r.y, Faction.LIGHT[r.owner], 3); continue; }
            if (r.targetPlanet() != null) { if (d < r.targetPlanet().radius() + 3 * dp) arrive(r); }
            else if (d < 3 * dp) { r.idle = true; r.vx = r.vy = 0; }
        }
        float m = 40 * dp;
        for (Asteroid a : neutrals) {
            a.x += a.vx * dt; a.y += a.vy * dt; a.rot += a.vr * dt;
            if (a.x < -m) a.x = W + m; else if (a.x > W + m) a.x = -m;
            if (a.y < -m) a.y = H + m; else if (a.y > H + m) a.y = -m;
        }
        if (rules().asteroidsHitPlanets()) {
            for (Asteroid a : neutrals) {
                if (a.dead) continue;
                for (Planet p : planets) if (hyp(a.x - p.x(), a.y - p.y()) < p.radius() + a.rad * .6f) { asteroidHit(a, p); break; }
            }
        }
        collisions.resolve(rocks, neutrals, W, H, dp, new CollisionGrid.Listener() {
            @Override public void onCollide(Body a, Body b) { onCollision(a, b); }
        });
        sweep(rocks);
        sweep(neutrals);
        for (int i = neutTimers.size() - 1; i >= 0; i--) {
            float t = neutTimers.get(i) - dt;
            if (t <= 0) { neutTimers.remove(i); spawnNeutral(true); } else neutTimers.set(i, t);
        }
        gestures.pruneSelection();
        checkEnd();
    }

    private void onCollision(Body e, Body f) {
        float mx = (e.x + f.x) / 2, my = (e.y + f.y) / 2;
        effects.burst(mx, my, 0xFFFFFFFF, 4);
        if (e.owner >= 0) effects.burst(mx, my, Faction.LIGHT[e.owner], 3);
        if (f.owner >= 0) effects.burst(mx, my, Faction.LIGHT[f.owner], 3);
        if (e.owner == Faction.NEUTRAL) neutTimers.add(randRange(4, 8));
        if (f.owner == Faction.NEUTRAL) neutTimers.add(randRange(4, 8));
    }

    private static <T extends Body> void sweep(ArrayList<T> list) {
        int w = 0;
        for (int i = 0; i < list.size(); i++) { T b = list.get(i); if (!b.dead) list.set(w++, b); }
        while (list.size() > w) list.remove(list.size() - 1);
    }

    private void checkEnd() {
        int mine = 0;
        for (Planet p : planets) if (Faction.isPlayer(p.owner())) mine++;
        if (mine == 0) { finish(false, EndReason.ALL_PLANETS_LOST); return; }
        if (mine == planets.size()) { finish(true, null); return; }
        int limit = rules().timeLimit();
        if (limit > 0 && time >= limit) { finish(false, EndReason.TIME_UP); return; }
        if (rules().finiteRocks()) {
            int t = 0;
            for (Planet p : planets) if (Faction.isPlayer(p.owner())) t += p.rocks();
            for (Rock r : rocks) if (Faction.isPlayer(r.owner)) t++;
            if (t == 0) finish(false, EndReason.OUT_OF_ROCKS);
        }
    }

    private void finish(boolean win, EndReason reason) {
        over = true; gestures.reset();
        if (listener != null) listener.onFinish(win, reason);
    }

    /** Cập nhật hiệu ứng và đồng hồ hình ảnh. Không gọi khi tạm dừng: mọi thứ đứng yên thật sự. */
    public void fx(float dt) {
        clock += dt;
        effects.update(dt);
        for (Planet p : planets) {
            p.visual.fade(dt);
            p.tickReveal(dt);
        }
    }

    // ---------- Quỹ đạo ----------
    /** Vị trí các viên đá đang bay hỗn loạn quanh hành tinh (tối đa 60). Dùng chung cho vẽ và cho vòng khoanh. */
    public int orbitDots(Planet p, float[] ox, float[] oy) {
        int dc = Math.min(p.rocks(), OrbitPattern.MAX_DOTS);
        for (int i = 0; i < dc; i++) {
            orbit.position(i, p.visual.seed, clock, p.x(), p.y(), p.radius(), orbitTmp);
            ox[i] = orbitTmp[0]; oy[i] = orbitTmp[1];
        }
        return dc;
    }

    /** Bán kính ngoài của dải quỹ đạo (bằng bán kính hành tinh nếu không còn đá chờ). */
    public float orbitOuterRadius(Planet p) {
        return p.rocks() > 0 ? orbit.outerRadius(p.radius()) : p.radius();
    }

    // ---------- Truy vấn cho HUD ----------
    public int playerPlanets() { int c = 0; for (Planet p : planets) if (Faction.isPlayer(p.owner())) c++; return c; }
    public int playerRocks() {
        int t = 0;
        for (Planet p : planets) if (Faction.isPlayer(p.owner())) t += p.rocks();
        for (Rock r : rocks) if (Faction.isPlayer(r.owner)) t++;
        return t;
    }
}
