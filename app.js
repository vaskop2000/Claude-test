'use strict';

const VERSION = 17; // увеличивать вместе с CACHE в sw.js

// ---------- Хранилище ----------
const KEY = 'wd.workouts.v1';
const DRAFT_KEY = 'wd.draft.v1';
const STASH_KEY = 'wd.stash.v1'; // недописанная тренировка, отложенная на время редактирования

const load = (k, fallback) => {
  try { return JSON.parse(localStorage.getItem(k)) ?? fallback; } catch { return fallback; }
};
const save = (k, v) => localStorage.setItem(k, JSON.stringify(v));

let workouts = load(KEY, []);
let draft = load(DRAFT_KEY, null) || newDraft();

function newDraft() {
  return { title: '', date: today(), note: '', exercises: [] };
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
    // Подсказываем только более ранние тренировки и не саму редактируемую
    if (w.id === draft.editId || w.date > draft.date) continue;
    const ex = w.exercises.find((e) => e.name.toLowerCase() === name.toLowerCase());
    if (ex) return { date: w.date, sets: ex.sets };
  }
  return null;
}

// ---------- Навигация ----------
const TITLES = { log: 'Тренировка', history: 'История', progress: 'Прогресс', guide: 'Техника', claude: 'Анализ Claude' };

function show(view) {
  document.querySelectorAll('.view').forEach((v) => v.classList.toggle('active', v.id === 'view-' + view));
  document.querySelectorAll('nav button').forEach((b) => b.classList.toggle('on', b.dataset.view === view));
  document.body.dataset.view = view;
  $('#title').textContent = view === 'log' && draft.editId ? 'Редактирование' : TITLES[view];
  if (view === 'history') renderHistory();
  if (view === 'progress') renderProgress();
  if (view === 'claude') renderPreview();
  if (view === 'guide') renderGuideList();
  window.scrollTo(0, 0);
}
document.querySelectorAll('nav button').forEach((b) => b.addEventListener('click', () => show(b.dataset.view)));

// ---------- Текущая тренировка ----------
function persistDraft() { save(DRAFT_KEY, draft); }

const toDraftExercises = (w) =>
  w.exercises.map((ex) => ({ name: ex.name, sets: ex.sets.map((s) => ({ w: String(s.w), r: String(s.r) })) }));
const draftIsEmpty = () => !draft.exercises.length && !(draft.title || '').trim() && !draft.note.trim();

function startEdit(w) {
  // Недописанную новую тренировку откладываем, чтобы вернуть её после редактирования
  if (!draft.editId && !draftIsEmpty()) save(STASH_KEY, draft);
  draft = { editId: w.id, title: w.title || '', date: w.date, note: w.note || '', exercises: toDraftExercises(w) };
  persistDraft();
  renderDraft();
  show('log');
}

function finishEdit() {
  draft = load(STASH_KEY, null) || newDraft();
  localStorage.removeItem(STASH_KEY);
  persistDraft();
  renderDraft();
}

function renderDraft() {
  $('#save-workout').textContent = draft.editId ? 'Сохранить изменения' : 'Сохранить тренировку';
  $('#cancel-edit').hidden = !draft.editId;
  if ($('#view-log').classList.contains('active')) $('#title').textContent = draft.editId ? 'Редактирование' : TITLES.log;
  $('#w-date').value = draft.date;
  $('#w-title').value = draft.title || '';
  $('#w-note').value = draft.note;
  $('#title-list').innerHTML = [...new Set(sortedWorkouts().map((w) => w.title).filter(Boolean))]
    .map((t) => `<option value="${esc(t)}">`).join('');
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
          <input class="ex-title" value="${esc(ex.name)}" data-name="${i}" list="ex-list" aria-label="Название упражнения" autocomplete="off">
          ${findExercise(ex.name) ? `<button class="btn ghost info" data-guide="${esc(ex.name)}" aria-label="Техника">?</button>` : ''}
          <button class="btn ghost" data-del-ex="${i}">Удалить</button>
        </div>
        ${hint}
        ${sets}
        <button class="btn small" data-add-set="${i}">+ подход</button>
      </div>`;
  }).join('');
}

const canonicalName = (name) =>
  exerciseNames().find((n) => n.toLowerCase() === name.toLowerCase()) || findExercise(name)?.name || name;

$('#draft-exercises').addEventListener('input', (e) => {
  if (e.target.dataset.name !== undefined) {
    draft.exercises[e.target.dataset.name].name = e.target.value;
    persistDraft();
    return;
  }
  const { i, j, f } = e.target.dataset;
  if (i === undefined) return;
  draft.exercises[i].sets[j][f] = e.target.value;
  persistDraft();
});

// Переименование упражнения: по завершении ввода приводим к известному названию
// и перерисовываем, чтобы обновилась подсказка «прошлый раз».
$('#draft-exercises').addEventListener('change', (e) => {
  const i = e.target.dataset.name;
  if (i === undefined) return;
  const ex = draft.exercises[i];
  ex.name = canonicalName(e.target.value.trim()) || ex.name;
  if (!ex.name) ex.name = 'Без названия';
  persistDraft();
  renderDraft();
});
$('#draft-exercises').addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && e.target.dataset.name !== undefined) e.target.blur();
});

$('#draft-exercises').addEventListener('click', (e) => {
  const t = e.target;
  if (t.dataset.guide) return openGuide(findExercise(t.dataset.guide), true);
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
  addExercise(name);
  $('#ex-name').value = '';
});

function addExercise(name) {
  // Используем каноническое написание, если упражнение уже встречалось
  const known = canonicalName(name);
  const last = lastSetsFor(known);
  const first = last ? last.sets[0] : { w: '', r: '' };
  draft.exercises.push({ name: known, sets: [{ w: String(first.w), r: String(first.r) }] });
  persistDraft();
  renderDraft();
}

$('#w-date').addEventListener('change', (e) => { draft.date = e.target.value || today(); persistDraft(); });
$('#w-note').addEventListener('input', (e) => { draft.note = e.target.value; persistDraft(); });
$('#w-title').addEventListener('input', (e) => { draft.title = e.target.value; persistDraft(); });
$('#w-title').addEventListener('keydown', (e) => { if (e.key === 'Enter') e.target.blur(); });

$('#save-workout').addEventListener('click', () => {
  const exercises = draft.exercises
    .map((ex) => ({
      name: canonicalName(ex.name.trim()) || 'Без названия',
      sets: ex.sets.map((s) => ({ w: num(s.w), r: Math.round(num(s.r)) })).filter((s) => s.r > 0),
    }))
    .filter((ex) => ex.sets.length);
  if (!exercises.length) return toast('Добавьте хотя бы один подход');
  const data = { title: (draft.title || '').trim(), date: draft.date, note: draft.note.trim(), exercises };
  const idx = draft.editId ? workouts.findIndex((w) => w.id === draft.editId) : -1;
  if (idx >= 0) {
    workouts[idx] = { id: draft.editId, ...data };
    save(KEY, workouts);
    finishEdit();
    show('history');
    return toast('Изменения сохранены');
  }
  workouts.push({ id: Date.now(), ...data });
  save(KEY, workouts);
  draft = newDraft();
  persistDraft();
  renderDraft();
  toast('Тренировка сохранена 💪');
});

$('#cancel-edit').addEventListener('click', () => {
  finishEdit();
  show('history');
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
        <div><b>${w.title ? esc(w.title) : fmtDate(w.date)}</b>${w.title ? ` <span class="muted">${fmtDate(w.date)}</span>` : ''}</div>
        <span class="muted">${Math.round(workoutVolume(w))} кг</span>
      </div>
      <ul>${w.exercises.map((ex) => `<li>${esc(ex.name)} <em>${ex.sets.map((s) => `${fmt(s.w)}×${s.r}`).join(' · ')}</em></li>`).join('')}</ul>
      ${w.note ? `<div class="note">${esc(w.note)}</div>` : ''}
      <div class="row" style="margin:8px 0 0">
        <button class="btn small" data-edit="${w.id}">Изменить</button>
        <button class="btn small" data-repeat="${w.id}">Повторить</button>
        <button class="btn ghost" data-del-w="${w.id}">Удалить</button>
      </div>
    </div>`).join('');
}

$('#history-list').addEventListener('click', (e) => {
  const { delW, repeat, edit } = e.target.dataset;
  const id = Number(delW || repeat || edit);
  if (!id) return;
  const w = workouts.find((x) => x.id === id);
  if (edit) {
    startEdit(w);
  } else if (delW) {
    if (!confirm('Удалить тренировку?')) return;
    workouts = workouts.filter((x) => x.id !== id);
    save(KEY, workouts);
    if (draft.editId === id) finishEdit();
    renderHistory();
  } else {
    if (draft.editId) finishEdit();
    draft = { title: w.title || '', date: today(), note: '', exercises: toDraftExercises(w) };
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

// Цвета линий подходов: 1-й, 2-й, 3-й… (различимы в светлой и тёмной теме)
const SET_COLORS = ['#d97757', '#3b82f6', '#22a06b', '#a855f7', '#e5484d', '#0ea5a5', '#d4a017', '#8b8b8b'];

// Тренировки с упражнением (от старых к новым) и подходы каждой из них
function exerciseSessions(name) {
  return workouts
    .filter((w) => w.exercises.some((e) => e.name === name))
    .sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id)
    .map((w) => ({ date: w.date, sets: w.exercises.find((e) => e.name === name).sets }));
}

function renderProgress() {
  const names = exerciseNames();
  const sel = $('#progress-ex');
  const current = sel.value;
  sel.innerHTML = names.map((n) => `<option>${esc(n)}</option>`).join('');
  if (names.includes(current)) sel.value = current;

  if (!names.length) {
    $('#chart').innerHTML = '<div class="empty">Сохраните тренировки, чтобы увидеть прогресс</div>';
    return;
  }
  renderSetCharts(exerciseSessions(sel.value));
}

// Мини-график на каждый подход (1-й, 2-й, …), строго друг под другом:
// X — тренировки по датам (общая ось для всех), Y — вес, над точкой — повторения.
// Если тренировок много, графики прокручиваются вбок синхронно.
function renderSetCharts(sessions) {
  const nSets = Math.max(...sessions.map((s) => s.sets.length));
  const AXIS = 40, STEP = 46, PAD = 22;
  const PH = 96, TOP = 20, BOT = 10, DATES = 22; // высота панели, отступы
  const avail = Math.max(240, $('#chart').clientWidth - AXIS);
  const plotW = Math.max(avail, PAD * 2 + (sessions.length - 1) * STEP);
  const X = (i) => (sessions.length === 1 ? plotW / 2 : PAD + (i * (plotW - PAD * 2)) / (sessions.length - 1));
  const short = (iso) => iso.slice(8, 10) + '.' + iso.slice(5, 7);

  const panels = [];
  for (let k = 0; k < nSets; k++) {
    const c = SET_COLORS[k % SET_COLORS.length];
    const pts = sessions.map((s, i) => s.sets[k] && { i, w: s.sets[k].w, r: s.sets[k].r }).filter(Boolean);
    const ws = pts.map((p) => p.w);
    let lo = Math.min(...ws), hi = Math.max(...ws);
    const span = hi - lo || Math.max(hi * 0.1, 5);
    const y0 = lo - span * 0.15, y1 = hi + span * 0.15;
    const isLast = k === nSets - 1;
    const H = PH + (isLast ? DATES : 0);
    const Y = (v) => (hi === lo ? TOP + (PH - TOP - BOT) / 2 : TOP + (1 - (v - y0) / (y1 - y0)) * (PH - TOP - BOT));

    const levels = [...new Set([lo, hi])];
    const axis = levels.map((v) => `<text class="lbl-t" x="${AXIS - 6}" y="${Y(v) + 4}" text-anchor="end">${fmt(v)}</text>`).join('');
    const grid = levels.map((v) => `<line class="grid" x1="0" x2="${plotW}" y1="${Y(v)}" y2="${Y(v)}"/>`).join('') +
      sessions.map((_, i) => `<line class="vgrid" x1="${X(i)}" x2="${X(i)}" y1="${TOP - 4}" y2="${PH - BOT + 4}"/>`).join('');
    const path = pts.map((p, j) => `${j ? 'L' : 'M'}${X(p.i).toFixed(1)},${Y(p.w).toFixed(1)}`).join('');
    const dots = pts.map((p, j) => {
      const last = j === pts.length - 1;
      return `<circle cx="${X(p.i).toFixed(1)}" cy="${Y(p.w).toFixed(1)}" r="${last ? 5 : 3.5}" fill="${c}"/>
        <text class="reps" x="${X(p.i).toFixed(1)}" y="${(Y(p.w) - 9).toFixed(1)}">${p.r}</text>`;
    }).join('');
    const dates = isLast
      ? sessions.map((s, i) => `<text class="lbl-t" x="${X(i)}" y="${PH + 12}" text-anchor="middle">${short(s.date)}</text>`).join('')
      : '';

    panels.push(`
      <div class="mini">
        <div class="mini-title"><i style="background:${c}"></i>Подход ${k + 1}</div>
        <div class="mini-row">
          <svg class="mini-axis" width="${AXIS}" height="${H}">${axis}</svg>
          <div class="mini-scroll">
            <svg width="${plotW}" height="${H}">${grid}
              <path d="${path}" fill="none" stroke="${c}" stroke-width="2" stroke-linejoin="round"/>${dots}${dates}
            </svg>
          </div>
        </div>
      </div>`);
  }
  $('#chart').innerHTML = `<div class="minis">${panels.join('')}</div>`;

  // Синхронная прокрутка; по умолчанию показываем последние тренировки
  const scrollers = [...document.querySelectorAll('#chart .mini-scroll')];
  let syncing = false;
  scrollers.forEach((el) => {
    el.scrollLeft = el.scrollWidth;
    el.addEventListener('scroll', () => {
      if (syncing) return;
      syncing = true;
      scrollers.forEach((o) => { if (o !== el) o.scrollLeft = el.scrollLeft; });
      requestAnimationFrame(() => { syncing = false; });
    });
  });
}

$('#progress-ex').addEventListener('change', renderProgress);

// ---------- Справочник техники ----------
let guideGroup = '';

function renderGuideList() {
  $('#guide-detail').hidden = true;
  $('#guide-list').hidden = false;
  $('#title').textContent = TITLES.guide;
  $('#guide-groups').innerHTML = ['', ...EX_GROUPS].map((g) =>
    `<button data-group="${g}" class="${g === guideGroup ? 'on' : ''}">${g || 'Все'}</button>`).join('');
  const q = normName($('#guide-search').value);
  const items = EXERCISES.filter((ex) =>
    (!guideGroup || ex.group === guideGroup) &&
    (!q || [ex.name, ...ex.aliases].some((n) => normName(n).includes(q))));
  $('#guide-items').innerHTML = items.length
    ? items.map((ex) => `
      <button class="guide-item" data-ex="${esc(ex.name)}">
        <b>${esc(ex.name)}</b>
        <span class="muted">${ex.primary.map((m) => MUSCLES[m]).join(', ')}</span>
      </button>`).join('')
    : '<div class="empty">Ничего не найдено</div>';
}

// fromLog — открыли кнопкой «?» из тренировки: «назад» вернёт туда
function openGuide(ex, fromLog = false) {
  if (!ex) return;
  if (!$('#view-guide').classList.contains('active')) show('guide');
  $('#guide-list').hidden = true;
  const d = $('#guide-detail');
  d.hidden = false;
  d.dataset.back = fromLog ? 'log' : 'list';
  $('#title').textContent = ex.name;
  const names = (ms) => ms.map((m) => MUSCLES[m]).join(', ');
  d.innerHTML = `
    <button class="btn ghost back" id="guide-back">← ${fromLog ? 'К тренировке' : 'Все упражнения'}</button>
    <div class="bodymap" id="guide-body"></div>
    <div class="muscle-legend">
      <div><i class="mp"></i><span><b>Основные:</b> ${names(ex.primary)}</span></div>
      ${ex.secondary.length ? `<div><i class="ms"></i><span><b>Помогают:</b> ${names(ex.secondary)}</span></div>` : ''}
    </div>
    <h3>Техника</h3>
    <ol>${ex.technique.map((t) => `<li>${esc(t)}</li>`).join('')}</ol>
    <h3>Частые ошибки</h3>
    <ul>${ex.mistakes.map((t) => `<li>${esc(t)}</li>`).join('')}</ul>
    ${fromLog ? '' : `<button class="btn primary wide" id="guide-add">Добавить в тренировку</button>`}
    <p class="muted small">Общие рекомендации. При боли или травмах проконсультируйтесь с тренером или врачом.</p>`;
  renderBodyMap($('#guide-body'), ex.primary, ex.secondary);
  window.scrollTo(0, 0);
  $('#guide-back').addEventListener('click', () => (d.dataset.back === 'log' ? show('log') : renderGuideList()));
  $('#guide-add')?.addEventListener('click', () => {
    addExercise(ex.name);
    show('log');
    toast(`Добавлено: ${ex.name}`);
  });
}

$('#guide-search').addEventListener('input', renderGuideList);
$('#guide-groups').addEventListener('click', (e) => {
  if (e.target.dataset.group === undefined) return;
  guideGroup = e.target.dataset.group;
  renderGuideList();
});
$('#guide-items').addEventListener('click', (e) => {
  const b = e.target.closest('[data-ex]');
  if (b) openGuide(findExercise(b.dataset.ex));
});

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
    return `${w.date}${w.title ? ` — ${w.title}` : ''}${w.note ? ` (заметка: ${w.note})` : ''}\n${ex}`;
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

// ---------- Таймер отдыха ----------
// Время окончания храним в localStorage: таймер переживает сворачивание и перезапуск.
const TIMER_KEY = 'wd.timer.v1';
let timer = load(TIMER_KEY, null); // { end, total }
let tick = null, wakeLock = null, audio = null;

const mmss = (sec) => `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;

function beep() {
  try {
    audio = audio || new (window.AudioContext || window.webkitAudioContext)();
    audio.resume();
    [0, 0.35, 0.7].forEach((t) => {
      const o = audio.createOscillator(), g = audio.createGain();
      o.frequency.value = 880;
      g.gain.setValueAtTime(0.25, audio.currentTime + t);
      g.gain.exponentialRampToValueAtTime(0.001, audio.currentTime + t + 0.25);
      o.connect(g).connect(audio.destination);
      o.start(audio.currentTime + t);
      o.stop(audio.currentTime + t + 0.3);
    });
  } catch {}
}

async function keepAwake(on) {
  // Экран не гаснет, пока идёт отдых, — иначе сигнал не прозвучит
  try {
    if (on && !wakeLock && navigator.wakeLock) wakeLock = await navigator.wakeLock.request('screen');
    if (!on && wakeLock) { await wakeLock.release(); wakeLock = null; }
  } catch {}
}

function renderTimer() {
  const el = $('#timer');
  clearInterval(tick);
  el.classList.remove('done');
  if (!timer) {
    el.classList.remove('running');
    $('#t-bar').style.width = '0';
    keepAwake(false);
    return;
  }
  el.classList.add('running');
  keepAwake(true);
  const update = () => {
    const left = Math.max(0, Math.ceil((timer.end - Date.now()) / 1000));
    $('#t-left').textContent = left ? mmss(left) : 'Отдых окончен';
    $('#t-bar').style.width = `${100 * (1 - left / timer.total)}%`;
    if (!left) finishTimer();
  };
  update();
  if (timer) tick = setInterval(update, 250);
}

function finishTimer() {
  clearInterval(tick);
  const late = Date.now() - timer.end > 5000; // вернулись в приложение уже после окончания
  timer = null;
  localStorage.removeItem(TIMER_KEY);
  keepAwake(false);
  const el = $('#timer');
  el.classList.add('done');
  if (!late) {
    beep();
    if (navigator.vibrate) navigator.vibrate([300, 150, 300, 150, 300]);
  }
  setTimeout(() => { if (!timer) { el.classList.remove('running', 'done'); $('#t-bar').style.width = '0'; } }, 6000);
}

function startTimer(sec) {
  // Браузер разрешает звук только после касания — готовим его заранее
  try { audio = audio || new (window.AudioContext || window.webkitAudioContext)(); audio.resume(); } catch {}
  timer = { end: Date.now() + sec * 1000, total: sec };
  save(TIMER_KEY, timer);
  renderTimer();
}

document.querySelectorAll('[data-rest]').forEach((b) => b.addEventListener('click', () => startTimer(Number(b.dataset.rest))));
$('#t-plus').addEventListener('click', () => {
  if (!timer) return;
  timer.end += 30000;
  timer.total += 30;
  save(TIMER_KEY, timer);
  renderTimer();
});
$('#t-stop').addEventListener('click', () => {
  timer = null;
  localStorage.removeItem(TIMER_KEY);
  renderTimer();
});
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') renderTimer();
});

// ---------- Экспорт в Заметки ----------
// Человекочитаемый текст всей истории. Android-меню «Поделиться» передаёт его
// в любое приложение, принимающее текст, — например, в «Заметки» Xiaomi.
function buildNotesText() {
  const list = sortedWorkouts();
  const longDate = (iso) => {
    const s = new Date(iso + 'T00:00').toLocaleDateString('ru-RU', { weekday: 'short', day: 'numeric', month: 'long', year: 'numeric' });
    return s.charAt(0).toUpperCase() + s.slice(1);
  };

  const records = exerciseNames().sort((a, b) => a.localeCompare(b, 'ru')).map((n) => {
    const s = seriesFor(n);
    const max = Math.max(...s.map((d) => d.max));
    const best = Math.max(...s.map((d) => d.e1rm));
    return `• ${n}: макс. ${fmt(max)} кг, 1ПМ ≈ ${fmt(best)} кг`;
  });

  const body = list.map((w) => [
    longDate(w.date) + (w.title ? ` — ${w.title}` : ''),
    ...w.exercises.map((ex) => `• ${ex.name}: ${ex.sets.map((s) => `${fmt(s.w)}×${s.r}`).join(', ')}`),
    `Объём: ${Math.round(workoutVolume(w))} кг`,
    ...(w.note ? [`Заметка: ${w.note}`] : []),
  ].join('\n'));

  return [
    'ДНЕВНИК ТРЕНИРОВОК',
    `Экспорт: ${longDate(today())} · тренировок: ${list.length}`,
    '',
    'ЛИЧНЫЕ РЕКОРДЫ',
    ...records,
    '',
    'ИСТОРИЯ',
    '',
    body.join('\n\n— — —\n\n'),
  ].join('\n');
}

$('#export-notes').addEventListener('click', async () => {
  if (!workouts.length) return toast('История пуста');
  const text = buildNotesText();
  if (navigator.share) {
    try { await navigator.share({ title: 'Дневник тренировок', text }); return; }
    catch (e) { if (e.name === 'AbortError') return; }
  }
  toast((await copyPrompt(text)) ? 'Скопировано — вставьте в Заметки' : 'Не удалось поделиться');
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
document.body.dataset.view = 'log';
renderTimer();
renderDraft();
// ---------- Обновления ----------
// Установленное приложение Android держит в памяти, поэтому сами проверяем
// обновления при каждом возврате в приложение и перезагружаемся, когда
// новая версия готова. Черновик хранится в localStorage, так что ничего не теряется.
$('#app-version').textContent = 'Версия ' + VERSION;
let swReg = null;
if ('serviceWorker' in navigator) {
  const hadController = !!navigator.serviceWorker.controller;
  let reloading = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (!hadController || reloading) return;
    reloading = true;
    location.reload();
  });
  navigator.serviceWorker.register('sw.js', { updateViaCache: 'none' }).then((r) => { swReg = r; });
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && swReg) swReg.update().catch(() => {});
  });
}

$('#check-update').addEventListener('click', async () => {
  toast('Проверяю…');
  try { if (swReg) await swReg.update(); } catch {}
  location.reload();
});
