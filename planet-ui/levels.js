// Prototype màn chọn màn: lộ trình hành tinh. 1 đơn vị CSS = 1 dp.
// Dữ liệu màn lấy từ Levels.java + strings.xml (level_names / level_limits). Tách: LEVELS (dữ liệu) / layout / vẽ.
'use strict';

const TAU = Math.PI * 2;
const W = 360, VIEW_H = 640;

// ---------- Hằng số bố cục (đặt tên giống bản native) ----------
const NODE_STEP = 128;            // khoảng cách dọc giữa hai hành tinh liên tiếp
const TOP_PAD = 110, BOTTOM_PAD = 90;
const ZIGZAG_X = [.5, .28, .72, .3, .7, .27, .73, .32, .68, .3, .7, .5];   // tỉ lệ x của từng nút (0 = Hướng dẫn ... 11 = Endless)
const NODE_R = 26, INTRO_R = 22, ENDLESS_R = 36;
const PATH_SAMPLES = 48, PATH_WOBBLE = 26, PATH_GAP = 10;               // điểm mẫu, biên độ uốn, chừa quanh hành tinh
const PATH_WIDTH = 3, PATH_DASH_OFF = [3, 8];
const DARK_BASE = 0xFF0B0E1C, DARK_AMOUNT = .78;                        // mức "tối" của hành tinh chưa qua
const LIT_SECONDS = .9;                                                 // thời gian sáng dần khi qua màn
const C_GOLD = 0xFFFFD166, C_INK = '#e9edff', C_MUTED = '#8f99c8', C_YOU = '#4ff0b4';

// ---------- Dữ liệu màn: mỗi hành tinh một màu và một đặc điểm riêng ----------
// kind = tên hàm vẽ đặc điểm; màu không trùng nhau
const LEVELS = [
  { name: 'Hướng dẫn',       limit: 'Không có hạn chế. Đối thủ đứng yên để bạn tập.',      planets: 2,  col: 0xFF9BE564, kind: 'sprout'  },
  { name: 'Làm quen',        limit: 'Không thể nâng cấp hành tinh.',                         planets: 3,  col: 0xFF3D6BFF, kind: 'ocean'   },
  { name: 'Kho nhỏ',         limit: 'Mỗi hành tinh chỉ chứa tối đa 30 đá, dù cấp nào.',    planets: 4,  col: 0xFFC98A3C, kind: 'cargo'   },
  { name: 'Mùa khô',         limit: 'Hành tinh không sinh thêm đá. Số đá là có hạn.',       planets: 4,  col: 0xFFF2E86D, kind: 'desert'  },
  { name: 'Chạy đua',        limit: 'Chỉ có 100 giây để chiếm hết hành tinh.',             planets: 5,  col: 0xFFFF4D5E, kind: 'speed'   },
  { name: 'Tay ngắn',        limit: 'Đá chỉ bay được một quãng ngắn.',                       planets: 6,  col: 0xFFA46BFF, kind: 'range'   },
  { name: 'Sương mù',        limit: 'Không thấy số đá và máu của đối thủ.',                 planets: 7,  col: 0xFF9FB0C8, kind: 'fog'     },
  { name: 'Bãi thiên thạch', limit: 'Thiên thạch dày đặc chặn đường: đá đụng vào sẽ vỡ.',  planets: 8,  col: 0xFF8A6A55, kind: 'belt'    },
  { name: 'Nạp đạn',         limit: 'Mỗi hành tinh chỉ gửi quân được một lần mỗi 4 giây.',  planets: 9,  col: 0xFFFF8FD8, kind: 'reload'  },
  { name: 'Đối thủ tăng tốc', limit: 'Mọi đối thủ sinh đá nhanh gấp đôi bạn.',              planets: 10, col: 0xFF00E5FF, kind: 'surge'   },
  { name: 'Viễn chinh',      limit: 'Các hành tinh cách nhau rất xa, đá bay rất chậm. Đánh được mọi hành tinh nhưng mỗi lần gửi đá đi là hở sườn.', planets: 5, col: 0xFF2EC4B6, kind: 'voyage'  },
  { name: 'Endless',         limit: 'Bản đồ ngẫu nhiên, không hạn chế. Thiên thạch đâm vào hành tinh.', planets: 0, col: 0xFFFFB347, kind: 'blackhole' },
];
const ENDLESS = LEVELS.length - 1;

// ---------- Tiện ích màu (ColorUtil) ----------
function mix(c, t, a) {
  const m = s => { const x = (c >>> s) & 255, y = (t >>> s) & 255; return Math.trunc(x + (y - x) * a); };
  return 0xFF000000 | (m(16) << 16) | (m(8) << 8) | m(0);
}
function rgba(c, a = 1) { return `rgba(${(c >>> 16) & 255},${(c >>> 8) & 255},${c & 255},${Math.max(0, Math.min(1, a))})`; }
function mulberry(a) { return () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
const lerp = (a, b, t) => a + (b - a) * t;
/** Màu hiển thị theo độ sáng lit (0 tối ... 1 sáng hết). */
const shade = (c, lit) => mix(c, DARK_BASE, DARK_AMOUNT * (1 - lit));

// ---------- Trạng thái ----------
const state = { cleared: new Array(LEVELS.length).fill(false), lit: new Array(LEVELS.length).fill(0), endlessBest: false, selected: -1, pause: false };
[0, 1, 2, 3].forEach(i => { state.cleared[i] = true; state.lit[i] = 1; });   // trạng thái mẫu: đã qua Hướng dẫn và màn 1-3

// ---------- Bố cục ----------
const MAP_H = TOP_PAD + (LEVELS.length - 1) * NODE_STEP + BOTTOM_PAD;
const nodes = LEVELS.map((L, i) => ({
  i, x: ZIGZAG_X[i] * W, y: MAP_H - BOTTOM_PAD - i * NODE_STEP,
  r: i === 0 ? INTRO_R : i === ENDLESS ? ENDLESS_R : NODE_R,
}));

/** Đường cong vẹo giữa hai nút: nội suy thẳng + lệch ngang bởi hai sóng sin, bao bởi sin(pi t) để hai đầu khớp hành tinh. */
function buildPath(a, b, seed) {
  const rnd = mulberry(seed * 977 + 13);
  const f1 = 1.3 + rnd() * 1.1, f2 = 2.6 + rnd() * 1.6, p1 = rnd() * TAU, p2 = rnd() * TAU, flip = rnd() < .5 ? -1 : 1;
  const dx = b.x - a.x, dy = b.y - a.y, len = Math.hypot(dx, dy), nx = -dy / len, ny = dx / len;
  const t0 = (a.r + PATH_GAP) / len, t1 = 1 - (b.r + PATH_GAP) / len;
  const pts = [];
  for (let k = 0; k <= PATH_SAMPLES; k++) {
    const t = lerp(t0, t1, k / PATH_SAMPLES), env = Math.pow(Math.sin(Math.PI * t), .8);
    const off = flip * PATH_WOBBLE * env * (Math.sin(t * TAU * f1 + p1) * .65 + Math.sin(t * TAU * f2 + p2) * .35);
    pts.push({ x: a.x + dx * t + nx * off, y: a.y + dy * t + ny * off });
  }
  return pts;
}
const paths = nodes.slice(0, -1).map((n, i) => buildPath(n, nodes[i + 1], i + 1));

// ---------- Canvas ----------
const cv = document.getElementById('map'), viewport = document.getElementById('viewport');
const dpr = Math.min(window.devicePixelRatio || 1, 3);
cv.width = W * dpr; cv.height = MAP_H * dpr; cv.style.height = MAP_H + 'px';
const ctx = cv.getContext('2d'); ctx.scale(dpr, dpr);

// Nền: tinh vân + sao trải theo cả chiều dài bản đồ (StarField)
const bg = document.createElement('canvas'); bg.width = W; bg.height = MAP_H;
(function buildBg() {
  const g = bg.getContext('2d'), lin = g.createLinearGradient(0, 0, 0, MAP_H);
  lin.addColorStop(0, '#04050d'); lin.addColorStop(1, '#070a1c'); g.fillStyle = lin; g.fillRect(0, 0, W, MAP_H);
  const nc = [[0x5A, 0x3C, 0xC8], [0x28, 0x8C, 0xC8], [0xC8, 0x46, 0x8C]];
  for (let k = 0; k < 9; k++) {
    const c = nc[k % 3], cx = (k * 0.37 % 1) * W, cy = (k + .5) / 9 * MAP_H, r = 340;
    const gr = g.createRadialGradient(cx, cy, 0, cx, cy, r);
    gr.addColorStop(0, `rgba(${c},.13)`); gr.addColorStop(1, `rgba(${c},0)`); g.fillStyle = gr; g.fillRect(0, cy - r, W, r * 2);
  }
  const rnd = mulberry(11);
  for (let i = 0; i < Math.round(MAP_H / 3.6); i++) {
    g.fillStyle = `rgba(255,255,255,${.2 + rnd() * .6})`; g.beginPath(); g.arc(rnd() * W, rnd() * MAP_H, rnd() * 1.1 + .2, 0, TAU); g.fill();
  }
})();

function text(s, x, y, size, color, align = 'center', weight = 'bold') {
  ctx.font = `${weight} ${size}px Roboto, "Helvetica Neue", Arial, sans-serif`;
  ctx.textAlign = align; ctx.textBaseline = 'middle';
  ctx.lineJoin = 'round'; ctx.lineWidth = 4; ctx.strokeStyle = 'rgba(5,7,15,.9)'; ctx.strokeText(s, x, y);   // viền tối để chữ đọc được trên đường nối
  ctx.fillStyle = color; ctx.fillText(s, x, y);
}
const circle = (x, y, r) => { ctx.beginPath(); ctx.arc(x, y, r, 0, TAU); };

// ---------- Vẽ đường nối ----------
function drawPath(i, clock) {
  const pts = paths[i], lit = Math.min(state.lit[i], 1), A = nodes[i], B = nodes[i + 1];
  const trace = () => { ctx.beginPath(); pts.forEach((p, k) => k ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)); };
  ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  // nền nét đứt mờ (đường chưa mở)
  trace(); ctx.setLineDash(PATH_DASH_OFF); ctx.lineWidth = PATH_WIDTH - 1; ctx.strokeStyle = 'rgba(160,175,255,.28)'; ctx.stroke(); ctx.setLineDash([]);
  if (lit <= 0.01) return;
  const g = ctx.createLinearGradient(A.x, A.y, B.x, B.y);
  g.addColorStop(0, rgba(LEVELS[i].col, lit)); g.addColorStop(1, rgba(LEVELS[i + 1].col, lit * (state.cleared[i + 1] ? 1 : .55)));
  trace(); ctx.lineWidth = PATH_WIDTH + 7; ctx.strokeStyle = g; ctx.globalAlpha = .13; ctx.stroke(); ctx.globalAlpha = 1;
  trace(); ctx.lineWidth = PATH_WIDTH; ctx.strokeStyle = g; ctx.stroke();
  // hạt sáng chạy dọc đường
  for (let k = 0; k < 3; k++) {
    const u = ((clock * .18 + k / 3) % 1) * (pts.length - 1), a = Math.floor(u), f = u - a, p = pts[a], q = pts[Math.min(a + 1, pts.length - 1)];
    ctx.fillStyle = rgba(0xFFFFFFFF, .85 * lit); circle(lerp(p.x, q.x, f), lerp(p.y, q.y, f), 2.2); ctx.fill();
  }
}

// ---------- Vẽ thân hành tinh dùng chung ----------
function body(x, y, R, col, lit, spin) {
  const c = shade(col, lit);
  if (lit > .02) { // quầng sáng chỉ xuất hiện khi đã sáng
    const h = ctx.createRadialGradient(x, y, 0, x, y, R * 2.2);
    h.addColorStop(0, rgba(col, .34 * lit)); h.addColorStop(.43, rgba(col, .34 * lit)); h.addColorStop(1, rgba(col, 0));
    ctx.fillStyle = h; circle(x, y, R * 2.2); ctx.fill();
  }
  const g = ctx.createRadialGradient(x - R * .35, y - R * .4, 0, x - R * .35, y - R * .4, R * 1.45);
  g.addColorStop(0, rgba(mix(c, 0xFFFFFFFF, .12 + .38 * lit))); g.addColorStop(.5, rgba(c)); g.addColorStop(1, rgba(mix(c, 0xFF000000, .62)));
  ctx.fillStyle = g; circle(x, y, R); ctx.fill();
}
function rim(x, y, R, lit) { ctx.strokeStyle = `rgba(255,255,255,${.08 + .20 * lit})`; ctx.lineWidth = 1.5; circle(x, y, R); ctx.stroke(); }
function clipBody(x, y, R) { ctx.save(); circle(x, y, R); ctx.clip(); }
/** Nửa vòng của một vành elip (back = nửa sau, front = nửa trước hành tinh). */
function ring(x, y, rx, ry, rot, half, color, w) {
  ctx.save(); ctx.translate(x, y); ctx.rotate(rot); ctx.strokeStyle = color; ctx.lineWidth = w;
  ctx.beginPath(); ctx.ellipse(0, 0, rx, ry, 0, half === 'back' ? Math.PI : 0, half === 'back' ? TAU : Math.PI); ctx.stroke(); ctx.restore();
}
function blob(x, y, rx, ry, rot, color) { ctx.fillStyle = color; ctx.beginPath(); ctx.ellipse(x, y, rx, ry, rot, 0, TAU); ctx.fill(); }

// ---------- Đặc điểm riêng của từng hành tinh (mỗi hàm: vẽ thân + chi tiết) ----------
const SKIN = {
  // 0 Hướng dẫn: hành tinh mầm, hai chiếc lá nhỏ
  sprout(x, y, R, c, lit, t) {
    body(x, y, R, c, lit); clipBody(x, y, R);
    const d = rgba(mix(shade(c, lit), 0xFF000000, .35), .55);
    blob(x - R * .3, y + R * .1, R * .5, R * .3, .5, d); blob(x + R * .35, y - R * .25, R * .32, R * .2, -.4, d);
    ctx.restore(); rim(x, y, R, lit);
  },
  // 1 Làm quen: đại dương, lục địa và mây
  ocean(x, y, R, c, lit, t) {
    body(x, y, R, c, lit); clipBody(x, y, R);
    const land = rgba(shade(0xFF7FE0B0, lit), .85), sh = (t * 6) % (R * 4) - R * 2;
    [[-.4, -.1, .5, .3, .4], [.45, .35, .4, .22, -.3], [.1, -.55, .3, .16, 0]].forEach(([a, b, rx, ry, r]) => blob(x + R * a, y + R * b, R * rx, R * ry, r, land));
    ctx.strokeStyle = `rgba(255,255,255,${.1 + .3 * lit})`; ctx.lineWidth = R * .1;
    for (const k of [-.45, .2, .6]) { ctx.beginPath(); ctx.moveTo(x - R + sh * 0, y + R * k); ctx.quadraticCurveTo(x, y + R * k - R * .18, x + R, y + R * k); ctx.stroke(); }
    ctx.restore(); rim(x, y, R, lit);
  },
  // 2 Kho nhỏ: sọc hàng hoá và vành đai chứa
  cargo(x, y, R, c, lit, t) {
    const rc = rgba(shade(0xFFFFE0A0, lit), .35 + .5 * lit);
    ring(x, y, R * 1.7, R * .46, -.32, 'back', rc, 3);
    body(x, y, R, c, lit); clipBody(x, y, R);
    ctx.fillStyle = rgba(mix(shade(c, lit), 0xFF000000, .45), .5);
    for (const k of [-.55, -.1, .35]) ctx.fillRect(x - R, y + R * k, R * 2, R * .2);
    ctx.restore(); rim(x, y, R, lit);
    ring(x, y, R * 1.7, R * .46, -.32, 'front', rc, 3);
  },
  // 3 Mùa khô: cồn cát và vết nứt
  desert(x, y, R, c, lit, t) {
    body(x, y, R, c, lit); clipBody(x, y, R);
    ctx.strokeStyle = rgba(mix(shade(c, lit), 0xFF000000, .4), .6); ctx.lineWidth = R * .07;
    for (const k of [-.5, -.05, .4]) { ctx.beginPath(); ctx.moveTo(x - R, y + R * k); ctx.bezierCurveTo(x - R * .3, y + R * (k - .25), x + R * .3, y + R * (k + .25), x + R, y + R * k); ctx.stroke(); }
    ctx.lineWidth = 1.4; ctx.beginPath();
    ctx.moveTo(x - R * .1, y - R * .9); ctx.lineTo(x + R * .05, y - R * .4); ctx.lineTo(x - R * .12, y - R * .05); ctx.lineTo(x + R * .1, y + R * .35); ctx.lineTo(x, y + R * .9); ctx.stroke();
    ctx.restore(); rim(x, y, R, lit);
  },
  // 4 Chạy đua: sọc chéo và vệt tốc độ phía sau
  speed(x, y, R, c, lit, t) {
    ctx.lineCap = 'round';
    for (let k = 0; k < 3; k++) {
      const yy = y + (k - 1) * R * .5, ph = (t * 1.6 + k * .33) % 1, len = R * (1.1 + .5 * ph);
      ctx.strokeStyle = rgba(shade(c, lit), (.15 + .45 * lit) * (1 - ph)); ctx.lineWidth = 2.2;
      ctx.beginPath(); ctx.moveTo(x - R * 1.1 - ph * R * .5, yy); ctx.lineTo(x - R * 1.1 - ph * R * .5 - len, yy); ctx.stroke();
    }
    body(x, y, R, c, lit); clipBody(x, y, R);
    ctx.strokeStyle = `rgba(255,255,255,${.1 + .3 * lit})`; ctx.lineWidth = R * .16;
    for (const k of [-.7, -.1, .5]) { ctx.beginPath(); ctx.moveTo(x - R, y + R * (k + .5)); ctx.lineTo(x + R, y + R * (k - .5)); ctx.stroke(); }
    ctx.restore(); rim(x, y, R, lit);
  },
  // 5 Tay ngắn: hành tinh nhỏ bên trong vòng nét đứt (tầm bay ngắn)
  range(x, y, R, c, lit, t) {
    body(x, y, R * .8, c, lit); clipBody(x, y, R * .8);
    ctx.strokeStyle = `rgba(255,255,255,${.08 + .22 * lit})`; ctx.lineWidth = R * .12;
    ctx.beginPath(); ctx.moveTo(x - R, y - R * .1); ctx.quadraticCurveTo(x, y + R * .2, x + R, y - R * .1); ctx.stroke();
    ctx.restore(); rim(x, y, R * .8, lit);
    ctx.setLineDash([5, 6]); ctx.lineDashOffset = -t * 14; ctx.lineWidth = 2; ctx.strokeStyle = rgba(shade(c, lit), .35 + .5 * lit);
    circle(x, y, R * 1.45); ctx.stroke(); ctx.setLineDash([]);
  },
  // 6 Sương mù: thân xám lam bị mây mờ che
  fog(x, y, R, c, lit, t) {
    body(x, y, R, c, lit); clipBody(x, y, R);
    for (let k = 0; k < 4; k++) {
      const dx = Math.sin(t * .5 + k * 1.7) * R * .35;
      blob(x + dx, y + (k - 1.5) * R * .5, R * (.9 - k * .05), R * .17, 0, `rgba(255,255,255,${(.12 + .22 * lit) * (k % 2 ? 1 : .7)})`);
    }
    ctx.restore(); rim(x, y, R, lit);
    const h = ctx.createRadialGradient(x, y, R * .8, x, y, R * 1.6);
    h.addColorStop(0, `rgba(210,225,255,${.18 * (.3 + lit)})`); h.addColorStop(1, 'rgba(210,225,255,0)');
    ctx.fillStyle = h; circle(x, y, R * 1.6); ctx.fill();
  },
  // 7 Bãi thiên thạch: miệng hố và vành đai đá vụn
  belt(x, y, R, c, lit, t) {
    const rock = rgba(shade(0xFFB89A80, lit), .5 + .45 * lit), rnd = mulberry(5);
    const dots = [];
    for (let k = 0; k < 16; k++) dots.push([rnd() * TAU, 1.5 + rnd() * .5, 1 + rnd() * 1.4]);
    const drawDots = back => dots.forEach(([a0, rr, s]) => {
      const a = a0 + t * .35, px = Math.cos(a) * R * rr, py = Math.sin(a) * R * rr * .32;
      if ((py < 0) === back) { ctx.fillStyle = rock; const rx = x + px * Math.cos(-.3) - py * Math.sin(-.3), ry = y + px * Math.sin(-.3) + py * Math.cos(-.3); circle(rx, ry, s); ctx.fill(); }
    });
    drawDots(true);
    body(x, y, R, c, lit); clipBody(x, y, R);
    [[-.3, -.25, .22], [.35, .1, .16], [-.05, .5, .13]].forEach(([a, b, r]) => {
      blob(x + R * a, y + R * b, R * r, R * r, 0, rgba(mix(shade(c, lit), 0xFF000000, .5), .7));
      ctx.strokeStyle = `rgba(255,255,255,${.08 + .15 * lit})`; ctx.lineWidth = 1; circle(x + R * a, y + R * b, R * r); ctx.stroke();
    });
    ctx.restore(); rim(x, y, R, lit); drawDots(false);
  },
  // 8 Nạp đạn: vòng nạp chia đoạn xoay quanh hành tinh
  reload(x, y, R, c, lit, t) {
    body(x, y, R, c, lit); clipBody(x, y, R);
    ctx.strokeStyle = `rgba(255,255,255,${.1 + .25 * lit})`; ctx.lineWidth = R * .1;
    ctx.beginPath(); ctx.moveTo(x - R, y + R * .15); ctx.quadraticCurveTo(x, y + R * .45, x + R, y + R * .15); ctx.stroke();
    ctx.restore(); rim(x, y, R, lit);
    const N = 8, gap = .22, ph = (t * .5) % 1;
    for (let k = 0; k < N; k++) {
      const a0 = -Math.PI / 2 + k * TAU / N + gap / 2, on = ((k / N) + ph) % 1 < .5 ? 1 : .35;
      ctx.strokeStyle = rgba(shade(c, lit), (.3 + .6 * lit) * on); ctx.lineWidth = 3; ctx.lineCap = 'round';
      ctx.beginPath(); ctx.arc(x, y, R * 1.38, a0, a0 + TAU / N - gap); ctx.stroke();
    }
  },
  // 9 Đối thủ tăng tốc: hai chữ V dồn dập và sóng xung toả ra
  surge(x, y, R, c, lit, t) {
    for (let k = 0; k < 2; k++) {
      const ph = (t * .7 + k * .5) % 1;
      ctx.strokeStyle = rgba(shade(c, lit), (1 - ph) * (.1 + .5 * lit)); ctx.lineWidth = 2; circle(x, y, R * (1.05 + ph * .75)); ctx.stroke();
    }
    body(x, y, R, c, lit); clipBody(x, y, R);
    ctx.strokeStyle = `rgba(255,255,255,${.15 + .6 * lit})`; ctx.lineWidth = R * .17; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    for (const dx of [-.28, .18]) { ctx.beginPath(); ctx.moveTo(x + R * (dx - .2), y - R * .42); ctx.lineTo(x + R * (dx + .15), y); ctx.lineTo(x + R * (dx - .2), y + R * .42); ctx.stroke(); }
    ctx.restore(); rim(x, y, R, lit);
  },
  // 10 Viễn chinh: hành tinh nhỏ, xa xa một đốm sáng; đoàn đá đi chậm trên đường chấm dài
  voyage(x, y, R, c, lit, t) {
    const far = R * 2.3, ang = -.62, fx = x + Math.cos(ang) * far, fy = y + Math.sin(ang) * far, ph = (t * .16) % 1;
    ctx.setLineDash([2, 6]); ctx.lineWidth = 1.6; ctx.strokeStyle = rgba(shade(c, lit), .3 + .4 * lit);
    ctx.beginPath(); ctx.moveTo(x + Math.cos(ang) * R * 1.15, y + Math.sin(ang) * R * 1.15); ctx.lineTo(fx - Math.cos(ang) * R * .3, fy - Math.sin(ang) * R * .3); ctx.stroke(); ctx.setLineDash([]);
    ctx.fillStyle = rgba(shade(0xFFFFFFFF, lit), .3 + .6 * lit); circle(x + (fx - x) * (.18 + .64 * ph), y + (fy - y) * (.18 + .64 * ph), 2); ctx.fill();   // đoàn đá chậm
    circle(fx, fy, R * .26); ctx.fillStyle = rgba(shade(c, lit), .45 + .5 * lit); ctx.fill();                                                            // hành tinh đích ở xa
    body(x, y, R * .9, c, lit); clipBody(x, y, R * .9);
    ctx.strokeStyle = `rgba(255,255,255,${.1 + .25 * lit})`; ctx.lineWidth = R * .1;
    ctx.beginPath(); ctx.moveTo(x - R, y + R * .2); ctx.quadraticCurveTo(x, y - R * .1, x + R, y + R * .25); ctx.stroke();
    ctx.restore(); rim(x, y, R * .9, lit);
  },
  // Endless: hố đen, đĩa bồi tụ xoay và vòng bẻ cong ánh sáng
  blackhole(x, y, R, c, lit, t) {
    const core = R * .5, tilt = -.35, rx = R * 1.75, ry = R * .5;
    const diskCol = (u, a) => rgba(mix(mix(0xFFFFB347, 0xFFFFFFFF, u * .6), DARK_BASE, DARK_AMOUNT * (1 - lit)), a);
    if (lit > .02) {
      const h = ctx.createRadialGradient(x, y, core, x, y, R * 2.4);
      h.addColorStop(0, `rgba(255,150,60,${.28 * lit})`); h.addColorStop(1, 'rgba(255,150,60,0)'); ctx.fillStyle = h; circle(x, y, R * 2.4); ctx.fill();
    }
    for (let k = 0; k < 4; k++) ring(x, y, rx - k * R * .1, ry - k * R * .03, tilt, 'back', diskCol(k / 3, (.25 + .5 * lit) * (1 - k * .15)), 3.4 - k * .6);
    ctx.fillStyle = '#000'; circle(x, y, core); ctx.fill();
    ctx.strokeStyle = diskCol(1, .35 + .6 * lit); ctx.lineWidth = 2; circle(x, y, core + 1.5); ctx.stroke();   // vòng photon
    ctx.strokeStyle = diskCol(.5, .18 + .25 * lit); ctx.lineWidth = 1.2; circle(x, y, core * 1.35); ctx.stroke();  // lensing
    for (let k = 0; k < 4; k++) ring(x, y, rx - k * R * .1, ry - k * R * .03, tilt, 'front', diskCol(k / 3, (.3 + .65 * lit) * (1 - k * .15)), 3.4 - k * .6);
    // hạt xoáy quanh đĩa
    for (let k = 0; k < 10; k++) {
      const a = t * (.9 + (k % 3) * .25) + k * .63, px = Math.cos(a) * rx * (.85 + (k % 4) * .05), py = Math.sin(a) * ry * (.85 + (k % 4) * .05);
      ctx.fillStyle = diskCol(1, .3 + .6 * lit); circle(x + px * Math.cos(tilt) - py * Math.sin(tilt), y + px * Math.sin(tilt) + py * Math.cos(tilt), 1.4); ctx.fill();
    }
  },
};

// ---------- Vẽ một nút ----------
function drawNode(n, clock) {
  const L = LEVELS[n.i], lit = state.lit[n.i], R = n.r, pulse = (Math.sin(clock * 3) + 1) / 2;
  const isNext = n.i === nextIndex();
  if (isNext) { // vòng gợi ý màn nên chơi
    ctx.setLineDash([6, 6]); ctx.lineDashOffset = -clock * 18; ctx.lineWidth = 2; ctx.strokeStyle = rgba(C_GOLD, .35 + .5 * pulse);
    circle(n.x, n.y, R * 1.95 + pulse * 3); ctx.stroke(); ctx.setLineDash([]);
  }
  SKIN[L.kind](n.x, n.y, R, L.col, lit, clock);
  if (state.selected === n.i) { ctx.strokeStyle = rgba(C_GOLD, .9); ctx.lineWidth = 2; circle(n.x, n.y, R * 1.6); ctx.stroke(); }
  // nhãn: số màn + tên
  const label = n.i === 0 ? 'Hướng dẫn' : n.i === ENDLESS ? 'Endless' : `Màn ${n.i} · ${L.name}`;
  text(label, n.x, n.y + R * (n.i === ENDLESS ? 1.75 : 1.55) + 14, 12.5, `rgba(233,237,255,${.45 + .55 * lit})`);
  if (state.cleared[n.i] && n.i !== ENDLESS) text('✓', n.x + R * 1.0, n.y - R * 1.0, 14, C_YOU);
  if (n.i === ENDLESS && state.endlessBest) text('Kỷ lục 12', n.x, n.y + R * 1.75 + 30, 11, C_MUTED, 'center', 'normal');
}

/** Màn nên chơi tiếp: màn chiến dịch đầu tiên chưa qua (Endless khi đã qua hết). */
function nextIndex() { for (let i = 0; i < ENDLESS; i++) if (!state.cleared[i]) return i; return ENDLESS; }

// ---------- Vòng lặp ----------
let last = performance.now(), clock = 0;
function frame(now) {
  const dt = Math.min(.05, (now - last) / 1000); last = now;
  if (!state.pause) clock += dt;
  nodes.forEach((n, i) => { // độ sáng bám theo trạng thái (Endless sáng khi có kỷ lục)
    const on = i === ENDLESS ? state.endlessBest : state.cleared[i];
    state.lit[i] = Math.max(0, Math.min(1, state.lit[i] + (on ? 1 : -1) * dt / LIT_SECONDS));
  });
  ctx.clearRect(0, 0, W, MAP_H); ctx.drawImage(bg, 0, 0);
  // đường nối: sáng theo độ sáng của màn đứng trước
  for (let i = 0; i < paths.length; i++) drawPath(i, clock);
  nodes.forEach(n => drawNode(n, clock));
  requestAnimationFrame(frame);
}

// ---------- Tương tác ----------
function showInfo(i) {
  const L = LEVELS[i], info = document.getElementById('info');
  const st = i === ENDLESS ? (state.endlessBest ? '<span class="ok">Kỷ lục 12</span>' : '') : state.cleared[i] ? '<span class="ok">Đã qua</span>' : '';
  const planets = i === ENDLESS ? '4–10 hành tinh' : `${L.planets} hành tinh`;
  info.innerHTML = `<b>${i === 0 ? '' : i === ENDLESS ? '∞ ' : 'Màn ' + i + ' · '}${L.name}</b>${st}<br>${L.limit}<br>${planets}`;
}
cv.addEventListener('click', e => {
  const r = cv.getBoundingClientRect(), px = (e.clientX - r.left) * W / r.width, py = (e.clientY - r.top) * MAP_H / r.height;
  const hit = nodes.find(n => Math.hypot(n.x - px, n.y - py) <= n.r * 1.4);
  state.selected = hit ? hit.i : -1;
  if (hit) showInfo(hit.i);
});
const $ = id => document.getElementById(id);
$('next').onclick = () => { const i = nextIndex(); if (i < ENDLESS) state.cleared[i] = true; };
$('reset').onclick = () => { state.cleared.fill(false); state.endlessBest = false; $('endlessBest').checked = false; };
$('all').onclick = () => { state.cleared.fill(true); state.cleared[ENDLESS] = false; };
$('rewind').onclick = () => { viewport.scrollTop = viewport.scrollHeight; };
$('endlessBest').onchange = e => { state.endlessBest = e.target.checked; };
$('pause').onchange = e => { state.pause = e.target.checked; };

// khởi tạo: cuộn tới màn nên chơi tiếp (bản native: scroll tới nextIndex khi vào màn)
viewport.scrollTop = Math.max(0, nodes[nextIndex()].y - VIEW_H * .6);
requestAnimationFrame(frame);
