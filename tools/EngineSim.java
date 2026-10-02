import com.planetconquest.game.engine.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * Kiểm thử engine trên JVM (không cần thiết bị Android).
 * Chạy: ./build.sh test
 */
public final class EngineSim implements Engine.Listener {
    final Engine e = new Engine();
    final ArrayList<Integer> events = new ArrayList<Integer>();
    String lastToast = "", finishReason;
    Boolean finishWin;
    int fails = 0;

    public void onToast(String m) { lastToast = m; }
    public void onHaptic(int k) {}
    public void onEvent(int ev) { events.add(ev); }
    public void onFinish(boolean win, String reason) { finishWin = win; finishReason = reason; }

    void check(boolean ok, String msg) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + msg);
        if (!ok) fails++;
    }

    void tick(int frames) { for (int i = 0; i < frames; i++) { e.step(1 / 60f); e.fx(1 / 60f); } }
    void secs(float s) { tick(Math.round(s * 60)); }
    void start(Level L) { finishWin = null; finishReason = null; events.clear(); e.start(L); }
    Planet me() { return e.planets.get(0); }

    void drag(Planet a, float bx, float by) {
        e.down(a.x, a.y);
        e.move((a.x + bx) / 2, (a.y + by) / 2);
        e.move(bx, by);
        e.up(bx, by);
    }
    void tap(float x, float y) { e.down(x, y); e.up(x, y); }
    void circle(float cx, float cy, float r) {
        e.down(cx + r, cy);
        for (int i = 1; i <= 40; i++) { double a = i / 40.0 * Math.PI * 2; e.move(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r); }
    }

    float minDist() {
        float m = Float.MAX_VALUE;
        for (int i = 0; i < e.planets.size(); i++) for (int j = i + 1; j < e.planets.size(); j++) {
            Planet a = e.planets.get(i), b = e.planets.get(j);
            m = Math.min(m, (float) Math.hypot(a.x - b.x, a.y - b.y));
        }
        return m;
    }
    boolean inBounds() {
        for (Planet p : e.planets) if (p.x < 25 * e.dp || p.x > e.W - 25 * e.dp || p.y < 70 * e.dp || p.y > e.H - 50 * e.dp) return false;
        return true;
    }

    public static void main(String[] args) {
        EngineSim t = new EngineSim();
        t.e.setListener(t);
        t.run();
        System.out.println(t.fails == 0 ? "\nTẤT CẢ ĐỀU QUA" : "\nCÓ " + t.fails + " LỖI");
        System.exit(t.fails == 0 ? 0 : 1);
    }

    void run() {
        Engine e = this.e;
        float[][] sizes = {{1080, 2340, 2.75f}, {720, 1600, 2f}, {1440, 3120, 3.5f}, {1080, 1920, 3f}};

        System.out.println("== Bản đồ trên 4 cỡ màn hình ==");
        for (float[] s : sizes) {
            e.setSize(s[0], s[1], s[2]);
            boolean ok = true;
            for (int i = 0; i < Levels.ALL.length; i++) {
                Level L = Levels.ALL[i];
                start(L);
                boolean good = e.planets.size() == L.planets && inBounds() && minDist() > 80 * e.dp;
                if (L.range > 0) {
                    float rp = e.rangePx(), far = 0;
                    for (Planet p : e.planets) far = Math.max(far, (float) Math.hypot(p.x - me().x, p.y - me().y));
                    boolean linked = true;
                    for (Planet p : e.planets) {
                        if (p == me()) continue;
                        boolean any = false;
                        for (Planet q : e.planets) if (q != p && Math.hypot(p.x - q.x, p.y - q.y) <= rp) any = true;
                        linked &= any;
                    }
                    good &= far > rp * 1.1f && linked;
                }
                if (!good) { ok = false; System.out.println("    lỗi bản đồ " + L.name + " @" + (int) s[0] + "x" + (int) s[1]); }
            }
            int bad = 0;
            int[] counts = new int[11];
            Random r = new Random(42);
            for (int k = 0; k < 300; k++) {
                start(Levels.endless(1 + k % 10, r));
                int n = e.planets.size();
                counts[n]++;
                if (n < 3 || n > 10 || !inBounds() || minDist() < 70 * e.dp) bad++;
            }
            check(ok && bad == 0 && counts[3] > 0 && counts[10] > 0,
                    (int) s[0] + "x" + (int) s[1] + ": 10 màn đúng số hành tinh, không chồng, trong khung; 300 bản đồ Endless có 3-10 hành tinh (lỗi " + bad + ")");
        }

        e.setSize(1080, 2340, 2.75f);
        System.out.println("== Hạn chế từng màn ==");

        start(Levels.ALL[1]); tick(60); tap(me().x, me().y); tick(5);
        check(me().level == 1 && lastToast.contains("không cho nâng cấp"), "Màn 1: chạm hành tinh không nâng cấp được");

        start(Levels.ALL[2]); for (Planet p : e.planets) p.think = 1e9f; me().n = 29; secs(3);
        int capNow = e.capOf(me()); me().level = 3;
        check(me().n == 30 && capNow == 30 && e.capOf(me()) == 30, "Màn 2: sức chứa dừng ở 30, lên cấp vẫn 30");

        start(Levels.ALL[3]); for (Planet p : e.planets) p.think = 1e9f; int n0 = me().n; secs(10);
        boolean noProd = me().n == n0;
        me().n = 0; e.rocks.clear(); tick(2);
        check(noProd && Boolean.FALSE.equals(finishWin) && finishReason.contains("hết đá"), "Màn 3: không sinh đá; hết đá thì thua");

        start(Levels.ALL[4]); for (Planet p : e.planets) p.think = 1e9f; secs(101);
        check(Boolean.FALSE.equals(finishWin) && finishReason.contains("Hết giờ"), "Màn 4: hết 100 giây thì thua");

        start(Levels.ALL[5]); for (Planet p : e.planets) p.think = 1e9f;
        Planet far = null, near = null;
        float fd = 0, nd = Float.MAX_VALUE;
        for (Planet p : e.planets) {
            if (p == me()) continue;
            float d = (float) Math.hypot(p.x - me().x, p.y - me().y);
            if (d > fd) { fd = d; far = p; }
            if (d < nd) { nd = d; near = p; }
        }
        int b0 = me().n; drag(me(), far.x, far.y); int sentFar = b0 - me().n; String farToast = lastToast;
        int b1 = me().n; drag(me(), near.x, near.y); int sentNear = b1 - me().n;
        check(sentFar == 0 && farToast.equals("Ngoài tầm bay") && sentNear > 0, "Màn 5: ngoài tầm không gửi được, trong tầm gửi được");

        start(Levels.ALL[6]); Planet e1 = e.planets.get(1); for (Planet p : e.planets) p.think = 1e9f;
        boolean fog0 = e.fogged(e1) && !e.fogged(me());
        Rock probe = new Rock(); probe.x = e1.x + e1.base; probe.y = e1.y; probe.s = 600; probe.o = 0; probe.t = e1; probe.rad = e.RR;
        e.rocks.add(probe); tick(3);
        boolean revealed = !e.fogged(e1); secs(3.5f);
        check(fog0 && revealed && e.fogged(e1), "Màn 6: đối thủ bị che; bị đánh thì lộ 3 giây rồi che lại");

        start(Levels.ALL[7]);
        check(e.neutrals.size() == 22, "Màn 7: có 22 thiên thạch chặn đường");

        start(Levels.ALL[8]); for (Planet p : e.planets) p.think = 1e9f; Planet tg = e.planets.get(1);
        int a1 = me().n; drag(me(), tg.x, tg.y); int a2 = me().n; drag(me(), tg.x, tg.y); int a3 = me().n; String cdToast = lastToast;
        secs(4.2f); int a4 = me().n; drag(me(), tg.x, tg.y);
        check(a1 > a2 && a2 == a3 && cdToast.startsWith("Đang nạp đạn") && a4 > me().n, "Màn 8: phải chờ 4 giây giữa hai lần gửi");

        start(Levels.ALL[9]); float rp0 = e.rateOf(me()), re0 = e.rateOf(e.planets.get(1));
        e.planets.get(1).owner = 0;
        check(rp0 == 1f && re0 == 2f && e.rateOf(e.planets.get(1)) == 1f, "Màn 9: đối thủ sinh x2, hành tinh đã chiếm sinh bình thường");

        System.out.println("== Luật chung ==");
        start(Levels.ALL[2]); for (Planet p : e.planets) p.think = 1e9f;
        Planet B = e.planets.get(2); B.level = 3; B.armor = 20; B.n = 5; B.acc = -1e9f;
        for (int i = 0; i < 25; i++) shoot(B);
        tick(3); boolean held = B.owner == 2 && B.n == 0;
        shoot(B); tick(3);
        check(held && B.owner == 0 && B.level == 1 && B.armor == 0 && B.captured && e.maxLvl(B) == 4,
                "Giáp trừ trước, hết máu thì bị chiếm, về cấp 1 và tối đa cấp 4");

        start(Levels.endless(1, new Random(3))); for (Planet p : e.planets) p.think = 1e9f;
        Planet tp = me(); tp.n = 60; tp.acc = -1e9f;
        float[] fr = {.014f, .025f, .04f, .05f};
        StringBuilder sb = new StringBuilder();
        boolean grows = true;
        int prevD = 0;
        for (float f : fr) {
            int before = tp.n + tp.armor;
            Asteroid a = new Asteroid(); a.x = tp.x; a.y = tp.y; a.rad = f * e.unit; a.o = -1;
            e.neutrals.add(a); tick(1);
            int d = before - (tp.n + tp.armor);
            sb.append(d).append(' ');
            grows &= d > prevD; prevD = d;
        }
        check(grows, "Endless: thiên thạch càng to trừ máu càng nhiều (sát thương " + sb.toString().trim() + ")");

        System.out.println("== Màn Hướng dẫn ==");
        start(Levels.ALL[0]);
        Planet enemy = e.planets.get(1);
        drag(me(), enemy.x, enemy.y); secs(2);
        boolean s1 = events.contains(Engine.EV_ATTACK) && enemy.owner == 1;
        circle(me().x, me().y, 70 * e.dp); e.up(me().x + 70 * e.dp, me().y);
        boolean s2 = events.contains(Engine.EV_LASSO) && e.selection != null;
        tap(e.W / 2, e.H * .55f); secs(2);
        int idle = 0; for (Rock r : e.rocks) if (r.idle && r.o == 0) idle++;
        boolean s3 = events.contains(Engine.EV_POINT) && idle > 0;
        secs(5); tap(me().x, me().y); secs(2);
        boolean s4 = events.contains(Engine.EV_UPGRADE);
        int guard = 0;
        while (finishWin == null && guard++ < 40) { secs(3); drag(me(), enemy.x, enemy.y); }
        secs(3);
        check(s1, "Bước 1: kéo tấn công (đối thủ chưa bị chiếm ngay nhờ giáp)");
        check(s2, "Bước 2: khoanh vòng chọn đá");
        check(s3, "Bước 3: chạm chỗ trống, đá bay tới chờ (" + idle + " viên)");
        check(s4, "Bước 4: chạm hành tinh để nâng cấp");
        check(Boolean.TRUE.equals(finishWin) && events.contains(Engine.EV_CAPTURE), "Bước 5: chiếm hành tinh đỏ, hoàn thành hướng dẫn");
        start(Levels.ALL[0]); secs(120);
        int launched = 0; for (Rock r : e.rocks) if (r.o != 0) launched++;
        check(launched == 0 && me().owner == 0, "Hướng dẫn: đối thủ không bao giờ tấn công");

        System.out.println("== Mô phỏng AI (người chơi đứng yên 150s) ==");
        for (int i = 1; i < Levels.ALL.length; i++) {
            start(Levels.ALL[i]);
            int atk = 0, sec;
            for (sec = 0; sec < 150 && finishWin == null; sec++) {
                secs(1);
                for (Rock r : e.rocks) if (r.t != null && r.t.owner != r.o && !r.feed) { atk++; break; }
            }
            System.out.println("    Màn " + i + " " + Levels.ALL[i].name + ": giây có giao tranh " + atk + "/" + sec + (finishWin != null ? ", kết thúc " + (finishWin ? "thắng" : "thua") + " @" + sec + "s" : ""));
        }

        System.out.println("== Bấm ngẫu nhiên (bắt lỗi runtime) ==");
        Random r = new Random(7);
        int runs = 0;
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < Levels.ALL.length; i++) { fuzz(Levels.ALL[i], r); runs++; }
            for (int k = 0; k < 4; k++) { fuzz(Levels.endless(1 + k, r), r); runs++; }
        }
        check(true, runs + " ván bấm ngẫu nhiên không ném ngoại lệ");
    }

    void shoot(Planet target) {
        Rock k = new Rock();
        k.x = target.x + target.base; k.y = target.y; k.s = 600; k.o = 0; k.t = target; k.rad = e.RR;
        e.rocks.add(k);
    }

    void fuzz(Level L, Random r) {
        start(L);
        for (int s = 0; s < 60 && finishWin == null; s++) {
            for (int a = 0; a < 2; a++) {
                float k = r.nextFloat();
                Planet p = e.planets.get(r.nextInt(e.planets.size())), q = e.planets.get(r.nextInt(e.planets.size()));
                if (k < .35f) drag(p, q.x, q.y);
                else if (k < .55f) tap(p.x, p.y);
                else if (k < .8f) { circle(r.nextFloat() * e.W, e.H * (.2f + r.nextFloat() * .6f), (40 + r.nextFloat() * 90) * e.dp); float tx = r.nextFloat() * e.W, ty = e.H * (.2f + r.nextFloat() * .6f); e.move(tx, ty); e.up(tx, ty); }
                else tap(r.nextFloat() * e.W, e.H * (.15f + r.nextFloat() * .7f));
            }
            e.updateLive();
            secs(1);
        }
    }
}
