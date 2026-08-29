import { state } from "./state.js";
import { fmtPct } from "./utils.js";
import { applyFilters } from "./filters.js";

const STATUS_STYLE = {
    Triggered: { color: "var(--triggered)", badge: "b-triggered" },
    Near: { color: "var(--near)", badge: "b-near" },
    Watch: { color: "var(--watch)", badge: "b-watch" },
};
const DEFAULT_STYLE = { color: "var(--far)", badge: "b-far" };

const SECTOR_ICONS = {
    auto: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 13l2-5a2 2 0 0 1 2-1.3h10a2 2 0 0 1 2 1.3l2 5" /><rect x="2" y="13" width="20" height="6" rx="1.5" /><circle cx="7" cy="19" r="1.6" /><circle cx="17" cy="19" r="1.6" /></svg>',
    bank: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10l9-6 9 6" /><path d="M5 10v9M10 10v9M14 10v9M19 10v9" /><path d="M3 21h18" /></svg>',
    capitalmarket: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19V10M10 19V5M16 19v-7M20 19V3" /></svg>',
    defence: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l8 3.5v5.4c0 5-3.4 8.6-8 11.1-4.6-2.5-8-6.1-8-11.1V5.5L12 2Z" /></svg>',
    fmcg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="20" r="1.3" /><circle cx="17" cy="20" r="1.3" /><path d="M2.5 3h2.4l2.4 12.6a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21 7H6" /></svg>',
    health: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20.8 8.6c0 5.6-8.8 10.9-8.8 10.9S3.2 14.2 3.2 8.6a4.6 4.6 0 0 1 8.8-1.9 4.6 4.6 0 0 1 8.8 1.9Z" /></svg>',
    it: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="12" rx="1.5" /><path d="M8 20h8M12 16v4" /></svg>',
    manufacturing: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21V11l6 4V11l6 4V8l6 4v9H3Z" /></svg>',
    metal: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="2" /><ellipse cx="12" cy="12" rx="9" ry="3.6" /><ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(60 12 12)" /><ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(120 12 12)" /></svg>',
    misc: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.2" /><rect x="14" y="3" width="7" height="7" rx="1.2" /><rect x="3" y="14" width="7" height="7" rx="1.2" /><rect x="14" y="14" width="7" height="7" rx="1.2" /></svg>',
    oilenergy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2s6 6.5 6 11.5a6 6 0 0 1-12 0C6 8.5 12 2 12 2Z" /></svg>',
    reality: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 21V9l8-5 8 5v12" /><path d="M9 21v-6h6v6" /></svg>',
};
const SECTOR_ICON_DEFAULT = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3" /></svg>';
const ALL_SECTORS_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="1.2" /><rect x="14" y="3" width="7" height="5" rx="1.2" /><rect x="14" y="12" width="7" height="9" rx="1.2" /><rect x="3" y="16" width="7" height="5" rx="1.2" /></svg>';

const SECTOR_COLORS = {
    auto: "#9CA3AF",
    bank: "#818CF8",
    capitalmarket: "#2DD4BF",
    defence: "#60A5FA",
    fmcg: "#34D399",
    health: "#C084FC",
    it: "#22D3EE",
    manufacturing: "#A78BFA",
    metal: "#94A3B8",
    misc: "#9CA3AF",
    oilenergy: "#FB923C",
    reality: "#FB7185",
};
const SECTOR_COLOR_DEFAULT = "#9CA3AF";
const SECTOR_COLOR_ALL = "#2DD4BF";

function hexToRgba(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`;
}

// No logo/company data is available from the sheet, so each symbol gets a
// deterministic colored initials avatar (same symbol -> same color, always).
const AVATAR_PALETTE = ["#2563EB", "#DC2626", "#059669", "#D97706", "#7C3AED", "#DB2777", "#0891B2", "#65A30D", "#4F46E5", "#EA580C", "#0D9488", "#9333EA"];

function avatarFor(symbol) {
    const s = String(symbol || "?");
    let hash = 0;
    for (let i = 0; i < s.length; i++) hash = (hash * 31 + s.charCodeAt(i)) >>> 0;
    const color = AVATAR_PALETTE[hash % AVATAR_PALETTE.length];
    const letter = (s.match(/[A-Za-z]/) || ["?"])[0].toUpperCase();
    return { color, letters: letter };
}

const STATUS_ICONS = {
    Triggered: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 5-2 6-2 7h16c0-1-2-2-2-7" /><path d="M10.3 20a1.8 1.8 0 0 0 3.4 0" /></svg>',
    Near: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="4" /></svg>',
    Watch: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.5 12s3.5-5 9.5-5 9.5 5 9.5 5-3.5 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2.5" /></svg>',
    Far: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><line x1="8.5" y1="12" x2="15.5" y2="12" /></svg>',
};
const STATUS_ICON_DEFAULT = STATUS_ICONS.Far;

function rowHtml(r) {
    const { color, badge } = STATUS_STYLE[r.status] || DEFAULT_STYLE;
    const chg = r.changePercent;
    const chgUp = chg >= 0;
    const changeHtml = (chg === undefined || chg === null) ? "" :
        `<span class="change-pct ${chgUp ? "up" : "down"}">${chgUp ? "+" : ""}${chg.toFixed(2)}%</span>`;

    const av = avatarFor(r.symbol);
    const sectorColor = SECTOR_COLORS[String(r.sheet).toLowerCase()] || SECTOR_COLOR_DEFAULT;
    const sectorIcon = SECTOR_ICONS[String(r.sheet).toLowerCase()] || SECTOR_ICON_DEFAULT;
    const statusIcon = STATUS_ICONS[r.status] || STATUS_ICON_DEFAULT;

    return `
        <tr>
            <td>
                <a href="${r.screenerUrl}" target="_blank" class="stock-link">
                    <span class="sym-avatar" style="background:${av.color}">${av.letters}</span>
                    <span class="sym-text">${r.symbol}</span>
                </a>
            </td>
            <td class="num">
                <div class="price-cell">
                    <span class="price">${r.currentPrice}</span>
                    ${changeHtml}
                </div>
            </td>
            <td class="num alertCell">${r.alertPrice}</td>
            <td class="num" style="color:${color};font-weight:bold">${fmtPct(r.distance)}</td>
            <td><span class="badge ${badge}">${statusIcon}${r.status}</span></td>
            <td><span class="sector-badge" style="color:${sectorColor};background:${hexToRgba(sectorColor, .12)};border-color:${hexToRgba(sectorColor, .28)}">${sectorIcon}${r.sheet || "—"}</span></td>
             <td>
    <button class="icon-btn" onclick="editAlert('${r.symbol}',this)">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
        </svg>
    </button>
</td>
        </tr> 
    `;
}

const STAT_ICONS = {
    Triggered: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 5-2 6-2 7h16c0-1-2-2-2-7" /><path d="M10.3 20a1.8 1.8 0 0 0 3.4 0" /></svg>',
    Near: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="4.5" /><circle cx="12" cy="12" r="0.8" fill="currentColor" stroke="none" /></svg>',
    Watch: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2.5 12s3.5-5 9.5-5 9.5 5 9.5 5-3.5 5-9.5 5-9.5-5-9.5-5Z" /><circle cx="12" cy="12" r="2.5" /></svg>',
    Total: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20V10M10 20V4M16 20v-7M20 20v-3.5" /></svg>'
};

function statsHtml(counts) {
    const card = (status, label, cls, count) => `
        <div class="stat stat-${cls} ${state.statusFilter === status ? "active" : ""}" data-status="${status}">
            <div class="stat-body">
                <div class="n">${count}</div>
                <div class="l">${label}</div>
            </div>
            <div class="stat-icon">${STAT_ICONS[label]}</div>
        </div>
    `;

    return (
        card("Triggered", "Triggered", "triggered", counts.triggered) +
        card("Near", "Near", "near", counts.near) +
        card("Watch", "Watch", "watch", counts.watch) +
        card("", "Total", "total", state.contextRows.length)
    );
}

function countByStatus(rows) {
    const counts = { triggered: 0, near: 0, watch: 0, far: 0 };
    rows.forEach(r => {
        switch (r.status) {
            case "Triggered": counts.triggered++; break;
            case "Near": counts.near++; break;
            case "Watch": counts.watch++; break;
            default: counts.far++;
        }
    });
    return counts;
}

export function renderShowingLine() {
    const el = document.getElementById("showingLine");
    const search = document.getElementById("searchBox").value.trim();

    const parts = [];
    if (state.statusFilter) parts.push(state.statusFilter);
    if (state.sectorFilter) parts.push(state.sectorFilter);
    if (search) parts.push(`"${search}"`);

    const label = parts.length ? parts.join(" • ") : "All";

    el.innerHTML = `Showing: <b>${label}</b> • <b>${state.rows.length}</b> Stocks` +
        (parts.length ? `<span class="clear" id="clearFilters">Clear</span>` : "");

    const clearBtn = document.getElementById("clearFilters");
    if (clearBtn) {
        clearBtn.addEventListener("click", () => {
            state.statusFilter = "";
            state.sectorFilter = "";
            document.getElementById("searchBox").value = "";
            applyFilters();
        });
    }
}

export function renderSectorList(searchFilteredRows) {
    const list = document.getElementById("sectorList");

    const sectors = [...new Set(state.allRows.map(s => s.sheet).filter(Boolean))].sort();
    const countFor = sector => searchFilteredRows.filter(s => s.sheet === sector).length;
    const iconFor = sector => SECTOR_ICONS[String(sector).toLowerCase()] || SECTOR_ICON_DEFAULT;
    const colorFor = sector => SECTOR_COLORS[String(sector).toLowerCase()] || SECTOR_COLOR_DEFAULT;

    let html = `
        <li class="sector-item ${state.sectorFilter === "" ? "active" : ""}" data-sector="">
            <span class="sector-icon" style="background:${hexToRgba(SECTOR_COLOR_ALL, .16)};color:${SECTOR_COLOR_ALL}">${ALL_SECTORS_ICON}</span>
            <span class="sector-name">All Sectors</span>
            <span class="count">${searchFilteredRows.length}</span>
        </li>
    `;

    sectors.forEach(sector => {
        const c = colorFor(sector);
        html += `
            <li class="sector-item ${state.sectorFilter === sector ? "active" : ""}" data-sector="${sector}">
                <span class="sector-icon" style="background:${hexToRgba(c, .16)};color:${c}">${iconFor(sector)}</span>
                <span class="sector-name">${sector}</span>
                <span class="count">${countFor(sector)}</span>
            </li>
        `;
    });

    list.innerHTML = html;
}

export function render() {
    const tbody = document.getElementById("tbody");
    tbody.innerHTML = state.rows.map(rowHtml).join("");

    const counts = countByStatus(state.contextRows);
    document.getElementById("stats").innerHTML = statsHtml(counts);

    document.getElementById("emptyMsg").style.display = state.rows.length ? "none" : "block";

    renderShowingLine();
}