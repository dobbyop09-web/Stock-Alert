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
    filtered: [],
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
    const st = status.value; // "", "above", "below"
    const from = dateFrom.value ? new Date(dateFrom.value + "T00:00:00") : null;
    const to = dateTo.value ? new Date(dateTo.value + "T23:59:59") : null;

    ha.filtered = ha.all.filter(a => {
        const matchesText = text === "" ||
            (a.symbol || "").toUpperCase().includes(text) ||
            (a.companyName || "").toUpperCase().includes(text) ||
            (a.watchlist || a.sheet || "").toUpperCase().includes(text);
        const matchesMonth = m === "" || monthKey(a.triggeredAt || a.date) === m;
        const matchesGroup = g === "" || (a.watchlist || a.sheet) === g;
        const vs = vsAlertPct(a) ?? 0;
        const matchesStatus = st === "" || (st === "above" ? vs >= 0 : vs < 0);
        const d = new Date(a.triggeredAt || a.date);
        const matchesFrom = !from || d >= from;
        const matchesTo = !to || d <= to;
        return matchesText && matchesMonth && matchesGroup && matchesStatus && matchesFrom && matchesTo;
    });

    ha.filtered.sort((x, y) => {
        const dx = new Date(x.triggeredAt || x.date).getTime();
        const dy = new Date(y.triggeredAt || y.date).getTime();
        return ha.sortDesc ? dy - dx : dx - dy;
    });

    ha.page = 1;
    renderHaStats();
    renderHaTable();
}

function renderHaStats() {
    const { stats, groupBreakdown } = els();
    const rows = ha.filtered;

    const above = rows.filter(a => (vsAlertPct(a) ?? 0) >= 0).length;
    const below = rows.length - above;

    const recoveries = rows.map(sinceTriggerPct).filter(v => v !== null);
    const avgRecovery = recoveries.length ? recoveries.reduce((s, v) => s + v, 0) / recoveries.length : 0;

    const BELL = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 5-2 6-2 7h16c0-1-2-2-2-7" /><path d="M10.3 20a1.8 1.8 0 0 0 3.4 0" /></svg>';
    const UP = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7" /></svg>';
    const DOWN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12l7 7 7-7" /></svg>';
    const TREND = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 17l6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>';

    const card = (label, count, cls, icon) => `
        <div class="stat stat-${cls}">
            <div class="stat-body">
                <div class="n">${count}</div>
                <div class="l">${label}</div>
            </div>
            <div class="stat-icon">${icon}</div>
        </div>
    `;

    stats.innerHTML =
        card("Total Alerts", rows.length, "total", BELL) +
        card("Above Alert Now", above, "watch", UP) +
        card("Below Alert Now", below, "triggered", DOWN) +
        `<div class="stat stat-near">
            <div class="stat-body">
                <div class="n">${avgRecovery >= 0 ? "+" : ""}${avgRecovery.toFixed(2)}%</div>
                <div class="l">Avg Recovery Since Trigger</div>
            </div>
            <div class="stat-icon">${TREND}</div>
        </div>`;

    const groupCounts = {};
    rows.forEach(a => {
        const g = a.watchlist || a.sheet || "—";
        groupCounts[g] = (groupCounts[g] || 0) + 1;
    });
    const groupEntries = Object.entries(groupCounts).sort((a, b) => b[1] - a[1]);
    groupBreakdown.innerHTML = groupEntries.length
        ? `<div class="ha-gb-title">Group Breakdown</div><div class="ha-gb-pills">` +
          groupEntries.map(([g, c]) => `<span class="ha-group-badge">${g} <b>${c}</b></span>`).join("") +
          `</div>`
        : `<div class="ha-gb-title">Group Breakdown</div><div class="ha-gb-pills" style="color:var(--muted);font-size:11.5px">No data</div>`;
}

function renderHaTable() {
    const { tbody, empty, tableWrap, showingLine, selectAll } = els();

    const total = ha.filtered.length;
    const totalPages = Math.max(1, Math.ceil(total / ha.perPage));
    ha.page = Math.min(ha.page, totalPages);

    const start = (ha.page - 1) * ha.perPage;
    const pageRows = ha.filtered.slice(start, start + ha.perPage);

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
        : `Showing ${start + 1}–${Math.min(start + ha.perPage, total)} of ${total} alerts`;

    selectAll.checked = pageRows.length > 0 && pageRows.every(a => ha.selected.has(keyOf(a)));
    selectAll.indeterminate = !selectAll.checked && pageRows.some(a => ha.selected.has(keyOf(a)));

    tbody.querySelectorAll(".ha-row-check").forEach(cb => {
        cb.addEventListener("change", () => {
            const k = cb.dataset.key;
            if (cb.checked) ha.selected.add(k); else ha.selected.delete(k);
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
        ? `🗑 Delete selected (${ha.selected.size})`
        : `🗑 Delete selected`;
}

const EYE_ICON = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.5 12s3.5-5 9.5-5 9.5 5 9.5 5-3.5 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2.5" /></svg>';
const EXTERNAL_ICON = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /><path d="M15 3h6v6" /><path d="M10 14 21 3" /></svg>';

function rowHtml(a) {
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
    const k = keyOf(a);
    const checked = ha.selected.has(k) ? "checked" : "";

    return `
        <tr>
            <td><input type="checkbox" class="ha-row-check" data-key="${k}" ${checked}></td>
            <td>${fmtDateTimeShort(a.triggeredAt || a.date)}</td>
            <td><span class="sym-text" style="font-weight:700">${a.symbol || "—"}</span></td>
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
    document.body.style.overflow = "";
}

export function initHistoricalAlerts() {
    const { openBtn, closeBtn, overlay, search, month, group, status, dateFrom, dateTo,
        clearBtn, perPage, selectAll, deleteOlderBtn, deleteSelectedBtn } = els();

    openBtn.addEventListener("click", openHaModal);
    closeBtn.addEventListener("click", closeHaModal);
    overlay.addEventListener("click", (e) => { if (e.target === overlay) closeHaModal(); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !overlay.hidden) closeHaModal(); });

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
        const pageRows = ha.filtered.slice(start, start + ha.perPage);
        pageRows.forEach(a => {
            if (selectAll.checked) ha.selected.add(keyOf(a)); else ha.selected.delete(keyOf(a));
        });
        renderHaTable();
    });

    deleteOlderBtn.addEventListener("click", deleteOlderAlerts);
    deleteSelectedBtn.addEventListener("click", deleteSelected);
}   