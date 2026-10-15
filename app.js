'use strict';

// ---------- Хранилище ----------
const KEY = 'wd.workouts.v1';
const DRAFT_KEY = 'wd.draft.v1';

const load = (k, fallback) => {
  try { return JSON.parse(localStorage.getItem(k)) ?? fallback; } catch { return fallback; }
};
const save = (k, v) => localStorage.setItem(k, JSON.stringify(v));

let workouts = load(KEY, []);
let draft = load(DRAFT_KEY, null) || newDraft();
let metric = 'e1rm';

function newDraft() {
  return { date: today(), note: '', exercises: [] };
}
function today() {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 10);
}

// ---------- Утилиты ----------
const $ = (s) => document.querySelector(s);
const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const num = (v) => { const n = parseFloat(String(v).replace(',', '.')); return isFinite(n) ? n : 0; };
const fmt = (n) => (Math.round(n * 10) / 10).toString();
const e1rm = (w, r) => (r <= 1 ? w : w * (1 + r / 30)); // формула Эпли
const fmtDate = (iso) => new Date(iso + 'T00:00').toLocaleDateString('ru-RU', { weekday: 'short', day: 'numeric', month: 'short' });

function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toast.t);
  toast.t = setTimeout(() => t.classList.remove('show'), 2000);
}

const sortedWorkouts = () => [...workouts].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id);
const exerciseNames = () => [...new Set(sortedWorkouts().flatMap((w) => w.exercises.map((e) => e.name)))];

function lastSetsFor(name) {
  for (const w of sortedWorkouts()) {
    const ex = w.exercises.find((e) => e.name.toLowerCase() === name.toLowerCase());
    if (ex) return { date: w.date, sets: ex.sets };
  }
  return null;
}

// ---------- Навигация ----------
const TITLES = { log: 'Тренировка', history: 'История', progress: 'Прогресс', claude: 'Анализ Claude' };

function show(view) {
  document.querySelectorAll('.view').forEach((v) => v.classList.toggle('active', v.id === 'view-' + view));
  document.querySelectorAll('nav button').forEach((b) => b.classList.toggle('on', b.dataset.view === view));
  $('#title').textContent = TITLES[view];
  if (view === 'history') renderHistory();
  if (view === 'progress') renderProgress();
  if (view === 'claude') renderPreview();
  window.scrollTo(0, 0);
}
document.querySelectorAll('nav button').forEach((b) => b.addEventListener('click', () => show(b.dataset.view)));

// ---------- Текущая тренировка ----------
function persistDraft() { save(DRAFT_KEY, draft); }

function renderDraft() {
  $('#w-date').value = draft.date;
  $('#w-note').value = draft.note;
  $('#ex-list').innerHTML = exerciseNames().map((n) => `<option value="${esc(n)}">`).join('');

  $('#draft-exercises').innerHTML = draft.exercises.map((ex, i) => {
    const last = lastSetsFor(ex.name);
    const hint = last
      ? `<div class="ex-hint">Прошлый раз (${fmtDate(last.date)}): ${last.sets.map((s) => `${fmt(s.w)}×${s.r}`).join(', ')}</div>`
      : '';
    const sets = ex.sets.map((s, j) => `
      <div class="set">
        <span>${j + 1}</span>
        <input inputmode="decimal" placeholder="кг" value="${esc(s.w)}" data-i="${i}" data-j="${j}" data-f="w">
        <span>×</span>
        <input inputmode="numeric" placeholder="повт" value="${esc(s.r)}" data-i="${i}" data-j="${j}" data-f="r">
        <button class="btn ghost" data-del-set="${i}:${j}" aria-label="Удалить подход">✕</button>
      </div>`).join('');
    return `
      <div class="ex">
        <div class="ex-head">
          <b>${esc(ex.name)}</b>
          <button class="btn ghost" data-del-ex="${i}">Удалить</button>
        </div>
        ${hint}
        ${sets}
        <button class="btn small" data-add-set="${i}">+ подход</button>
      </div>`;
  }).join('');
}

$('#draft-exercises').addEventListener('input', (e) => {
  const { i, j, f } = e.target.dataset;
  if (i === undefined) return;
  draft.exercises[i].sets[j][f] = e.target.value;
  persistDraft();
});

$('#draft-exercises').addEventListener('click', (e) => {
  const t = e.target;
  if (t.dataset.addSet !== undefined) {
    const sets = draft.exercises[t.dataset.addSet].sets;
    const prev = sets[sets.length - 1] || { w: '', r: '' };
    sets.push({ w: prev.w, r: prev.r }); // копируем предыдущий подход — обычно он такой же
  } else if (t.dataset.delSet) {
    const [i, j] = t.dataset.delSet.split(':').map(Number);
    draft.exercises[i].sets.splice(j, 1);
  } else if (t.dataset.delEx !== undefined) {
    draft.exercises.splice(t.dataset.delEx, 1);
  } else return;
  persistDraft();
  renderDraft();
});

$('#add-ex-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const name = $('#ex-name').value.trim();
  if (!name) return;
  // Используем каноническое написание, если упражнение уже встречалось
  const known = exerciseNames().find((n) => n.toLowerCase() === name.toLowerCase()) || name;
  const last = lastSetsFor(known);
  const first = last ? last.sets[0] : { w: '', r: '' };
  draft.exercises.push({ name: known, sets: [{ w: String(first.w), r: String(first.r) }] });
  $('#ex-name').value = '';
  persistDraft();
  renderDraft();
});

$('#w-date').addEventListener('change', (e) => { draft.date = e.target.value || today(); persistDraft(); });
$('#w-note').addEventListener('input', (e) => { draft.note = e.target.value; persistDraft(); });

$('#save-workout').addEventListener('click', () => {
  const exercises = draft.exercises
    .map((ex) => ({
      name: ex.name,
      sets: ex.sets.map((s) => ({ w: num(s.w), r: Math.round(num(s.r)) })).filter((s) => s.r > 0),
    }))
    .filter((ex) => ex.sets.length);
  if (!exercises.length) return toast('Добавьте хотя бы один подход');
  workouts.push({ id: Date.now(), date: draft.date, note: draft.note.trim(), exercises });
  save(KEY, workouts);
  draft = newDraft();
  persistDraft();
  renderDraft();
  toast('Тренировка сохранена 💪');
});

// ---------- История ----------
function workoutVolume(w) {
  return w.exercises.reduce((sum, ex) => sum + ex.sets.reduce((s, x) => s + x.w * x.r, 0), 0);
}

function renderHistory() {
  const list = sortedWorkouts();
  if (!list.length) {
    $('#history-list').innerHTML = '<div class="empty">Пока нет тренировок</div>';
    return;
  }
  $('#history-list').innerHTML = list.map((w) => `
    <div class="card">
      <div class="card-head">
        <b>${fmtDate(w.date)}</b>
        <span class="muted">${Math.round(workoutVolume(w))} кг</span>
      </div>
      <ul>${w.exercises.map((ex) => `<li>${esc(ex.name)} <em>${ex.sets.map((s) => `${fmt(s.w)}×${s.r}`).join(' · ')}</em></li>`).join('')}</ul>
      ${w.note ? `<div class="note">${esc(w.note)}</div>` : ''}
      <div class="row" style="margin:8px 0 0">
        <button class="btn small" data-repeat="${w.id}">Повторить</button>
        <button class="btn ghost" data-del-w="${w.id}">Удалить</button>
      </div>
    </div>`).join('');
}

$('#history-list').addEventListener('click', (e) => {
  const id = Number(e.target.dataset.delW || e.target.dataset.repeat);
  if (!id) return;
  const w = workouts.find((x) => x.id === id);
  if (e.target.dataset.delW) {
    if (!confirm('Удалить тренировку?')) return;
    workouts = workouts.filter((x) => x.id !== id);
    save(KEY, workouts);
    renderHistory();
  } else {
    draft = {
      date: today(), note: '',
      exercises: w.exercises.map((ex) => ({ name: ex.name, sets: ex.sets.map((s) => ({ w: String(s.w), r: String(s.r) })) })),
    };
    persistDraft();
    renderDraft();
    show('log');
  }
});

// ---------- Прогресс ----------
function seriesFor(name) {
  const byDate = new Map();
  for (const w of workouts) {
    const ex = w.exercises.find((e) => e.name === name);
    if (!ex) continue;
    const cur = byDate.get(w.date) || { e1rm: 0, max: 0, volume: 0 };
    for (const s of ex.sets) {
      cur.e1rm = Math.max(cur.e1rm, e1rm(s.w, s.r));
      cur.max = Math.max(cur.max, s.w);
      cur.volume += s.w * s.r;
    }
    byDate.set(w.date, cur);
  }
  return [...byDate.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([date, v]) => ({ date, ...v }));
}

function renderProgress() {
  const names = exerciseNames();
  const sel = $('#progress-ex');
  const current = sel.value;
  sel.innerHTML = names.map((n) => `<option>${esc(n)}</option>`).join('');
  if (names.includes(current)) sel.value = current;

  if (!names.length) {
    $('#chart').innerHTML = '<div class="empty">Сохраните тренировки, чтобы увидеть прогресс</div>';
    $('#progress-stats').innerHTML = '';
    return;
  }
  const data = seriesFor(sel.value);
  $('#chart').innerHTML = chartSVG(data.map((d) => ({ x: d.date, y: d[metric] })));

  const first = data[0][metric], last = data[data.length - 1][metric];
  const best = Math.max(...data.map((d) => d[metric]));
  const diff = first ? ((last - first) / first) * 100 : 0;
  $('#progress-stats').innerHTML = `
    <div class="stat"><div>Рекорд</div><b>${fmt(best)}</b></div>
    <div class="stat"><div>Последний</div><b>${fmt(last)}</b></div>
    <div class="stat"><div>Изменение</div><b>${diff >= 0 ? '+' : ''}${fmt(diff)}%</b></div>`;
}

function chartSVG(points) {
  const W = 340, H = 200, P = { l: 36, r: 10, t: 12, b: 24 };
  if (points.length < 2) {
    return `<div class="empty">Нужно минимум 2 тренировки с этим упражнением${points.length ? ` (сейчас: ${fmt(points[0].y)})` : ''}</div>`;
  }
  const ys = points.map((p) => p.y);
  let min = Math.min(...ys), max = Math.max(...ys);
  if (min === max) { min -= 1; max += 1; }
  const pad = (max - min) * 0.1; min -= pad; max += pad;
  const X = (i) => P.l + (i / (points.length - 1)) * (W - P.l - P.r);
  const Y = (v) => P.t + (1 - (v - min) / (max - min)) * (H - P.t - P.b);
  const path = points.map((p, i) => `${i ? 'L' : 'M'}${X(i).toFixed(1)},${Y(p.y).toFixed(1)}`).join('');
  const dots = points.map((p, i) => `<circle class="dot" cx="${X(i)}" cy="${Y(p.y)}" r="3"/>`).join('');
  const short = (iso) => iso.slice(8, 10) + '.' + iso.slice(5, 7);
  return `<svg viewBox="0 0 ${W} ${H}" role="img">
    <line class="axis" x1="${P.l}" y1="${H - P.b}" x2="${W - P.r}" y2="${H - P.b}"/>
    <text class="lbl-t" x="${P.l - 6}" y="${Y(max - pad) + 4}" text-anchor="end">${Math.round(max - pad)}</text>
    <text class="lbl-t" x="${P.l - 6}" y="${Y(min + pad) + 4}" text-anchor="end">${Math.round(min + pad)}</text>
    <text class="lbl-t" x="${P.l}" y="${H - 6}">${short(points[0].x)}</text>
    <text class="lbl-t" x="${W - P.r}" y="${H - 6}" text-anchor="end">${short(points[points.length - 1].x)}</text>
    <path class="line" d="${path}"/>${dots}
  </svg>`;
}

$('#progress-ex').addEventListener('change', renderProgress);
document.querySelectorAll('#metric-seg button').forEach((b) => b.addEventListener('click', () => {
  metric = b.dataset.metric;
  document.querySelectorAll('#metric-seg button').forEach((x) => x.classList.toggle('on', x === b));
  renderProgress();
}));

// ---------- Claude ----------
const PRESETS = {
  analyze: 'Проанализируй мой прогресс: что растёт, что стоит на месте, есть ли признаки перетренированности или недовосстановления. Дай 3–5 конкретных рекомендаций.',
  plan: 'Составь план тренировок на следующую неделю с конкретными весами и повторениями для каждого упражнения, опираясь на мои последние результаты и принцип прогрессивной перегрузки.',
  plateau: 'Найди упражнения, где у меня застой (нет роста расчётного 1ПМ или рабочих весов). Объясни возможные причины и предложи, как выйти из плато.',
  balance: 'Оцени баланс моей программы: распределение объёма по мышечным группам, соотношение жимовых/тяговых движений, частоту тренировок. Что добавить или убрать?',
};

function buildPrompt() {
  const days = Number($('#ai-period').value);
  const from = days ? new Date(Date.now() - days * 864e5).toISOString().slice(0, 10) : '0000';
  const list = sortedWorkouts().filter((w) => w.date >= from).reverse();
  const preset = $('#ai-preset').value;
  const question = preset === 'custom' ? ($('#ai-custom').value.trim() || PRESETS.analyze) : PRESETS[preset];

  const log = list.map((w) => {
    const ex = w.exercises.map((e) => `  - ${e.name}: ${e.sets.map((s) => `${fmt(s.w)}кг×${s.r}`).join(', ')}`).join('\n');
    return `${w.date}${w.note ? ` (заметка: ${w.note})` : ''}\n${ex}`;
  }).join('\n\n');

  const names = [...new Set(list.flatMap((w) => w.exercises.map((e) => e.name)))];
  const summary = names.map((n) => {
    const s = seriesFor(n).filter((d) => d.date >= from);
    if (!s.length) return '';
    return `- ${n}: расчётный 1ПМ ${fmt(s[0].e1rm)} → ${fmt(s[s.length - 1].e1rm)} кг, тренировок: ${s.length}`;
  }).filter(Boolean).join('\n');

  return `Ты — опытный тренер по силовым тренировкам. Ниже мой дневник тренировок (вес × повторения в каждом подходе).

${question}

Сводка по упражнениям (1ПМ по формуле Эпли):
${summary || '—'}

Дневник (${list.length} тренировок${days ? ` за последние ${days} дн.` : ''}):
${log || 'Нет записей за период.'}

Отвечай по-русски, кратко и по делу.`;
}

function renderPreview() { $('#ai-preview').textContent = buildPrompt(); }
['#ai-period', '#ai-preset', '#ai-custom'].forEach((s) => $(s).addEventListener('input', () => {
  $('#ai-custom').hidden = $('#ai-preset').value !== 'custom';
  renderPreview();
}));

async function copyPrompt(text) {
  try { await navigator.clipboard.writeText(text); return true; } catch { return false; }
}

// Системное меню «Поделиться» Android → выбрать приложение Claude.
// Запрос откроется в Claude с вашим аккаунтом Pro.
$('#ai-share').addEventListener('click', async () => {
  const text = buildPrompt();
  if (navigator.share) {
    try { await navigator.share({ text }); return; }
    catch (e) { if (e.name === 'AbortError') return; }
  }
  await copyPrompt(text);
  toast('Скопировано — вставьте в Claude');
});

// claude.ai/new?q=… открывает новый чат с готовым запросом.
// Очень длинные запросы не влезают в URL — тогда копируем в буфер.
$('#ai-open').addEventListener('click', async () => {
  const text = buildPrompt();
  if (text.length < 6000) {
    window.open('https://claude.ai/new?q=' + encodeURIComponent(text), '_blank');
  } else {
    await copyPrompt(text);
    toast('Запрос длинный — скопирован, вставьте в чат');
    window.open('https://claude.ai/new', '_blank');
  }
});

$('#ai-copy').addEventListener('click', async () => {
  toast((await copyPrompt(buildPrompt())) ? 'Скопировано' : 'Не удалось скопировать');
});

// ---------- Резервная копия ----------
$('#export').addEventListener('click', () => {
  const blob = new Blob([JSON.stringify(workouts, null, 2)], { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `workouts-${today()}.json`;
  a.click();
  URL.revokeObjectURL(a.href);
});

$('#import').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  try {
    const data = JSON.parse(await file.text());
    if (!Array.isArray(data)) throw new Error();
    const ids = new Set(workouts.map((w) => w.id));
    const added = data.filter((w) => w && w.date && Array.isArray(w.exercises) && !ids.has(w.id));
    workouts.push(...added);
    save(KEY, workouts);
    toast(`Импортировано: ${added.length}`);
    renderDraft();
  } catch {
    toast('Неверный файл');
  }
  e.target.value = '';
});

// ---------- Старт ----------
renderDraft();
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js'));
}
