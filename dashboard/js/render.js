import { state } from "./state.js";
import { fmtPct } from "./utils.js";
import { applyFilters } from "./filters.js";

const STATUS_STYLE = {
    Triggered: { color: "var(--triggered)", badge: "b-triggered" },
    Near: { color: "var(--near)", badge: "b-near" },
    Watch: { color: "var(--watch)", badge: "b-watch" },
};
const DEFAULT_STYLE = { color: "var(--far)", badge: "b-far" };

function rowHtml(r) {
    const { color, badge } = STATUS_STYLE[r.status] || DEFAULT_STYLE;
    const chg = r.changePercent;
    const chgUp = chg >= 0;
    const changeHtml = (chg === undefined || chg === null) ? "" :
        `<span class="change-pct ${chgUp ? "up" : "down"}">${chgUp ? "+" : ""}${chg.toFixed(2)}%</span>`;

    return `
        <tr>
            <td>
                <a href="${r.screenerUrl}" target="_blank" class="stock-link">${r.symbol}</a>
            </td>
            <td class="num">
                <div class="price-cell">
                    <span class="price">${r.currentPrice}</span>
                    ${changeHtml}
                </div>
            </td>
            <td class="num alertCell">${r.alertPrice}</td>
            <td class="num" style="color:${color};font-weight:bold">${fmtPct(r.distance)}</td>
            <td><span class="badge ${badge}">${r.status}</span></td>
            <td>${r.sheet}</td>
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

function statsHtml(counts) {
    const card = (status, label, color, count) => `
        <div class="stat ${state.statusFilter === status ? "active" : ""}" data-status="${status}" style="color:${color}">
            <div class="n" style="color:${color}">${count}</div>
            <div class="l">${label}</div>
        </div>
    `;

    return (
        card("Triggered", "Triggered", "var(--triggered)", counts.triggered) +
        card("Near", "Near", "var(--near)", counts.near) +
        card("Watch", "Watch", "var(--watch)", counts.watch) +
        card("", "Total", "var(--text)", state.contextRows.length)
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

    let html = `
        <li class="sector-item ${state.sectorFilter === "" ? "active" : ""}" data-sector="">
            <span>All Sectors</span>
            <span class="count">${searchFilteredRows.length}</span>
        </li>
    `;

    sectors.forEach(sector => {
        html += `
            <li class="sector-item ${state.sectorFilter === sector ? "active" : ""}" data-sector="${sector}">
                <span>${sector}</span>
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
