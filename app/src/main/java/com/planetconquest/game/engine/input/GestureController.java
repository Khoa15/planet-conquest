package com.planetconquest.game.engine.input;

import com.planetconquest.game.engine.Engine;
import com.planetconquest.game.engine.GameEvent;
import com.planetconquest.game.engine.Haptic;
import com.planetconquest.game.engine.Msg;
import com.planetconquest.game.engine.Notice;
import com.planetconquest.game.engine.model.Faction;
import com.planetconquest.game.engine.model.Planet;
import com.planetconquest.game.engine.model.Rock;
import com.planetconquest.game.engine.model.Selection;

import java.util.ArrayList;
import java.util.Arrays;

import static com.planetconquest.game.engine.util.MathUtil.bboxMin;
import static com.planetconquest.game.engine.util.MathUtil.clamp;
import static com.planetconquest.game.engine.util.MathUtil.hyp;
import static com.planetconquest.game.engine.util.MathUtil.inPoly;

/**
 * Hiểu cử chỉ một ngón tay (kéo nhanh, khoanh vòng, mang vùng chọn tới đích, chạm nâng cấp) và biến thành lệnh cho Engine
 * (launch, redirect...). Tách khỏi luật chơi: Engine không biết ngón tay vẽ gì.
 */
public final class GestureController {
    private final Engine eng;
    private final float[] dotX = new float[64], dotY = new float[64];
    private Pointer ptr;
    private Selection selection;

    public GestureController(Engine eng) { this.eng = eng; }

    public Pointer pointer() { return ptr; }
    public Selection selection() { return selection; }

    /** Hủy mọi cử chỉ và vùng chọn (khi bắt đầu hoặc kết thúc màn). */
    public void reset() { ptr = null; selection = null; }

    /** Bỏ vùng chọn khi không còn đá nào thuộc người chơi. */
    public void pruneSelection() { if (selection != null && selection.total() == 0) selection = null; }

    // ---------- Cử chỉ một ngón tay ----------
    public Planet hit(float x, float y) {
        Planet best = null;
        float bd = Float.MAX_VALUE, slop = Math.max(22 * eng.dp, eng.unit * .05f);
        for (Planet p : eng.planets()) {
            float d = hyp(p.x - x, p.y - y);
            if (d <= p.radius() + slop && d < bd) { best = p; bd = d; }
        }
        return best;
    }

    public int quickCount(Planet s) { return s.rocks >= 1 ? Math.max(1, (int) Math.floor(s.rocks * Engine.QUICK_SEND)) : 0; }

    public void down(float x, float y) {
        if (eng.over) return;
        ptr = new Pointer();
        ptr.sx = ptr.x = x; ptr.sy = ptr.y = y;
        ptr.startPlanet = ptr.hover = hit(x, y);
        ptr.add(x, y);
    }

    public void move(float x, float y) {
        if (ptr == null) return;
        ptr.x = x; ptr.y = y; ptr.hover = hit(x, y);
        if (ptr.mode == GestureMode.UNDECIDED && hyp(x - ptr.sx, y - ptr.sy) > 12 * eng.dp) {
            if (ptr.startPlanet != null && Faction.isPlayer(ptr.startPlanet.owner)) { ptr.mode = GestureMode.QUICK; ptr.qsel.add(ptr.startPlanet); selection = null; }
            else if (ptr.startPlanet == null) ptr.mode = GestureMode.LASSO;
            else ptr.mode = GestureMode.IGNORE;
        }
        if (ptr.mode == GestureMode.QUICK) {
            Planet h = ptr.hover;
            if (h != null && Faction.isPlayer(h.owner) && !ptr.qsel.contains(h)) ptr.qsel.add(h);   // kéo qua nhiều hành tinh = chọn thêm
        } else if (ptr.mode == GestureMode.LASSO) {
            float d = hyp(x - ptr.px[ptr.pn - 1], y - ptr.py[ptr.pn - 1]);
            if (d > 5 * eng.dp) { ptr.add(x, y); ptr.len += d; }
            if (lassoClosed(ptr)) tryLock();                                        // khép vòng: khóa vùng chọn, tay tiếp tục kéo tới đích
        }
    }

    public void up(float x, float y) {
        if (ptr == null) return;
        Pointer p = ptr;
        ptr = null;
        if (eng.over) return;
        Planet h = hit(x, y);
        if (p.mode == GestureMode.UNDECIDED) {
            if (selection != null) dispatch(selection, h, x, y);
            else if (h != null && Faction.isPlayer(h.owner)) upgradeTap(h);
        } else if (p.mode == GestureMode.QUICK) quickSend(p.qsel, h, x, y);
        else if (p.mode == GestureMode.LASSO) finishLasso(p);
        else if (p.mode == GestureMode.CARRY) { if (hyp(x - p.lockX, y - p.lockY) > 36 * eng.dp) dispatch(selection, h, x, y); }
    }

    public void cancelPointer() { ptr = null; }

    public void cancelSelection() { selection = null; }

    /** Gọi trước khi vẽ để có vùng chọn tạm thời khi đang khoanh. */
    public void updateLive() {
        if (ptr != null && ptr.mode == GestureMode.LASSO) ptr.live = ptr.pn >= 3 ? computeSelection(ptr.px, ptr.py, ptr.pn) : null;
    }

    private boolean lassoClosed(Pointer p) {
        if (p.pn < 10 || p.len < 110 * eng.dp || bboxMin(p.px, p.py, p.pn) < 36 * eng.dp) return false;
        return hyp(p.x - p.px[0], p.y - p.py[0]) < Math.max(26 * eng.dp, p.len * .1f);
    }

    private void tryLock() {
        Selection s = computeSelection(ptr.px, ptr.py, ptr.pn);
        if (s.total() > 0) {
            selection = s; eng.haptic(Haptic.LIGHT); eng.event(GameEvent.LASSO);
            ptr.mode = GestureMode.CARRY; ptr.lockX = ptr.x; ptr.lockY = ptr.y;
        } else {
            eng.notice(Msg.LASSO_EMPTY);
            ptr.pn = 0; ptr.add(ptr.x, ptr.y); ptr.len = 0;
        }
    }

    private void finishLasso(Pointer p) {
        if (p.len < 90 * eng.dp || bboxMin(p.px, p.py, p.pn) < 28 * eng.dp) { eng.notice(Msg.LASSO_TOO_SMALL); return; }
        Selection s = computeSelection(p.px, p.py, p.pn);
        if (s.total() > 0) { selection = s; eng.haptic(Haptic.LIGHT); eng.event(GameEvent.LASSO); }
        else eng.notice(Msg.LASSO_EMPTY);
    }

    private void upgradeTap(Planet p) {
        if (p.level >= eng.maxLvl(p)) { eng.notice(eng.capMsg(p)); return; }
        int c = Math.min(Engine.upgradeCost(p.level) - p.upgradeProgress, p.rocks);
        if (c < 1) { eng.notice(Msg.PLANET_OUT_OF_ROCKS); return; }
        if (eng.launch(p, p, 0, 0, c, true) > 0) eng.event(GameEvent.UPGRADE);
    }

    /** Điểm đến của một lệnh điều quân: tâm hành tinh nếu thả lên hành tinh, ngược lại là điểm chạm (kẹp trong khung). */
    private float targetX(Planet h, float x) { return h != null ? h.x : clamp(x, 14 * eng.dp, eng.W - 14 * eng.dp); }

    private float targetY(Planet h, float y) { return h != null ? h.y : clamp(y, 70 * eng.dp, eng.H - 70 * eng.dp); }

    /** Loại sự kiện của lệnh điều quân tới h: điểm trống, chuyển quân nội bộ hay tấn công. */
    private static GameEvent moveEvent(Planet h) {
        return h == null ? GameEvent.POINT : (Faction.isPlayer(h.owner) ? GameEvent.MOVE : GameEvent.ATTACK);
    }

    private void quickSend(ArrayList<Planet> qsel, Planet h, float x, float y) {
        selection = null;
        if (h != null && qsel.size() == 1 && h == qsel.get(0)) return;           // kéo về chính nó: hủy
        float dx = targetX(h, x), dy = targetY(h, y);
        int any = 0;
        Notice reason = null;
        for (Planet s : qsel) {
            if (s == h) continue;
            Notice why = eng.blockReason(s, dx, dy, false);
            if (why != null) { reason = why; continue; }
            int c = quickCount(s);
            if (c > 0 && eng.launch(s, h, dx, dy, c, false) > 0) any++;
        }
        if (any == 0) eng.notice(reason != null ? reason : new Notice(Msg.NO_ROCKS_TO_SEND));
        else eng.event(moveEvent(h));
    }

    private void dispatch(Selection sel, Planet h, float x, float y) {
        selection = null;
        float dx = targetX(h, x), dy = targetY(h, y);
        Notice reason = null;
        boolean sent = false, fed = false;
        for (int i = 0; i < sel.planets.size(); i++) {
            Planet p = sel.planets.get(i);
            if (!Faction.isPlayer(p.owner) || p.rocks <= 0) continue;
            int c = Math.min(sel.counts[i], p.rocks);
            if (c <= 0) continue;
            if (h != null && h == p) {                                           // thả lại chính hành tinh nguồn: nạp nâng cấp
                if (p.level >= eng.maxLvl(p)) { eng.notice(eng.capMsg(p)); continue; }
                if (eng.launch(p, p, 0, 0, c, true) > 0) fed = true;
            } else {
                Notice why = eng.blockReason(p, dx, dy, false);
                if (why != null) { reason = why; continue; }
                if (eng.launch(p, h, dx, dy, c, false) > 0) sent = true;
            }
        }
        int li = 0;
        for (Rock r : sel.loose) if (!r.dead && Faction.isPlayer(r.owner)) { eng.redirect(r, h, dx, dy, li++); sent = true; }
        if (reason != null) eng.notice(reason);
        if (fed) eng.event(GameEvent.UPGRADE);
        if (sent) eng.event(moveEvent(h));
    }

    // ---------- Vòng khoanh ----------
    public Selection computeSelection(float[] xs, float[] ys, int n) {
        Selection s = new Selection();
        if (n >= 3) {
            for (Planet p : eng.planets()) {
                if (!Faction.isPlayer(p.owner) || p.rocks <= 0) continue;
                int dc = eng.orbitDots(p, dotX, dotY), ic = 0;
                for (int i = 0; i < dc; i++) if (inPoly(dotX[i], dotY[i], xs, ys, n)) ic++;
                if (ic > 0) s.addGroup(p, Math.min(p.rocks, ic >= dc ? p.rocks : Math.max(1, Math.round((float) ic / dc * p.rocks))));
            }
            for (Rock r : eng.rocks()) if (!r.dead && Faction.isPlayer(r.owner) && !r.feed && inPoly(r.x, r.y, xs, ys, n)) s.loose.add(r);
        }
        s.xs = Arrays.copyOf(xs, n); s.ys = Arrays.copyOf(ys, n); s.n = n;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { cx += xs[i]; cy += ys[i]; }
        s.cx = n > 0 ? cx / n : 0; s.cy = n > 0 ? cy / n : 0;
        return s;
    }

    /** Số viên đá quỹ đạo của p cần sáng lên vì đang được chọn. */
    public int highlightCount(Planet p) {
        Selection s = ptr != null && ptr.mode == GestureMode.LASSO ? ptr.live : selection;
        if (s == null || p.rocks <= 0) return 0;
        int c = s.countFor(p);
        if (c == 0) return 0;
        return Math.round(Math.min(c, p.rocks) / (float) p.rocks * Math.min(p.rocks, 60));
    }

}
