/* QQ农场坐标标定工具 —— 数据与 QQFarmCoordinates.properties 一一对应 */

const STATUS = {
  active: { cls: 'active', color: '#2f855a', label: '在用' },
  ref:    { cls: 'ref',    color: '#2b6cb0', label: '备用' },
  legacy: { cls: 'legacy', color: '#9ca3af', label: '未使用' },
};

/* 与 properties 文件结构一一对应（含注释），导出时按此顺序生成 */
const SECTIONS = [
  {
    comment: '# Store location',
    points: [
      { key: 'qqFarm.store.location', name: '商店入口按钮', status: 'active', x: 230, y: 759,
        desc: '主界面右下方的「商店」按钮。买种流程第一步点它打开种子商店。' },
    ],
  },
  {
    comment: '# Screen projection coordinates (left-top, right-top, left-bottom, right-bottom)',
    points: [
      { key: 'qqFarm.screen.leftTop', name: '投屏画面 · 左上角', status: 'ref', x: 123, y: 31,
        desc: '投屏软件中游戏画面的四个角。<code>CoordinateConfig.getScreenBounds()</code> / <code>toAbsolutePosition()</code> 会读它，但当前主流程没有调用这两个方法。投屏窗口挪动后建议一起更新，示意图的绿框也靠它绘制。' },
      { key: 'qqFarm.screen.rightTop', name: '投屏画面 · 右上角', status: 'ref', x: 644, y: 31,
        desc: '同上，画面右上角。' },
      { key: 'qqFarm.screen.leftBottom', name: '投屏画面 · 左下角', status: 'ref', x: 123, y: 810,
        desc: '同上，画面左下角。' },
      { key: 'qqFarm.screen.rightBottom', name: '投屏画面 · 右下角', status: 'ref', x: 644, y: 810,
        desc: '同上，画面右下角。' },
    ],
  },
  {
    comment: '# Seed grid area coordinates (left-top, right-top, left-bottom, right-bottom)',
    points: [
      { key: 'qqFarm.seedGrid.leftTop', name: '种子网格 · 左上角', status: 'active', x: 193, y: 221,
        desc: '商店弹窗里种子卡片网格的四角（找最贵种子的扫描范围）。经 <code>GameConfig.GRID_LEFT/TOP</code> 生效，改这里即可。' },
      { key: 'qqFarm.seedGrid.rightTop', name: '种子网格 · 右上角', status: 'active', x: 574, y: 222,
        desc: '经 <code>GameConfig.GRID_RIGHT</code> / <code>GRID_TOP</code> 生效。' },
      { key: 'qqFarm.seedGrid.leftBottom', name: '种子网格 · 左下角', status: 'active', x: 191, y: 787,
        desc: '经 <code>GameConfig.GRID_LEFT</code> / <code>GRID_BOTTOM</code> 生效。' },
      { key: 'qqFarm.seedGrid.rightBottom', name: '种子网格 · 右下角', status: 'active', x: 574, y: 788,
        desc: '经 <code>GameConfig.GRID_RIGHT</code> / <code>GRID_BOTTOM</code> 生效。' },
    ],
  },
  {
    comment: '# Store confirm button location',
    points: [
      { key: 'qqFarm.store.confirms.location', name: '购买确认按钮', status: 'legacy', x: 384, y: 542,
        desc: '购买弹窗「确认」按钮的固定坐标。遗留键：现在用模板图「购买种子确认.png」识别，此键不会被读取。' },
    ],
  },
  {
    comment: '# Store close button location',
    points: [
      { key: 'qqFarm.store.close.location', name: '商店关闭按钮', status: 'legacy', x: 580, y: 96,
        desc: '商店右上角关闭按钮。遗留键：现在用模板图「商店关闭.png」识别。' },
    ],
  },
  {
    comment: '# Seed position (when only one seed)',
    points: [
      { key: 'qqFarm.only.seed.position', name: '单颗种子位置', status: 'legacy', x: 798, y: 491,
        desc: '种子栏只有一颗种子时的假定位置。遗留键：播种现在靠「种子角标」模板识别，此键不读。' },
    ],
  },
  {
    comment: '# First field location',
    points: [
      { key: 'qqFarm.first.field.location', name: '第一块田', status: 'active', x: 408, y: 426,
        desc: '<span class="used">两处在用</span>：SowSeedAction 点它弹出种子栏；FieldCheckAction 点它检测小铲子判断是否已播种。' },
    ],
  },
  {
    comment: '# Field check exit location (to exit field status detection)',
    points: [
      { key: 'qqFarm.field.check.exit', name: '田地检查退出点', status: 'active', x: 400, y: 150,
        desc: '<span class="used">在用</span>：FieldCheckAction 查完小铲子后点此处空白，退出田地选中状态。' },
    ],
  },
  {
    comment: '# Second field location',
    points: [
      { key: 'qqFarm.second.field.location', name: '第二块田', status: 'legacy', x: 872, y: 463,
        desc: '遗留键：播种已改为「点击种子一键种植」，这些分田坐标不被读取。' },
    ],
  },
  {
    comment: '# Third field location',
    points: [
      { key: 'qqFarm.third.field.location', name: '第三块田', status: 'legacy', x: 844, y: 478,
        desc: '同上，未使用。' },
    ],
  },
  {
    comment: '# Fourth field location',
    points: [
      { key: 'qqFarm.fourth.field.location', name: '第四块田', status: 'legacy', x: 770, y: 442,
        desc: '同上，未使用。' },
    ],
  },
  {
    comment: '# Fifth field location',
    points: [
      { key: 'qqFarm.fifth.field.location', name: '第五块田', status: 'legacy', x: 741, y: 450,
        desc: '同上，未使用。' },
    ],
  },
  {
    comment: '# Sixth field location',
    points: [
      { key: 'qqFarm.sixth.field.location', name: '第六块田', status: 'legacy', x: 819, y: 490,
        desc: '同上，未使用。' },
    ],
  },
  {
    comment: '# Seed bar area (badge recognition region when sowing)',
    scalars: [
      { key: 'qqFarm.seedBar.left', name: '种子栏 · 左边', value: 130,
        desc: '播种时识别「种子角标」的矩形区域（点田后弹出的横条），经 <code>GameConfig.SEED_BAR_*</code> 生效。' },
      { key: 'qqFarm.seedBar.top', name: '种子栏 · 顶边', value: 467, desc: '同上。' },
      { key: 'qqFarm.seedBar.width', name: '种子栏 · 宽度', value: 508, desc: '同上，由 638-130 换算。' },
      { key: 'qqFarm.seedBar.height', name: '种子栏 · 高度', value: 83, desc: '同上，由 550-467 换算。' },
    ],
  },
  {
    comment: '# Badge to seed center offset',
    scalars: [
      { key: 'qqFarm.badge.offset.x', name: '角标→种子中心 · X偏移', value: 25,
        desc: '角标在种子左上角，找到角标后往右下偏移到种子中心，经 <code>GameConfig.BADGE_TO_SEED_OFFSET_X</code> 生效。' },
      { key: 'qqFarm.badge.offset.y', name: '角标→种子中心 · Y偏移', value: 35, desc: '同上，Y 方向。' },
    ],
  },
  {
    comment: '# Seed grid columns and rows',
    scalars: [
      { key: 'qqFarm.seedGrid.cols', name: '种子网格 · 列数', value: 4,
        desc: '商店里种子卡片的列数，经 <code>GameConfig.GRID_COLS</code> 生效，决定找最贵种子的扫描步进。' },
      { key: 'qqFarm.seedGrid.rows', name: '种子网格 · 行数', value: 6,
        desc: '同上，行数，经 <code>GameConfig.GRID_ROWS</code> 生效。' },
    ],
  },
];

const INITIAL = JSON.parse(JSON.stringify(SECTIONS));

/* ---------------- 坐标清单渲染 ---------------- */

const listEl = document.getElementById('list');

function renderList() {
  listEl.innerHTML = '';
  SECTIONS.forEach(section => {
    const g = document.createElement('div');
    g.className = 'group';
    const title = document.createElement('div');
    title.className = 'group-title';
    title.textContent = section.comment;
    g.appendChild(title);

    (section.points || []).forEach(p => {
      const st = STATUS[p.status];
      const row = document.createElement('div');
      row.className = 'row';
      row.dataset.key = p.key;
      row.innerHTML = `
        <span class="dot" style="background:${st.color}"></span>
        <div class="info">
          <div class="name">${p.name} <span class="chip ${st.cls}" style="font-size:10.5px;padding:1px 7px">${st.label}</span></div>
          <div class="key">${p.key}</div>
        </div>
        <div class="axis"><span class="ax">X</span><input type="number" data-axis="x" value="${p.x}" title="X 坐标"></div>
        <div class="axis"><span class="ax">Y</span><input type="number" data-axis="y" value="${p.y}" title="Y 坐标"></div>
        <div class="desc">${p.desc}</div>`;
      g.appendChild(row);

      row.querySelectorAll('input').forEach(input => {
        input.addEventListener('input', () => {
          const v = parseInt(input.value, 10);
          input.classList.toggle('bad', Number.isNaN(v));
          if (!Number.isNaN(v)) {
            p[input.dataset.axis] = v;
            drawMap();
            updateExport();
          }
        });
      });
      row.addEventListener('mouseenter', () => highlight(p.key, true));
      row.addEventListener('mouseleave', () => highlight(p.key, false));
    });

    (section.scalars || []).forEach(s => {
      const row = document.createElement('div');
      row.className = 'row scalar';
      row.dataset.key = s.key;
      row.innerHTML = `
        <span class="dot" style="background:${STATUS.active.color}"></span>
        <div class="info">
          <div class="name">${s.name} <span class="chip active" style="font-size:10.5px;padding:1px 7px">在用</span></div>
          <div class="key">${s.key}</div>
        </div>
        <div class="axis"><input type="number" data-value value="${s.value}" title="数值"></div>
        <div class="desc">${s.desc}</div>`;
      g.appendChild(row);

      const input = row.querySelector('input');
      input.addEventListener('input', () => {
        const v = parseInt(input.value, 10);
        input.classList.toggle('bad', Number.isNaN(v));
        if (!Number.isNaN(v)) {
          s.value = v;
          if (s.key.startsWith('qqFarm.seedBar')) drawMap();
          updateExport();
        }
      });
    });

    listEl.appendChild(g);
  });
}

function highlight(key, on) {
  const row = document.querySelector(`.row[data-key="${key}"]`);
  const pt = document.querySelector(`circle[data-key="${key}"]`);
  if (row) row.classList.toggle('hl', on);
  if (pt) {
    pt.setAttribute('r', on ? 9 : 5.5);
    pt.setAttribute('stroke-width', on ? 3 : 1.5);
  }
  const lbl = document.getElementById('mapLabel');
  if (on && pt && lbl) {
    const p = findPoint(key);
    lbl.textContent = `${p.name} (${p.x}, ${p.y})`;
    lbl.setAttribute('x', pt.getAttribute('cx'));
    lbl.setAttribute('y', parseFloat(pt.getAttribute('cy')) - 14);
    lbl.style.display = 'block';
  } else if (lbl && !on) {
    lbl.style.display = 'none';
  }
}

function findPoint(key) {
  for (const s of SECTIONS) for (const p of (s.points || [])) if (p.key === key) return p;
  return null;
}

function findScalar(key) {
  for (const s of SECTIONS) for (const v of (s.scalars || [])) if (v.key === key) return v;
  return null;
}

/* ---------------- 示意图渲染 ---------------- */

const svg = document.getElementById('map');
const NS = 'http://www.w3.org/2000/svg';
const PAD = 46;
let scale = 1, originX = 0, originY = 0;

function sx(x) { return originX + x * scale; }
function sy(y) { return originY + y * scale; }

function el(tag, attrs, parent) {
  const e = document.createElementNS(NS, tag);
  for (const k in attrs) e.setAttribute(k, attrs[k]);
  (parent || svg).appendChild(e);
  return e;
}

function drawMap() {
  svg.innerHTML = '';

  const screen = SECTIONS[1].points;
  const scorners = [[screen[0].x, screen[0].y], [screen[1].x, screen[1].y],
                    [screen[2].x, screen[2].y], [screen[3].x, screen[3].y]];

  /* 视野范围 = 所有坐标点 + 投屏四角 + 种子栏矩形 的并集 */
  let x1 = Infinity, y1 = Infinity, x2 = -Infinity, y2 = -Infinity;
  const grow = (x, y) => { x1 = Math.min(x1, x); y1 = Math.min(y1, y); x2 = Math.max(x2, x); y2 = Math.max(y2, y); };
  SECTIONS.forEach(s => (s.points || []).forEach(p => grow(p.x, p.y)));
  scorners.forEach(([x, y]) => grow(x, y));
  const sb = getSeedBar();
  if (sb) { grow(sb.left, sb.top); grow(sb.left + sb.width, sb.top + sb.height); }

  const spanX = Math.max(x2 - x1, 1), spanY = Math.max(y2 - y1, 1);
  scale = Math.min((620 - PAD * 2) / spanX, (860 - PAD * 2) / spanY);
  originX = PAD - x1 * scale;
  originY = PAD - y1 * scale;

  const screenBBox = {
    x1: Math.min(...scorners.map(c => c[0])), x2: Math.max(...scorners.map(c => c[0])),
    y1: Math.min(...scorners.map(c => c[1])), y2: Math.max(...scorners.map(c => c[1])),
  };
  const isOut = p => p.x < screenBBox.x1 || p.x > screenBBox.x2 || p.y < screenBBox.y1 || p.y > screenBBox.y2;

  /* 投屏画面四边形 */
  const quad = [scorners[0], scorners[1], scorners[3], scorners[2]]
    .map(([x, y]) => `${sx(x)},${sy(y)}`).join(' ');
  el('polygon', { points: quad, fill: '#e6f4ec', stroke: '#2f855a', 'stroke-width': 2 });
  el('text', { x: sx(scorners[0][0]) + 6, y: sy(scorners[0][1]) + 18,
    fill: '#2f855a', 'font-size': 13, 'font-weight': 600 }).textContent = '投屏画面';

  /* 商店种子网格（虚线） */
  const grid = SECTIONS[2].points;
  const gquad = [grid[0], grid[1], grid[3], grid[2]]
    .map(p => `${sx(p.x)},${sy(p.y)}`).join(' ');
  el('polygon', { points: gquad, fill: 'rgba(217,119,6,.08)', stroke: '#d97706', 'stroke-width': 1.5, 'stroke-dasharray': '6 4' });
  el('text', { x: sx(grid[0].x) + 6, y: sy(grid[0].y) - 6,
    fill: '#b7791f', 'font-size': 12 }).textContent = '商店种子网格';

  /* 种子栏识别区域 */
  if (sb) {
    const out = sb.left < screenBBox.x1 || sb.left + sb.width > screenBBox.x2 ||
                sb.top < screenBBox.y1 || sb.top + sb.height > screenBBox.y2;
    el('rect', {
      x: sx(sb.left), y: sy(sb.top), width: sb.width * scale, height: sb.height * scale,
      fill: 'rgba(43,108,176,.08)', stroke: out ? '#c53030' : '#2b6cb0',
      'stroke-width': 1.5, 'stroke-dasharray': '5 3',
    });
    el('text', { x: sx(sb.left), y: sy(sb.top) - 6,
      fill: out ? '#c53030' : '#2b6cb0', 'font-size': 12 }).textContent =
      out ? '种子栏识别区域（越出投屏画面！）' : '种子栏识别区域';
  }

  /* 所有坐标点 */
  SECTIONS.forEach(sec => (sec.points || []).forEach(p => {
    const out = isOut(p);
    const c = el('circle', {
      cx: sx(p.x), cy: sy(p.y), r: 5.5,
      fill: out ? '#fff' : STATUS[p.status].color,
      stroke: out ? '#c53030' : '#fff',
      'stroke-width': out ? 3 : 1.5,
      style: 'cursor:pointer',
    });
    c.dataset.key = p.key;
    c.addEventListener('mouseenter', () => highlight(p.key, true));
    c.addEventListener('mouseleave', () => highlight(p.key, false));
  }));

  el('text', {
    id: 'mapLabel', 'text-anchor': 'middle', 'font-size': 13, 'font-weight': 600,
    fill: '#1f2937', style: 'display:none;paint-order:stroke;stroke:#fff;stroke-width:3px',
  });
}

function getSeedBar() {
  const l = findScalar('qqFarm.seedBar.left');
  const t = findScalar('qqFarm.seedBar.top');
  const w = findScalar('qqFarm.seedBar.width');
  const h = findScalar('qqFarm.seedBar.height');
  if (!l || !t || !w || !h) return null;
  return { left: l.value, top: t.value, width: w.value, height: h.value };
}

/* ---------------- 导出 ---------------- */

const exportBox = document.getElementById('exportBox');

function buildProperties() {
  const lines = [];
  SECTIONS.forEach(sec => {
    lines.push(sec.comment);
    (sec.points || []).forEach(p => {
      lines.push(`${p.key}.x = ${p.x}`);
      lines.push(`${p.key}.y = ${p.y}`);
    });
    (sec.scalars || []).forEach(s => {
      lines.push(`${s.key} = ${s.value}`);
    });
    lines.push('');
  });
  return lines.join('\n');
}

function updateExport() { exportBox.value = buildProperties(); }

function toast(msg) {
  let t = document.querySelector('.toast');
  if (!t) {
    t = document.createElement('div');
    t.className = 'toast';
    document.body.appendChild(t);
  }
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove('show'), 2200);
}

document.getElementById('btnExport').addEventListener('click', async () => {
  updateExport();
  const text = exportBox.value;
  try {
    await navigator.clipboard.writeText(text);
    toast('已生成并复制到剪贴板 ✓ 覆盖粘贴到 properties 文件即可');
  } catch (e) {
    exportBox.focus();
    exportBox.select();
    document.execCommand('copy');
    toast('已生成并复制 ✓（如未复制成功请手动全选复制）');
  }
});

document.getElementById('btnReset').addEventListener('click', () => {
  SECTIONS.splice(0, SECTIONS.length, ...JSON.parse(JSON.stringify(INITIAL)));
  renderList();
  drawMap();
  updateExport();
  toast('已恢复初始值');
});

/* ---------------- 启动 ---------------- */

renderList();
drawMap();
updateExport();
