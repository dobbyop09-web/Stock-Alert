(() => {
  'use strict';

  // Heatmap is intentionally excluded.
  const page = (location.pathname.split('/').pop() || 'index.html').toLowerCase();
  if (page === 'heatmap.html') return;

  const state = {
    query: '',
    matches: [],
    active: 0,
    popupOpen: false
  };

  const SKIP = new Set(['SCRIPT', 'STYLE', 'NOSCRIPT', 'SVG', 'PATH']);

  function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[c]);
  }

  function normalize(value) {
    return String(value ?? '').replace(/\s+/g, ' ').trim();
  }

  function looksLikeSymbol(value) {
    const v = normalize(value).toUpperCase();
    if (!v || v.length < 2 || v.length > 25) return false;
    if (!/^[A-Z][A-Z0-9.&_-]*$/.test(v)) return false;
    // Prevent ordinary UI labels from being interpreted as symbols.
    const stop = new Set([
      'ALL', 'TOTAL', 'CURRENT', 'ALERT', 'STATUS', 'SECTOR', 'SYMBOL', 'PRICE',
      'WATCH', 'NEAR', 'TRIGGERED', 'MARKET', 'CAP', 'DATE', 'BUY', 'SELL', 'PROFIT',
      'LOSS', 'RETURN', 'VALUE', 'QUANTITY', 'DAYS', 'EDIT', 'SEARCH', 'PORTFOLIO',
      'ACTIVE', 'CANCELLED', 'CANCELED', 'TRIGGERED', 'GTT', 'ORDER', 'ORDERS'
    ]);
    return !stop.has(v);
  }

  function getSearchHost() {
    return document.querySelector('.app-main, .main') || document.body;
  }

  function buildUi() {
    if (document.getElementById('universalSearchRoot')) return;

    const root = document.createElement('div');
    root.id = 'universalSearchRoot';
    root.innerHTML = `
      <div class="us-search" role="search" aria-label="Universal search">
        <span class="us-search-icon">⌕</span>
        <input id="universalSearchInput" autocomplete="off" spellcheck="false"
               placeholder="Type a symbol…" aria-label="Search symbol on this page">
        <span class="us-count" id="universalSearchCount"></span>
        <button type="button" class="us-icon-btn" id="universalPrev" aria-label="Previous result">↑</button>
        <button type="button" class="us-icon-btn" id="universalNext" aria-label="Next result">↓</button>
        <button type="button" class="us-close" id="universalSearchClose" aria-label="Close search">×</button>
      </div>
      <div class="us-popup" id="universalPopup" hidden>
        <div class="us-popup-head">
          <div>
            <div class="us-popup-kicker">PAGE INFORMATION</div>
            <div class="us-popup-title" id="universalPopupTitle">—</div>
          </div>
          <div class="us-popup-page" id="universalPopupPage"></div>
        </div>
        <div class="us-popup-body" id="universalPopupBody"></div>
      </div>
    `;

    document.body.appendChild(root);

    const input = root.querySelector('#universalSearchInput');
    const close = root.querySelector('#universalSearchClose');
    const next = root.querySelector('#universalNext');
    const prev = root.querySelector('#universalPrev');

    input.addEventListener('input', () => {
      state.query = input.value.trim();
      runSearch();
    });

    input.addEventListener('keydown', e => {
      if (e.key === 'Enter') {
        e.preventDefault();
        move(e.shiftKey ? -1 : 1);
      } else if (e.key === 'Escape') {
        e.preventDefault();
        closeSearch();
      }
    });

    close.addEventListener('click', closeSearch);
    next.addEventListener('click', () => move(1));
    prev.addEventListener('click', () => move(-1));

    root.querySelector('#universalPopup').addEventListener('click', e => {
      const btn = e.target.closest('[data-us-match]');
      if (!btn) return;
      const idx = Number(btn.dataset.usMatch);
      if (!Number.isInteger(idx)) return;
      state.active = idx;
      selectMatch();
    });

    // Close popup/search when clicking outside the universal-search UI.
    document.addEventListener('mousedown', e => {
      const rootEl = document.getElementById('universalSearchRoot');
      if (!rootEl || !rootEl.contains(e.target)) {
        // Keep results available, but hide the popup to avoid blocking the page.
        hidePopup();
      }
    });

    // Re-run when the page changes dynamically (tables are rendered after load).
    const observer = new MutationObserver(() => {
      if (state.query) runSearch(false);
    });
    observer.observe(getSearchHost(), { childList: true, subtree: true });
  }

  function openSearch(seed = '') {
    buildUi();
    const root = document.getElementById('universalSearchRoot');
    const input = document.getElementById('universalSearchInput');
    root.classList.add('is-open');
    input.value = seed;
    input.focus();
    input.setSelectionRange(input.value.length, input.value.length);
    state.query = seed;
    runSearch();
  }

  function closeSearch() {
    const root = document.getElementById('universalSearchRoot');
    if (!root) return;
    root.classList.remove('is-open');
    hidePopup();
    const input = document.getElementById('universalSearchInput');
    if (input) input.value = '';
    state.query = '';
    state.matches = [];
    state.active = 0;
    clearSelected();
  }

  function hidePopup() {
    const popup = document.getElementById('universalPopup');
    if (popup) popup.hidden = true;
    state.popupOpen = false;
  }

  function clearSelected() {
    document.querySelectorAll('.us-row-match-active').forEach(el => el.classList.remove('us-row-match-active'));
    document.querySelectorAll('.us-row-match').forEach(el => el.classList.remove('us-row-match'));
  }

  function collectCandidateRows() {
    // Tables are the primary data source across the dashboard pages.
    const rows = [...document.querySelectorAll('table tbody tr')].filter(tr =>
      normalize(tr.textContent) && !tr.closest('#universalSearchRoot')
    );

    // Some pages use non-table cards/rows. Include likely stock containers as a fallback.
    const fallback = [...document.querySelectorAll('[data-symbol], .holding-row, .trade-row, .gtt-row, .stock-row')]
      .filter(el => !el.closest('#universalSearchRoot'));

    const result = [...rows];
    fallback.forEach(el => {
      if (!result.includes(el)) result.push(el);
    });
    return result;
  }

  function candidateSymbols(text) {
    return normalize(text)
      .split(/[|•·\n\t,/:()]+/)
      .map(s => normalize(s))
      .filter(looksLikeSymbol);
  }

  function rowMatches(row, query) {
    const q = query.toUpperCase();
    const text = normalize(row.textContent).toUpperCase();
    if (!text.includes(q)) return false;

    const symbols = candidateSymbols(row.textContent);
    const exactSymbol = symbols.some(s => s === q);
    return exactSymbol || text.includes(q);
  }

  function titleForRow(row, query) {
    const q = query.toUpperCase();
    const symbols = candidateSymbols(row.textContent);
    const exact = symbols.find(s => s === q);
    if (exact) return exact;

    const explicit = row.querySelector('[data-symbol], .stock-link, .sym-avatar + strong, strong');
    if (explicit && looksLikeSymbol(explicit.textContent)) return normalize(explicit.textContent).toUpperCase();
    return q;
  }

  function extractCells(row) {
    const cells = [...row.querySelectorAll(':scope > td, :scope > th')];
    if (cells.length) {
      return cells.map(cell => normalize(cell.innerText || cell.textContent)).filter(Boolean);
    }

    return normalize(row.innerText || row.textContent)
      .split(/\n+/)
      .map(normalize)
      .filter(Boolean);
  }

  function nearbyContext(row) {
    const parent = row.closest('.table-panel, .trades-panel, .table-scroll, .trades-scroll, .panel') || row.parentElement;
    if (!parent) return '';
    const heading = parent.querySelector('.panel-title, .trades-title, .table-toolbar-title, h2, h3');
    return heading ? normalize(heading.textContent) : '';
  }

  function getHeaders(row) {
    const table = row.closest('table');
    if (!table) return [];
    const header = table.querySelector('thead tr');
    if (!header) return [];
    return [...header.querySelectorAll('th')].map(th => normalize(th.innerText || th.textContent));
  }

  function renderPopup() {
    const popup = document.getElementById('universalPopup');
    const body = document.getElementById('universalPopupBody');
    const title = document.getElementById('universalPopupTitle');
    const pageEl = document.getElementById('universalPopupPage');
    if (!popup || !body || !title) return;

    if (!state.matches.length) {
      title.textContent = state.query.toUpperCase() || 'Search';
      pageEl.textContent = '';
      body.innerHTML = `<div class="us-empty">No matching symbol/data found on this page.</div>`;
      popup.hidden = false;
      state.popupOpen = true;
      return;
    }

    const current = state.matches[state.active];
    const symbol = titleForRow(current, state.query);
    title.textContent = symbol;
    pageEl.textContent = page.replace(/\.html$/, '').replace(/[-_]/g, ' ');

    const headers = getHeaders(current);
    const values = extractCells(current);
    const context = nearbyContext(current);
    const fields = [];

    values.forEach((value, i) => {
      if (!value) return;
      const label = headers[i] || `Information ${i + 1}`;
      if (/^(edit|delete|action)$/i.test(label)) return;
      // Skip the symbol itself as a field; it is already shown in the title.
      if (value.toUpperCase() === symbol.toUpperCase()) return;
      fields.push(`<div class="us-field"><div class="us-label">${esc(label)}</div><div class="us-value">${esc(value)}</div></div>`);
    });

    const matchesHtml = state.matches.length > 1
      ? `<div class="us-matches"><div class="us-matches-title">Matches on this page</div>${state.matches.map((r, i) => `
          <button class="us-match-btn ${i === state.active ? 'active' : ''}" data-us-match="${i}">
            <span>${esc(titleForRow(r, state.query))}</span><span>${i + 1}</span>
          </button>`).join('')}</div>`
      : '';

    body.innerHTML = `
      ${context ? `<div class="us-context">${esc(context)}</div>` : ''}
      <div class="us-fields">${fields.join('')}</div>
      ${matchesHtml}
    `;

    popup.hidden = false;
    state.popupOpen = true;
  }

  function selectMatch() {
    if (!state.matches.length) return;
    clearSelected();
    const row = state.matches[state.active];
    row.classList.add('us-row-match');
    row.classList.add('us-row-match-active');
    row.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
    renderPopup();
  }

  function move(direction) {
    if (!state.matches.length) return;
    state.active = (state.active + direction + state.matches.length) % state.matches.length;
    selectMatch();
  }

  function runSearch(keepActive = true) {
    buildUi();
    const count = document.getElementById('universalSearchCount');
    const query = state.query.trim();

    clearSelected();

    if (!query) {
      state.matches = [];
      state.active = 0;
      if (count) count.textContent = '';
      hidePopup();
      return;
    }

    const rows = collectCandidateRows().filter(row => rowMatches(row, query));
    state.matches = rows;
    if (!keepActive || state.active >= rows.length) state.active = 0;

    if (count) count.textContent = rows.length ? `${state.active + 1} / ${rows.length}` : '0 results';

    if (rows.length) {
      selectMatch();
    } else {
      renderPopup();
    }
  }

  // Start universal search when the user simply begins typing anywhere on the page.
  document.addEventListener('keydown', e => {
    if (e.ctrlKey || e.metaKey || e.altKey) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        openSearch('');
      }
      return;
    }

    const target = e.target;
    const typingField = target && (
      target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT' ||
      target.isContentEditable || target.closest('input, textarea, select, [contenteditable="true"]')
    );

    if (typingField) return;
    if (e.key === 'Escape') {
      closeSearch();
      return;
    }

    if (e.key === '/' || e.key === '?') {
      e.preventDefault();
      openSearch('');
      return;
    }

    // Search starts from printable keys, including letters/numbers and a few stock-symbol characters.
    if (e.key.length === 1 && /[A-Za-z0-9.&_-]/.test(e.key)) {
      e.preventDefault();
      const existing = document.getElementById('universalSearchInput')?.value || '';
      openSearch(existing + e.key);
    }
  });

  // Expose a tiny public hook for pages that render new content after an async request.
  window.UniversalSearch = {
    open: openSearch,
    close: closeSearch,
    refresh: () => runSearch(false)
  };

  // Build immediately so the widget has its CSS/DOM ready before dynamic content arrives.
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', buildUi, { once: true });
  } else {
    buildUi();
  }
})();
