import { state } from "./state.js";
import { render, renderSectorList, setCompanyLogos } from "./render.js";
import { timeAgo } from "./utils.js";

export async function loadDashboard() {
    const BASE_URL = "https://tiny-art-8473.dobbyop09.workers.dev";

    try {
        const [dataRes, metaRes, logoRes] = await Promise.all([
            fetch(`${BASE_URL}/dashboard-data?t=${Date.now()}`),
            fetch(`${BASE_URL}/dashboard-status?t=${Date.now()}`),
            fetch(`logo.json?t=${Date.now()}`, { cache: "no-cache" })
        ]);

        const data = await dataRes.json();
        const meta = await metaRes.json();

        let logoData = {};

        try {
            if (logoRes.ok) {
                logoData = await logoRes.json();
            }
        } catch (logoError) {
            console.warn("Unable to load logo.json", logoError);
        }

        setCompanyLogos(logoData);

        // Exclude ETFs from dashboard
        const filteredData = data.filter(
            row => row.sheet?.toLowerCase() !== "etf"
        );

        state.allRows = filteredData;
        state.rows = [...filteredData];
        state.contextRows = [...filteredData];

        renderSectorList(filteredData);

        const updated = new Date(meta.lastUpdated);
        const subline = document.getElementById("subline");

        subline.innerText = `🟢 Updated ${timeAgo(updated)}`;
        subline.title =
            `Last updated: ${updated.toLocaleString()}\n` +
            `Generated in ${(meta.generationTimeMs / 1000).toFixed(1)} sec`;

        render();

    } catch (e) {
        document.getElementById("subline").innerText =
            "Unable to load dashboard data.";

        console.error(e);
    }
}