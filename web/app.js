/* Java + DSA study site: roadmap and practice tracker. Data comes from web/data.js (tools/build_site.py). Notes open on GitHub. */
(() => {
  'use strict';
  const S = window.STUDY;
  const $ = (sel, el = document) => el.querySelector(sel);
  const $$ = (sel, el = document) => [...el.querySelectorAll(sel)];
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const today = () => new Date().toISOString().slice(0, 10);
  const main = $('#main'), side = $('#sidebar');
  const ROADMAP_PATH = 'java_dsa_study_roadmap.md';

  // ---------- storage (per browser + address; wrapped because storage can be blocked) ----------
  const KEYS = { practice: 'study.practice.v1', roadmap: 'study.roadmap.v1', ui: 'study.ui.v1' };
  function load(key, fallback) {
    try { const v = JSON.parse(localStorage.getItem(key)); return v && typeof v === 'object' ? v : fallback; } catch { return fallback; }
  }
  function save(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* storage unavailable */ } }
  const progress = load(KEYS.practice, {});   // slug -> {s: 'done' | 'revisit', d: date}
  const topicsDone = load(KEYS.roadmap, {});  // topic id -> date
  const ui = Object.assign({ theme: '', group: 'roadmap', q: '', diff: 'all', status: 'all', open: {} }, load(KEYS.ui, {}));
  const saveUi = () => save(KEYS.ui, { theme: ui.theme, group: ui.group, diff: ui.diff, status: ui.status });

  // ---------- indexes ----------
  const problems = S.practice.problems;
  const sections = Object.fromEntries(S.practice.sections.map(s => [s.id, s]));
  const roadmapSecs = S.roadmap.sections;
  const topicSecs = roadmapSecs.filter(s => s.kind === 'topics');
  const topicInfo = {};      // id -> {topic, section}
  topicSecs.forEach(sec => sec.topics.forEach(t => { topicInfo[t.id] = { t, sec }; }));
  const byTopic = {};
  problems.forEach(p => (byTopic[p.topic] ||= []).push(p));
  const notesByPath = Object.fromEntries(S.notes.map(n => [n.path, n]));
  const allTopicIds = topicSecs.flatMap(s => s.topics.map(t => t.id));

  const isDone = p => progress[p.slug]?.s === 'done';
  const isRevisit = p => !!progress[p.slug]?.r;
  const countDone = list => list.filter(isDone).length;
  const cellOf = (sec, t, name) => { const i = sec.headers.indexOf(name); return i < 0 ? '' : t.cells[i] || ''; };
  const topicTitleMd = (sec, t) => cellOf(sec, t, 'Topic');
  const plainTopic = (sec, t) => topicTitleMd(sec, t).replace(/\[([^\]]+)\]\([^)]*\)/g, '$1').replace(/[*`]/g, '').replace(/\s*[🔴🟡🟢]\s*/gu, ' ').trim();

  // ---------- markdown ----------
  const hasMarked = typeof window.marked !== 'undefined';
  if (hasMarked) {
    marked.use({
      gfm: true,
      renderer: {
        code(code, lang) {
          if ((lang || '').trim() === 'mermaid') return `<pre class="mermaid">${esc(code)}</pre>`;
          return false;
        },
      },
    });
  }
  if (window.mermaid) mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'strict' });
  const mdBlock = s => hasMarked ? marked.parse(s || '') : `<pre>${esc(s)}</pre>`;
  const mdInline = s => hasMarked ? marked.parseInline(s || '') : esc(s);

  function slugify(text) {
    return text.trim().toLowerCase().replace(/[^\p{L}\p{N}\p{M}\p{Pc}\- ]/gu, '').replace(/ /g, '-');
  }
  function resolvePath(href, basePath) {
    const u = new URL(href, 'http://x/' + basePath.split('/').map(encodeURIComponent).join('/'));
    return { path: decodeURIComponent(u.pathname.slice(1)), hash: decodeURIComponent(u.hash.slice(1)) };
  }
  // notes are read on GitHub, where they render with their diagrams
  const ghHref = (path, anchor) => `${S.repo}/blob/main/${path.split('/').map(encodeURIComponent).join('/')}${anchor ? '#' + encodeURIComponent(anchor) : ''}`;
  const ghLink = (path, text, cls = '') => `<a${cls ? ` class="${cls}"` : ''} href="${ghHref(path)}" target="_blank" rel="noopener" title="Open ${esc(notesByPath[path]?.title || path)} on GitHub">${text}</a>`;

  // links inside rendered markdown: roadmap anchors stay in the app, repo files open on GitHub in a new tab
  function fixLinks(root, basePath) {
    $$('a[href]', root).forEach(a => {
      const href = a.getAttribute('href');
      if (href.startsWith('#/')) return;
      if (href.startsWith('#')) { a.setAttribute('href', '#/roadmap?s=' + encodeURIComponent(decodeURIComponent(href.slice(1)))); return; }
      if (!/^(https?:|mailto:)/i.test(href)) { const { path, hash } = resolvePath(href, basePath); a.setAttribute('href', ghHref(path, hash)); }
      a.target = '_blank'; a.rel = 'noopener';
    });
  }
  function decorate(root, basePath) {
    $$('blockquote', root).forEach(bq => {
      const p = bq.querySelector('p');
      const m = p && p.innerHTML.match(/^\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\s*(<br\s*\/?>)?\s*/i);
      if (!m) return;
      const kind = m[1].toLowerCase();
      p.innerHTML = p.innerHTML.slice(m[0].length);
      if (!p.innerHTML.trim()) p.remove();
      bq.classList.add('callout', 'callout-' + kind);
      bq.insertAdjacentHTML('afterbegin', `<div class="callout-title">${kind[0].toUpperCase() + kind.slice(1)}</div>`);
    });
    $$('table', root).forEach(t => {
      if (t.parentElement.classList.contains('table-wrap')) return;
      const w = document.createElement('div'); w.className = 'table-wrap'; t.replaceWith(w); w.appendChild(t);
    });
    fixLinks(root, basePath);
    return renderMermaid(root);
  }
  async function renderMermaid(root) {
    const nodes = $$('pre.mermaid', root).filter(n => !n.dataset.processed);
    if (!nodes.length || !window.mermaid) return;
    try { await mermaid.run({ nodes, suppressErrors: true }); } catch (e) { console.warn('mermaid', e); }
  }

  // ---------- theme ----------
  function effectiveDark() {
    if (ui.theme) return ui.theme === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
  function applyTheme() {
    if (ui.theme) document.documentElement.dataset.theme = ui.theme; else delete document.documentElement.dataset.theme;
    const dark = effectiveDark();
    document.documentElement.toggleAttribute('data-dark', dark);
  }
  $('#theme-btn').addEventListener('click', () => { ui.theme = effectiveDark() ? 'light' : 'dark'; saveUi(); applyTheme(); });
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyTheme);
  applyTheme();

  // ---------- sidebar on phones ----------
  $('#menu-btn').addEventListener('click', () => document.body.classList.toggle('side-open'));
  $('#backdrop').addEventListener('click', () => document.body.classList.remove('side-open'));
  side.addEventListener('click', e => { if (e.target.closest('a')) document.body.classList.remove('side-open'); });

  // ---------- progress header + dialog ----------
  function updateTop() {
    const done = countDone(problems);
    const tDone = allTopicIds.filter(id => topicsDone[id]).length;
    const pill = (label, d, n) => `<div class="tp" title="${label}: ${d} of ${n}">${label} <b>${d}/${n}</b><div class="mini"><span style="width:${n ? d / n * 100 : 0}%"></span></div></div>`;
    $('#top-progress').innerHTML = pill('Topics', tDone, allTopicIds.length) + pill('Problems', done, problems.length);
  }
  const pct = (d, n) => n ? Math.round(d / n * 100) : 0;
  const ring = (d, n, cls = '') => `<div class="ring ${cls}" style="--p:${pct(d, n)}"><i>${pct(d, n)}%</i></div>`;
  const ICON = {
    code: '<svg viewBox="0 0 24 24"><path d="m8 9-3 3 3 3M16 9l3 3-3 3M13.5 6l-3 12"/></svg>',
    book: '<svg viewBox="0 0 24 24"><path d="M5 4.5A1.5 1.5 0 0 1 6.5 3H19v15H6.5A1.5 1.5 0 0 0 5 19.5zM5 19.5A1.5 1.5 0 0 0 6.5 21H19v-3"/></svg>',
    ext: '<svg viewBox="0 0 24 24"><path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/></svg>',
    search: '<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>',
    dice: '<svg viewBox="0 0 24 24"><rect x="4" y="4" width="16" height="16" rx="3"/><circle cx="9" cy="9" r=".8" fill="currentColor"/><circle cx="15" cy="15" r=".8" fill="currentColor"/><circle cx="15" cy="9" r=".8" fill="currentColor"/><circle cx="9" cy="15" r=".8" fill="currentColor"/></svg>',
    up: '<svg viewBox="0 0 24 24"><path d="m6 15 6-6 6 6"/></svg>',
    arrow: '<svg viewBox="0 0 24 24"><path d="M5 12h14m-6-6 6 6-6 6"/></svg>',
  };
  $('#origin').textContent = location.origin === 'null' ? location.href.replace(/[#?].*$/, '') : location.origin;
  $('#progress-btn').addEventListener('click', () => {
    const rev = problems.filter(isRevisit).length;
    const plural = (n, w) => `${n} ${w}${n === 1 ? '' : 's'}`;
    $('#progress-summary').textContent = `${plural(countDone(problems), 'problem')} solved, ${rev} marked to revisit, ${plural(Object.keys(topicsDone).length, 'roadmap topic')} ticked.`;
    $('#import-msg').textContent = '';
    $('#progress-dialog').showModal();
  });
  $('#export-btn').addEventListener('click', () => {
    const blob = new Blob([JSON.stringify({ app: 'java-dsa-study', version: 1, saved: new Date().toISOString(), practice: progress, roadmap: topicsDone }, null, 1)],
      { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob); a.download = `study-progress-${today()}.json`; a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  });
  $('#import-file').addEventListener('change', async e => {
    const f = e.target.files[0]; e.target.value = '';
    if (!f) return;
    try {
      const d = JSON.parse(await f.text());
      if (d.app !== 'java-dsa-study' || typeof d.practice !== 'object' || typeof d.roadmap !== 'object') throw new Error('not a progress file from this site');
      if (!confirm('Replace the progress in this browser with the file\'s progress?')) return;
      Object.keys(progress).forEach(k => delete progress[k]); Object.assign(progress, d.practice);
      Object.keys(topicsDone).forEach(k => delete topicsDone[k]); Object.assign(topicsDone, d.roadmap);
      save(KEYS.practice, progress); save(KEYS.roadmap, topicsDone);
      $('#import-msg').textContent = `Loaded: ${countDone(problems)} solved problems, ${Object.keys(topicsDone).length} topics.`;
      route();
    } catch (err) { $('#import-msg').textContent = 'Could not load that file: ' + err.message; }
  });

  // ---------- router ----------
  function parseHash() {
    const h = location.hash.slice(1) || '/roadmap';
    const [path, query] = h.split('?');
    return { view: path.replace(/^\//, '').split('/')[0] || 'roadmap', params: new URLSearchParams(query || '') };
  }
  let currentView = '';
  function route() {
    const { params } = parseHash();
    const view = parseHash().view === 'practice' ? 'practice' : 'roadmap';
    $$('.tabs a').forEach(a => a.classList.toggle('active', a.dataset.view === view));
    updateTop();
    currentView = view;
    if (view === 'practice') showPractice(params); else showRoadmap(params);
    onScroll();
  }
  window.addEventListener('hashchange', route);

  function scrollToAnchor(a) {
    if (!a) { window.scrollTo(0, 0); return; }
    const el = document.getElementById(a);
    if (el) {
      for (let d = el.closest('details'); d; d = d.parentElement?.closest('details')) d.open = true;
      el.scrollIntoView({ block: 'start' }); el.classList.add('flash'); setTimeout(() => el.classList.remove('flash'), 1700); }
  }

  // ================= ROADMAP =================
  function phaseProblems(sec) { return sec.topics.flatMap(t => byTopic[t.id] || []); }

  function showRoadmap(params) {
    document.title = 'Roadmap · Java + DSA Study';
    const tDone = allTopicIds.filter(id => topicsDone[id]).length;
    side.innerHTML = `<div class="side-title">Roadmap</div>` + roadmapSecs.map(sec => {
      if (sec.kind !== 'topics') return `<a class="side-link" href="#/roadmap?s=${sec.anchor}">${esc(sec.title)}</a>`;
      const n = sec.topics.length, d = sec.topics.filter(t => topicsDone[t.id]).length;
      return `<a class="side-link" href="#/roadmap?s=${sec.anchor}">${esc(sec.title)}<span class="count">${d}/${n}</span>
        <div class="bar"><span style="width:${n ? d / n * 100 : 0}%"></span></div></a>`;
    }).join('');

    const [titleMain, titleSub] = S.roadmap.title.split(/\s+—\s+/);
    const next = allTopicIds.find(id => !topicsDone[id]);
    const nextInfo = next && topicInfo[next];
    const phasesDone = topicSecs.filter(s => s.topics.every(t => topicsDone[t.id])).length;
    const solved = countDone(problems);
    let html = `<section class="hero">
        <p class="eyebrow">Your study path</p>
        <h1>${esc(titleMain)}</h1>
        ${titleSub ? `<p>${esc(titleSub)}: the playlist videos, the extra topics and the practice problems, each placed right after what it depends on.</p>` : ''}
        <div class="row-btns">
          ${next ? `<a class="btn solid" href="#/roadmap?s=t-${encodeURIComponent(next)}">${ICON.arrow} Continue with ${esc(next)}: ${esc(plainTopic(nextInfo.sec, nextInfo.t).slice(0, 42))}</a>` : ''}
          <a class="btn" href="#/practice">${ICON.code} Practice</a>
          <a class="btn" href="${S.repo}" target="_blank" rel="noopener">${ICON.book} Notes on GitHub</a>
        </div>
      </section>
      <div class="stats">
        <div class="stat">${ring(tDone, allTopicIds.length)}<div><b>${tDone} / ${allTopicIds.length}</b><span>roadmap topics done</span></div></div>
        <div class="stat">${ring(solved, problems.length, 'good')}<div><b>${solved} / ${problems.length}</b><span>practice problems solved</span></div></div>
        <div class="stat">${ring(phasesDone, topicSecs.length, 'warn')}<div><b>${phasesDone} / ${topicSecs.length}</b><span>phases and tracks finished · ${problems.filter(isRevisit).length} ★ to revisit</span></div></div>
      </div>
      <details class="fold" id="rm-intro"><summary>How to use this roadmap <small>· legend, tips and the practice sheet</small></summary>
        <div class="fold-body md">${mdBlock(S.roadmap.intro)}</div></details>`;
    for (const sec of roadmapSecs) {
      if (sec.kind === 'text') {
        const open = /at a glance/i.test(sec.title) ? ' open' : '';
        html += `<details class="fold" id="${sec.anchor}"${open}><summary>${esc(sec.title)}</summary><div class="fold-body md">${mdBlock(sec.md)}</div></details>`;
        continue;
      }
      const pp = phaseProblems(sec);
      const n = sec.topics.length, d = sec.topics.filter(t => topicsDone[t.id]).length;
      const extraCols = sec.headers.filter(h => !['Order', 'Topic', 'Source', 'Priority'].includes(h));
      const shortTitle = sec.title.replace(/^Phase \d+:\s*/, '').replace(/^(Parallel )?Track:\s*/, '');
      const badge = sec.num || (sec.title.startsWith('Parallel') ? 'J' : 'S');
      html += `<section class="card phase" id="${sec.anchor}">
        <div class="phase-head">
          <div class="pnum${sec.num ? '' : ' alt'}">${esc(badge)}</div>
          <div><h2>${esc(shortTitle)}</h2>
            <div class="phase-meta"><span>${esc(sec.num ? 'Phase ' + sec.num : sec.title.split(':')[0])}</span>
              <span data-ph-count="${sec.anchor}">${d} / ${n} topics</span>
              ${pp.length ? `<a href="#/practice?phase=${sec.anchor}">${countDone(pp)} / ${pp.length} problems →</a>` : ''}</div></div>
          <div class="phase-prog"><span class="pct" data-ph-pct="${sec.anchor}">${pct(d, n)}%</span><div class="bar"><span data-ph-bar="${sec.anchor}" style="width:${pct(d, n)}%"></span></div></div>
        </div>
        <div class="phase-body">
        ${sec.before ? `<div class="md">${mdBlock(sec.before)}</div>` : ''}
        <div class="topics">` + sec.topics.map(t => {
        const list = byTopic[t.id] || [];
        const dd = countDone(list);
        const meta = extraCols.map(h => cellOf(sec, t, h) ? `<div class="tmeta"><b>${esc(h)}:</b> ${mdInline(cellOf(sec, t, h))}</div>` : '').join('');
        return `<div class="topic${topicsDone[t.id] ? ' done' : ''}" id="t-${esc(t.id)}">
          <input type="checkbox" class="ck" data-topic="${esc(t.id)}" ${topicsDone[t.id] ? 'checked' : ''} aria-label="Mark topic ${esc(t.id)} done">
          <span class="tid">${esc(t.id)}</span>
          <div class="tbody"><div class="ttitle">${mdInline(topicTitleMd(sec, t))}</div>
            <div class="tsrc">${mdInline(cellOf(sec, t, 'Source'))}</div>${meta}</div>
          <div class="tright">${list.length ? `<a class="chip${dd === list.length ? ' full' : ''}" href="#/practice?topic=${encodeURIComponent(t.id)}">${ICON.code}${dd}/${list.length}</a>` : ''}</div>
        </div>`;
      }).join('') + `</div>${sec.after ? `<div class="md">${mdBlock(sec.after)}</div>` : ''}</div></section>`;
    }
    main.innerHTML = html;
    decorate(main, ROADMAP_PATH);
    $$('input[data-topic]', main).forEach(cb => cb.addEventListener('change', () => {
      const id = cb.dataset.topic;
      if (cb.checked) topicsDone[id] = today(); else delete topicsDone[id];
      save(KEYS.roadmap, topicsDone);
      cb.closest('.topic').classList.toggle('done', cb.checked);
      updateTop();
      const sec = topicSecs.find(s => s.topics.some(t => t.id === id));
      const n = sec.topics.length, d = sec.topics.filter(t => topicsDone[t.id]).length;
      $(`[data-ph-count="${sec.anchor}"]`, main).textContent = `${d} / ${n} topics`;
      $(`[data-ph-pct="${sec.anchor}"]`, main).textContent = `${pct(d, n)}%`;
      $(`[data-ph-bar="${sec.anchor}"]`, main).style.width = `${pct(d, n)}%`;
      const link = $(`.side-link[href="#/roadmap?s=${sec.anchor}"]`, side);
      if (link) { $('.count', link).textContent = `${d}/${n}`; $('.bar span', link).style.width = `${d / n * 100}%`; }
    }));
    const target = params.get('s');
    if (target) scrollToAnchor(target); else window.scrollTo(0, 0);
  }

  // ================= PRACTICE =================
  function matches(p) {
    if (ui.q) {
      const hay = `${p.name} ${p.slug} ${p.lc ? 'lc ' + p.lc.n : ''} ${sections[p.section].title}`.toLowerCase();
      if (!ui.q.toLowerCase().split(/\s+/).every(w => hay.includes(w))) return false;
    }
    if (ui.diff !== 'all') {
      const d = p.lc ? p.lc.diff : 'Sheet';
      if (d !== ui.diff) return false;
    }
    if (ui.status === 'todo' && isDone(p)) return false;
    if (ui.status === 'done' && !isDone(p)) return false;
    if (ui.status === 'revisit' && !isRevisit(p)) return false;
    return true;
  }

  // groups: [{id, title, sub, topics: [{id, title, notes:[sections], list}]}]
  function buildGroups(filterFn) {
    const groups = [];
    if (ui.group === 'roadmap') {
      for (const sec of topicSecs) {
        const topics = [];
        for (const t of sec.topics) {
          const all = byTopic[t.id] || [];
          if (!all.length) continue;
          const list = all.filter(filterFn);
          topics.push({ id: 't-' + t.id, label: t.id, title: plainTopic(sec, t), prio: t.priority, all, list,
            secs: [...new Set(all.map(p => p.section))].map(id => sections[id]) });
        }
        if (topics.length) groups.push({ id: sec.anchor, title: sec.title, topics });
      }
    } else {
      const parts = [];
      for (const s of S.practice.sections) {
        let g = parts.find(x => x.title === s.part);
        if (!g) { g = { id: 'part-' + slugify(s.part), title: s.part, topics: [] }; parts.push(g); }
        const all = problems.filter(p => p.section === s.id);
        g.topics.push({ id: 's-' + s.id, label: '', title: s.title, prio: '', all, list: all.filter(filterFn), secs: [s], pattern: true });
      }
      groups.push(...parts);
    }
    return groups;
  }

  function badgesHtml(p) {
    const b = [];
    if (p.lc) {
      b.push(`<a class="lc" href="https://leetcode.com/problems/${esc(p.lc.slug)}/" target="_blank" rel="noopener" title="Open on LeetCode">LC ${p.lc.n}${p.lc.paid ? ' 🔒' : ''}</a>`);
      b.push(`<span class="badge d-${p.lc.diff}">${p.lc.diff}</span>`);
    } else {
      b.push(`<span class="badge d-sheet" title="No exact LeetCode page: described below">Sheet · ${esc(p.level)}</span>`);
    }
    if (p.notesLc && p.notesLc.length) b.push(ghLink(p.notesLc[0], '📘', 'nb'));
    return b.join('');
  }
  function detailHtml(p) {
    const parts = [];
    if (p.desc) parts.push(`<p>${esc(p.desc)}</p>`);
    if (p.note) parts.push(`<div class="note">⚠️ ${esc(p.note)}</div>`);
    if (p.cases) parts.push(`<table class="cases"><thead><tr><th>Input</th><th>Expected output</th></tr></thead><tbody>${p.cases.map(([i, o]) => `<tr><td>${esc(i)}</td><td>${esc(o)}</td></tr>`).join('')}</tbody></table>`);
    const links = [];
    if (p.lc) links.push(`<a class="primary" href="https://leetcode.com/problems/${esc(p.lc.slug)}/" target="_blank" rel="noopener">${ICON.code} Solve on LeetCode · LC ${p.lc.n}</a>`);
    if (p.related) links.push(`<a href="https://leetcode.com/problems/${esc(p.related.slug)}/" target="_blank" rel="noopener">Related: LC ${p.related.n}${p.related.paid ? ' 🔒' : ''} (${p.related.diff})</a>`);
    links.push(`<a href="${esc(p.tuf)}" target="_blank" rel="noopener">${ICON.ext} Sheet statement</a>`);
    (p.notesLc || []).forEach(f => links.push(ghLink(f, `📘 ${esc(notesByPath[f]?.title || f)}`)));
    parts.push(`<div class="links">${links.join('')}</div>`);
    const st = progress[p.slug];
    if (st?.d) parts.push(`<p class="solved-on">✓ Solved on ${esc(st.d)}${st.r ? ' · marked to revisit' : ''}</p>`);
    else if (st?.r) parts.push(`<p class="solved-on" style="color:var(--warn)">★ Marked to revisit</p>`);
    return parts.join('');
  }
  function probHtml(p) {
    const done = isDone(p), rev = isRevisit(p);
    return `<div class="prob${done ? ' done' : ''}" data-slug="${esc(p.slug)}">
      <div class="prow">
        <input type="checkbox" class="ck" ${done ? 'checked' : ''} aria-label="Solved: ${esc(p.name)}">
        <button class="pname" type="button">${esc(p.name)}</button>
        <span class="badges">${badgesHtml(p)}</span>
        <button class="star${rev ? ' on' : ''}" type="button" title="Revisit later" aria-label="Revisit ${esc(p.name)} later">${rev ? '★' : '☆'}</button>
      </div>
      <div class="pdetail"></div></div>`;
  }

  function showPractice(params) {
    document.title = 'Practice · Java + DSA Study';
    if (params.get('topic') || params.get('phase') || params.get('section')) {
      // arriving from a link: make sure the target is visible
      ui.q = ''; ui.status = 'all'; ui.diff = 'all';
      if (params.get('section')) ui.group = 'pattern';
      else ui.group = 'roadmap';
    }
    main.innerHTML = `<p class="eyebrow">Practice</p><h1>${problems.length} problems, in roadmap order</h1>
      <p class="lead" style="margin-bottom:20px">From ${esc(S.practice.source.split(',')[0])}. Tick a problem when it's solved, ☆ to come back to it, and click a name for details.
        Sheet-only problems come with their own description and test cases.</p>
      <div class="diffbars" id="diffbars"></div>
      <div class="toolbar">
        <label class="search">${ICON.search}<input type="search" id="q" placeholder="Search name, LC number or pattern…" value="${esc(ui.q)}" aria-label="Search problems"><kbd>/</kbd></label>
        <div class="seg" id="group-seg" role="group" aria-label="Group by">
          <button data-v="roadmap">By roadmap</button><button data-v="pattern">By pattern</button></div>
        <select id="diff" aria-label="Difficulty"><option value="all">All levels</option><option>Easy</option><option>Medium</option><option>Hard</option><option value="Sheet">Sheet-only</option></select>
        <select id="status" aria-label="Status"><option value="all">Any status</option><option value="todo">Not solved</option><option value="done">Solved</option><option value="revisit">To revisit ★</option></select>
        <button class="btn" id="random-btn" title="Open a random unsolved problem from the current list">${ICON.dice} Random</button>
      </div>
      <p class="result-count" id="result-count"></p>
      <div id="plist-root"></div>`;
    $('#diff').value = ui.diff; $('#status').value = ui.status;
    const refresh = () => { saveUi(); renderPracticeList(); };
    $('#q').addEventListener('input', e => { ui.q = e.target.value.trim(); renderPracticeList(); });
    $('#diff').addEventListener('change', e => { ui.diff = e.target.value; refresh(); });
    $('#status').addEventListener('change', e => { ui.status = e.target.value; refresh(); });
    $$('#group-seg button').forEach(b => b.addEventListener('click', () => { ui.group = b.dataset.v; refresh(); }));
    $('#random-btn').addEventListener('click', () => {
      const pool = $$('.prob', main).filter(el => !el.classList.contains('done'));
      if (!pool.length) return;
      const el = pool[Math.floor(Math.random() * pool.length)];
      openProb(el, true);
      el.scrollIntoView({ block: 'center' }); el.classList.add('flash');
    });
    renderPracticeList();
    const target = params.get('topic') ? 't-' + params.get('topic') : params.get('phase') || (params.get('section') ? 's-' + params.get('section') : '');
    if (target) scrollToAnchor(target); else window.scrollTo(0, 0);
  }

  function renderDiffbars() {
    const lvl = p => p.lc ? p.lc.diff : 'Sheet';
    const dbar = (cls, label, key) => {
      const all = problems.filter(p => lvl(p) === key), d = countDone(all);
      return `<div class="dbar ${cls}"><div class="top">${label}<b>${d}<span class="muted" style="font-size:12px;font-weight:500"> / ${all.length}</span></b></div><div class="bar"><span style="width:${pct(d, all.length)}%"></span></div></div>`;
    };
    $('#diffbars').innerHTML = dbar('e', 'Easy', 'Easy') + dbar('m', 'Medium', 'Medium') + dbar('h', 'Hard', 'Hard') + dbar('s', 'Sheet-only', 'Sheet');
  }

  function renderPracticeList() {
    renderDiffbars();
    $$('#group-seg button').forEach(b => b.classList.toggle('on', b.dataset.v === ui.group));
    const groups = buildGroups(matches);
    let shown = 0;
    let html = '';
    for (const g of groups) {
      const gAll = g.topics.flatMap(t => t.all), gList = g.topics.flatMap(t => t.list);
      if (!gList.length) continue;
      html += `<h2 class="group-h" id="${esc(g.id)}">${esc(g.title)}</h2><p class="group-sub">${countDone(gAll)} / ${gAll.length} solved · ${pct(countDone(gAll), gAll.length)}%</p>`;
      for (const t of g.topics) {
        if (!t.list.length) continue;
        shown += t.list.length;
        html += `<h3 class="topic-h" id="${esc(t.id)}">${t.label ? `<span class="tid">${esc(t.label)}</span>` : ''}${esc(t.title)} ${t.prio}
          <small class="muted">${countDone(t.all)}/${t.all.length}</small></h3>`;
        for (const s of t.secs) {
          html += `<div class="spot">${t.pattern ? '' : `<b>${esc(s.title)}.</b> `}${esc(s.spot)}<br>📘 ${s.notes.map(f => ghLink(f, esc(notesByPath[f]?.title || f))).join(' · ')}</div>`;
        }
        html += `<div class="plist">${t.list.map(probHtml).join('')}</div>`;
      }
    }
    $('#plist-root').innerHTML = shown ? html : `<div class="empty"><b>No problems match</b>Try a different search, level or status.</div>`;
    const filtered = ui.q || ui.diff !== 'all' || ui.status !== 'all';
    $('#result-count').textContent = filtered ? `Showing ${shown} of ${problems.length} problems` : '';
    $$('.prob', main).forEach(el => { if (ui.open[el.dataset.slug]) openProb(el, true); });
    // sidebar
    side.innerHTML = `<div class="side-title">${ui.group === 'roadmap' ? 'Roadmap phases' : 'Pattern parts'}</div>` + groups.map(g => {
      const all = g.topics.flatMap(t => t.all), d = countDone(all);
      const subs = g.topics.map(t => `<a class="side-link lvl3" href="#/practice?${ui.group === 'roadmap' ? 'topic=' + encodeURIComponent(t.label) : 'section=' + t.id.slice(2)}">${t.label ? esc(t.label) + ' ' : ''}${esc(t.title)}<span class="count">${countDone(t.all)}/${t.all.length}</span></a>`).join('');
      return `<a class="side-link" href="#/practice?${ui.group === 'roadmap' ? 'phase=' + g.id : 'section=' + g.topics[0].id.slice(2)}">${esc(g.title)}<span class="count">${d}/${all.length}</span>
        <div class="bar"><span style="width:${all.length ? d / all.length * 100 : 0}%"></span></div></a>${subs}`;
    }).join('');
  }

  function openProb(el, open) {
    const p = problems.find(x => x.slug === el.dataset.slug);
    if (open && !$('.pdetail', el).innerHTML) $('.pdetail', el).innerHTML = detailHtml(p);
    el.classList.toggle('open', open);
    if (open) ui.open[p.slug] = 1; else delete ui.open[p.slug];
  }
  function onPracticeClick(e) {
    const el = e.target.closest('.prob');
    if (!el) return;
    if (e.target.closest('.pname')) openProb(el, !el.classList.contains('open'));
    else if (e.target.closest('.star')) {
      const p = problems.find(x => x.slug === el.dataset.slug);
      const st = progress[p.slug] || {};
      if (st.r) delete st.r; else st.r = 1;
      if (!st.s && !st.r) delete progress[p.slug]; else progress[p.slug] = st;
      save(KEYS.practice, progress);
      const b = e.target.closest('.star'); b.classList.toggle('on', !!st.r); b.textContent = st.r ? '★' : '☆';
      $('.pdetail', el).innerHTML = el.classList.contains('open') ? detailHtml(p) : '';
    }
  }
  function onPracticeChange(e) {
    const cb = e.target;
    const el = cb.closest('.prob');
    if (!el || cb.type !== 'checkbox') return;
    const slug = el.dataset.slug;
    const st = progress[slug] || {};
    if (cb.checked) { st.s = 'done'; st.d = today(); } else { delete st.s; delete st.d; }
    if (!st.s && !st.r) delete progress[slug]; else progress[slug] = st;
    save(KEYS.practice, progress);
    el.classList.toggle('done', cb.checked);
    if (el.classList.contains('open')) $('.pdetail', el).innerHTML = detailHtml(problems.find(x => x.slug === slug));
    updateTop();
    // refresh counters without losing the scroll position
    const y = window.scrollY; renderPracticeList(); window.scrollTo(0, y);
  }

  // ---------- back-to-top button ----------
  const toTop = document.createElement('button');
  toTop.className = 'icon-btn to-top'; toTop.type = 'button'; toTop.setAttribute('aria-label', 'Back to top'); toTop.innerHTML = ICON.up;
  toTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  document.body.appendChild(toTop);
  function onScroll() { toTop.classList.toggle('show', scrollY > 900); }
  window.addEventListener('scroll', onScroll, { passive: true });

  // "/" jumps to the search box, Escape leaves it
  document.addEventListener('keydown', e => {
    const box = $('#q');
    if (e.key === '/' && box && !/^(INPUT|SELECT|TEXTAREA)$/.test(document.activeElement?.tagName) && !e.ctrlKey && !e.metaKey) {
      e.preventDefault(); box.focus(); box.select();
    } else if (e.key === 'Escape' && document.activeElement === box) box.blur();
  });

  main.addEventListener('click', e => { if (currentView === 'practice') onPracticeClick(e); });
  main.addEventListener('change', e => { if (currentView === 'practice') onPracticeChange(e); });

  if (!S) { main.innerHTML = '<p class="pad">web/data.js is missing: run <code>python tools/build_site.py</code>.</p>'; return; }
  if (!hasMarked) console.warn('marked did not load (offline?): markdown is shown as plain text');
  // the router scrolls itself; stop the browser restoring the old position after a reload
  try { history.scrollRestoration = 'manual'; } catch (e) { /* not supported */ }
  route();
})();
