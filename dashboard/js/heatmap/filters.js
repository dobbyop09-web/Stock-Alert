import { state } from "./state.js";
import { render } from "./render.js";
import { BUCKETS, getBucket } from "./utils.js";
import { renderLegend } from "./legend.js";

function applySort(rows) {
    const sorted = [...rows];
    switch (state.sortMode) {
        case "changeDesc":
            sorted.sort((a, b) => b.changePercent - a.changePercent);
            break;
        case "changeAsc":
            sorted.sort((a, b) => a.changePercent - b.changePercent);
            break;
        case "symbol":
            sorted.sort((a, b) => a.symbol.localeCompare(b.symbol));
            break;
        default:
            sorted.sort((a, b) => (b.marketCap || 0) - (a.marketCap || 0));
    }
    return sorted;
}

// Rows filtered by search text only — used as the basis for sidebar sector counts,
// matching the same "search narrows first" convention as the dashboard page.
export function getSearchFilteredRows() {
    const q = document.getElementById("search").value.trim().toUpperCase();

    return state.allRows.filter(s =>
        q === "" ||
        s.symbol.toUpperCase().includes(q) ||
        (s.sheet ?? "").toUpperCase().includes(q)
    );
}

export function getFilteredRows() {
    const rows = getSearchFilteredRows().filter(s => {
        const matchesSector = state.sectorFilter === "" || s.sheet === state.sectorFilter;
        const matchesBucket = state.activeBuckets.has(getBucket(s.changePercent).id);
        return matchesSector && matchesBucket;
    });

    return applySort(rows);
}

export function renderSectorList() {
    const searchFilteredRows = getSearchFilteredRows();
    const list = document.getElementById("sectorList");

    const sectors = [...new Set(state.allRows.map(r => r.sheet).filter(Boolean))].sort();
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

export function renderShowingLine(visibleCount) {
    const el = document.getElementById("showingLine");
    const search = document.getElementById("search").value.trim();

    const parts = [];
    if (state.sectorFilter) parts.push(state.sectorFilter);

    if (state.activeBuckets.size < BUCKETS.length) {
        const activeLabels = BUCKETS
            .filter(b => state.activeBuckets.has(b.id))
            .map(b => b.label);
        parts.push(activeLabels.length ? activeLabels.join(", ") : "no ranges selected");
    }

    if (search) parts.push(`"${search}"`);

    const label = parts.length ? parts.join(" • ") : "All";

    el.innerHTML = `Showing: <b>${label}</b> • <b>${visibleCount}</b> Symbols` +
        (parts.length ? `<span class="clear" id="clearFilters">Clear</span>` : "");

    const clearBtn = document.getElementById("clearFilters");
    if (clearBtn) {
        clearBtn.addEventListener("click", () => {
            state.sectorFilter = "";
            state.activeBuckets = new Set(BUCKETS.map(b => b.id));
            document.getElementById("search").value = "";
            renderLegend();
            refresh();
        });
    }
}

// Re-run the sidebar + treemap together; call this after any filter change.
export function refresh() {
    renderSectorList();
    render();
}

export function initFilters() {
    document.getElementById("search").addEventListener("input", refresh);

    document.getElementById("sortBy").addEventListener("change", function () {
        state.sortMode = this.value;
        render();
    });

    document.getElementById("sectorList").addEventListener("click", (e) => {
        const item = e.target.closest(".sector-item");
        if (!item) return;
        const clicked = item.dataset.sector;
        state.sectorFilter = (state.sectorFilter === clicked) ? "" : clicked;
        refresh();
    });

    window.addEventListener("resize", () => {
        clearTimeout(window._heatmapResizeTimer);
        window._heatmapResizeTimer = setTimeout(render, 150);
    });
}