import { state } from "./state.js";
import { render, renderSectorList } from "./render.js";

export function applyFilters() {
    const text = document.getElementById("searchBox").value.trim().toUpperCase();

    const searchFilteredRows = state.allRows.filter(stock =>
        text === "" ||
        stock.symbol.toUpperCase().includes(text) ||
        (stock.sheet ?? "").toUpperCase().includes(text)
    );

    state.contextRows = searchFilteredRows.filter(stock =>
        state.sectorFilter === "" || state.sectorFilter === stock.sheet
    );

    state.rows = state.statusFilter === ""
        ? state.contextRows
        : state.contextRows.filter(stock => (stock.status || "Far") === state.statusFilter);

    renderSectorList(searchFilteredRows);
    render();
}

function toggleSectorFilter(sector) {
    state.sectorFilter = (state.sectorFilter === sector) ? "" : sector;
    applyFilters();
}

function toggleStatusFilter(status) {
    state.statusFilter = (state.statusFilter === status) ? "" : status;
    applyFilters();
}

function sortByDistance() {
    state.rows.sort((a, b) => a.distance - b.distance);
    render();
}

function sortByMarketCap() {
    const mcBtn = document.getElementById("sortMcBtn");

    state.rows.sort((a, b) =>
        state.marketCapAscending
            ? Number(a.marketCap || 0) - Number(b.marketCap || 0)
            : Number(b.marketCap || 0) - Number(a.marketCap || 0)
    );

    state.marketCapAscending = !state.marketCapAscending;
    mcBtn.textContent = state.marketCapAscending ? "Market Cap ↑" : "Market Cap ↓";

    render();
}

export function initFilters() {
    document.getElementById("searchBox").addEventListener("input", applyFilters);

    document.getElementById("sectorList").addEventListener("click", (e) => {
        const item = e.target.closest(".sector-item");
        if (!item) return;
        toggleSectorFilter(item.dataset.sector);
    });

    document.getElementById("stats").addEventListener("click", (e) => {
        const card = e.target.closest(".stat");
        if (!card) return;
        toggleStatusFilter(card.dataset.status);
    });

    document.getElementById("sortBtn").addEventListener("click", sortByDistance);
    document.getElementById("sortMcBtn").addEventListener("click", sortByMarketCap);
}
