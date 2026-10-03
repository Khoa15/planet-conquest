// Dựng lại hành tinh y như WorldRenderer.drawPlanet / drawOrbit và Engine.orbitDots / orbitRings (bản Android).
// Mọi hằng số (màu, tỉ lệ, độ trong suốt) lấy từ mã nguồn Java; 1 đơn vị CSS = 1 dp.
'use strict';

const TAU = Math.PI * 2;
const DP = 1;
const W = 360, H = 420;
const UNIT = 360;                       // Engine: unit = min(W, H * 0.6) trên màn hình phổ biến 360dp
const BASE_R = UNIT * 0.07;             // MapGenerator: kích cỡ gốc .07 (.062 / .056 khi bản đồ đông)
const MAX_LEVEL = 5, CAPTURED_MAX_LEVEL = 4;
const C_GOLD = '#ffd166', C_GOLD_I = 0xFFFFD166, C_YOU = 0xFF4FF0B4, C_DANGER = 0xFFFF6B7D, C_MUTED = 0xFF8F99C8, C_FAR = 0xFFFF9B6B;
const ROCK_R = Math.max(2.4 * DP, UNIT * 0.0075), COOLDOWN_SECONDS = 5, REVEAL_SECONDS = 3;
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

// ---- Trạng thái hành tinh (Planet.java) ----
const planet = { owner: 0, rocks: 23, armor: 10, level: 2, upgradeProgress: 9, captured: false, seed: 1.1, flash: 0, cooldown: 0, reveal: 0 };
const radius = p => BASE_R * (1 + 0.08 * (p.level - 1));
const maxLvl = p => p.captured ? CAPTURED_MAX_LEVEL : MAX_LEVEL;
const hp = p => p.rocks + p.armor;
const upgradeCost = lv => 10 + 8 * lv;

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

// ---- Vẽ ----
const cv = document.getElementById('stage');
const dpr = Math.min(window.devicePixelRatio || 1, 3);
cv.width = W * dpr; cv.height = H * dpr;
const ctx = cv.getContext('2d');
ctx.scale(dpr, dpr);

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

function drawPlanet(p, fogged, clock) {
  const x = W / 2, y = H / 2, R = radius(p);
  drawBody(x, y, R, p.owner, p.seed);
  ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  if (p.level < maxLvl(p) && !fogged) {
    ctx.lineWidth = 3 * DP;
    ctx.strokeStyle = alpha(C_GOLD_I, .16); circle(x, y, R + 4 * DP, true);
    if (p.upgradeProgress > 0) { ctx.strokeStyle = C_GOLD; arc(x, y, R + 4 * DP, p.upgradeProgress / upgradeCost(p.level)); }
  }
  if (p.cooldown > 0) {   // vòng nạp đạn xanh nhạt
    ctx.lineWidth = 2.5 * DP; ctx.strokeStyle = rgba(0xD98FC8FF);
    arc(x, y, R + 8 * DP, p.cooldown / COOLDOWN_SECONDS);
  }
  if (p.flash > 0) {
    ctx.lineWidth = 3 * DP; ctx.strokeStyle = `rgba(255,255,255,${p.flash})`;
    circle(x, y, R * (1 + (1 - p.flash) * 1.3), true);
  }
  const n = fogged ? '?' : String(p.rocks);
  const big = Math.max(13 * DP, R * .6), small = Math.max(8 * DP, R * .27);
  text(n, x + DP, y - R * .24 + DP, big, 'rgba(0,0,0,' + (0x73 / 255) + ')');
  text(n, x, y - R * .24, big, '#fff');
  text(fogged ? 'Cấp ?' : 'Cấp ' + p.level + '/' + maxLvl(p), x, y + R * .28, small, 'rgba(255,255,255,' + (0xD9 / 255) + ')');
  text('Máu ' + (fogged ? '?' : hp(p)), x, y + R * .6, small, 'rgba(255,255,255,' + (0xB3 / 255) + ')');
}

// Engine.orbitDots / orbitRings: tối đa 60 viên, vòng m = 10 + 6*ring, vòng lẻ quay ngược.
// hl = số viên đầu tiên được tô vàng và phóng to 1.3× (đang được chọn)
function drawOrbit(p, clock, hl) {
  const x = W / 2, y = H / 2, R = radius(p);
  const gap = Math.max(6 * DP, UNIT * .017), first = Math.max(9 * DP, UNIT * .026);
  const dc = Math.min(p.rocks, 60), col = COLORS[p.owner];
  const dot = Math.max(1.8 * DP, UNIT * .0055);
  ctx.lineWidth = DP;
  let idx = 0, ring = 0;
  const dots = [];
  while (idx < dc) {
    const m = Math.min(10 + ring * 6, dc - idx);
    const rad = R + first + ring * gap, sp = (ring % 2 === 1 ? -1 : 1) * (.9 / (1 + ring * .45));
    ctx.strokeStyle = alpha(col, .12); circle(x, y, rad, true);
    for (let k = 0; k < m; k++) {
      const a = p.seed + clock * sp + k * TAU / m;
      dots.push([x + Math.cos(a) * rad, y + Math.sin(a) * rad]);
    }
    idx += m; ring++;
  }
  dots.forEach(([dx, dy], i) => {
    const on = i < hl;
    ctx.fillStyle = on ? C_GOLD : alpha(col, .92);
    circle(dx, dy, on ? dot * 1.3 : dot, false);
  });
}

// ---- Hiệu ứng (Effects.java) ----
const parts = [], texts = [], flying = [], asteroids = [];
function rr(a, b) { return a + Math.random() * (b - a); }
function burst(x, y, color, n) {
  if (parts.length > 420) return;
  for (let i = 0; i < n; i++) {
    const a = rr(0, TAU), s = rr(20, 90) * DP;
    parts.push({ x, y, vx: Math.cos(a) * s, vy: Math.sin(a) * s, life: rr(.25, .55), max: .55, color, size: rr(1, 2.2) * DP });
  }
}
function popText(x, y, s, color) { texts.push({ x, y, s, color, life: 1.6 }); }
function fmt1(v) { const r = Math.round(v * 10) / 10; return Number.isInteger(r) ? String(r) : String(r); }
const rateOf = p => 1 + .5 * (p.level - 1);
const capOf = p => 40 + 20 * p.level;

function updateFx(dt) {
  for (let i = parts.length - 1; i >= 0; i--) {
    const q = parts[i]; q.life -= dt;
    if (q.life <= 0) { parts.splice(i, 1); continue; }
    q.x += q.vx * dt; q.y += q.vy * dt; q.vx *= .94; q.vy *= .94;
  }
  for (let i = texts.length - 1; i >= 0; i--) {
    const t = texts[i]; t.life -= dt;
    if (t.life <= 0) { texts.splice(i, 1); continue; }
    t.y -= 22 * DP * dt;
  }
}

// ---- Logic tối thiểu của Engine: viên đá bay tới hành tinh ----
function sendRock(kind) {                        // kind: 'attack' | 'feed' | 'upgrade'
  const p = planet, R = radius(p);
  const owner = kind === 'attack' ? (p.owner === 0 ? 1 : 0) : p.owner;
  const a = rr(0, TAU), spd = UNIT * .55 * rr(.9, 1.15);
  const x = W / 2 + Math.cos(a) * (R + 90), y = H / 2 + Math.sin(a) * (R + 90);
  const feed = kind !== 'attack';
  const k = feed ? .55 : 1;
  flying.push({ x, y, vx: -Math.cos(a) * spd * k, vy: -Math.sin(a) * spd * k, s: spd, owner, feed, kind });
}
function addUpg(p) {
  if (p.level >= maxLvl(p)) { p.rocks++; return; }
  if (++p.upgradeProgress >= upgradeCost(p.level)) {
    p.upgradeProgress = 0; p.level++; p.armor += 10; p.flash = .8;
    if (p.owner === 0) popText(W / 2, H / 2 - radius(p) - 14 * DP,
      `Cấp ${p.level}: +${fmt1(rateOf(p))}/s, chứa ${capOf(p)}, +10 máu`, C_GOLD);
  }
}
function takeHit(p) { if (p.armor > 0) { p.armor--; return true; } if (p.rocks > 0) { p.rocks--; return true; } return false; }
function arrive(r) {
  const p = planet;
  if (r.owner === p.owner) { if (r.feed && r.kind === 'upgrade') addUpg(p); else p.rocks++; return; }
  if (fogOn() && r.owner === 0) p.reveal = REVEAL_SECONDS;     // FogRule.onHit
  if (takeHit(p)) { burst(r.x, r.y, LIGHT[r.owner], 5); burst(r.x, r.y, LIGHT[p.owner], 3); return; }
  const old = p.owner;                                         // Engine.capture
  p.owner = r.owner; p.upgradeProgress = 0; p.armor = 0; p.level = 1; p.captured = true; p.rocks = 1; p.flash = 1;
  if (old === 0 || p.owner === 0) popText(W / 2, H / 2 - radius(p) - 14 * DP, p.owner === 0 ? 'Chiếm được: về cấp 1' : 'Bị chiếm', COLORS[p.owner]);
  ui('owner').value = p.owner; ui('captured').checked = true; ui('fog').dispatchEvent(new Event('input'));
}
function stepWorld(dt) {
  const p = planet;
  p.cooldown = Math.max(0, p.cooldown - dt);
  p.reveal = Math.max(0, p.reveal - dt);
  if (ui('auto').checked) {                                    // Planet.produce
    if (p.rocks < capOf(p)) { p.acc = (p.acc || 0) + rateOf(p) * dt; while (p.acc >= 1) { p.acc--; p.rocks++; } } else p.acc = 0;
  }
  for (let i = flying.length - 1; i >= 0; i--) {
    const r = flying[i], dx = W / 2 - r.x, dy = H / 2 - r.y, d = Math.hypot(dx, dy) || 1;
    const k = Math.min(1, (r.feed ? 2.4 : 5) * dt);
    r.vx += (dx / d * r.s - r.vx) * k; r.vy += (dy / d * r.s - r.vy) * k;
    r.x += r.vx * dt; r.y += r.vy * dt;
    if (d < radius(p) + 3 * DP) { flying.splice(i, 1); arrive(r); }
  }
  for (let i = asteroids.length - 1; i >= 0; i--) {
    const a = asteroids[i]; a.x += a.vx * dt; a.y += a.vy * dt; a.rot += a.vr * dt;
    if (Math.hypot(a.x - W / 2, a.y - H / 2) < radius(p) + a.rad * .6) {   // asteroidHit
      asteroids.splice(i, 1);
      const dmg = Math.max(2, Math.round((a.rad / UNIT) ** 2 * 11000));
      for (let l = dmg; l > 0 && takeHit(p); l--);
      burst(a.x, a.y, 0xFFFFB36B, 8 + Math.round(a.rad / DP)); p.flash = .5;
      popText(W / 2, H / 2 - radius(p) - 10 * DP, '-' + dmg, 0xFFFF9B6B);
    }
  }
}
function sendAsteroid() {
  const rad = UNIT * rr(.026, .04), a = rr(0, TAU), sp = UNIT * rr(.1, .16);
  asteroids.push({ x: W / 2 + Math.cos(a) * 190, y: H / 2 + Math.sin(a) * 190, vx: -Math.cos(a) * sp, vy: -Math.sin(a) * sp,
    rad, rot: rr(0, TAU), vr: rr(-.8, .8), verts: Array.from({ length: 8 }, () => rr(.72, 1.15)) });
}

function drawRocks() {
  for (const r of flying) {
    ctx.strokeStyle = alpha(COLORS[r.owner], .5); ctx.lineWidth = ROCK_R * 1.1;
    ctx.beginPath(); ctx.moveTo(r.x - r.vx * .07, r.y - r.vy * .07); ctx.lineTo(r.x, r.y); ctx.stroke();
    ctx.fillStyle = rgba(LIGHT[r.owner]); circle(r.x, r.y, ROCK_R, false);
  }
}
function drawAsteroids() {
  for (const a of asteroids) {
    ctx.beginPath();
    for (let i = 0; i < 8; i++) {
      const ang = a.rot + i / 8 * TAU, r = a.rad * a.verts[i], px = a.x + Math.cos(ang) * r, py = a.y + Math.sin(ang) * r;
      i ? ctx.lineTo(px, py) : ctx.moveTo(px, py);
    }
    ctx.closePath();
    ctx.fillStyle = rgba(0xFF7D6A5C); ctx.fill();
    ctx.strokeStyle = rgba(0xBFFFAA6E); ctx.lineWidth = 1.6 * DP; ctx.stroke();
    ctx.fillStyle = 'rgba(0,0,0,' + (0x38 / 255) + ')';
    circle(a.x + Math.cos(a.rot + .3) * a.rad * .3, a.y + Math.sin(a.rot + .3) * a.rad * .3, a.rad * .28, false);
  }
}
function drawFx() {
  for (const q of parts) { ctx.fillStyle = alpha(q.color, Math.max(0, q.life / q.max)); circle(q.x, q.y, q.size, false); }
  for (const t of texts) {
    const a = Math.min(1, t.life);
    text(t.s, t.x + DP, t.y + DP, 14 * DP, 'rgba(0,0,0,' + .6 * a + ')');
    text(t.s, t.x, t.y, 14 * DP, alpha(t.color, a));
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
// Vòng báo hiệu của WorldRenderer: tầm bay, vòng chọn, vòng đích, gợi ý đầu ván
function drawMarks(p, clock) {
  const x = W / 2, y = H / 2, R = radius(p);
  if (ui('range').checked) {
    ctx.setLineDash([3 * DP, 8 * DP]); ctx.lineWidth = 1.5 * DP; ctx.strokeStyle = alpha(C_MUTED, .16);
    circle(x, y, UNIT * .55 * 1.0, true); ctx.setLineDash([]);
  }
  if (ui('hint').checked) {
    ctx.strokeStyle = alpha(C_YOU, .8); ctx.lineWidth = 2 * DP; dashed(true, clock * .3);
    circle(x, y, R + (14 + 3 * Math.sin(clock * 4)) * DP, true); dashed(false, 0);
  }
  if (ui('drag').checked) {
    ctx.strokeStyle = rgba(C_YOU); ctx.lineWidth = 2 * DP; dashed(true, clock);
    circle(x, y, R + 9 * DP, true); dashed(false, 0);
  }
  const tg = ui('target').value;
  if (tg !== 'none') {
    const col = tg === 'attack' ? C_DANGER : tg === 'far' ? C_FAR : C_YOU;
    ctx.strokeStyle = rgba(col); ctx.lineWidth = 3 * DP; circle(x, y, R + 11 * DP, true);
    const lbl = tg === 'attack' ? `Tấn công · 12 (máu ${hp(p)})` : tg === 'far' ? 'Ngoài tầm bay' : tg === 'cancel' ? 'Thả để hủy' : 'Chuyển quân · 12';
    pill(x, y - R - 34 * DP, lbl, col);
  }
}

// ---- Vòng lặp ----
let clock = 0, last = performance.now();
const ui = id => document.getElementById(id);
const fogOn = () => ui('fog').checked;
function frame(now) {
  const dt = Math.min(.05, (now - last) / 1000); last = now;
  if (!ui('pause').checked) { clock += dt; stepWorld(dt); updateFx(dt); }
  planet.flash = Math.max(0, planet.flash - dt * 1.4);   // PlanetVisual.fade
  ctx.clearRect(0, 0, W, H);
  ctx.drawImage(bg, 0, 0, W, H);
  for (const t of twinkle) {
    ctx.fillStyle = `rgba(255,255,255,${.25 + .35 * Math.sin(clock * t[3] + t[2])})`;
    ctx.fillRect(t[0], t[1], 1.6 * DP, 1.6 * DP);
  }
  const fogged = fogOn() && planet.owner !== 0 && planet.reveal <= 0;
  const hl = ui('sel').checked && planet.rocks > 0
    ? Math.round(Math.min(+ui('selN').value, planet.rocks) / planet.rocks * Math.min(planet.rocks, 60)) : 0;
  drawPlanet(planet, fogged, clock);       // thân hành tinh vẽ trước, quỹ đạo sau
  if (!fogged) drawOrbit(planet, clock, hl);
  drawMarks(planet, clock);
  drawAsteroids();
  drawRocks();
  drawFx();
  if (!ui('rocks').matches(':active') && document.activeElement !== ui('rocks')) ui('rocks').value = Math.min(planet.rocks, 100);
  ui('rocksV').textContent = planet.rocks;
  ui('armorV').textContent = planet.armor;
  ui('levelV').textContent = planet.level + '/' + maxLvl(planet);
  ui('progV').textContent = planet.upgradeProgress + '/' + upgradeCost(planet.level);
  requestAnimationFrame(frame);
}

// ---- Điều khiển ----
OWNER_NAMES.forEach((n, i) => ui('owner').add(new Option(n, i)));
function sync() {
  planet.owner = +ui('owner').value;
  planet.rocks = +ui('rocks').value;
  planet.armor = +ui('armor').value;
  planet.captured = ui('captured').checked;
  planet.level = Math.min(+ui('level').value, maxLvl(planet));
  ui('level').max = maxLvl(planet);
  ui('level').value = planet.level;
  ui('prog').max = upgradeCost(planet.level) - 1;
  planet.upgradeProgress = Math.min(+ui('prog').value, upgradeCost(planet.level) - 1);
  ui('prog').value = planet.upgradeProgress;
  ui('armor').value = planet.armor;
}
document.querySelectorAll('.panel input, .panel select').forEach(e => e.addEventListener('input', sync));
ui('zoom').addEventListener('input', e => cv.classList.toggle('zoom', e.target.checked));
ui('flash').addEventListener('click', () => { planet.flash = 1; });
ui('flashUp').addEventListener('click', () => { planet.flash = .8; });
ui('flashAst').addEventListener('click', () => { planet.flash = .5; });
ui('btnAttack').addEventListener('click', () => { for (let i = 0; i < 5; i++) setTimeout(() => sendRock('attack'), i * 120); });
ui('btnFeed').addEventListener('click', () => { for (let i = 0; i < 5; i++) setTimeout(() => sendRock('feed'), i * 120); });
ui('btnUpg').addEventListener('click', () => { for (let i = 0; i < 8; i++) setTimeout(() => sendRock('upgrade'), i * 100); });
ui('btnAst').addEventListener('click', sendAsteroid);
ui('btnCd').addEventListener('click', () => { planet.cooldown = COOLDOWN_SECONDS; });
sync();
requestAnimationFrame(frame);
