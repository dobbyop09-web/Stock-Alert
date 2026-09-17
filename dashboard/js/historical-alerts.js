// Historical Alerts modal.
// Fetches the triggered-alert log from the worker (GET /alert-history) and
// renders it in a searchable / filterable / paginated table inside a popup.
//
// Expected response shape (per the sample the endpoint returns):
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
//
// `status` isn't part of the payload, so every row here represents an alert
// that already fired; there's no Pending/Missed distinction to render.

const BASE_URL = "https://tiny-art-8473.dobbyop09.workers.dev";
const HISTORY_ENDPOINT = `${BASE_URL}/alert-history`;

const ha = {
    all: [],          // raw alerts as loaded
    filtered: [],      // after search/month filters
    sortDesc: true,    // newest first by default
    page: 1,
    perPage: 10,
    loaded: false,
};

function fmtDateTime(iso) {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso || "—";
    return d.toLocaleString(undefined, {
        day: "2-digit", month: "short", year: "numeric",
        hour: "numeric", minute: "2-digit", hour12: true
    });
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

function changePct(a) {
    const base = a.triggerPrice ?? a.alertPrice;
    if (!base) return null;
    return ((a.currentPrice - base) / base) * 100;
}

function els() {
    return {
        overlay: document.getElementById("haOverlay"),
        openBtn: document.getElementById("historyAlertsBtn"),
        closeBtn: document.getElementById("haCloseBtn"),
        search: document.getElementById("haSearch"),
        month: document.getElementById("haMonth"),
        sortBtn: document.getElementById("haSortBtn"),
        clearBtn: document.getElementById("haClearBtn"),
        stats: document.getElementById("haStats"),
        tbody: document.getElementById("haTbody"),
        empty: document.getElementById("haEmpty"),
        tableWrap: document.getElementById("haTableWrap"),
        showingLine: document.getElementById("haShowingLine"),
        perPage: document.getElementById("haPerPage"),
        pager: document.getElementById("haPager"),
    };
}

function populateMonths() {
    const { month } = els();
    const keys = [...new Set(ha.all.map(a => monthKey(a.triggeredAt || a.date)))].filter(Boolean).sort().reverse();
    month.innerHTML = `<option value="">All Months</option>` +
        keys.map(k => `<option value="${k}">${monthLabel(k)}</option>`).join("");
}

function applyHaFilters() {
    const { search, month } = els();
    const text = search.value.trim().toUpperCase();
    const m = month.value;

    ha.filtered = ha.all.filter(a => {
        const matchesText = text === "" ||
            (a.symbol || "").toUpperCase().includes(text) ||
            (a.companyName || "").toUpperCase().includes(text) ||
            (a.watchlist || a.sheet || "").toUpperCase().includes(text);
        const matchesMonth = m === "" || monthKey(a.triggeredAt || a.date) === m;
        return matchesText && matchesMonth;
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
    const { stats } = els();
    const rows = ha.filtered;
    const gainers = rows.filter(a => (changePct(a) ?? 0) >= 0).length;
    const losers = rows.length - gainers;
    const thisMonthKey = (() => {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    })();
    const thisMonth = rows.filter(a => monthKey(a.triggeredAt || a.date) === thisMonthKey).length;

    const card = (label, count, cls, icon) => `
        <div class="stat stat-${cls}">
            <div class="stat-body">
                <div class="n">${count}</div>
                <div class="l">${label}</div>
            </div>
            <div class="stat-icon">${icon}</div>
        </div>
    `;

    const BELL = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 5-2 6-2 7h16c0-1-2-2-2-7" /><path d="M10.3 20a1.8 1.8 0 0 0 3.4 0" /></svg>';
    const UP = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 17l6-6 4 4 8-8" /><path d="M15 7h6v6" /></svg>';
    const DOWN = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7l6 6 4-4 8 8" /><path d="M15 17h6v-6" /></svg>';
    const CAL = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" /></svg>';

    stats.innerHTML =
        card("Total Alerts", rows.length, "total", BELL) +
        card("Gainers Since", gainers, "watch", UP) +
        card("Losers Since", losers, "triggered", DOWN) +
        card("This Month", thisMonth, "near", CAL);
}

function renderHaTable() {
    const { tbody, empty, tableWrap, showingLine, pager } = els();

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

    renderPager(totalPages);
}

function rowHtml(a) {
    const chg = changePct(a);
    const up = (chg ?? 0) >= 0;
    const chgHtml = chg === null
        ? "—"
        : `<span class="change-pct ${up ? "up" : "down"}">${up ? "+" : ""}${chg.toFixed(2)}%</span>`;

    const group = a.watchlist || a.sheet || "—";
    const symbolSafe = String(a.symbol || "").replace(/'/g, "\\'");

    return `
        <tr>
            <td>${fmtDateTime(a.triggeredAt || a.date)}</td>
            <td>
                <a href="${a.screenerUrl || "#"}" target="_blank" class="stock-link" style="gap:0">
                    <span class="sym-text" title="${(a.companyName || "").replace(/"/g, "&quot;")}">${a.symbol || "—"}</span>
                </a>
            </td>
            <td class="num">${a.alertPrice ?? "—"}</td>
            <td class="num">${a.triggerPrice ?? "—"}</td>
            <td class="num">${a.currentPrice ?? "—"}</td>
            <td class="num">${chgHtml}</td>
            <td><span class="ha-group-badge">${group}</span></td>
            <td>
                <div class="ha-actions-cell">
                    <button class="icon-btn" title="Open on Screener" onclick="window.open('${a.screenerUrl || "#"}','_blank')">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.5 12s3.5-5 9.5-5 9.5 5 9.5 5-3.5 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2.5" /></svg>
                    </button>
                    <button class="icon-btn" title="Remove from list" onclick="window.__removeHaAlert('${symbolSafe}','${a.triggeredAt || a.date}')">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" /></svg>
                    </button>
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

// Best-effort client-side removal. The worker only documents a GET endpoint
// for this data, so there's no confirmed delete route to call yet — this
// just hides the row locally and tries a DELETE in case the worker supports
// one, without blocking the UI if it doesn't.
window.__removeHaAlert = function (symbol, triggeredAt) {
    if (!confirm(`Remove ${symbol} from the history list?`)) return;

    ha.all = ha.all.filter(a => !(a.symbol === symbol && (a.triggeredAt || a.date) === triggeredAt));
    applyHaFilters();

    fetch(HISTORY_ENDPOINT, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ symbol, triggeredAt })
    }).catch(() => { /* endpoint may not support delete yet; ignore */ });
};

async function loadHistoricalAlerts() {
    const { tbody } = els();
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--muted);padding:24px">Loading...</td></tr>`;

    try {
        const res = await fetch(`${HISTORY_ENDPOINT}?t=${Date.now()}`);
        const data = await res.json();
        ha.all = Array.isArray(data.alerts) ? data.alerts : [];
        ha.loaded = true;
        populateMonths();
        applyHaFilters();
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--triggered);padding:24px">Unable to load alert history.</td></tr>`;
        console.error("loadHistoricalAlerts", e);
    }
}

function openHaModal() {
    const { overlay } = els();
    overlay.hidden = false;
    document.body.style.overflow = "hidden";
    if (!ha.loaded) loadHistoricalAlerts();
}

function closeHaModal() {
    const { overlay } = els();
    overlay.hidden = true;
    document.body.style.overflow = "";
}

export function initHistoricalAlerts() {
    const { openBtn, closeBtn, overlay, search, month, sortBtn, clearBtn, perPage } = els();

    openBtn.addEventListener("click", openHaModal);
    closeBtn.addEventListener("click", closeHaModal);
    overlay.addEventListener("click", (e) => { if (e.target === overlay) closeHaModal(); });
    document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !overlay.hidden) closeHaModal(); });

    search.addEventListener("input", applyHaFilters);
    month.addEventListener("change", applyHaFilters);

    sortBtn.addEventListener("click", () => {
        ha.sortDesc = !ha.sortDesc;
        sortBtn.querySelector(".sort-label").textContent = ha.sortDesc ? "Newest First" : "Oldest First";
        applyHaFilters();
    });

    clearBtn.addEventListener("click", () => {
        search.value = "";
        month.value = "";
        ha.sortDesc = true;
        sortBtn.querySelector(".sort-label").textContent = "Newest First";
        applyHaFilters();
    });

    perPage.addEventListener("change", () => {
        ha.perPage = Number(perPage.value);
        ha.page = 1;
        renderHaTable();
    });
}
