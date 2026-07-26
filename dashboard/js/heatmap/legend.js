import { state } from "./state.js";
import { BUCKETS } from "./utils.js";
import { refresh } from "./filters.js";

export function renderLegend() {
    const el = document.getElementById("legend");

    el.innerHTML = BUCKETS.map(b => `
        <button
            type="button"
            class="legend-item ${state.activeBuckets.has(b.id) ? "" : "inactive"}"
            data-bucket="${b.id}"
            role="checkbox"
            aria-checked="${state.activeBuckets.has(b.id)}"
        >
            <span class="sw" style="background:${b.color}"></span>${b.label}
        </button>
    `).join("");
}

function toggleBucket(id) {
    if (state.activeBuckets.has(id)) {
        state.activeBuckets.delete(id);
    } else {
        state.activeBuckets.add(id);
    }
    renderLegend();
    refresh();
}

export function initLegend() {
    renderLegend();

    document.getElementById("legend").addEventListener("click", (e) => {
        const item = e.target.closest(".legend-item");
        if (!item) return;
        toggleBucket(item.dataset.bucket);
    });
}
