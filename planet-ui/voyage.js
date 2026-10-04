// Prototype màn 10 · Viễn chinh. Dùng đúng bộ vẽ của game (copy từ planet.js: drawBody, drawPlanet, drawOrbit,
// StarField, đá có vệt đuôi, vòng đích + nhãn pill). Khác planet.js: nhiều hành tinh, kéo-thả như GestureController.
// Hằng số đặt tên giống bản native (LevelRule: RangeRule + SlowRocksRule + Level.gap). 1 đơn vị CSS = 1 dp.
'use strict';
const TAU = Math.PI * 2, DP = 1, W = 360, H = 640, UNIT = Math.min(W, H * .6);
const BODY_SCALE = .4;                        // MỚI: thu nhỏ hành tinh, quỹ đạo, đá để khoảng cách trông xa hơn
const BASE_R = UNIT * .07 * BODY_SCALE, MAX_LEVEL = 5, CAPTURED_MAX_LEVEL = 4;
const C_GOLD = '#ffd166', C_GOLD_I = 0xFFFFD166, C_YOU = 0xFF4FF0B4, C_DANGER = 0xFFFF6B7D, C_MUTED = 0xFF8F99C8, C_FAR = 0xFFFF9B6B;
const ROCK_R = Math.max(1.3 * DP, UNIT * .0075 * BODY_SCALE), ROCK_SPEED = .55, QUICK_SEND = .5, ARMOR_PER_LEVEL = 10;
// ---- Luật màn Viễn chinh ----
const EDGE_MARGIN_X = 72, EDGE_MARGIN_Y = 100;                          // CLAUDE.md mục 4: lề tâm hành tinh so với viền màn hình (dp)
const ROCK_SPEED_SCALE = .25, PLANET_GAP = .47;                        // hệ số tốc độ đá, khoảng cách tối thiểu (unit); KHÔNG giới hạn tầm bay
// ---- HUD: nút icon vuông (DrawKit.drawButton, style ICON) ----
const HUD_BTN = 44, HUD_MARGIN = 12, HUD_TOP = 9, HUD_GAP = 8, C_PANEL = 0xE00E122A, C_PANEL_PRESSED = 0xF01E2650, C_LINE = 0x40A0AFFF, C_INK = 0xFFE9EDFF;
const BACK_ACTION = 'về Chọn màn';   // chờ user chốt: về Chọn màn, hoặc mở hộp Tạm dừng
const THIN_GUARD = 8;                                                  // dưới ngưỡng này hành tinh của bạn bị cảnh báo "hở sườn"
const AI_GRACE = 16, AI_THINK = [3, 6], AI_MIN_ROCKS = 12, AI_SEND = .6;
// bố cục {x, y, level}: người chơi ở đáy giữa; mọi cặp cách nhau >= PLANET_GAP * UNIT
const LAYOUT = [[180, 540, 1, 30], [72, 410, 1, 22], [288, 380, 1, 24], [72, 240, 1, 20], [288, 210, 1, 26]];
const COLORS = [0xFF4FF0B4, 0xFFFF6B7D, 0xFFFFB347, 0xFFA46BFF, 0xFF5CC8FF,
                0xFFFF8FD8, 0xFFF2E86D, 0xFF9BE564, 0xFFC9B79C, 0xFF6F8BFF];
const OWNER_NAMES = ['0 · Người chơi', '1 · AI đỏ', '2 · AI cam', '3 · AI tím', '4 · AI lam',
                     '5 · AI hồng', '6 · AI vàng', '7 · AI lục', '8 · AI be', '9 · AI xanh'];

// ---- ColorUtil.mix / alpha ----
function mix(c, t, a) {
  const ch = s => [(c >>> s) & 255, (t >>> s) & 255];
  const m = s => { const [x, y] = ch(s); return Math.trunc(x + (y - x) * a); };
  return 0xFF000000 | (m(16) << 16) | (m(8) << 8) | m(0);
}
const LIGHT = COLORS.map(c => mix(c, 0xFFFFFFFF, .5));
const DARK = COLORS.map(c => mix(c, 0xFF000000, .62));
function rgba(c, a = 1) {
  const al = a * ((c >>> 24) / 255);
  return `rgba(${(c >>> 16) & 255},${(c >>> 8) & 255},${c & 255},${al})`;
}
// alpha(): giữ RGB, thay độ trong suốt
const alpha = (c, a) => rgba((c & 0x00FFFFFF) | 0xFF000000, Math.max(0, Math.min(1, a)));

const radius = p => BASE_R * (1 + .08 * (p.level - 1));
const maxLvl = p => p.captured ? CAPTURED_MAX_LEVEL : MAX_LEVEL;
const hp = p => p.rocks + p.armor;
// ---- Nền: tinh vân + sao (StarField.java) ----
function mulberry(a) { return () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
const bg = document.createElement('canvas'); bg.width = W; bg.height = H;
let twinkle = [];
(function buildBg() {
  const g = bg.getContext('2d');
  const lin = g.createLinearGradient(0, 0, 0, H);
  lin.addColorStop(0, '#070a1c'); lin.addColorStop(1, '#04050d');
  g.fillStyle = lin; g.fillRect(0, 0, W, H);
  const neb = [[.2, .25, .55], [.88, .62, .6], [.4, .95, .5]], nc = [[0x5A, 0x3C, 0xC8], [0x28, 0x8C, 0xC8], [0xC8, 0x46, 0x8C]];
  neb.forEach((n, i) => {
    const r = n[2] * Math.max(W, H), gr = g.createRadialGradient(n[0] * W, n[1] * H, 0, n[0] * W, n[1] * H, r);
    gr.addColorStop(0, `rgba(${nc[i]},.15)`); gr.addColorStop(1, `rgba(${nc[i]},0)`);
    g.fillStyle = gr; g.fillRect(0, 0, W, H);
  });
  const rnd = mulberry(11);
  for (let i = 0; i < 170; i++) {
    g.fillStyle = `rgba(255,255,255,${.2 + rnd() * .6})`;
    g.beginPath(); g.arc(rnd() * W, rnd() * H, (rnd() * 1.1 + .2) * DP, 0, TAU); g.fill();
  }
  for (let i = 0; i < 28; i++) twinkle.push([rnd() * W, rnd() * H, rnd() * TAU, .8 + rnd() * 1.4]);
})();

const cv = document.getElementById('stage'), dpr = Math.min(window.devicePixelRatio || 1, 3);
cv.width = W * dpr; cv.height = H * dpr;
const ctx = cv.getContext('2d'); ctx.scale(dpr, dpr);
const ui = id => document.getElementById(id);
function text(s, x, y, size, color, align = 'center') {
  ctx.font = `bold ${size}px Roboto, "Helvetica Neue", Arial, sans-serif`;
  ctx.textAlign = align; ctx.textBaseline = 'alphabetic';
  const m = ctx.measureText(s);
  const asc = m.fontBoundingBoxAscent ?? size * .93, desc = m.fontBoundingBoxDescent ?? size * .25;
  ctx.fillStyle = color;
  ctx.fillText(s, x, y + (asc - desc) / 2);   // giống DrawKit.text: canh giữa theo ascent/descent
}

function arc(x, y, r, frac) {
  ctx.beginPath(); ctx.arc(x, y, r, -Math.PI / 2, -Math.PI / 2 + TAU * frac); ctx.stroke();
}

function drawBody(x, y, R, owner, seed) {
  const col = COLORS[owner];
  // Quầng sáng
  let g = ctx.createRadialGradient(x, y, 0, x, y, R * 2.1);
  g.addColorStop(0, alpha(col, .34)); g.addColorStop(.43, alpha(col, .34)); g.addColorStop(1, alpha(col, 0));
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, R * 2.1, 0, TAU); ctx.fill();
  // Thân
  g = ctx.createRadialGradient(x - R * .35, y - R * .4, 0, x - R * .35, y - R * .4, R * 1.45);
  g.addColorStop(0, rgba(LIGHT[owner])); g.addColorStop(.5, rgba(col)); g.addColorStop(1, rgba(DARK[owner]));
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, R, 0, TAU); ctx.fill();
  // Dải mây
  ctx.save();
  ctx.beginPath(); ctx.arc(x, y, R, 0, TAU); ctx.clip();
  ctx.strokeStyle = 'rgba(0,0,0,' + (0x24 / 255) + ')'; ctx.lineWidth = R * .13;
  for (const k of [-.4, .05, .5]) {
    ctx.beginPath(); ctx.moveTo(x - R, y + R * k);
    ctx.quadraticCurveTo(x, y + R * k + R * .2 * Math.sin(seed + k * 5), x + R, y + R * k);
    ctx.stroke();
  }
  ctx.restore();
  // Viền sáng
  ctx.strokeStyle = 'rgba(255,255,255,' + (0x47 / 255) + ')'; ctx.lineWidth = 1.5 * DP;
  ctx.beginPath(); ctx.arc(x, y, R, 0, TAU); ctx.stroke();
}

function dashed(on, phase) {
  ctx.setLineDash(on ? [7 * DP, 7 * DP] : []);
  ctx.lineDashOffset = on ? -(14 * DP - (phase * 50 * DP) % (14 * DP)) : 0;
}
function circle(x, y, r, stroke) { ctx.beginPath(); ctx.arc(x, y, r, 0, TAU); stroke ? ctx.stroke() : ctx.fill(); }

const ORBIT_MAX_DOTS = 60;
const ORBIT_BAND_RINGS = 4;       // độ dày dải quỹ đạo, tính theo số "gap"
const ORBIT_SPEED_MIN = .5, ORBIT_SPEED_MAX = 1.6;    // rad/s, chiều quay ngẫu nhiên theo viên
const ORBIT_RADIAL_AMP = 1.3;     // biên độ dao động hướng tâm (đơn vị gap)
const ORBIT_RADIAL_FREQ = [.6, 1.9]; // rad/s
const ORBIT_JITTER_AMP = .6, ORBIT_JITTER_FREQ = [.8, 2.4]; // nhiễu góc (rad) và tần số
function orbitHash(i, salt) { const v = Math.sin(i * 127.1 + salt * 311.7) * 43758.5453; return v - Math.floor(v); }
function orbitDotPos(i, seed, clock, R, gap, first) {
  const h = salt => orbitHash(i, salt);
  const w = (h(1) < .5 ? -1 : 1) * (ORBIT_SPEED_MIN + h(2) * (ORBIT_SPEED_MAX - ORBIT_SPEED_MIN));
  const base = R + first + h(3) * ORBIT_BAND_RINGS * gap;
  const rad = base + ORBIT_RADIAL_AMP * gap * Math.sin(clock * (ORBIT_RADIAL_FREQ[0] + h(4) * (ORBIT_RADIAL_FREQ[1] - ORBIT_RADIAL_FREQ[0])) + h(5) * TAU);
  const jit = ORBIT_JITTER_AMP * Math.sin(clock * (ORBIT_JITTER_FREQ[0] + h(6) * (ORBIT_JITTER_FREQ[1] - ORBIT_JITTER_FREQ[0])) + h(7) * TAU);
  const a = seed + h(8) * TAU + clock * w + jit;
  return [Math.cos(a) * rad, Math.sin(a) * rad];
}
function drawOrbit(p, clock, hl) {
  const x = p.x, y = p.y, R = radius(p);
  const gap = Math.max(2.4 * DP, UNIT * .017 * BODY_SCALE), first = Math.max(3.5 * DP, UNIT * .026 * BODY_SCALE);
  const dc = Math.min(p.rocks, ORBIT_MAX_DOTS), col = COLORS[p.owner];
  const dot = Math.max(1.1 * DP, UNIT * .0055 * BODY_SCALE);
  for (let i = 0; i < dc; i++) {
    const [ox, oy] = orbitDotPos(i, p.seed, clock, R, gap, first), on = i < hl;
    ctx.fillStyle = on ? C_GOLD : alpha(col, .92);
    circle(x + ox, y + oy, on ? dot * 1.3 : dot, false);
  }
}

function pill(x, y, s, col) {                    // DrawKit.pill
  ctx.font = `bold ${12.5 * DP}px Roboto, "Helvetica Neue", Arial, sans-serif`;
  const pw = ctx.measureText(s).width + 20 * DP, ph = 26 * DP;
  const px = Math.max(6 * DP, Math.min(x - pw / 2, W - pw - 6 * DP)), py = Math.max(y - ph / 2, 6 * DP);
  ctx.beginPath(); ctx.roundRect(px, py, pw, ph, ph / 2); ctx.fillStyle = rgba(0xE6080A1A); ctx.fill();
  ctx.strokeStyle = rgba(col); ctx.lineWidth = 1.5 * DP; ctx.stroke();
  text(s, px + pw / 2, py + ph / 2, 12.5 * DP, '#fff');
}

// kiểm tra bố cục: lề và khoảng cách tối thiểu
LAYOUT.forEach((a, i) => { console.assert(a[0] >= EDGE_MARGIN_X && a[0] <= W - EDGE_MARGIN_X && a[1] >= EDGE_MARGIN_Y && a[1] <= H - EDGE_MARGIN_Y, 'hành tinh ' + i + ' ngoài lề');
  LAYOUT.forEach((b, j) => { if (j > i) console.assert(Math.hypot(a[0] - b[0], a[1] - b[1]) >= PLANET_GAP * UNIT - 1, 'cặp ' + i + '-' + j + ' quá gần'); }); });

// ---- Nút HUD: Back (trái), Âm thanh (phải, cạnh Tạm dừng), Tạm dừng (phải cùng) ----
const hud = { sound: true, pressed: null };
const BUTTONS = [
  { id: 'back', x: HUD_MARGIN },
  { id: 'sound', x: W - HUD_MARGIN - 2 * HUD_BTN - HUD_GAP },
  { id: 'pause', x: W - HUD_MARGIN - HUD_BTN },
];
function drawHudButton(b) {
  const x0 = b.x, y0 = HUD_TOP, cx = x0 + HUD_BTN / 2, cy = y0 + HUD_BTN / 2, pressed = hud.pressed === b.id;
  ctx.save();
  if (pressed) { ctx.translate(cx, cy); ctx.scale(.96, .96); ctx.translate(-cx, -cy); }
  const on = b.id === 'sound' && hud.sound;
  ctx.fillStyle = rgba(pressed ? C_PANEL_PRESSED : C_PANEL); ctx.beginPath(); ctx.roundRect(x0, y0, HUD_BTN, HUD_BTN, 14); ctx.fill();
  ctx.strokeStyle = on ? alpha(C_YOU, .55) : rgba(C_LINE); ctx.lineWidth = 1.2; ctx.beginPath(); ctx.roundRect(x0, y0, HUD_BTN, HUD_BTN, 14); ctx.stroke();
  const ink = b.id === 'sound' ? (hud.sound ? rgba(C_YOU) : rgba(C_MUTED)) : rgba(C_INK);   // âm thanh bật: xanh ngọc; tắt: xám
  ctx.fillStyle = ink; ctx.strokeStyle = ink; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  if (b.id === 'pause') { ctx.beginPath(); ctx.roundRect(cx - 7, cy - 8, 4.5, 16, 1.5); ctx.roundRect(cx + 2.5, cy - 8, 4.5, 16, 1.5); ctx.fill(); }
  else if (b.id === 'back') { ctx.lineWidth = 2.6; ctx.beginPath(); ctx.moveTo(cx + 4, cy - 8); ctx.lineTo(cx - 4, cy); ctx.lineTo(cx + 4, cy + 8); ctx.stroke(); }
  else {
    ctx.beginPath(); ctx.moveTo(cx - 9, cy - 3.5); ctx.lineTo(cx - 5, cy - 3.5); ctx.lineTo(cx + 1, cy - 8); ctx.lineTo(cx + 1, cy + 8); ctx.lineTo(cx - 5, cy + 3.5); ctx.lineTo(cx - 9, cy + 3.5); ctx.closePath(); ctx.fill();
    ctx.lineWidth = 2;
    if (hud.sound) { for (const r of [5, 9]) { ctx.beginPath(); ctx.arc(cx + 1, cy, r, -.9, .9); ctx.stroke(); } }
    else { ctx.beginPath(); ctx.moveTo(cx + 5, cy - 4); ctx.lineTo(cx + 12, cy + 4); ctx.moveTo(cx + 12, cy - 4); ctx.lineTo(cx + 5, cy + 4); ctx.stroke(); }
  }
  ctx.restore();
}
const hudButtonAt = (x, y) => BUTTONS.find(b => x >= b.x && x <= b.x + HUD_BTN && y >= HUD_TOP && y <= HUD_TOP + HUD_BTN);

// ---- Hành tinh (Planet.java rút gọn) ----
function newPlanet(i) {
  const [x, y, level, rocks] = LAYOUT[i];
  return { x, y, level, rocks, owner: i, armor: 0, captured: false, seed: 1.1 + i * 1.7, flash: 0, think: AI_GRACE + i * 1.3, cooldown: 0, prog: 0 };
}
const rateOf = p => 1 + .5 * (p.level - 1), capOf = p => 40 + 20 * p.level;
const hyp = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
function rr(a, b) { return a + Math.random() * (b - a); }

// ---- Hiệu ứng (Effects.java) ----
let parts = [], texts = [];
function burst(x, y, color, n) { for (let i = 0; i < n && parts.length < 420; i++) { const a = rr(0, TAU), s = rr(20, 90); parts.push({ x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: rr(.25, .55), max: .55, color, size: rr(1, 2.2) }); } }
function popText(x, y, s, color) { texts.push({ x, y, s, color, life: 1.6 }); }
function updateFx(dt) {
  parts = parts.filter(q => (q.life -= dt) > 0); parts.forEach(q => { q.x += q.vx * dt; q.y += q.vy * dt; q.vx *= .94; q.vy *= .94; });
  texts = texts.filter(t => (t.life -= dt) > 0); texts.forEach(t => { t.y -= 22 * dt; });
}
function drawFx() {
  for (const q of parts) { ctx.fillStyle = alpha(q.color, Math.max(0, q.life / q.max)); circle(q.x, q.y, q.size, false); }
  for (const t of texts) { const a = Math.min(1, t.life); text(t.s, t.x + 1, t.y + 1, 14, 'rgba(0,0,0,' + .6 * a + ')'); text(t.s, t.x, t.y, 14, alpha(t.color, a)); }
}

// ---- Mô phỏng tối thiểu của Engine ----
let planets, rocks, time, over, drag = null, hover = -1, clock = 0, last = performance.now();
function reset() { planets = LAYOUT.map((_, i) => newPlanet(i)); rocks = []; parts = []; texts = []; time = 0; over = false; drag = null; ui('end').textContent = ''; }
const speedScale = () => +ui('spd').value / 100;
function launch(src, dst, count) {
  count = Math.min(count, src.rocks); if (count <= 0) return 0;
  src.rocks -= count;
  const aim = Math.atan2(dst.y - src.y, dst.x - src.x), spd = UNIT * ROCK_SPEED * speedScale(), sr = radius(src);
  for (let i = 0; i < count; i++) {
    const a = aim + rr(-.8, .8), s = spd * rr(.9, 1.15);
    rocks.push({ x: src.x + Math.cos(a) * (sr + 8), y: src.y + Math.sin(a) * (sr + 8), vx: Math.cos(a) * s, vy: Math.sin(a) * s, s, owner: src.owner, dst, dist: 0 });
  }
  return count;
}
function takeHit(p) { if (p.armor > 0) { p.armor--; return true; } if (p.rocks > 0) { p.rocks--; return true; } return false; }
function arrive(r) {
  const p = r.dst; r.dead = true;
  if (r.owner === p.owner) { p.rocks++; return; }
  if (takeHit(p)) { burst(r.x, r.y, LIGHT[r.owner], 5); burst(r.x, r.y, LIGHT[p.owner], 3); return; }
  const old = p.owner; p.owner = r.owner; p.rocks = 1; p.level = 1; p.armor = 0; p.captured = true; p.flash = 1;
  popText(p.x, p.y - radius(p) - 14, r.owner === 0 ? 'Chiếm được' : old === 0 ? 'Bị chiếm' : 'Đổi chủ', COLORS[r.owner]);
}
function aiThink(p) {
  const t = planets.filter(q => q.owner !== p.owner).sort((a, b) => hyp(p, a) - hyp(p, b))[0];
  if (t && p.rocks > AI_MIN_ROCKS) launch(p, t, Math.floor(p.rocks * AI_SEND));
}
function step(dt) {
  if (over) return; time += dt;
  for (const p of planets) {
    if (p.rocks < capOf(p)) p.rocks = Math.min(capOf(p), p.rocks + rateOf(p) * dt);
    p.flash = Math.max(0, p.flash - dt * 1.4);
    if (p.owner !== 0 && time > AI_GRACE && (p.think -= dt) < 0) { p.think = rr(AI_THINK[0], AI_THINK[1]); aiThink(p); }
  }
  for (const r of rocks) {
    const dx = r.dst.x - r.x, dy = r.dst.y - r.y, d = Math.hypot(dx, dy) || 1, k = Math.min(1, 5 * dt);
    r.vx += (dx / d * r.s - r.vx) * k; r.vy += (dy / d * r.s - r.vy) * k; r.x += r.vx * dt; r.y += r.vy * dt;
    if (d < radius(r.dst) + 3) arrive(r);
  }
  rocks = rocks.filter(r => !r.dead);
  if (!planets.some(p => p.owner === 0) && !rocks.some(r => r.owner === 0)) { over = true; ui('end').textContent = 'Thua: mất hết hành tinh'; }
  else if (planets.every(p => p.owner === 0)) { over = true; ui('end').textContent = 'Thắng!'; }
  updateFx(dt);
}

// ---- Vẽ ----
function drawPlanet(p) {
  const x = p.x, y = p.y, R = radius(p);
  drawBody(x, y, R, p.owner, p.seed);
  ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  if (p.owner === 0 && Math.floor(p.rocks) < THIN_GUARD) {   // MỚI: cảnh báo hở sườn (vòng đỏ nhịp thở ngoài vòng nâng cấp)
    ctx.lineWidth = 3; ctx.strokeStyle = alpha(C_DANGER, .45 + .45 * Math.abs(Math.sin(clock * 4))); circle(x, y, R + 12, true);
  }
  if (p.flash > 0) { ctx.lineWidth = 3; ctx.strokeStyle = `rgba(255,255,255,${p.flash})`; circle(x, y, R * (1 + (1 - p.flash) * 1.3), true); }
  const n = String(Math.floor(p.rocks)), big = Math.max(11, R * .8);
  text(n, x + 1, y + 1, big, 'rgba(0,0,0,.45)'); text(n, x, y, big, '#fff');   // hành tinh nhỏ: chỉ còn số đá ở giữa
}
function drawRocks() {
  for (const r of rocks) {
    ctx.strokeStyle = alpha(COLORS[r.owner], .5); ctx.lineWidth = ROCK_R * 1.1;
    ctx.beginPath(); ctx.moveTo(r.x - r.vx * .07, r.y - r.vy * .07); ctx.lineTo(r.x, r.y); ctx.stroke();
    ctx.fillStyle = rgba(LIGHT[r.owner]); circle(r.x, r.y, ROCK_R, false);
  }
}
function planetAt(x, y) { return planets.findIndex(p => Math.hypot(p.x - x, p.y - y) < radius(p) + 14); }
function drawDrag() {
  if (!drag) return;
  const s = planets[drag.src], R = radius(s);
  ctx.strokeStyle = rgba(C_YOU); ctx.lineWidth = 2; dashed(true, clock); circle(s.x, s.y, R + 9, true); dashed(false, 0);                              // vòng chọn
  const i = planetAt(drag.x, drag.y), t = i >= 0 && i !== drag.src ? planets[i] : null, cnt = Math.max(1, Math.floor(s.rocks * ui('frac').value / 100));
  let col = C_YOU, lbl = null;
  if (t) {
    const atk = t.owner !== s.owner;
    col = atk ? C_DANGER : C_YOU;
    lbl = atk ? `Tấn công · ${cnt} (máu ${hp(t) | 0})` : `Chuyển quân · ${cnt}`;
    ctx.strokeStyle = rgba(col); ctx.lineWidth = 3; circle(t.x, t.y, radius(t) + 11, true);
    pill(t.x, t.y - radius(t) - 34, lbl, col);
  }
  ctx.strokeStyle = alpha(col, .7); ctx.lineWidth = 2.5; dashed(true, clock);
  ctx.beginPath(); ctx.moveTo(s.x, s.y); ctx.lineTo(t ? t.x : drag.x, t ? t.y : drag.y); ctx.stroke(); dashed(false, 0);
}
function frame(now) {
  const dt = Math.min(.05, (now - last) / 1000); last = now; clock += dt; step(dt);
  ctx.clearRect(0, 0, W, H); ctx.drawImage(bg, 0, 0, W, H);
  for (const t of twinkle) { ctx.fillStyle = `rgba(255,255,255,${.25 + .35 * Math.sin(clock * t[3] + t[2])})`; ctx.fillRect(t[0], t[1], 1.6, 1.6); }
  for (const p of planets) drawPlanet(p);
  for (const p of planets) drawOrbit(Object.assign(p, { rocks: Math.floor(p.rocks) }), clock, drag && planets[drag.src] === p ? Math.round(Math.min(p.rocks * ui('frac').value / 100, p.rocks) / Math.max(1, p.rocks) * Math.min(p.rocks, 60)) : 0);
  drawDrag(); drawRocks(); drawFx();
  BUTTONS.forEach(drawHudButton);
  requestAnimationFrame(frame);
}

// ---- Cử chỉ: kéo từ hành tinh của bạn tới hành tinh khác ----
const pos = e => { const b = cv.getBoundingClientRect(); return [(e.clientX - b.left) * W / b.width, (e.clientY - b.top) * H / b.height]; };
cv.addEventListener('pointerdown', e => {
  const [x, y] = pos(e), hb = hudButtonAt(x, y);
  if (hb) { hud.pressed = hb.id; return; }
  const i = planetAt(x, y); if (i >= 0 && planets[i].owner === 0 && !over) { drag = { src: i, x, y }; cv.setPointerCapture(e.pointerId); } });
cv.addEventListener('pointermove', e => { if (drag) { [drag.x, drag.y] = pos(e); } });
cv.addEventListener('pointerup', e => {
  if (hud.pressed) {
    const [x, y] = pos(e), hb = hudButtonAt(x, y);
    if (hb && hb.id === hud.pressed) {
      if (hb.id === 'sound') hud.sound = !hud.sound;
      else ui('end').textContent = hb.id === 'back' ? '(Back: ' + BACK_ACTION + ')' : '(Tạm dừng: mở hộp thoại Tạm dừng)';
    }
    hud.pressed = null; return;
  }
  if (!drag) return; const [x, y] = pos(e), i = planetAt(x, y), s = planets[drag.src];
  if (i >= 0 && i !== drag.src) launch(s, planets[i], Math.floor(s.rocks * ui('frac').value / 100));
  drag = null;
});
ui('reset').onclick = reset;
reset(); requestAnimationFrame(frame);
