// Historical Alerts modal.
// Fetches the triggered-alert log from the worker (GET /alert-history).
//
// IMPORTANT: the history endpoint snapshots `currentPrice` at the time it
// logged the alert, so it goes stale the moment the page has moved since.
// The live dashboard (state.allRows, populated by data-loader.js from
// /dashboard-data) always has the freshest price per symbol, so every row
// here overrides currentPrice from state.allRows when a match is found and
// only falls back to the history payload's own value if the symbol isn't
// in the live sheet (e.g. it was removed from the watchlist).
//
// Expected response shape:
// {
//   "alerts": [
//     {
//       "date": "2026-09-17",
//       "triggeredAt": "2026-09-17T16:04:07.974488Z",
//       "symbol": "PHARMABEES",
//       "companyName": "Nippon India Mutual Fund - Nippon India Nifty Pharma ETF",
//       "sheet": "Etf",
//       "alertPrice": 29,
//       "triggerPrice": 26.68,
//       "currentPrice": 27.36,
//       "watchlist": "Etf",
//       "screenerUrl": "https://www.screener.in/company/PHARMABEES/consolidated"
//     }
//   ],
//   "lastUpdated": "2026-09-17T16:04:08.381338600Z"
// }

import { state } from "./state.js";

const BASE_URL = "https://tiny-art-8473.dobbyop09.workers.dev";
const HISTORY_ENDPOINT = `${BASE_URL}/alert-history`;

const ha = {
    all: [],           // raw alerts, currentPrice patched in from the live dashboard
    filtered: [],       // individual alerts after all filters
    statBase: [],       // alerts after every filter EXCEPT the status filter (feeds the stat cards)
    groupBase: [],      // alerts after every filter EXCEPT the group filter (feeds the group pills)
    clubs: [],          // filtered alerts clubbed per stock: [{ symbol, alerts[], latest }]
    selected: new Set(), // key = `${symbol}__${triggeredAt}`
    sortDesc: true,
    page: 1,
    perPage: 10,
    loaded: false,
    lastUpdated: null,
};

const keyOf = a => `${a.symbol}__${a.triggeredAt || a.date}`;

function fmtDateTimeShort(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso || "—";
    const date = d.toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric" });
    const time = d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit", hour12: true });
    return `<div>${date}</div><div style="color:var(--muted);font-size:11px">${time}</div>`;
}

function monthKey(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "";
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function monthLabel(key) {
    const [y, m] = key.split("-");
    return new Date(Number(y), Number(m) - 1, 1).toLocaleString(undefined, { month: "long", year: "numeric" });
}

function vsAlertPct(a) {
    if (!a.alertPrice || a.currentPrice === undefined || a.currentPrice === null) return null;
    return ((a.currentPrice - a.alertPrice) / a.alertPrice) * 100;
}

function sinceTriggerPct(a) {
    const base = a.triggerPrice ?? a.alertPrice;
    if (!base || a.currentPrice === undefined || a.currentPrice === null) return null;
    return ((a.currentPrice - base) / base) * 100;
}

const escAttr = s => String(s ?? "").replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;");
const symKey = a => String(a.symbol || "").toUpperCase();

function fmtDateTimeInline(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso || "—";
    const date = d.toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric" });
    const time = d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit", hour12: true });
    return `${date}, ${time}`;
}

function injectHaStyles() {
    if (document.getElementById("haExtraStyles")) return;
    const style = document.createElement("style");
    style.id = "haExtraStyles";
    style.textContent = `
        /* clickable stat cards */
        #haStats .stat { cursor: pointer; transition: transform .15s ease, box-shadow .15s ease, filter .15s ease; position: relative; }
        #haStats .stat:hover { transform: translateY(-2px); filter: brightness(1.12); }
        #haStats .stat.is-active { box-shadow: 0 0 0 2px #2dd4bf, 0 8px 24px rgba(45,212,191,.18); }
        #haStats .stat:focus-visible { outline: 2px solid #2dd4bf; outline-offset: 2px; }

        /* clickable group pills */
        #haGroupBreakdown .ha-group-badge { cursor: pointer; user-select: none; transition: background .15s ease, box-shadow .15s ease, opacity .15s ease; }
        #haGroupBreakdown .ha-group-badge:hover { filter: brightness(1.25); }
        #haGroupBreakdown .ha-group-badge.is-active { background: #2dd4bf; color: #04201c; box-shadow: 0 0 0 2px rgba(45,212,191,.35); }
        #haGroupBreakdown .ha-group-badge.is-active b { color: inherit; }
        #haGroupBreakdown.has-active .ha-group-badge:not(.is-active) { opacity: .55; }

        /* clubbed stock rows */
        .ha-sym-link { font-weight: 700; cursor: pointer; border-bottom: 1px dashed currentColor; background: none; border-top: 0; border-left: 0; border-right: 0; color: inherit; font: inherit; font-weight: 700; padding: 0; }
        .ha-sym-link:hover { color: #2dd4bf; }
        .ha-count-badge { display: inline-block; margin-left: 6px; padding: 1px 7px; border-radius: 999px; font-size: 10.5px; font-weight: 700; background: rgba(45,212,191,.16); color: #2dd4bf; border: 1px solid rgba(45,212,191,.35); cursor: pointer; vertical-align: middle; }
        .ha-count-badge:hover { background: rgba(45,212,191,.3); }
        tr.ha-club-row { cursor: default; }

        /* stock popup */
        .ha-sp-overlay { position: fixed; inset: 0; z-index: 100000; background: rgba(2,6,15,.7); display: flex; align-items: center; justify-content: center; padding: 20px; backdrop-filter: blur(3px); }
        .ha-sp-overlay[hidden] { display: none; }
        .ha-sp-modal { width: min(920px, 100%); max-height: 86vh; display: flex; flex-direction: column; background: var(--panel, #0f1b2d); color: var(--text, #e5edf7); border: 1px solid var(--border, rgba(148,163,184,.2)); border-radius: 16px; box-shadow: 0 30px 80px rgba(0,0,0,.6); overflow: hidden; }
        .ha-sp-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 18px 22px; border-bottom: 1px solid var(--border, rgba(148,163,184,.2)); }
        .ha-sp-title { font-size: 20px; font-weight: 800; letter-spacing: .3px; }
        .ha-sp-company { color: var(--muted, #8aa0bb); font-size: 12.5px; margin-top: 3px; }
        .ha-sp-head-actions { display: flex; gap: 8px; align-items: center; }
        .ha-sp-meta { display: flex; flex-wrap: wrap; gap: 10px; padding: 14px 22px 4px; }
        .ha-sp-chip { background: rgba(148,163,184,.08); border: 1px solid var(--border, rgba(148,163,184,.2)); border-radius: 10px; padding: 8px 12px; min-width: 110px; }
        .ha-sp-chip small { display: block; color: var(--muted, #8aa0bb); font-size: 10.5px; text-transform: uppercase; letter-spacing: .6px; margin-bottom: 2px; }
        .ha-sp-chip span { font-weight: 700; font-size: 14px; }
        .ha-sp-note { padding: 6px 22px 0; color: var(--muted, #8aa0bb); font-size: 11.5px; }
        .ha-sp-scroll { overflow: auto; padding: 10px 22px 20px; }
        .ha-sp-table { width: 100%; border-collapse: collapse; font-size: 13px; }
        .ha-sp-table th { text-align: left; color: var(--muted, #8aa0bb); font-size: 10.5px; font-weight: 600; letter-spacing: .8px; text-transform: uppercase; padding: 10px 8px; border-bottom: 1px solid var(--border, rgba(148,163,184,.2)); position: sticky; top: 0; background: var(--panel, #0f1b2d); }
        .ha-sp-table td { padding: 10px 8px; border-bottom: 1px solid rgba(148,163,184,.1); }
        .ha-sp-table .num, .ha-sp-table th.num { text-align: right; }
        .ha-sp-close { width: 34px; height: 34px; border-radius: 9px; border: 1px solid var(--border, rgba(148,163,184,.2)); background: transparent; color: inherit; cursor: pointer; font-size: 16px; }
        .ha-sp-close:hover { background: rgba(148,163,184,.12); }
    `;
    document.head.appendChild(style);
}

function ensureStockPopup() {
    let ov = document.getElementById("haStockOverlay");
    if (ov) return ov;
    ov = document.createElement("div");
    ov.id = "haStockOverlay";
    ov.className = "ha-sp-overlay";
    ov.hidden = true;
    ov.innerHTML = `<div class="ha-sp-modal" role="dialog" aria-modal="true"><div id="haStockBody" style="display:flex;flex-direction:column;min-height:0"></div></div>`;
    document.body.appendChild(ov);
    ov.addEventListener("click", e => { if (e.target === ov) closeStockPopup(); });
    return ov;
}

function closeStockPopup() {
    const ov = document.getElementById("haStockOverlay");
    if (ov) ov.hidden = true;
    ha.popupSymbol = null;
}

function openStockPopup(symbol) {
    const ov = ensureStockPopup();
    const body = document.getElementById("haStockBody");
    const sym = String(symbol).toUpperCase();
    const club = ha.clubs.find(c => c.symbol === sym);
    if (!club) { closeStockPopup(); return; }
    ha.popupSymbol = sym;

    const alerts = club.alerts; // newest first
    const totalForStock = ha.all.filter(a => symKey(a) === sym).length;
    const latest = alerts[0];
    const oldest = alerts[alerts.length - 1];
    const group = latest.watchlist || latest.sheet || "—";
    const cur = latest.currentPrice;

    const rowsHtml = alerts.map((a, i) => {
        const vs = vsAlertPct(a);
        const since = sinceTriggerPct(a);
        const above = (vs ?? 0) >= 0;
        const vsHtml = vs === null ? "—" :
            `<span class="ha-vs-badge ${above ? "up" : "down"}">${above ? "▲ +" : "▼ "}${vs.toFixed(2)}%<small>${above ? "Above Alert" : "Below Alert"}</small></span>`;
        const sinceHtml = since === null ? "—" :
            `<span class="change-pct ${since >= 0 ? "up" : "down"}">${since >= 0 ? "+" : ""}${since.toFixed(2)}%</span>`;
        return `<tr>
            <td>${alerts.length - i}</td>
            <td>${fmtDateTimeInline(a.triggeredAt || a.date)}</td>
            <td class="num">${a.alertPrice ?? "—"}</td>
            <td class="num">${a.triggerPrice ?? "—"}</td>
            <td class="num">${a.currentPrice ?? "—"}</td>
            <td class="num">${vsHtml}</td>
            <td class="num">${sinceHtml}</td>
        </tr>`;
    }).join("");

    body.innerHTML = `
        <div class="ha-sp-head">
            <div>
                <div class="ha-sp-title">${escAttr(latest.symbol)} <span class="ha-group-badge" style="font-size:11px;vertical-align:middle;margin-left:6px">${escAttr(group)}</span></div>
                <div class="ha-sp-company">${escAttr(latest.companyName || "")}</div>
            </div>
            <div class="ha-sp-head-actions">
                ${latest.screenerUrl ? `<button class="icon-btn" title="Open on Screener" id="haSpScreener">${EXTERNAL_ICON}</button>` : ""}
                <button class="ha-sp-close" id="haSpClose" aria-label="Close">✕</button>
            </div>
        </div>
        <div class="ha-sp-meta">
            <div class="ha-sp-chip"><small>Total alerts</small><span>${alerts.length}</span></div>
            <div class="ha-sp-chip"><small>Current price</small><span>${cur ?? "—"}</span></div>
            <div class="ha-sp-chip"><small>First alert</small><span>${fmtDateTimeInline(oldest.triggeredAt || oldest.date)}</span></div>
            <div class="ha-sp-chip"><small>Latest alert</small><span>${fmtDateTimeInline(latest.triggeredAt || latest.date)}</span></div>
        </div>
        ${totalForStock > alerts.length ? `<div class="ha-sp-note">Showing ${alerts.length} of ${totalForStock} alerts for this stock (your current filters are applied).</div>` : ""}
        <div class="ha-sp-scroll">
            <table class="ha-sp-table">
                <thead><tr>
                    <th>#</th><th>Date / Time</th><th class="num">Alert</th><th class="num">Trigger</th>
                    <th class="num">Current</th><th class="num">Vs Alert</th><th class="num">Since Trigger</th>
                </tr></thead>
                <tbody>${rowsHtml}</tbody>
            </table>
        </div>`;

    document.getElementById("haSpClose").addEventListener("click", closeStockPopup);
    const sc = document.getElementById("haSpScreener");
    if (sc) sc.addEventListener("click", () => window.open(latest.screenerUrl, "_blank"));
    ov.hidden = false;
}

function els() {
    return {
        overlay: document.getElementById("haOverlay"),
        openBtn: document.getElementById("historyAlertsBtn"),
        closeBtn: document.getElementById("haCloseBtn"),
        search: document.getElementById("haSearch"),
        month: document.getElementById("haMonth"),
        group: document.getElementById("haGroup"),
        status: document.getElementById("haStatus"),
        dateFrom: document.getElementById("haDateFrom"),
        dateTo: document.getElementById("haDateTo"),
        clearBtn: document.getElementById("haClearBtn"),
        deleteOlderBtn: document.getElementById("haDeleteOlderBtn"),
        deleteSelectedBtn: document.getElementById("haDeleteSelectedBtn"),
        stats: document.getElementById("haStats"),
        groupBreakdown: document.getElementById("haGroupBreakdown"),
        subline: document.getElementById("haSubline"),
        tbody: document.getElementById("haTbody"),
        empty: document.getElementById("haEmpty"),
        tableWrap: document.getElementById("haTableWrap"),
        showingLine: document.getElementById("haShowingLine"),
        perPage: document.getElementById("haPerPage"),
        pager: document.getElementById("haPager"),
        selectAll: document.getElementById("haSelectAll"),
    };
}

// ── Always use the live dashboard price for "current", never the stale
// value baked into the history payload. If a symbol isn't in the live
// sheet anymore, currentPrice is left undefined and rendered as "—".
function patchLivePrices(alerts) {
    const live = new Map((state.allRows || []).map(r => [String(r.symbol || "").toUpperCase(), r]));
    return alerts.map(a => {
        const liveRow = live.get(String(a.symbol || "").toUpperCase());
        const { currentPrice, ...rest } = a;
        return { ...rest, currentPrice: liveRow ? liveRow.currentPrice : undefined };
    });
}

function populateSelects() {
    const { month, group } = els();

    const monthKeys = [...new Set(ha.all.map(a => monthKey(a.triggeredAt || a.date)))].filter(Boolean).sort().reverse();
    month.innerHTML = `<option value="">All Months</option>` +
        monthKeys.map(k => `<option value="${k}">${monthLabel(k)}</option>`).join("");

    const groups = [...new Set(ha.all.map(a => a.watchlist || a.sheet).filter(Boolean))].sort();
    group.innerHTML = `<option value="">All Groups</option>` +
        groups.map(g => `<option value="${g}">${g}</option>`).join("");
}

function applyHaFilters() {
    const { search, month, group, status, dateFrom, dateTo } = els();
    const text = search.value.trim().toUpperCase();
    const m = month.value;
    const g = group.value;
    const st = status.value; // "", "above", "below", "recovered"
    const from = dateFrom.value ? new Date(dateFrom.value + "T00:00:00") : null;
    const to = dateTo.value ? new Date(dateTo.value + "T23:59:59") : null;

    const matches = (a, { skipStatus = false, skipGroup = false } = {}) => {
        const matchesText = text === "" ||
            (a.symbol || "").toUpperCase().includes(text) ||
            (a.companyName || "").toUpperCase().includes(text) ||
            (a.watchlist || a.sheet || "").toUpperCase().includes(text);
        const matchesMonth = m === "" || monthKey(a.triggeredAt || a.date) === m;
        const matchesGroup = skipGroup || g === "" || (a.watchlist || a.sheet) === g;
        let matchesStatus = true;
        if (!skipStatus && st !== "") {
            if (st === "recovered") {
                const since = sinceTriggerPct(a);
                matchesStatus = since !== null && since >= 0;
            } else {
                const vs = vsAlertPct(a) ?? 0;
                matchesStatus = st === "above" ? vs >= 0 : vs < 0;
            }
        }
        const d = new Date(a.triggeredAt || a.date);
        const matchesFrom = !from || d >= from;
        const matchesTo = !to || d <= to;
        return matchesText && matchesMonth && matchesGroup && matchesStatus && matchesFrom && matchesTo;
    };

    const time = a => new Date(a.triggeredAt || a.date).getTime();
    const sortFn = (x, y) => ha.sortDesc ? time(y) - time(x) : time(x) - time(y);

    ha.filtered = ha.all.filter(a => matches(a)).sort(sortFn);
    ha.statBase = ha.all.filter(a => matches(a, { skipStatus: true }));
    ha.groupBase = ha.all.filter(a => matches(a, { skipGroup: true }));

    // Club alerts per stock. Alerts inside a club are always newest-first;
    // the clubs themselves are ordered by their newest/oldest alert per sort direction.
    const map = new Map();
    ha.filtered.forEach(a => {
        const k = symKey(a);
        if (!map.has(k)) map.set(k, []);
        map.get(k).push(a);
    });
    ha.clubs = [...map.entries()].map(([symbol, alerts]) => {
        alerts.sort((x, y) => time(y) - time(x));
        return { symbol, alerts, latest: alerts[0] };
    });
    ha.clubs.sort((x, y) => ha.sortDesc ? time(y.latest) - time(x.latest) : time(x.latest) - time(y.latest));

    ha.page = 1;
    renderHaStats();
    renderHaTable();

    // keep an open stock popup in sync with filters / deletions
    if (ha.popupSymbol) openStockPopup(ha.popupSymbol);
}

function renderHaStats() {
    const { stats, groupBreakdown } = els();
    const rows = ha.statBase;
    const activeStatus = els().status.value;
    const activeGroup = els().group.value;

    const above = rows.filter(a => (vsAlertPct(a) ?? 0) >= 0).length;
    const below = rows.length - above;

    const recoveries = rows.map(sinceTriggerPct).filter(v => v !== null);
    const avgRecovery = recoveries.length ? recoveries.reduce((s, v) => s + v, 0) / recoveries.length : 0;

    const BELL = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 5-2 6-2 7h16c0-1-2-2-2-7" /><path d="M10.3 20a1.8 1.8 0 0 0 3.4 0" /></svg>';
    const UP = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7" /></svg>';
    const DOWN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12l7 7 7-7" /></svg>';
    const TREND = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 17l6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>';

    const card = (label, count, cls, icon, filterVal) => `
        <div class="stat stat-${cls}${activeStatus === filterVal ? " is-active" : ""}" data-status-filter="${filterVal}" role="button" tabindex="0" title="Click to filter">
            <div class="stat-body">
                <div class="n">${count}</div>
                <div class="l">${label}</div>
            </div>
            <div class="stat-icon">${icon}</div>
        </div>
    `;

    stats.innerHTML =
        card("Total Alerts", rows.length, "total", BELL, "") +
        card("Above Alert Now", above, "watch", UP, "above") +
        card("Below Alert Now", below, "triggered", DOWN, "below") +
        `<div class="stat stat-near${activeStatus === "recovered" ? " is-active" : ""}" data-status-filter="recovered" role="button" tabindex="0" title="Click to show alerts that have recovered since trigger">
            <div class="stat-body">
                <div class="n">${avgRecovery >= 0 ? "+" : ""}${avgRecovery.toFixed(2)}%</div>
                <div class="l">Avg Recovery Since Trigger</div>
            </div>
            <div class="stat-icon">${TREND}</div>
        </div>`;

    const groupCounts = {};
    ha.groupBase.forEach(a => {
        const g = a.watchlist || a.sheet || "—";
        groupCounts[g] = (groupCounts[g] || 0) + 1;
    });
    const groupEntries = Object.entries(groupCounts).sort((a, b) => b[1] - a[1]);
    groupBreakdown.classList.toggle("has-active", !!activeGroup);
    groupBreakdown.innerHTML = groupEntries.length
        ? `<div class="ha-gb-title">Group Breakdown</div><div class="ha-gb-pills">` +
          groupEntries.map(([g, c]) => `<span class="ha-group-badge${g === activeGroup ? " is-active" : ""}" data-group="${escAttr(g)}" role="button" tabindex="0" title="Click to filter by ${escAttr(g)}">${g} <b>${c}</b></span>`).join("") +
          `</div>`
        : `<div class="ha-gb-title">Group Breakdown</div><div class="ha-gb-pills" style="color:var(--muted);font-size:11.5px">No data</div>`;
}

function renderHaTable() {
    const { tbody, empty, tableWrap, showingLine, selectAll } = els();

    const total = ha.clubs.length;
    const totalPages = Math.max(1, Math.ceil(total / ha.perPage));
    ha.page = Math.min(ha.page, totalPages);

    const start = (ha.page - 1) * ha.perPage;
    const pageRows = ha.clubs.slice(start, start + ha.perPage);

    if (total === 0) {
        tableWrap.style.display = "none";
        empty.style.display = "block";
    } else {
        tableWrap.style.display = "block";
        empty.style.display = "none";
    }

    tbody.innerHTML = pageRows.map(rowHtml).join("");

    showingLine.textContent = total === 0
        ? "No alerts found"
        : `Showing ${start + 1}–${Math.min(start + ha.perPage, total)} of ${total} stock${total === 1 ? "" : "s"} (${ha.filtered.length} alert${ha.filtered.length === 1 ? "" : "s"})`;

    const clubSel = c => c.alerts.every(a => ha.selected.has(keyOf(a)));
    const clubAny = c => c.alerts.some(a => ha.selected.has(keyOf(a)));
    selectAll.checked = pageRows.length > 0 && pageRows.every(clubSel);
    selectAll.indeterminate = !selectAll.checked && pageRows.some(clubAny);

    tbody.querySelectorAll(".ha-row-check").forEach(cb => {
        cb.addEventListener("change", () => {
            const club = ha.clubs.find(c => c.symbol === cb.dataset.symbol);
            if (!club) return;
            club.alerts.forEach(a => {
                if (cb.checked) ha.selected.add(keyOf(a)); else ha.selected.delete(keyOf(a));
            });
            renderHaTable();
        });
    });

    renderPager(totalPages);
    updateDeleteSelectedBtn();
}

function updateDeleteSelectedBtn() {
    const { deleteSelectedBtn } = els();
    deleteSelectedBtn.disabled = ha.selected.size === 0;
    deleteSelectedBtn.innerHTML = ha.selected.size
        ? `🗑 Delete selected (${ha.selected.size} alert${ha.selected.size === 1 ? "" : "s"})`
        : `🗑 Delete selected`;
}

const EYE_ICON = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.5 12s3.5-5 9.5-5 9.5 5 9.5 5-3.5 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2.5" /></svg>';
const EXTERNAL_ICON = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /><path d="M15 3h6v6" /><path d="M10 14 21 3" /></svg>';

function rowHtml(club) {
    const a = club.latest;
    const n = club.alerts.length;
    const vs = vsAlertPct(a);
    const since = sinceTriggerPct(a);
    const above = (vs ?? 0) >= 0;

    const vsBadge = vs === null ? "—" : `
        <span class="ha-vs-badge ${above ? "up" : "down"}">
            ${above ? "▲" : "▼"} ${above ? "+" : ""}${vs.toFixed(2)}%
            <small>${above ? "Above Alert" : "Below Alert"}</small>
        </span>`;

    const sinceHtml = since === null ? "—" :
        `<span class="change-pct ${since >= 0 ? "up" : "down"}">${since >= 0 ? "+" : ""}${since.toFixed(2)}%</span>`;

    const group = a.watchlist || a.sheet || "—";
    const checked = club.alerts.every(x => ha.selected.has(keyOf(x))) ? "checked" : "";

    return `
        <tr class="ha-club-row">
            <td><input type="checkbox" class="ha-row-check" data-symbol="${escAttr(club.symbol)}" ${checked}></td>
            <td>${fmtDateTimeShort(a.triggeredAt || a.date)}</td>
            <td>
                <button type="button" class="ha-sym-link sym-text" data-symbol="${escAttr(club.symbol)}" title="View all ${n} alert${n === 1 ? "" : "s"} for ${escAttr(a.symbol)}">${a.symbol || "—"}</button>${n > 1 ? `<span class="ha-count-badge" data-symbol="${escAttr(club.symbol)}" title="${n} alerts — click to view">×${n}</span>` : ""}
            </td>
            <td class="ha-company" title="${(a.companyName || "").replace(/"/g, "&quot;")}">${a.companyName || "—"}</td>
            <td><span class="ha-group-badge">${group}</span></td>
            <td class="num">${a.alertPrice ?? "—"}</td>
            <td class="num">${a.triggerPrice ?? "—"}</td>
            <td class="num">${a.currentPrice ?? "—"}</td>
            <td class="num">${vsBadge}</td>
            <td class="num">${sinceHtml}</td>
            <td>
                <div class="ha-actions-cell">
                    <button class="icon-btn" title="View company" onclick="window.open('${a.screenerUrl || "#"}','_blank')">${EYE_ICON}</button>
                    <button class="icon-btn" title="Open on Screener" onclick="window.open('${a.screenerUrl || "#"}','_blank')">${EXTERNAL_ICON}</button>
                </div>
            </td>
        </tr>
    `;
}

function renderPager(totalPages) {
    const { pager } = els();
    if (totalPages <= 1) { pager.innerHTML = ""; return; }

    const btn = (label, page, opts = {}) => `
        <button ${opts.active ? 'class="active"' : ""} ${opts.disabled ? "disabled" : ""} data-page="${page}">${label}</button>
    `;

    let html = btn("‹", ha.page - 1, { disabled: ha.page === 1 });
    const pages = new Set([1, totalPages, ha.page, ha.page - 1, ha.page + 1]);
    let prev = 0;
    [...pages].filter(p => p >= 1 && p <= totalPages).sort((a, b) => a - b).forEach(p => {
        if (prev && p - prev > 1) html += `<span style="color:var(--muted);padding:0 2px">…</span>`;
        html += btn(p, p, { active: p === ha.page });
        prev = p;
    });
    html += btn("›", ha.page + 1, { disabled: ha.page === totalPages });
    pager.innerHTML = html;

    pager.querySelectorAll("button[data-page]").forEach(b => {
        b.addEventListener("click", () => {
            const p = Number(b.dataset.page);
            if (!p || p < 1) return;
            ha.page = p;
            renderHaTable();
        });
    });
}

// Best-effort deletes. The worker only documents a GET endpoint for this
// data, so these hide rows locally right away and fire a DELETE in the
// background in case the worker supports one — without blocking the UI.
function removeAlerts(keysToRemove) {
    ha.all = ha.all.filter(a => !keysToRemove.has(keyOf(a)));
    keysToRemove.forEach(k => ha.selected.delete(k));
    applyHaFilters();
    updateSubline();

    fetch(HISTORY_ENDPOINT, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ keys: [...keysToRemove] })
    }).catch(() => { /* endpoint may not support delete yet; ignore */ });
}

function deleteSelected() {
    if (ha.selected.size === 0) return;
    if (!confirm(`Remove ${ha.selected.size} selected alert(s)?`)) return;
    removeAlerts(new Set(ha.selected));
}

function deleteOlderAlerts() {
    const days = prompt("Delete alerts older than how many days?", "30");
    if (days === null) return;
    const n = Number(days);
    if (!Number.isFinite(n) || n < 0) return;

    const cutoff = Date.now() - n * 86400000;
    const toRemove = new Set(
        ha.all.filter(a => new Date(a.triggeredAt || a.date).getTime() < cutoff).map(keyOf)
    );
    if (toRemove.size === 0) {
        alert("No alerts older than that.");
        return;
    }
    if (!confirm(`Delete ${toRemove.size} alert(s) older than ${n} day(s)?`)) return;
    removeAlerts(toRemove);
}

async function loadHistoricalAlerts() {
    const { tbody } = els();
    tbody.innerHTML = `<tr><td colspan="11" style="text-align:center;color:var(--muted);padding:24px">Loading...</td></tr>`;

    try {
        const res = await fetch(`${HISTORY_ENDPOINT}?t=${Date.now()}`);
        const data = await res.json();
        const rawAlerts = Array.isArray(data.alerts) ? data.alerts : [];
        ha.all = patchLivePrices(rawAlerts);
        ha.lastUpdated = data.lastUpdated ? new Date(data.lastUpdated) : new Date();
        ha.loaded = true;

        populateSelects();
        applyHaFilters();
        updateSubline();
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="11" style="text-align:center;color:var(--triggered);padding:24px">Unable to load alert history.</td></tr>`;
        console.error("loadHistoricalAlerts", e);
    }
}

function updateSubline() {
    const { subline } = els();
    const count = ha.all.length;
    const updated = ha.lastUpdated ? ha.lastUpdated.toLocaleString(undefined, {
        day: "2-digit", month: "short", year: "numeric", hour: "numeric", minute: "2-digit", hour12: true
    }) : "—";
    subline.textContent = `${count} triggered alert${count === 1 ? "" : "s"} • Last updated ${updated}`;
}

function openHaModal() {
    const { overlay } = els();
    overlay.hidden = false;
    document.body.style.overflow = "hidden";
    // Reload every time it's opened so currentPrice is patched from
    // whatever the dashboard has most recently fetched.
    loadHistoricalAlerts();
}

function closeHaModal() {
    const { overlay } = els();
    overlay.hidden = true;
    closeStockPopup();
    document.body.style.overflow = "";
}

export function initHistoricalAlerts() {
    const { openBtn, closeBtn, overlay, search, month, group, status, dateFrom, dateTo,
        clearBtn, perPage, selectAll, deleteOlderBtn, deleteSelectedBtn } = els();

    injectHaStyles();
    ensureStockPopup();

    // "Recovered" option used by the Avg Recovery stat card
    if (!status.querySelector('option[value="recovered"]')) {
        status.insertAdjacentHTML("beforeend", '<option value="recovered">Recovered since trigger</option>');
    }

    const { stats, groupBreakdown, tbody } = els();
    const toggleStatus = val => {
        status.value = (val === "" || status.value === val) ? "" : val;
        applyHaFilters();
    };
    stats.addEventListener("click", e => {
        const card = e.target.closest("[data-status-filter]");
        if (card) toggleStatus(card.dataset.statusFilter);
    });
    stats.addEventListener("keydown", e => {
        if (e.key !== "Enter" && e.key !== " ") return;
        const card = e.target.closest("[data-status-filter]");
        if (card) { e.preventDefault(); toggleStatus(card.dataset.statusFilter); }
    });

    const toggleGroup = g => {
        group.value = (group.value === g) ? "" : g;
        applyHaFilters();
    };
    groupBreakdown.addEventListener("click", e => {
        const pill = e.target.closest("[data-group]");
        if (pill) toggleGroup(pill.dataset.group);
    });
    groupBreakdown.addEventListener("keydown", e => {
        if (e.key !== "Enter" && e.key !== " ") return;
        const pill = e.target.closest("[data-group]");
        if (pill) { e.preventDefault(); toggleGroup(pill.dataset.group); }
    });

    tbody.addEventListener("click", e => {
        const t = e.target.closest(".ha-sym-link, .ha-count-badge");
        if (t) openStockPopup(t.dataset.symbol);
    });

    openBtn.addEventListener("click", openHaModal);
    closeBtn.addEventListener("click", closeHaModal);
    overlay.addEventListener("click", (e) => { if (e.target === overlay) closeHaModal(); });
    document.addEventListener("keydown", (e) => {
        if (e.key !== "Escape") return;
        const sp = document.getElementById("haStockOverlay");
        if (sp && !sp.hidden) { closeStockPopup(); return; }
        if (!overlay.hidden) closeHaModal();
    });

    search.addEventListener("input", applyHaFilters);
    month.addEventListener("change", applyHaFilters);
    group.addEventListener("change", applyHaFilters);
    status.addEventListener("change", applyHaFilters);
    dateFrom.addEventListener("change", applyHaFilters);
    dateTo.addEventListener("change", applyHaFilters);

    clearBtn.addEventListener("click", () => {
        search.value = "";
        month.value = "";
        group.value = "";
        status.value = "";
        dateFrom.value = "";
        dateTo.value = "";
        applyHaFilters();
    });

    perPage.addEventListener("change", () => {
        ha.perPage = Number(perPage.value);
        ha.page = 1;
        renderHaTable();
    });

    selectAll.addEventListener("change", () => {
        const start = (ha.page - 1) * ha.perPage;
        const pageRows = ha.clubs.slice(start, start + ha.perPage);
        pageRows.forEach(c => c.alerts.forEach(a => {
            if (selectAll.checked) ha.selected.add(keyOf(a)); else ha.selected.delete(keyOf(a));
        }));
        renderHaTable();
    });

    deleteOlderBtn.addEventListener("click", deleteOlderAlerts);
    deleteSelectedBtn.addEventListener("click", deleteSelected);
}