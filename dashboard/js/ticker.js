export function renderTicker(indices) {
    const el = document.getElementById('indexTicker');
    if (!el) return;

    const itemHtml = (row) => {
        const isUp = row.percentChange >= 0;
        const arrow = isUp ? '▲' : '▼';
        const sign = isUp ? '+' : '';
        return `

            <a class="ticker-item" href="${row.screenerUrl}" target="_blank" rel="noopener noreferrer">
                <span class="ticker-name">${row.name}</span>
                <span class="ticker-last">${row.last.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                <span class="ticker-change ${isUp ? 'up' : 'down'}">
                    ${arrow} ${sign}${row.percentChange.toFixed(2)}%
                </span>
            </a>
        `;
    };

    // duplicate once so translateX(-50%) loops seamlessly
    const singlePass = indices.map(itemHtml).join('');
    el.innerHTML = singlePass + singlePass;

    // Fixed 25s duration made the loop feel uneven depending on how many
    // indices load. Derive duration from actual content width instead, so
    // scroll speed stays constant (~40px/s) no matter the item count.
    requestAnimationFrame(() => {
        const singlePassWidth = el.scrollWidth / 2;
        const PIXELS_PER_SECOND = 40;
        el.style.animationDuration = `${(singlePassWidth / PIXELS_PER_SECOND).toFixed(2)}s`;
    });
}

async function loadIndices() {
    try {
        const res = await fetch(
            `https://tiny-art-8473.dobbyop09.workers.dev/dashboard-indices?t=${Date.now()}`
        );
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        renderTicker(data);
    } catch (err) {
        console.error('Failed to load index data:', err);
    }
}

loadIndices();