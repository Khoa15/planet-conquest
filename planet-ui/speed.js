// Prototype nút tốc độ. Ánh xạ native: PlayScreen.layout()/drawHud, Engine.speed (nhân dt trong PlayScreen.update).
const DP = 1;                                 // 1 đơn vị = 1dp (canvas 360x640 = màn 360dp)
const SPEEDS = [1, 1.25, 1.5, 2];             // GameSpeed.STEPS
const PAUSE_BTN = {l: 360 - 56, t: 9, r: 360 - 12, b: 53};   // đúng PlayScreen
const SPEED_BTN_W = 52, SPEED_BTN_GAP = 8;    // nút tốc độ: rộng 52dp, cách nút Tạm dừng 8dp, cao bằng nút Tạm dừng
const SPEED_BTN = {l: PAUSE_BTN.l - SPEED_BTN_GAP - SPEED_BTN_W, t: PAUSE_BTN.t, r: PAUSE_BTN.l - SPEED_BTN_GAP, b: PAUSE_BTN.b};
const COLOR_IDLE = '#dfe6ff', COLOR_FAST = '#ffd45e';   // x1: Palette.C_INK, nhanh hơn: Palette.C_GOLD
const LINE = 'rgba(160,175,255,.25)';
const POP_MS = 160;                           // nhịp phồng khi đổi tốc độ

const cv = document.getElementById('stage'), g = cv.getContext('2d');
let idx = 0, popAt = -1e9, rocks = [];
for (let i = 0; i < 18; i++) rocks.push({a: Math.random() * 6.28, r: 30 + (i % 3) * 14, w: .8 + Math.random() * .6});

function label(s) { return 'x' + (s === 1 ? '1' : String(s)); }

function drawPanel(l, t, r, b, rad, stroke) {
  g.beginPath(); g.roundRect(l, t, r - l, b - t, rad);
  g.fillStyle = 'rgba(14,20,52,.78)'; g.fill(); g.strokeStyle = stroke; g.lineWidth = 1; g.stroke();
}
function drawPauseIcon() {
  const b = PAUSE_BTN; drawPanel(b.l, b.t, b.r, b.b, 14, LINE);
  g.fillStyle = COLOR_IDLE;
  const cx = (b.l + b.r) / 2, cy = (b.t + b.b) / 2;
  g.fillRect(cx - 7, cy - 8, 4.5, 16); g.fillRect(cx + 2.5, cy - 8, 4.5, 16);
}
function drawSpeedButton(now) {
  const b = SPEED_BTN, fast = SPEEDS[idx] > 1;
  const k = Math.max(0, 1 - (now - popAt) / POP_MS), s = 1 + .12 * k;
  const cx = (b.l + b.r) / 2, cy = (b.t + b.b) / 2;
  g.save(); g.translate(cx, cy); g.scale(s, s); g.translate(-cx, -cy);
  drawPanel(b.l, b.t, b.r, b.b, 14, fast ? 'rgba(255,212,94,.55)' : LINE);
  g.fillStyle = fast ? COLOR_FAST : COLOR_IDLE; g.font = 'bold 16px sans-serif';
  g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(label(SPEEDS[idx]), cx, cy + 1);
  g.restore();
}
function drawTimeChip() {
  if (!document.getElementById('timeChip').checked) return;
  drawPanel(12, 12, 150, 50, 12, LINE);
  g.fillStyle = '#9aa6d6'; g.font = '11.5px sans-serif'; g.textAlign = 'left'; g.textBaseline = 'middle'; g.fillText('Còn lại', 22, 32);
  g.fillStyle = COLOR_IDLE; g.font = 'bold 16.5px sans-serif'; g.fillText(Math.max(0, Math.ceil(TIME_LIMIT - gameT)) + ' s', 78, 31);
}

// Mô phỏng thu nhỏ: MỌI thứ chạy theo dt đã nhân tốc độ (quỹ đạo, sinh đá, đá bay, AI, đồng hồ màn).
const PRODUCE_PER_SEC = 1.5, FLY_SPEED = 120, AI_ATTACK_EVERY = 4, TIME_LIMIT = 90;
const planets = [
  {x: 90, y: 200, r: 24, col: '#2f6bff', n: 12, acc: 0},
  {x: 270, y: 330, r: 24, col: '#ff4d5e', n: 12, acc: 0},
  {x: 120, y: 500, r: 24, col: '#3ddc97', n: 12, acc: 0},
];
let flying = [], aiT = 0, gameT = 0;
function sim(dt) {
  gameT += dt; aiT += dt;
  for (const k of rocks) k.a += dt * k.w;
  for (const p of planets) { p.acc += PRODUCE_PER_SEC * dt; while (p.acc >= 1) { p.acc -= 1; p.n++; } }
  if (aiT >= AI_ATTACK_EVERY) {                       // AI bắn một đợt đá sang hành tinh khác
    aiT = 0;
    const from = planets[1 + Math.floor(Math.random() * 2)], to = planets[0];
    const cnt = Math.min(6, from.n - 1);
    for (let i = 0; i < cnt; i++) { from.n--; flying.push({x: from.x, y: from.y, to, d: i * -10}); }
  }
  for (const f of flying) {
    if (f.d < 0) { f.d += FLY_SPEED * dt; continue; }
    const dx = f.to.x - f.x, dy = f.to.y - f.y, L = Math.hypot(dx, dy), st = FLY_SPEED * dt;
    if (L <= st) { f.done = true; f.to.n++; } else { f.x += dx / L * st; f.y += dy / L * st; }
  }
  flying = flying.filter(f => !f.done);
}
let last = performance.now();
function frame(now) {
  const dt = Math.min(.05, (now - last) / 1000) * SPEEDS[idx]; last = now; sim(dt);   // dt nhân tốc độ
  g.fillStyle = '#070b22'; g.fillRect(0, 0, 360, 640);
  for (const p of planets) {
    g.fillStyle = p.col; g.beginPath(); g.arc(p.x, p.y, p.r, 0, 6.28); g.fill();
    g.fillStyle = 'rgba(223,230,255,.9)';
    for (const k of rocks) { g.beginPath(); g.arc(p.x + Math.cos(k.a) * (k.r + 6), p.y + Math.sin(k.a) * (k.r + 6), 1.8, 0, 6.28); g.fill(); }
    g.fillStyle = '#fff'; g.font = 'bold 13px sans-serif'; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(p.n, p.x, p.y);
  }
  g.fillStyle = '#cfe0ff';
  for (const f of flying) if (f.d >= 0) { g.beginPath(); g.arc(f.x, f.y, 2.5, 0, 6.28); g.fill(); }
  drawTimeChip(); drawSpeedButton(now); drawPauseIcon();
  document.getElementById('info').textContent = 'Tốc độ: ' + label(SPEEDS[idx]) + ' (dt × ' + SPEEDS[idx] + ') | thời gian game: ' + gameT.toFixed(1) + ' s';
  requestAnimationFrame(frame);
}
cv.addEventListener('pointerdown', e => {
  const r = cv.getBoundingClientRect(), x = e.clientX - r.left, y = e.clientY - r.top, b = SPEED_BTN;
  if (x >= b.l && x <= b.r && y >= b.t && y <= b.b) { idx = (idx + 1) % SPEEDS.length; popAt = performance.now(); }
});
requestAnimationFrame(frame);
