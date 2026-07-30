import { state } from "./state.js";
import { render, renderSectorList } from "./render.js";
import { timeAgo } from "./utils.js";

export async function loadDashboard() {
 const BASE_URL = "https://tiny-art-8473.dobbyop09.workers.dev";
    try {
       const [dataRes, metaRes] = await Promise.all([
           fetch(`${BASE_URL}/dashboard-data?t=${Date.now()}`),
           fetch(`${BASE_URL}/dashboard-status?t=${Date.now()}`)
       ]);
        const data = await dataRes.json();
        const meta = await metaRes.json();

        state.allRows = data;
        state.rows = [...state.allRows];
        state.contextRows = [...state.allRows];
        renderSectorList(state.allRows);

        const updated = new Date(meta.lastUpdated);
        const subline = document.getElementById("subline");

        subline.innerText = `🟢 Updated ${timeAgo(updated)}`;
        subline.title = `Last updated: ${updated.toLocaleString()}\nGenerated in ${(meta.generationTimeMs / 1000).toFixed(1)} sec`;

        render();
    } catch (e) {
        document.getElementById("subline").innerText = "Unable to load dashboard data.";
        console.error(e);
    }
}
