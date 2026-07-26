import { state } from "./state.js";
import { renderSectorList } from "./filters.js";
import { render } from "./render.js";
import { timeAgo } from "../utils.js";

export async function load() {
    try {
        const [dataRes, metaRes] = await Promise.all([
            fetch("dashboard-data.json?t=" + Date.now()),
            fetch("dashboard-meta.json?t=" + Date.now())
        ]);

        state.allRows = await dataRes.json();

        const subline = document.getElementById("updated");

        // Meta file is shared with the dashboard page; fall back gracefully
        // if it's ever unavailable so the heatmap still renders.
        if (metaRes.ok) {
            const meta = await metaRes.json();
            const updated = new Date(meta.lastUpdated);
            subline.textContent = `🟢 Updated ${timeAgo(updated)}`;
            subline.title = `Last updated: ${updated.toLocaleString()}\nGenerated in ${(meta.generationTimeMs / 1000).toFixed(1)} sec`;
        } else {
            subline.textContent = "Last Updated : " + new Date().toLocaleString();
        }

        renderSectorList();
        render();
    } catch (e) {
        document.getElementById("updated").textContent = "Unable to load dashboard data.";
        console.error(e);
    }
}
