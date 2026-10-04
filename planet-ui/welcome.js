// Prototype màn Welcome: bản dựng 1-1 của WelcomeScene.java + StarField.java + WelcomeScreen.draw (bản Android hiện tại).
// Tên hàm/hằng giữ nguyên như bản Java để đối chiếu; 1 đơn vị CSS = 1 dp. Cần java.util.Random để bố cục trùng hệt (JRandom).
'use strict';

const TAU = Math.PI * 2, DP = 1;
const sin = Math.sin, cos = Math.cos;
const rad = d => d * Math.PI / 180;

// ---------- java.util.Random (LCG 48-bit) để bố cục ngẫu nhiên giống bản Android ----------
class JRandom {
  constructor(seed) { this.s = (BigInt(seed) ^ 0x5DEECE66Dn) & ((1n << 48n) - 1n); this.haveG = false; this.nextG = 0; }
  next(bits) { this.s = (this.s * 0x5DEECE66Dn + 0xBn) & ((1n << 48n) - 1n); return Number(BigInt.asIntN(32, this.s >> BigInt(48 - bits))); }
  nextFloat() { return this.next(24) / (1 << 24); }
  nextDouble() { return (this.next(26) * 134217728 + this.next(27)) / 9007199254740992; }
  nextGaussian() {
    if (this.haveG) { this.haveG = false; return this.nextG; }
    let v1, v2, s;
    do { v1 = 2 * this.nextDouble() - 1; v2 = 2 * this.nextDouble() - 1; s = v1 * v1 + v2 * v2; } while (s >= 1 || s === 0);
    const mul = Math.sqrt(-2 * Math.log(s) / s);
    this.nextG = v2 * mul; this.haveG = true; return v1 * mul;
  }
}

// ---------- Màu (ColorUtil, Faction, Palette) ----------
function mix(c, t, a) {
  const m = s => { const x = (c >>> s) & 255, y = (t >>> s) & 255; return Math.trunc(x + (y - x) * a); };
  return 0xFF000000 | (m(16) << 16) | (m(8) << 8) | m(0);
}
/** Giữ RGB, đặt độ trong suốt a (0..1) → chuỗi CSS. */
function alpha(c, a) { return `rgba(${(c >>> 16) & 255},${(c >>> 8) & 255},${c & 255},${Math.max(0, Math.min(1, a))})`; }
/** Số ARGB đầy đủ (alpha ở byte cao) → chuỗi CSS. */
function argb(c) { return alpha(c, (c >>> 24) / 255); }
const FC = [0xFF4FF0B4, 0xFFFF6B7D, 0xFFFFB347, 0xFFA46BFF, 0xFF5CC8FF, 0xFFFF8FD8, 0xFFF2E86D, 0xFF9BE564, 0xFFC9B79C, 0xFF6F8BFF];
const FL = FC.map(c => mix(c, 0xFFFFFFFF, .5));
const C_BG = 0xFF05070F, C_INK = 0xFFE9EDFF, C_MUTED = 0xFF8F99C8, C_YOU = 0xFF4FF0B4, C_GOLD = 0xFFFFD166, C_LINE = 0x40A0AFFF;
const frac = v => v - Math.floor(v);

// ---------- Hằng số của WelcomeScene ----------
const PL = [   // x, y (tỉ lệ màn hình), bán kính (× cạnh ngắn), phe, độ sâu parallax, tốc độ trôi dải mây, số đá quay quanh
  [.50, .50, .17, 0, 1, .35, 0], [.19, .335, .075, 1, .92, -.5, 10], [.86, .635, .062, 2, 1.08, .42, 8],
  [.13, .615, .05, 3, .78, -.3, 0], [.085, .45, .028, 4, .6, .6, 0],
  // Khu vực dưới (trước đây là chỗ nút chữ nhật): bốn hành tinh nhỏ xa dần, chừa trống góc hai nút phụ
  [.30, .80, .036, 5, .86, .4, 0], [.64, .755, .024, 6, .70, -.45, 0], [.47, .845, .02, 8, .62, .5, 0], [.80, .775, .03, 9, .90, -.3, 0]];
const SAND = 8, TILT = -16, SQ = .3;
const CAM_PERIOD = 24, LAUNCH_PERIOD = 4.2, FLIGHT = 1.5, LAUNCH_GAP = .09;
const LAUNCH_N = 9, BELT = 46, DISK_P = 64, ARCS = 16, FRAG = 60, DUST = 22, FG = 5;
const NEB = [[.18, .30, .42, 0x6A3CE0, .16], [.85, .20, .35, 0x2A70E0, .14], [.70, .78, .45, 0x5A3CC8, .12], [.30, .70, .30, 0x288CC8, .10]];
const FG_Y = [.30, .47, .62, .70, .40];
const RING_CNT = [12, 18, 24], RING_SPD = [.6, -.4, .28], SLOT_OFF = [0, 2, -2];
const CHARGE = .3, LAUNCH_AT = .35, REGROW_A = 2.9, REGROW_B = 3.8;
const MESSY_COUNT_PLAYER = 36, LAUNCH_DOT_STEP = 4;   // hành tinh xanh: 36 đá hỗn loạn, loạt phóng lấy đá 0,4,8...
const MESSY_EXTRA = -2, MESSY_SPEED_MIN = .25, MESSY_SPEED_RANGE = 1.1;   // quỹ đạo hỗn loạn của hành tinh đỏ/vàng
const LOW_BELT = [1.15, .70, -.15, .80], LOW_BELT_SPEED = 1.5, LOW_BELT_HALF = .035;   // vành đai dưới: bay từ phải sang trái
const TORN_TH0 = 3.0, TORN_RS = .30, TORN_SW = -2.6, BITE = 1.25, BITE_N = 18, CRACKS = 4;

// ---------- Canvas ----------
let W = 360, H = 780, U = 360;
const cv = document.getElementById('stage');
const dpr = Math.min(window.devicePixelRatio || 1, 3);
const ctx = cv.getContext('2d');
function resizeCanvas() {
  cv.width = W * dpr; cv.height = H * dpr; cv.style.width = W + 'px'; cv.style.height = H + 'px';
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}
const circ = (x, y, r) => { ctx.beginPath(); ctx.arc(x, y, r, 0, TAU); };
/** Paint FILL: màu hoặc shader đã gán vào fillStyle. */
const fillCirc = (x, y, r) => { circ(x, y, r); ctx.fill(); };
const strokeCirc = (x, y, r) => { circ(x, y, r); ctx.stroke(); };
/** Canvas.drawArc(oval, start, sweep) với đơn vị độ. */
function drawArc(l, t, r, b, start, sweep) {
  ctx.beginPath(); ctx.ellipse((l + r) / 2, (t + b) / 2, (r - l) / 2, (b - t) / 2, 0, rad(start), rad(start + sweep)); ctx.stroke();
}
function radial(cx, cy, r, stops) { const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r); stops.forEach(([p, c]) => g.addColorStop(p, c)); return g; }
function linear(x0, y0, x1, y1, stops) { const g = ctx.createLinearGradient(x0, y0, x1, y1); stops.forEach(([p, c]) => g.addColorStop(p, c)); return g; }

// ---------- StarField: nền chung (bitmap tinh vân + sao tĩnh, sao nhấp nháy) ----------
const StarField = {
  bg: null, tw: [],
  build() {
    this.bg = document.createElement('canvas'); this.bg.width = W; this.bg.height = H;
    const g = this.bg.getContext('2d');
    g.fillStyle = (() => { const l = g.createLinearGradient(0, 0, 0, H); l.addColorStop(0, argb(0xFF070A1C)); l.addColorStop(1, argb(0xFF04050D)); return l; })();
    g.fillRect(0, 0, W, H);
    const neb = [[.2, .25, .55], [.88, .62, .6], [.4, .95, .5]], nc = [0x5A3CC8, 0x288CC8, 0xC8468C];
    neb.forEach((n, i) => {
      const gr = g.createRadialGradient(n[0] * W, n[1] * H, 0, n[0] * W, n[1] * H, n[2] * Math.max(W, H));
      gr.addColorStop(0, alpha(nc[i], 0x26 / 255)); gr.addColorStop(1, alpha(nc[i], 0)); g.fillStyle = gr; g.fillRect(0, 0, W, H);
    });
    const r = new JRandom(11);
    for (let i = 0; i < 170; i++) {
      const a = Math.trunc((.2 + r.nextFloat() * .6) * 255), x = r.nextFloat() * W, y = r.nextFloat() * H, rr = (r.nextFloat() * 1.1 + .2) * DP;
      g.fillStyle = `rgba(255,255,255,${a / 255})`; g.beginPath(); g.arc(x, y, rr, 0, TAU); g.fill();
    }
    this.tw = [];
    for (let i = 0; i < 28; i++) this.tw.push([r.nextFloat() * W, r.nextFloat() * H, r.nextFloat() * TAU, .8 + r.nextFloat() * 1.4]);
  },
  draw(t) {
    ctx.drawImage(this.bg, 0, 0);
    for (const [x, y, ph, sp] of this.tw) { ctx.fillStyle = alpha(0xFFFFFFFF, .25 + .35 * sin(t * sp + ph)); ctx.fillRect(x, y, 1.6 * DP, 1.6 * DP); }
  },
};

// ---------- WelcomeScene ----------
const Scene = (() => {
  const rockShape = [];
  const beltS = [], beltN = [], beltSize = [], beltRot = [], beltSpin = [], beltDepth = [], beltCol = [];
  const diskR = [], diskA = [], diskSz = [], arcR = [], arcA = [], arcSw = [], arcW = [], arcAl = [];
  const jit = [], biteF = [], crackK = [], crackL = [], crackB = [], fgX = [], fgV = [], fgSize = [];
  const lSlot = new Array(LAUNCH_N).fill(0);
  let glowS = [], bodyS = [], nebS = [], sandS, heatS, haloS, shadowS, blurS, topS, botS;
  let bx, by, rh, b0x, b0y, bdx, bdy, bLen, zoom = 0, swayX = 0, swayY = 0, tornR = 0, tornBody, tornEdge, tornCracks, tornGhost;
  let spX = 0, spY = 0, spPow = 1.5;
  let playFx = null;   // hiệu ứng nhấn/chạm của hành tinh xanh: {scale, glow}

  // Khởi tạo ngẫu nhiên: đúng thứ tự gọi như constructor Java (Random(7))
  const r = new JRandom(7);
  for (let k = 0; k < 8; k++) {
    const p = new Path2D();
    for (let i = 0; i < 8; i++) { const a = i / 8 * TAU, rr = .72 + r.nextFloat() * .38; i === 0 ? p.moveTo(cos(a) * rr, sin(a) * rr) : p.lineTo(cos(a) * rr, sin(a) * rr); }
    p.closePath(); rockShape.push(p);
  }
  for (let i = 0; i < BELT; i++) {
    beltS[i] = r.nextFloat(); beltN[i] = r.nextGaussian() * .45; beltDepth[i] = .55 + r.nextFloat() * .6;
    beltSize[i] = (1.8 + r.nextFloat() * 3.6) * beltDepth[i]; beltRot[i] = r.nextFloat() * 360; beltSpin[i] = (r.nextFloat() - .5) * 60; beltCol[i] = r.nextFloat();
  }
  for (let i = 0; i < DISK_P; i++) { diskR[i] = 1.3 + r.nextFloat() * 1.7; diskA[i] = r.nextFloat() * TAU; diskSz[i] = .8 + r.nextFloat() * .9; }
  for (let i = 0; i < ARCS; i++) { arcR[i] = 1.7 + r.nextFloat() * 1.9; arcA[i] = r.nextFloat() * 360; arcSw[i] = 10 + r.nextFloat() * 30; arcW[i] = .6 + r.nextFloat() * .6; arcAl[i] = .15 + r.nextFloat() * .3; }
  for (let i = 0; i < FRAG + DUST; i++) jit[i] = r.nextFloat() - .5;
  for (let k = 0; k <= BITE_N; k++) { const base = Math.pow(Math.sin(Math.PI * k / BITE_N), .6); biteF[k] = 1 - (.36 + (k % 2 === 0 ? .14 : 0) + r.nextFloat() * .12) * base; }
  for (let i = 0; i < CRACKS; i++) { crackK[i] = 3 + Math.trunc(i * (BITE_N - 6) / (CRACKS - 1)); crackL[i] = .35 + r.nextFloat() * .3; crackB[i] = (r.nextFloat() - .5) * .5; }
  for (let i = 0; i < FG; i++) { fgX[i] = r.nextFloat(); fgV[i] = 34 + r.nextFloat() * 26; fgSize[i] = 10 + r.nextFloat() * 12; }

  function setSize() {
    U = Math.min(W, H);
    glowS = []; bodyS = []; nebS = [];
    PL.forEach((p, i) => {
      const rr = p[2] * U, o = p[3], fog = 0xFF0A0D20, haze = Math.max(0, 1 - p[4]) * .6;
      const col = mix(FC[o], fog, haze), lt = mix(FL[o], fog, haze), dk = mix(col, 0xFF000000, .62);
      glowS[i] = radial(0, 0, rr * 2.2, [[0, alpha(col, .38)], [.45, alpha(col, .3)], [1, alpha(col, 0)]]);
      bodyS[i] = radial(-rr * .35, -rr * .4, rr * 1.45, [[0, alpha(lt, 1)], [.5, alpha(col, 1)], [1, alpha(dk, 1)]]);
    });
    buildTorn();
    NEB.forEach((n, i) => { const c = n[3]; nebS[i] = radial(0, 0, n[2] * Math.max(W, H), [[0, alpha(c, n[4])], [.5, alpha(c, n[4] * .4)], [1, alpha(c, 0)]]); });
    bx = W * .80; by = H * .30; rh = .065 * U;
    haloS = radial(0, 0, rh * 5, [[.18, alpha(0xFFFFB45A, .30)], [.4, alpha(0xFFFF8040, .12)], [.7, alpha(0xFF8040C0, .05)], [1, 'rgba(0,0,0,0)']]);
    shadowS = radial(0, 0, rh * 1.7, [[0, '#000'], [.58, '#000'], [1, 'rgba(0,0,0,0)']]);
    blurS = radial(0, 0, 100, [[0, argb(0xFF6B5A4D)], [.5, argb(0xFF4E4239)], [1, argb(0x004E4239)]]);
    topS = linear(0, 0, 0, H * .26, [[0, alpha(C_BG, .6)], [1, alpha(C_BG, 0)]]);
    botS = linear(0, H * .66, 0, H, [[0, alpha(C_BG, 0)], [1, alpha(C_BG, .8)]]);
    const x0 = -.15 * W, y0 = .64 * H, x1 = 1.15 * W, y1 = .40 * H;
    bLen = Math.hypot(x1 - x0, y1 - y0); b0x = x0; b0y = y0; bdx = (x1 - x0) / bLen; bdy = (y1 - y0) / bLen;
  }

  function draw(t) {
    const ph = TAU * t / CAM_PERIOD;
    zoom = .03 * (.5 - .5 * cos(ph)); swayX = sin(ph) * 5 * DP; swayY = (cos(ph) - 1) * 3 * DP;
    drawNebula(t); drawPlanet(4, t); drawPlanet(3, t); drawPlanet(7, t); drawPlanet(6, t); drawPlanet(8, t); drawPlanet(5, t); drawBlackHole(t); drawBelt(t, false); drawBelt(t, true); drawPlanet(1, t);
    launchSlots(); drawPlanet(0, t); drawLaunch(t); drawPlanet(2, t); drawForeground(t);
    ctx.fillStyle = topS; ctx.fillRect(0, 0, W, H * .26);
    ctx.fillStyle = botS; ctx.fillRect(0, H * .66, W, H - H * .66);
  }

  // Camera: phóng to quanh tâm rồi lệch nhẹ; lớp càng gần (depth lớn) càng chuyển động nhiều
  function cam(depth) {
    ctx.save(); ctx.translate(swayX * depth, swayY * depth);
    const s = 1 + zoom * depth, px = W * .5, py = H * .45;
    ctx.translate(px, py); ctx.scale(s, s); ctx.translate(-px, -py);
  }
  const camX = (x, d) => (x - W * .5) * (1 + zoom * d) + W * .5 + swayX * d;
  const camY = (y, d) => (y - H * .45) * (1 + zoom * d) + H * .45 + swayY * d;

  function drawNebula(t) {
    cam(.3);
    for (let i = 0; i < NEB.length; i++) {
      const x = NEB[i][0] * W + sin(t * .025 + i * 1.7) * 18 * DP, y = NEB[i][1] * H + cos(t * .02 + i) * 12 * DP, k = 1 + .06 * sin(t * .03 + i * 2.3);
      ctx.save(); ctx.translate(x, y); ctx.scale(k, k); ctx.fillStyle = nebS[i]; fillCirc(0, 0, NEB[i][2] * Math.max(W, H)); ctx.restore();
    }
    ctx.restore();
  }

  const ringRad = k => PL[0][2] * U + (14 + 10 * k) * DP;

  function drawPlanet(i, t) {
    const p = PL[i], r = p[2] * U, spin = p[5], o = p[3];
    cam(p[4]); ctx.translate(p[0] * W, p[1] * H);
    if (i === 0 && playFx) ctx.scale(playFx.scale, playFx.scale);
    const pulse = .5 + .5 * sin(t * (i === 0 ? 1.7 : .9) + i);
    ctx.save(); const k = 1 + (i === 0 ? .07 : .03) * pulse; ctx.scale(k, k);
    ctx.fillStyle = glowS[i]; ctx.globalAlpha = .75 + .25 * pulse; fillCirc(0, 0, r * 2.2); ctx.globalAlpha = 1; ctx.restore();
    ctx.fillStyle = bodyS[i]; fillCirc(0, 0, r);

    ctx.save(); circ(0, 0, r); ctx.clip();
    for (let b = 0; b < 5; b++) {                       // dải mây uốn lượn trôi chậm
      const kb = -.64 + b * .32, w = i * 1.7 + b * 2.1 + t * spin;
      ctx.strokeStyle = b % 2 === 0 ? argb(0x2A000000) : argb(0x1CFFFFFF); ctx.lineWidth = r * (b % 2 === 0 ? .14 : .08);
      ctx.beginPath(); ctx.moveTo(-r * 1.1, r * kb);
      ctx.bezierCurveTo(-r * .4, r * (kb + .12 * sin(w)), r * .3, r * (kb - .12 * sin(w * .8 + 1.3)), r * 1.1, r * kb); ctx.stroke();
    }
    const u = frac(t * Math.abs(spin) * .08 + i * .37), sx = (u * 2.6 - 1.3) * r * Math.sign(spin);
    ctx.fillStyle = argb(0x22FFFFFF); ctx.beginPath(); ctx.ellipse(sx, r * .22, r * .22, r * .1, 0, 0, TAU); ctx.fill();   // cơn bão trôi ngang
    ctx.restore();
    ctx.strokeStyle = argb(0x47FFFFFF); ctx.lineWidth = 1.5 * DP; strokeCirc(0, 0, r);
    if (i === 0 && playFx && playFx.glow > 0) { ctx.fillStyle = alpha(0xFFFFFFFF, playFx.glow); fillCirc(0, 0, r); }

    if (p[6] > 0 || i === 0) {                        // đá quay hỗn loạn: mỗi viên bán kính/tốc độ/chiều/độ lệch tâm riêng
      const n = i === 0 ? MESSY_COUNT_PLAYER : p[6] + MESSY_EXTRA, lt = t % LAUNCH_PERIOD;
      for (let d = 0; d < n; d++) {
        const sc = i === 0 ? dotScale(d, lt) : 1;
        if (sc <= 0) continue;                          // đá này đã rời quỹ đạo, đang bay hoặc chưa mọc lại
        const m = messyDot(i, d, t, r), hot = Math.max(0, sc - 1) / .6;
        if (i === 0) { ctx.fillStyle = alpha(FC[0], (.12 + .25 * hot) * m.al); fillCirc(m.x, m.y, 4.5 * DP * sc); }
        ctx.fillStyle = i === 0 ? alpha(mix(FL[0], 0xFFFFFFFF, hot), .95 * m.al) : alpha(FC[o], .55 + .4 * m.h1);
        fillCirc(m.x, m.y, (i === 0 ? 2.1 : 1.2 + m.h2 * 1.2) * DP * sc);
      }
    }
    ctx.restore();
  }

  /** Vị trí (so với tâm hành tinh i) của viên đá d ở thời điểm t trên quỹ đạo hỗn loạn; dir = chiều quay (±1). */
  function messyDot(i, d, t, r) {
    const h1 = frac(sin(i * 91.7 + d * 12.9898) * 43758.5453), h2 = frac(sin(i * 37.1 + d * 78.233) * 24634.6345), h3 = frac(sin(i * 5.3 + d * 3.17) * 9371.13);
    const dir = h2 < .4 ? -1 : 1, a = t * (MESSY_SPEED_MIN + h1 * MESSY_SPEED_RANGE) * dir + h3 * TAU, rx = r + (5 + h2 * 20) * DP, ry = rx * (.55 + h1 * .45), tilt = h3 * TAU;
    const ex = cos(a) * rx, ey = sin(a) * ry;
    return { x: ex * cos(tilt) - ey * sin(tilt), y: ex * sin(tilt) + ey * cos(tilt), h1, h2, dir, al: .6 + .4 * h1 };
  }
  // Viên thứ i của loạt phóng lấy từ đá số i*LAUNCH_DOT_STEP của hành tinh xanh
  function launchSlots() { for (let i = 0; i < LAUNCH_N; i++) lSlot[i] = i * LAUNCH_DOT_STEP; }
  // Tỉ lệ vẽ một viên đá: >1 đang tụ sáng, 0 đã bay đi, 0..1 đang mọc lại, 1 bình thường
  function dotScale(d, lt) {
    for (let i = 0; i < LAUNCH_N; i++) {
      if (lSlot[i] !== d) continue;
      const li = LAUNCH_AT + i * LAUNCH_GAP;
      if (lt < li - CHARGE) return 1;
      if (lt < li) return 1 + .6 * (lt - (li - CHARGE)) / CHARGE;
      if (lt < REGROW_A) return 0;
      if (lt < REGROW_B) return (lt - REGROW_A) / (REGROW_B - REGROW_A);
      return 1;
    }
    return 1;
  }

  function drawLaunch(t) {
    const lt = t % LAUNCH_PERIOD, cs = t - lt, a = PL[0], b = PL[1];
    const ax = a[0] * W, ay = a[1] * H, wtx = b[0] * W, wty = b[1] * H, wtr = b[2] * U, dx = wtx - ax, dy = wty - ay, d = Math.hypot(dx, dy);
    const wex = wtx - dx / d * wtr * .9, wey = wty - dy / d * wtr * .9;
    for (let i = 0; i < LAUNCH_N; i++) {
      const li = LAUNCH_AT + i * LAUNCH_GAP, u = (lt - li) / FLIGHT;
      if (u < 0 || u > 1) continue;
      const m = messyDot(0, lSlot[i], cs + li, a[2] * U), an = Math.atan2(m.y, m.x), sg = m.dir;
      const p0x = ax + m.x, p0y = ay + m.y, vx = wex - p0x, vy = wey - p0y, dd = Math.hypot(vx, vy);
      const p1x = p0x - sin(an) * sg * dd * .22 + cos(an) * dd * .1, p1y = p0y + cos(an) * sg * dd * .22 + sin(an) * dd * .1;
      const p2x = (p0x + wex) / 2 - vy * .22, p2y = (p0y + wey) / 2 + vx * .22, e = u * (.6 + .4 * u);
      for (let tr = 3; tr >= 0; tr--) {
        const q = e - tr * .03;
        if (q < 0) continue;
        const m = 1 - q, w0 = m * m * m, w1 = 3 * m * m * q, w2 = 3 * m * q * q, w3 = q * q * q;
        const x = w0 * p0x + w1 * p1x + w2 * p2x + w3 * wex, y = w0 * p0y + w1 * p1y + w2 * p2y + w3 * wey, dep = 1 - .08 * q, sx = camX(x, dep), sy = camY(y, dep);
        if (tr === 0) { ctx.fillStyle = alpha(FC[0], .3); fillCirc(sx, sy, 4.5 * DP); }
        ctx.fillStyle = alpha(tr === 0 ? FL[0] : FC[0], 1 - tr / 4); fillCirc(sx, sy, (2.4 - .4 * tr) * DP);
      }
    }
    const ex = camX(wex, b[4]), ey = camY(wey, b[4]), tx = camX(wtx, b[4]), ty = camY(wty, b[4]), tr = wtr * (1 + zoom * b[4]);
    for (let i = 0; i < LAUNCH_N; i++) {                // chớp sáng khi trúng đích
      const age = lt - (LAUNCH_AT + i * LAUNCH_GAP + FLIGHT);
      if (age < 0 || age > .5) continue;
      const f = 1 - age / .5;
      ctx.fillStyle = alpha(0xFFFFFFFF, f * .45); fillCirc(ex, ey, (5 + 12 * age / .5) * DP);
      ctx.strokeStyle = alpha(FL[1], f * .8); ctx.lineWidth = 2 * DP; strokeCirc(ex, ey, (4 + 26 * age) * DP);
      ctx.strokeStyle = alpha(FL[1], f * .3); ctx.lineWidth = 1.5 * DP; strokeCirc(tx, ty, tr * (1 + age * .6));
      ctx.fillStyle = alpha(FL[0], f);
      for (let j = 0; j < 5; j++) { const ang = i * 1.3 + j * TAU / 5; fillCirc(ex + cos(ang) * age * 40 * DP, ey + sin(ang) * age * 40 * DP, 1.4 * DP); }
    }
  }

  function diskGradient(t) {   // SweepGradient 7 màu, xoay -(t*20)° bằng local matrix
    const g = ctx.createConicGradient(rad(-((t * 20) % 360)), 0, 0);
    [0xFFFFF3D6, 0xFFFFC46B, 0xFFFF8A3D, 0xFFE0602A, 0xFFFF9A48, 0xFFFFD58A, 0xFFFFF3D6].forEach((c, k, a) => g.addColorStop(k / (a.length - 1), argb(c)));
    return g;
  }

  function drawBlackHole(t) {
    cam(.8); ctx.translate(bx, by);
    for (let i = 0; i < ARCS; i++) {                     // ánh sao bị bẻ cong thành những cung quanh hố đen
      const r = arcR[i] * rh; ctx.strokeStyle = alpha(0xFFDDE6FF, arcAl[i]); ctx.lineWidth = arcW[i] * DP; ctx.lineCap = 'round';
      drawArc(-r, -r, r, r, (arcA[i] - t * 8 / arcR[i]) % 360, arcSw[i]);
    }
    ctx.fillStyle = haloS; ctx.globalAlpha = .85 + .15 * sin(t * .8); fillCirc(0, 0, rh * 5); ctx.globalAlpha = 1;
    const diskS = diskGradient(t);
    ctx.save(); ctx.rotate(rad(TILT)); ctx.scale(1, SQ);
    diskRing(180, diskS); diskDots(t, false);            // nửa sau của đĩa, bị chân trời che
    ctx.restore();
    ctx.fillStyle = shadowS; fillCirc(0, 0, rh * 1.7);
    ctx.fillStyle = '#000'; fillCirc(0, 0, rh);
    ctx.save(); ctx.rotate(rad(TILT));                   // mặt sau đĩa bị thấu kính hấp dẫn uốn vòng qua đỉnh
    ctx.strokeStyle = diskS; ctx.globalAlpha = 190 / 255; ctx.lineWidth = rh * .2; drawArc(-rh * 1.45, -rh * 1.32, rh * 1.45, rh * 1.32, 195, 150);
    ctx.globalAlpha = 90 / 255; ctx.lineWidth = rh * .1; drawArc(-rh * 1.3, -rh * 1.2, rh * 1.3, rh * 1.2, 25, 130);
    ctx.globalAlpha = 1; ctx.restore();
    ctx.strokeStyle = alpha(0xFFFFE7B8, .9); ctx.lineWidth = 1.6 * DP; strokeCirc(0, 0, rh * 1.04);
    ctx.save(); ctx.rotate(rad(TILT)); ctx.scale(1, SQ);
    diskRing(0, diskS); diskDots(t, true);               // nửa trước của đĩa, đè lên chân trời
    ctx.restore();
    stream(t, FRAG, DUST / 2, 1.2, .33 * U, -2.2, .08, 1.6, .45);   // bụi thiên thạch lượn vào
    stream(t, FRAG + DUST / 2, DUST / 2, 1.9, .36 * U, -1.5, .07, 1.4, .35);
    drawTornPlanet(t);
    ctx.restore();
  }

  function diskRing(start, diskS) {
    const n = 8, r0 = 1.32 * rh, r1 = 3 * rh, w = (r1 - r0) / n;
    ctx.lineCap = 'butt'; ctx.strokeStyle = diskS;
    for (let j = 0; j < n; j++) {
      const r = r0 + (j + .5) * w, a = j === 0 ? 1 : .9 * Math.pow(1 - j / n, 1.4) + .06;
      ctx.lineWidth = w * 1.25; ctx.globalAlpha = a; drawArc(-r, -r, r, r, start, 180);
    }
    ctx.globalAlpha = 1; ctx.strokeStyle = alpha(0xFFFFF6E0, .9); ctx.lineWidth = w * .5; drawArc(-r0, -r0, r0, r0, start, 180);
    ctx.lineCap = 'round';
  }
  function diskDots(t, front) {
    ctx.fillStyle = alpha(0xFFFFE0A8, .7);
    for (let i = 0; i < DISK_P; i++) {
      const a = diskA[i] - t * 1.6 / Math.pow(diskR[i], 1.5), s = sin(a);
      if ((s >= 0) !== front) continue;
      const r = diskR[i] * rh; fillCirc(cos(a) * r, s * r, diskSz[i] * DP);
    }
  }

  // Điểm trên đường xoắn từ (th0, rs) vào mép đĩa, trong hệ toạ độ đã dời tới tâm hố đen
  function spiral(u, th0, rs, sweep) {
    const e = Math.pow(Math.max(0, u), spPow), th = th0 + sweep * e, r = rs + (2.85 * rh - rs) * e, q = .82 + (SQ - .82) * e;
    const x = cos(th) * r, y = sin(th) * r * q, ct = cos(rad(TILT)), st = sin(rad(TILT));
    spX = ct * x - st * y; spY = st * x + ct * y;
  }
  function stream(t, j0, n, th0, rs, sweep, speed, size, al) {
    for (let i = 0; i < n; i++) {
      const u = frac(t * speed * (1 + jit[j0 + i] * .3) + i / n), fade = Math.min(1, u * 12) * Math.min(1, (1 - u) * 8) * al;
      spiral(Math.max(0, u - .02), th0, rs, sweep); const px = spX, py = spY;
      spiral(u, th0, rs, sweep);
      const off = jit[j0 + i] * 16 * DP * (1 - u); let nx = -(spY - py), ny = spX - px; const nl = Math.hypot(nx, ny);
      if (nl > 0) { nx = nx / nl * off; ny = ny / nl * off; }
      ctx.strokeStyle = alpha(mix(FC[SAND], 0xFFFFA04A, u), fade); ctx.lineWidth = size * (1 - .6 * u) * DP;
      ctx.beginPath(); ctx.moveTo(px + nx, py + ny); ctx.lineTo(spX + nx, spY + ny); ctx.stroke();
    }
  }

  // Hình hành tinh bị xé trong hệ toạ độ riêng: +x hướng về hố đen
  function buildTorn() {
    const R = tornR = .046 * U, bd = BITE * 180 / Math.PI;
    tornBody = new Path2D(); tornEdge = new Path2D(); tornCracks = new Path2D(); tornGhost = new Path2D();
    tornBody.moveTo(cos(rad(bd)) * R, sin(rad(bd)) * R); tornBody.arc(0, 0, R, rad(bd), rad(bd + 360 - 2 * bd));
    for (let k = 0; k <= BITE_N; k++) {
      const a = -BITE + 2 * BITE * k / BITE_N, rr = R * biteF[k];
      tornBody.lineTo(cos(a) * rr, sin(a) * rr);
      k === 0 ? tornEdge.moveTo(cos(a) * rr, sin(a) * rr) : tornEdge.lineTo(cos(a) * rr, sin(a) * rr);
    }
    tornBody.closePath();
    tornGhost.addPath(tornEdge);                         // phần thân cũ đã bị rút ra: giữa mép khoét và đường tròn ban đầu
    tornGhost.arc(0, 0, R, rad(bd), rad(-bd), true); tornGhost.closePath();
    for (let i = 0; i < CRACKS; i++) {
      const k = crackK[i], a = -BITE + 2 * BITE * k / BITE_N, rr = R * biteF[k], x = cos(a) * rr, y = sin(a) * rr, L = crackL[i] * R;
      tornCracks.moveTo(x, y); tornCracks.lineTo(x - L * .5, y + (crackB[i] + y / R * .3) * L * .6); tornCracks.lineTo(x - L, y + (crackB[i] * .2 + y / R * .5) * L);
    }
    sandS = radial(R * .35, -R * .3, R * 1.6, [[0, alpha(FL[SAND], 1)], [.45, alpha(mix(FC[SAND], 0xFF8A5A40, .4), 1)], [1, alpha(mix(FC[SAND], 0xFF000000, .72), 1)]]);
    heatS = radial(0, 0, R * 1.6, [[0, alpha(0xFFFF9A40, .55)], [.45, alpha(0xFFFF6A20, .18)], [1, 'rgba(0,0,0,0)']]);
  }

  function drawTornPlanet(t) {
    const R = tornR, rs = TORN_RS * U;
    spPow = 2.2;                                         // đá lưu lại lâu gần hành tinh rồi mới tăng tốc vào đĩa
    spiral(0, TORN_TH0, rs, TORN_SW);
    const sx0 = spX, sy0 = spY, dl = Math.hypot(sx0, sy0), hx = -sx0 / dl, hy = -sy0 / dl;
    const px = sx0 - hx * R * .62, py = sy0 - hy * R * .62, ang = Math.atan2(hy, hx);
    const seg = [0, .3, .65, 1];                         // vệt bụi mờ dọc đường bị hút, thuôn dần
    for (let sgi = 0; sgi < 3; sgi++) {
      ctx.beginPath();
      for (let k = 0; k <= 12; k++) { spiral(seg[sgi] + (seg[sgi + 1] - seg[sgi]) * k / 12, TORN_TH0, rs, TORN_SW); k === 0 ? ctx.moveTo(spX, spY) : ctx.lineTo(spX, spY); }
      ctx.strokeStyle = alpha(0xFFFF9050, sgi === 0 ? .14 : .1); ctx.lineWidth = R * (sgi === 0 ? 1.3 : sgi === 1 ? .6 : .25); ctx.stroke();
    }
    ctx.save(); ctx.translate(px + hx * R * .55, py + hy * R * .55);
    ctx.fillStyle = heatS; ctx.globalAlpha = .8 + .2 * sin(t * 2.3); fillCirc(0, 0, R * 1.6); ctx.globalAlpha = 1; ctx.restore();

    drawFragments(t, R, rs);

    ctx.save(); ctx.translate(px, py); ctx.rotate(ang); ctx.scale(1.1 + .03 * sin(t * 1.3), .93);   // phình nhẹ về phía hố đen do lực thuỷ triều
    const gl = .5 + .5 * sin(t * 1.9);
    ctx.fillStyle = alpha(0xFFFF6A2A, .16 + .08 * gl); ctx.fill(tornGhost);
    dashGhost(t);
    ctx.fillStyle = sandS; ctx.fill(tornBody);
    ctx.save(); ctx.clip(tornBody);
    ctx.strokeStyle = argb(0x2A000000); ctx.lineWidth = R * .14;
    for (let b = 0; b < 3; b++) { const k = -.45 + b * .48; ctx.beginPath(); ctx.moveTo(-R * 1.1, R * k); ctx.quadraticCurveTo(0, R * (k + .15 * sin(b * 2.1 + t * .3)), R * 1.1, R * k); ctx.stroke(); }
    ctx.translate(R * .7, 0); ctx.scale(.6, .6);         // bề mặt sát vết khoét bị nung đỏ
    ctx.fillStyle = heatS; ctx.globalAlpha = 210 / 255; fillCirc(0, 0, R * 1.6); ctx.globalAlpha = 1;
    ctx.restore();
    const bd = BITE * 180 / Math.PI;
    ctx.strokeStyle = argb(0x70FFFFFF); ctx.lineWidth = 1.2 * DP; drawArc(-R, -R, R, R, bd + 6, 360 - 2 * bd - 12);
    const fl = .55 + .45 * sin(t * 3.1);
    ctx.strokeStyle = alpha(0xFFFF8A3A, .35 * fl); ctx.lineWidth = 3 * DP; ctx.stroke(tornCracks);
    ctx.strokeStyle = alpha(0xFFFFD08A, .85 * fl); ctx.lineWidth = DP; ctx.stroke(tornCracks);
    ctx.strokeStyle = alpha(0xFFFF7A2A, .5); ctx.lineWidth = 4 * DP; ctx.stroke(tornEdge);
    ctx.strokeStyle = alpha(0xFFFFE2A0, .95); ctx.lineWidth = 1.4 * DP; ctx.stroke(tornEdge);
    ctx.restore();
    spPow = 1.5;
  }
  // Đường viền cũ của hành tinh phía bị khoét: chỉ còn những đoạn đứt quãng mờ dần, đang tan ra
  function dashGhost(t) {
    const R = tornR, bd = BITE * 180 / Math.PI;
    ctx.lineWidth = .8 * DP;
    for (let k = 0; k < 7; k++) {
      const a0 = -bd + k * 2 * bd / 7, f = .5 + .5 * sin(t * 1.4 + k * 1.9);
      ctx.strokeStyle = alpha(0xFFFFB070, (50 + 70 * f) / 255); drawArc(-R, -R, R, R, a0, 2 * bd / 7 * (.3 + .4 * f));
    }
  }

  function drawFragments(t, R, rs) {
    for (let i = 0; i < FRAG; i++) {
      const jt = jit[i], jt2 = jit[(i * 7 + 3) % FRAG], u = frac(t * .05 * (1 + jt * .3) + i / FRAG), fade = Math.min(1, u * 40) * Math.min(1, (1 - u) * 8);
      spiral(Math.min(1, u + .01), TORN_TH0, rs, TORN_SW); const nx2 = spX, ny2 = spY;
      spiral(u, TORN_TH0, rs, TORN_SW);
      let nx = -(ny2 - spY), ny = nx2 - spX; const nl = Math.hypot(nx, ny), off = (jt * 1.6 + jt2 * .5) * R * Math.pow(1 - u, 1.4);
      if (nl > 0) { nx = nx / nl * off; ny = ny / nl * off; } else { nx = 0; ny = 0; }
      const x = spX + nx, y = spY + ny;
      if (u < .62) {
        const near = u < .25;                            // mảng lớn vừa bong khỏi vết khoét
        const size = (near ? R * (.26 - .14 * u / .25) : R * .12 + (1.5 * DP - R * .12) * (u - .25) / .37) * (1 + jt2 * .5);
        const heat = near ? u / .25 * .3 : .3 + .7 * (u - .25) / .37;
        if (heat > .3) { ctx.fillStyle = alpha(0xFFFF8A3A, .35 * heat * fade); fillCirc(x, y, size * 2); }
        const rock = rockShape[i % rockShape.length];
        ctx.save(); ctx.translate(x, y); ctx.rotate(rad(jt * 720 + t * (40 + jt * 80) * (1 + u * 3))); ctx.scale(size, size);
        ctx.fillStyle = alpha(mix(0xFF9C8E7E, 0xFFFF9A48, heat), fade); ctx.fill(rock);
        ctx.strokeStyle = alpha(0xFFFFB070, (.25 + .6 * heat) * fade); ctx.lineWidth = DP / size; ctx.stroke(rock);
        ctx.restore();
      } else {                                           // đã bị hút: vệt nóng sáng chảy vào đĩa
        const k = (u - .62) / .38;
        spiral(u - .025, TORN_TH0, rs, TORN_SW); const qx = spX + nx, qy = spY + ny;
        ctx.strokeStyle = alpha(0xFFFF8A3A, .25 * fade); ctx.lineWidth = 3.5 * DP; ctx.beginPath(); ctx.moveTo(qx, qy); ctx.lineTo(x, y); ctx.stroke();
        ctx.strokeStyle = alpha(mix(0xFFFF9A48, 0xFFFFF4D0, k), fade); ctx.lineWidth = (2.2 - 1.2 * k) * DP; ctx.beginPath(); ctx.moveTo(qx, qy); ctx.lineTo(x, y); ctx.stroke();
      }
    }
  }

  function drawBelt(t, low) {
    cam(low ? .7 : .85);
    let ox = b0x, oy = b0y, dx = bdx, dy = bdy, len = bLen, half = .05 * H, spd = 9, ph = 0;
    if (low) {
      const [x0, y0, x1, y1] = LOW_BELT; ox = x0 * W; oy = y0 * H; len = Math.hypot((x1 - x0) * W, (y1 - y0) * H);
      dx = (x1 - x0) * W / len; dy = (y1 - y0) * H / len; half = LOW_BELT_HALF * H; spd = LOW_BELT_SPEED * 9; ph = .37;
    }
    const nx = -dy, ny = dx;
    for (let i = 0; i < BELT; i++) {
      const s = frac(beltS[i] + ph + t * beltDepth[i] * spd * DP / len) * len, x = ox + dx * s + nx * beltN[i] * half, y = oy + dy * s + ny * beltN[i] * half;
      const sz = beltSize[i] * DP, a = .45 + .5 * (beltDepth[i] - .55) / .6, rock = rockShape[i % rockShape.length];
      ctx.save(); ctx.translate(x, y); ctx.rotate(rad(beltRot[i] + t * beltSpin[i])); ctx.scale(sz, sz);
      ctx.fillStyle = alpha(mix(0xFF7B7168, 0xFF8E7B66, beltCol[i]), a); ctx.fill(rock);
      ctx.translate(.22, .22); ctx.scale(.65, .65); ctx.fillStyle = alpha(0xFF000000, .25 * a); ctx.fill(rock);
      ctx.restore();
    }
    ctx.restore();
  }

  // Thiên thạch tiền cảnh: to, mờ nhoè (ngoài vùng nét) và trôi nhanh hơn để tạo parallax
  function drawForeground(t) {
    cam(1.8);
    const slope = bdy / bdx, m = 40 * DP;
    ctx.fillStyle = blurS;
    for (let i = 0; i < FG; i++) {
      const x = frac(fgX[i] + t * fgV[i] * DP / (W + 2 * m)) * (W + 2 * m) - m, y = FG_Y[i] * H + slope * x, k = fgSize[i] * DP / 100;
      ctx.save(); ctx.translate(x, y); ctx.scale(k, k); ctx.globalAlpha = (120 + i * 15) / 255; fillCirc(0, 0, 100); ctx.restore();
    }
    ctx.restore();
  }

  /** Tâm và bán kính hành tinh xanh trên màn hình ở khung vừa vẽ (đã tính camera). */
  function playPlanet() {
    const s = 1 + zoom, p = PL[0];
    return { x: camX(p[0] * W, p[4]), y: camY(p[1] * H, p[4]), R: p[2] * U * s, orbitOuter: ringRad(2) * s };
  }
  return { setSize, draw, playPlanet, setPlayFx: fx => { playFx = fx; }, ringRad, launchRingPoint: (k, a) => ({ r: ringRad(k), a }) };
})();

// ---------- WelcomeScreen ----------
// Hằng số hướng 3: hành tinh xanh là nút "Chơi ngay", la bàn là nút "Bản đồ"
const PLAY_PRESS_SCALE = .965, PLAY_HIT_SCALE = 1.15, PLAY_PRESS_GLOW = .14;
const HINT_RING_GAP = 50, HINT_RING_PULSE = 3;                     // vòng vàng nét đứt ngoài vòng đá ngoài cùng
const PLAY_ICON_H = .56, PLAY_DISC_R = .56, PLAY_DISC_ALPHA = .58; // biểu tượng play: tam giác cao .56 × bán kính hành tinh trên đĩa lõi bán kính .56, đặt đúng tâm
const PLAY_PULSE = .03;                                            // nhịp thở 3%
const SOUND_BTN = 44, SOUND_MARGIN = 16, SOUND_TOP = 18;        // nút âm thanh dạng icon vuông 44dp (như nút Back)
const LANG_W = 58, LANG_H = 34, LANG_MARGIN = 16, LANG_TOP = 18, LANG_RADIUS = 12; // nút ngôn ngữ dạng pill 58×34dp góc trên phải: quả cầu + mã VI/EN, bấm để đổi
const LANG_GLOBE_R = 7, LANG_GAP = 6, LANG_TEXT = 13;            // quả cầu bán kính 7dp, cách chữ 6dp, chữ 13dp; cả cụm canh giữa nút, cùng tâm dọc
const LANGS = ['vi', 'en'], LANG_CODES = { vi: 'VI', en: 'EN' };
const VERSION_LABEL = { vi: 'Phiên bản', en: 'Version' };
const BAR_MARGIN = 28, BAR_Y_FROM_BOTTOM = 76;                  // bố cục 'bar': hai nút phụ cùng một hàng dưới, cách mép 28dp, tâm cách đáy 76dp
const TAP_SECONDS = .9, BURST_ROCKS = 16, BURST_SPEED = 300, TAP_POP = .09;
const COMPASS_R = 34, COMPASS_Y_FROM_BOTTOM = 100;              // bố cục 'center': la bàn giữa dưới
const COMPASS_PRESS_SCALE = .92, COMPASS_HIT_SCALE = 1.35, SPIN_SECONDS = 1.0, SPIN_TURNS = 2;
const LEVEL_NAMES = { 3: 'Mùa khô', 9: 'Đối thủ tăng tốc' };

function text(s, x, y, size, color, align = 'center', weight = '700') {
  ctx.font = `${weight} ${size}px Roboto, "Helvetica Neue", Arial, sans-serif`;
  ctx.textAlign = align; ctx.textBaseline = 'alphabetic';
  const m = ctx.measureText(s), asc = m.fontBoundingBoxAscent ?? size * .93, desc = m.fontBoundingBoxDescent ?? size * .25;
  ctx.fillStyle = typeof color === 'number' ? argb(color) : color;
  ctx.fillText(s, x, y + (asc - desc) / 2);               // DrawKit.text: canh giữa theo ascent/descent
}
function button(label, x0, y0, x1, y1, style, pressed) {
  const rad2 = Math.min((y1 - y0) / 2, 16);
  ctx.save();
  if (pressed) { ctx.translate((x0 + x1) / 2, (y0 + y1) / 2); ctx.scale(.96, .96); ctx.translate(-(x0 + x1) / 2, -(y0 + y1) / 2); }
  let fillC, textC, strokeC = 0;
  if (style === 'PRIMARY') { fillC = pressed ? mix(C_YOU, 0xFF000000, .18) : C_YOU; textC = 0xFF04170F; }
  else if (style === 'SECONDARY') { fillC = pressed ? 0x2EFFFFFF : 0x14FFFFFF; textC = C_INK; strokeC = 0x55A0AFFF; }
  else { fillC = pressed ? mix(C_GOLD, 0xFF000000, .18) : C_GOLD; textC = 0xFF241A00; }
  ctx.fillStyle = argb(fillC); ctx.beginPath(); ctx.roundRect(x0, y0, x1 - x0, y1 - y0, rad2); ctx.fill();
  if (strokeC) { ctx.strokeStyle = argb(strokeC); ctx.lineWidth = 1.2 * DP; ctx.beginPath(); ctx.roundRect(x0, y0, x1 - x0, y1 - y0, rad2); ctx.stroke(); }
  text(label, (x0 + x1) / 2, (y0 + y1) / 2, style === 'SMALL' ? 13.5 : 16.5, textC);
  ctx.restore();
}

const state = { lang: 'vi', sound: false,  t: 0, pause: false, speed: 1, mode: 'planet', next: 3, pos: 'bar', hint: true, pressed: null, inside: false, playT: -1, mapT: -1, rocks: [] };
const easeOut = u => 1 - Math.pow(1 - u, 3);

// Nút chữ nhật (bản hiện tại)
function rectButtons() {
  const bw = Math.min(W - 48, 420), bx = (W - bw) / 2;
  return [
    { id: 'play', label: 'Chơi ngay', style: 'PRIMARY', r: [bx, H - 190, bx + bw, H - 134] },
    { id: 'map', label: 'Bản đồ', style: 'SECONDARY', r: [bx, H - 122, bx + bw, H - 66] },
  ];
}
/** Vị trí nút âm thanh theo bố cục: 'bar' = góc dưới trái cùng hàng với la bàn; 'center' = góc trên phải. */
function soundRect() {
  if (state.pos === 'bar') { const cy = H - BAR_Y_FROM_BOTTOM; return [BAR_MARGIN, cy - SOUND_BTN / 2, BAR_MARGIN + SOUND_BTN, cy + SOUND_BTN / 2]; }
  return [W - SOUND_MARGIN - SOUND_BTN, SOUND_TOP, W - SOUND_MARGIN, SOUND_TOP + SOUND_BTN];
}

// ----- Hướng 3: hành tinh là nút Chơi ngay -----
function nextLabel() { return state.next === 0 ? 'Hướng dẫn' : state.next < 0 ? 'Endless' : `Màn ${state.next} · ${LEVEL_NAMES[state.next] || ''}`; }
const playActive = () => state.pressed === 'play' && state.inside;
const mapActive = () => state.pressed === 'map' && state.inside;

/** Hiệu ứng truyền vào Scene trước khi vẽ: nhấn thì nhỏ lại và sáng hơn; chạm thì phồng lên rồi co về. */
function playFx() {
  if (state.playT >= 0) { const u = state.playT / TAP_SECONDS; return { scale: 1 + TAP_POP * Math.sin(Math.min(1, u * 2.2) * Math.PI), glow: Math.max(0, 1 - u * 1.6) * .35 }; }
  return playActive() ? { scale: PLAY_PRESS_SCALE, glow: PLAY_PRESS_GLOW } : { scale: 1, glow: 0 };
}

function drawPlayLabel() {
  const p = Scene.playPlanet(), fx = playFx();
  ctx.save(); ctx.translate(p.x, p.y); ctx.scale(fx.scale, fx.scale); ctx.translate(-p.x, -p.y);
  // Lõi kính tối cùng tông hành tinh (xanh ngọc đậm) để biểu tượng sáng nổi bật
  const breath = 1 + PLAY_PULSE * Math.sin(state.t * 2.4), dr = p.R * PLAY_DISC_R * breath, dark = mix(FC[0], 0xFF000000, .62), light = FL[0];
  ctx.fillStyle = radial(p.x - dr * .25, p.y - dr * .3, dr * 1.3, [[0, alpha(mix(FC[0], 0xFF000000, .35), PLAY_DISC_ALPHA)], [1, alpha(dark, PLAY_DISC_ALPHA + .12)]]); fillCirc(p.x, p.y, dr);
  ctx.strokeStyle = alpha(light, .5); ctx.lineWidth = 1.5; strokeCirc(p.x, p.y, dr);
  const h = p.R * PLAY_ICON_H, w = h * .87, x0 = p.x - w / 3, x1 = p.x + w * 2 / 3;   // tam giác có trọng tâm đúng tâm hành tinh
  ctx.save(); ctx.shadowColor = alpha(FC[0], .9); ctx.shadowBlur = h * .5;
  ctx.fillStyle = linear(x0, p.y - h / 2, x1, p.y + h / 2, [[0, '#ffffff'], [1, argb(mix(light, 0xFFFFFFFF, .3))]]);
  ctx.strokeStyle = ctx.fillStyle; ctx.lineJoin = 'round'; ctx.lineWidth = h * .16;
  ctx.beginPath(); ctx.moveTo(x0, p.y - h / 2); ctx.lineTo(x0, p.y + h / 2); ctx.lineTo(x1, p.y); ctx.closePath(); ctx.fill(); ctx.stroke();
  ctx.restore();
  ctx.restore();
}

function launchBurst() {
  const p = Scene.playPlanet(), r = new JRandom(Math.floor(state.t * 1000)); state.rocks = [];
  for (let k = 0; k < BURST_ROCKS; k++) {
    const a = r.nextFloat() * TAU, s = BURST_SPEED * (.6 + r.nextFloat() * .6);
    state.rocks.push({ x: p.x + cos(a) * p.orbitOuter, y: p.y + sin(a) * p.orbitOuter, vx: cos(a) * s, vy: sin(a) * s, life: 1, trail: [] });
  }
}
function drawBurst(dt) {
  ctx.lineCap = 'round';
  for (const r of state.rocks) {
    r.trail.push([r.x, r.y]); if (r.trail.length > 8) r.trail.shift();
    r.x += r.vx * dt; r.y += r.vy * dt; r.life -= dt / TAP_SECONDS;
    for (let i = 1; i < r.trail.length; i++) { ctx.strokeStyle = alpha(FC[0], Math.max(0, r.life) * i / r.trail.length * .6); ctx.lineWidth = 2.2; ctx.beginPath(); ctx.moveTo(...r.trail[i - 1]); ctx.lineTo(...r.trail[i]); ctx.stroke(); }
    ctx.fillStyle = alpha(0xFFFFFFFF, Math.max(0, r.life)); fillCirc(r.x, r.y, 2.4);
  }
  state.rocks = state.rocks.filter(r => r.life > 0);
}

// ----- Hướng 3: la bàn là nút Bản đồ -----
function compass() { return state.pos === 'center' ? { x: W / 2, y: H - COMPASS_Y_FROM_BOTTOM, R: COMPASS_R } : { x: W - BAR_MARGIN - COMPASS_R, y: H - BAR_Y_FROM_BOTTOM, R: COMPASS_R }; }
function drawCompass() {
  const { x, y, R } = compass(), pressed = mapActive(), t = state.t;
  let needle = Math.sin(t * .9) * .35 + Math.sin(t * 2.3) * .08 - .5, glow = 0;
  if (state.mapT >= 0) { const u = Math.min(1, state.mapT / SPIN_SECONDS); needle = -.5 + easeOut(u) * TAU * SPIN_TURNS; glow = 1 - u; }
  ctx.save(); ctx.translate(x, y); const sc = pressed ? COMPASS_PRESS_SCALE : 1; ctx.scale(sc, sc);
  if (glow > 0) { ctx.fillStyle = radial(0, 0, R * 2.4, [[.33, alpha(C_GOLD, .35 * glow)], [1, alpha(C_GOLD, 0)]]); fillCirc(0, 0, R * 2.4); }
  ctx.fillStyle = radial(-R * .3, -R * .35, R * 1.1, [[0, 'rgba(255,255,255,.16)'], [1, 'rgba(14,18,42,.88)']]); fillCirc(0, 0, R);
  ctx.strokeStyle = alpha(C_GOLD, pressed ? 1 : .7); ctx.lineWidth = 1.5; strokeCirc(0, 0, R);   // viền vàng: màu của "đường đi / màn kế"
  ctx.setLineDash([2, 5]); ctx.lineWidth = 1; ctx.strokeStyle = alpha(C_GOLD, .3); strokeCirc(0, 0, R * .78); ctx.setLineDash([]);
  for (let k = 0; k < 12; k++) {   // vạch chia
    const a = k * TAU / 12, long = k % 3 === 0, r0 = R * (long ? .82 : .88);
    ctx.strokeStyle = 'rgba(233,237,255,.5)'; ctx.lineWidth = long ? 1.6 : 1; ctx.beginPath(); ctx.moveTo(cos(a) * r0, sin(a) * r0); ctx.lineTo(cos(a) * R * .96, sin(a) * R * .96); ctx.stroke();
  }
  [[-2.2, 2], [.4, 3], [2.5, 4]].forEach(([a, o]) => { ctx.fillStyle = argb(FC[o]); fillCirc(cos(a) * R * .62, sin(a) * R * .62, 2.6); });   // ba hành tinh nhỏ gợi bản đồ
  ctx.save(); ctx.rotate(needle);   // kim: đầu vàng, đuôi trắng
  ctx.fillStyle = argb(C_GOLD); ctx.beginPath(); ctx.moveTo(0, -R * .72); ctx.lineTo(R * .14, 0); ctx.lineTo(-R * .14, 0); ctx.closePath(); ctx.fill();
  ctx.fillStyle = 'rgba(233,237,255,.85)'; ctx.beginPath(); ctx.moveTo(0, R * .55); ctx.lineTo(R * .14, 0); ctx.lineTo(-R * .14, 0); ctx.closePath(); ctx.fill();
  ctx.restore();
  ctx.fillStyle = argb(C_BG); fillCirc(0, 0, 2.6); ctx.strokeStyle = argb(C_GOLD); ctx.lineWidth = 1; strokeCirc(0, 0, 2.6);
  ctx.restore();
}

/** Nút âm thanh dạng icon (kiểu ICON của DrawKit.drawButton): loa + sóng khi bật, loa + dấu X khi tắt. */
function drawSoundButton() {
  const [x0, y0, x1, y1] = soundRect(), cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, pressed = state.pressed === 'sound' && state.inside;
  ctx.save();
  if (pressed) { ctx.translate(cx, cy); ctx.scale(.96, .96); ctx.translate(-cx, -cy); }
  ctx.fillStyle = argb(pressed ? 0xF01E2650 : 0xE00E122A); ctx.beginPath(); ctx.roundRect(x0, y0, SOUND_BTN, SOUND_BTN, 14); ctx.fill();
  ctx.strokeStyle = state.sound ? alpha(C_YOU, .55) : argb(C_LINE); ctx.lineWidth = 1.2; ctx.beginPath(); ctx.roundRect(x0, y0, SOUND_BTN, SOUND_BTN, 14); ctx.stroke();
  const ink = state.sound ? argb(C_YOU) : argb(C_MUTED);   // bật: xanh ngọc (màu hành động chính), tắt: xám
  ctx.fillStyle = ink; ctx.strokeStyle = ink; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  ctx.beginPath(); ctx.moveTo(cx - 9, cy - 3.5); ctx.lineTo(cx - 5, cy - 3.5); ctx.lineTo(cx + 1, cy - 8); ctx.lineTo(cx + 1, cy + 8); ctx.lineTo(cx - 5, cy + 3.5); ctx.lineTo(cx - 9, cy + 3.5); ctx.closePath(); ctx.fill();
  ctx.lineWidth = 2;
  if (state.sound) {
    ctx.beginPath(); ctx.arc(cx + 1, cy, 5, -.9, .9); ctx.stroke();
    ctx.beginPath(); ctx.arc(cx + 1, cy, 9, -.9, .9); ctx.stroke();
  } else {
    ctx.beginPath(); ctx.moveTo(cx + 5, cy - 4); ctx.lineTo(cx + 12, cy + 4); ctx.moveTo(cx + 12, cy - 4); ctx.lineTo(cx + 5, cy + 4); ctx.stroke();
  }
  ctx.restore();
}

function langRect() { return [W - LANG_MARGIN - LANG_W, LANG_TOP, W - LANG_MARGIN, LANG_TOP + LANG_H]; }

/** Nút ngôn ngữ (UiButton.Style.ICON kèm nhãn): quả cầu (vòng tròn, kinh tuyến, xích đạo) + mã ngôn ngữ đang dùng. */
function drawLangButton() {
  const [x0, y0] = langRect(), cx = x0 + LANG_W / 2, cy = y0 + LANG_H / 2, pressed = state.pressed === 'lang' && state.inside;
  ctx.save();
  if (pressed) { ctx.translate(cx, cy); ctx.scale(.96, .96); ctx.translate(-cx, -cy); }
  ctx.fillStyle = argb(pressed ? 0xF01E2650 : 0xE00E122A); ctx.beginPath(); ctx.roundRect(x0, y0, LANG_W, LANG_H, LANG_RADIUS); ctx.fill();
  ctx.strokeStyle = alpha(C_YOU, .55); ctx.lineWidth = 1.2; ctx.beginPath(); ctx.roundRect(x0, y0, LANG_W, LANG_H, LANG_RADIUS); ctx.stroke();
  const ink = argb(C_YOU), r = LANG_GLOBE_R, code = LANG_CODES[state.lang];
  ctx.font = `800 ${LANG_TEXT}px Roboto, "Helvetica Neue", Arial, sans-serif`;
  const tw = Math.max(...LANGS.map(l => ctx.measureText(LANG_CODES[l]).width)), start = cx - (2 * r + LANG_GAP + tw) / 2, gx = start + r;   // đo theo mã rộng nhất: cụm không nhảy khi đổi VI/EN
  ctx.strokeStyle = ink; ctx.lineWidth = 1.5; ctx.lineCap = 'round';
  ctx.beginPath(); ctx.arc(gx, cy, r, 0, Math.PI * 2); ctx.stroke();
  ctx.beginPath(); ctx.ellipse(gx, cy, r * .42, r, 0, 0, Math.PI * 2); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(gx - r, cy); ctx.lineTo(gx + r, cy); ctx.stroke();
  text(code, start + 2 * r + LANG_GAP, cy, LANG_TEXT, C_INK, 'left', '800');
  ctx.restore();
}

function drawScreen(dt) {
  const planetMode = state.mode === 'planet';
  Scene.setPlayFx(planetMode ? playFx() : null);
  StarField.draw(state.t);
  Scene.draw(state.t);
  const ty = Math.max(80 * DP, H * .13);
  text('Planet', W / 2, ty, 44, C_INK, 'center', '900'); text('Conquest', W / 2, ty + 48, 44, C_INK, 'center', '900');
  if (!planetMode) text('Kỷ lục Endless: 1 bản đồ', W / 2, H - 44, 12.5, C_MUTED, 'center', '400');   // hướng 3 bỏ dòng này
  text(VERSION_LABEL[state.lang] + ' 0.3.0', W / 2, H - 24, 11.5, alpha(C_MUTED, .7), 'center', '400');
  if (planetMode) { drawPlayLabel(); drawBurst(dt); drawCompass(); }
  else for (const b of rectButtons()) button(b.label, ...b.r, b.style, state.pressed === b.id && state.inside);
  drawSoundButton();
  drawLangButton();
}

// ---------- Vòng lặp & điều khiển ----------
let last = performance.now();
function frame(now) {
  const dt = Math.min(.05, (now - last) / 1000); last = now;
  const sdt = state.pause ? 0 : dt * state.speed;
  state.t += sdt;
  if (state.playT >= 0) { state.playT += dt; if (state.playT > TAP_SECONDS + .5) state.playT = -1; }
  if (state.mapT >= 0) { state.mapT += dt; if (state.mapT > SPIN_SECONDS + .4) state.mapT = -1; }
  document.getElementById('tV').textContent = state.t.toFixed(1) + ' s';
  ctx.clearRect(0, 0, W, H); drawScreen(dt);
  requestAnimationFrame(frame);
}
const $ = id => document.getElementById(id);
const log = m => { $('log').innerHTML = m; };
function setScreen(h) { H = h; resizeCanvas(); StarField.build(); Scene.setSize(); }
function hit(x, y) {
  const lr = langRect();
  if (x >= lr[0] - 8 && x <= lr[2] + 8 && y >= lr[1] - 8 && y <= lr[3] + 8) return 'lang';
  const sr = soundRect();
  if (x >= sr[0] - 8 && x <= sr[2] + 8 && y >= sr[1] - 8 && y <= sr[3] + 8) return 'sound';   // nút icon có vùng chạm rộng thêm 8dp
  if (state.mode === 'planet') {
    const p = Scene.playPlanet(), c = compass();
    if (Math.hypot(x - p.x, y - p.y) <= p.R * PLAY_HIT_SCALE) return 'play';
    if (Math.hypot(x - c.x, y - c.y) <= c.R * COMPASS_HIT_SCALE) return 'map';
    return null;
  }
  const b = rectButtons().find(b => x >= b.r[0] && x <= b.r[2] && y >= b.r[1] && y <= b.r[3]);
  return b ? b.id : null;
}
function activate(id) {
  if (id === 'play') { if (state.mode === 'planet') { state.playT = 0; launchBurst(); } log(`Chơi ngay → <b>${nextLabel()}</b>`); }
  else if (id === 'map') { if (state.mode === 'planet') state.mapT = 0; log('Bản đồ → mở màn chọn màn'); }
  else if (id === 'lang') { state.lang = LANGS[(LANGS.indexOf(state.lang) + 1) % LANGS.length]; log('Ngôn ngữ → <b>' + LANG_CODES[state.lang] + '</b> (lưu lại, chữ đổi ngay)'); }
  else { state.sound = !state.sound; log('Âm thanh → ' + (state.sound ? '<b>bật</b>' : '<b>tắt</b>')); }
}
function pos(e) { const r = cv.getBoundingClientRect(); return [(e.clientX - r.left) * W / r.width, (e.clientY - r.top) * H / r.height]; }
cv.addEventListener('pointerdown', e => { state.pressed = hit(...pos(e)); state.inside = !!state.pressed; if (state.pressed) cv.setPointerCapture(e.pointerId); });
cv.addEventListener('pointermove', e => { if (state.pressed) state.inside = hit(...pos(e)) === state.pressed; });
cv.addEventListener('pointerup', () => { if (state.pressed && state.inside) activate(state.pressed); state.pressed = null; state.inside = false; });
cv.addEventListener('pointercancel', () => { state.pressed = null; state.inside = false; });

$('lang').onchange = e => { state.lang = e.target.value; };
$('mode').onchange = e => { state.mode = e.target.value; };
$('next').onchange = e => { state.next = +e.target.value; };
$('pos').onchange = e => { state.pos = e.target.value; };
$('size').onchange = e => setScreen(+e.target.value);
$('pause').onchange = e => { state.pause = e.target.checked; };
$('speed').oninput = e => { state.speed = +e.target.value; $('speedV').textContent = '×' + state.speed; };
$('t').oninput = e => { state.t = +e.target.value; };
$('jumpLaunch').onclick = () => { state.t = Math.floor(state.t / LAUNCH_PERIOD) * LAUNCH_PERIOD + LAUNCH_AT; $('t').value = state.t; };
$('tapPlay').onclick = () => activate('play');
$('tapMap').onclick = () => activate('map');

setScreen(780);
requestAnimationFrame(frame);
