import { getFilteredRows } from "./filters.js";
import { color, fmtNum, truncateToWidth } from "./utils.js";

const tooltip = document.getElementById("tooltip");

function showTooltip(e, d) {
    const c = d.data.changePercent;
    const cls = c >= 0 ? "t-pos" : "t-neg";
    const ca = d.data.distance;
    const clss = ca >= 0 ? "t-pos" : "t-neg";

    tooltip.innerHTML = `
        <div class="t-symbol">${d.data.symbol}</div>
        <div class="t-row"><span>Sector</span><span>${d.data.sheet || "-"}</span></div>
        <div class="t-row"><span>Price</span><span>${d.data.currentPrice !== undefined ? "₹" + fmtNum(d.data.currentPrice) : "-"}</span></div>
        <div class="t-row"><span>AlertPrice</span><span>${d.data.alertPrice !== undefined ? "₹" + fmtNum(d.data.alertPrice) : "-"}</span></div>
        <div class="t-row"><span>Change</span><span class="${cls}">${Number(c).toFixed(2)}%</span></div>
        <div class="t-row"><span>ChangeAlert</span><span class="${clss}">${Number(ca).toFixed(2)}%</span></div>
    `;
    tooltip.style.opacity = 1;
    tooltip.style.left = (e.clientX + 16) + "px";
    tooltip.style.top = (e.clientY + 16) + "px";
}

function hideTooltip() {
    tooltip.style.opacity = 0;
}

function openScreener(d) {
    window.open(`https://www.screener.in/company/${d.data.symbol}/consolidated/`, "_blank");
}

function drawLabels(nodes) {
    nodes.each(function (d) {
        const w = d.x1 - d.x0;
        const h = d.y1 - d.y0;

        // Only truly tiny slivers get nothing at all
        if (w < 18 || h < 12) return;

        const g = d3.select(this);
        const symFont = Math.max(8, Math.min(14, w / 5, h / 2.2));
        const label = truncateToWidth(d.data.symbol, w - 8, symFont);

        g.append("text")
            .attr("class", "tile-text")
            .attr("x", 5)
            .attr("y", symFont + 4)
            .attr("fill", "white")
            .style("font-size", symFont + "px")
            .style("font-weight", "bold")
            .text(label);

        // Only add the change% line if there's room below the symbol
        if (h > symFont + 16) {
            const pctFont = Math.max(7, Math.min(12, symFont - 1));
            g.append("text")
                .attr("class", "tile-text")
                .attr("x", 5)
                .attr("y", symFont + pctFont + 6)
                .attr("fill", "white")
                .style("font-size", pctFont + "px")
                .text(`${Number(d.data.changePercent).toFixed(2)}%`);
        }
    });
}

export function render() {
    const container = document.getElementById("heatmap");
    container.innerHTML = "";

    const rows = getFilteredRows();

    if (rows.length === 0) {
        const empty = document.createElement("div");
        empty.className = "empty-state";
        empty.textContent = "No symbols match the current filters.";
        container.appendChild(empty);
        return;
    }

    const width = container.clientWidth;
    const height = container.clientHeight;

    // Group into sectors for nested treemap
    const grouped = d3.groups(rows, d => d.sheet || "Other")
        .map(([sheet, children]) => ({ sheet, children }));

    const root = d3.hierarchy({ children: grouped }, d => d.children)
        .sum(d => d.marketCap || 0)
        .sort((a, b) => (b.value || 0) - (a.value || 0));

    d3.treemap()
        .size([width, height])
        .paddingOuter(3)
        .paddingTop(d => d.depth === 1 ? 18 : 0)
        .paddingInner(2)
        (root);

    const svg = d3.select(container)
        .append("svg")
        .attr("width", width)
        .attr("height", height);

    // Sector group backgrounds + labels
    const sectorGroups = svg.selectAll("g.sector")
        .data(root.children || [])
        .enter()
        .append("g")
        .attr("class", "sector");

    sectorGroups.append("rect")
        .attr("x", d => d.x0)
        .attr("y", d => d.y0)
        .attr("width", d => Math.max(0, d.x1 - d.x0))
        .attr("height", d => Math.max(0, d.y1 - d.y0))
        .attr("fill", "none")
        .attr("stroke", "#2A313D")
        .attr("stroke-width", 1);

    sectorGroups.filter(d => (d.x1 - d.x0) > 60)
        .append("text")
        .attr("class", "sector-label")
        .attr("x", d => d.x0 + 4)
        .attr("y", d => d.y0 + 13)
        .text(d => d.data.sheet);

    // Leaf tiles
    const nodes = svg.selectAll("g.tile")
        .data(root.leaves())
        .enter()
        .append("g")
        .attr("class", "tile")
        .attr("transform", d => `translate(${d.x0},${d.y0})`);

    nodes.append("rect")
        .attr("class", "tile-rect")
        .attr("width", d => Math.max(0, d.x1 - d.x0))
        .attr("height", d => Math.max(0, d.y1 - d.y0))
        .attr("fill", d => color(d.data.changePercent))
        .on("click", (e, d) => openScreener(d))
        .on("mousemove", showTooltip)
        .on("mouseleave", hideTooltip);

    // Native tooltip fallback — always available, even on the tiniest tiles
    nodes.append("title")
        .text(d => `${d.data.symbol} — ${Number(d.data.changePercent).toFixed(2)}%`);

    // Labels — font size scales with tile size instead of a hard show/hide cutoff
    drawLabels(nodes);
}
