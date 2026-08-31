// Same sector colors used across the dashboard (index chips, sector badges).
const SECTOR_COLORS = {
    auto: "#9CA3AF",
    bank: "#818CF8",
    capitalmarket: "#2DD4BF",
    defence: "#60A5FA",
    fmcg: "#34D399",
    health: "#C084FC",
    it: "#22D3EE",
    metal: "#94A3B8",
    oilenergy: "#FB923C",
};
const SECTOR_COLOR_DEFAULT = "#9CA3AF";

// Same icon set used for sector chips elsewhere in the dashboard.
const SECTOR_ICONS = {
    auto: '<path d="M3 13l2-5a2 2 0 0 1 2-1.3h10a2 2 0 0 1 2 1.3l2 5" /><rect x="2" y="13" width="20" height="6" rx="1.5" /><circle cx="7" cy="19" r="1.6" /><circle cx="17" cy="19" r="1.6" />',
    bank: '<path d="M3 10l9-6 9 6" /><path d="M5 10v9M10 10v9M14 10v9M19 10v9" /><path d="M3 21h18" />',
    capitalmarket: '<path d="M4 19V10M10 19V5M16 19v-7M20 19V3" />',
    defence: '<path d="M12 2l8 3.5v5.4c0 5-3.4 8.6-8 11.1-4.6-2.5-8-6.1-8-11.1V5.5L12 2Z" />',
    fmcg: '<circle cx="9" cy="20" r="1.3" /><circle cx="17" cy="20" r="1.3" /><path d="M2.5 3h2.4l2.4 12.6a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21 7H6" />',
    health: '<path d="M20.8 8.6c0 5.6-8.8 10.9-8.8 10.9S3.2 14.2 3.2 8.6a4.6 4.6 0 0 1 8.8-1.9 4.6 4.6 0 0 1 8.8 1.9Z" />',
    it: '<rect x="3" y="4" width="18" height="12" rx="1.5" /><path d="M8 20h8M12 16v4" />',
    metal: '<circle cx="12" cy="12" r="2" /><ellipse cx="12" cy="12" rx="9" ry="3.6" /><ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(60 12 12)" /><ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(120 12 12)" />',
    oilenergy: '<path d="M12 2s6 6.5 6 11.5a6 6 0 0 1-12 0C6 8.5 12 2 12 2Z" />',
};
const SECTOR_ICON_DEFAULT = '<rect x="3" y="3" width="7" height="9" rx="1.2" /><rect x="14" y="3" width="7" height="5" rx="1.2" /><rect x="14" y="12" width="7" height="9" rx="1.2" /><rect x="3" y="16" width="7" height="5" rx="1.2" />';

// NIFTY index names don't map 1:1 to the "sheet" sector strings used
// elsewhere, so match on keywords instead.
const INDEX_SECTOR_RULES = [
    [/bank/i, "bank"],
    [/auto/i, "auto"],
    [/fmcg/i, "fmcg"],
    [/energy|oil\s*&?\s*gas/i, "oilenergy"],
    [/metal/i, "metal"],
    [/health/i, "health"],
    [/defence/i, "defence"],
    [/\bit\b/i, "it"],
    [/capital\s*markets?/i, "capitalmarket"],
];

function sectorKeyForIndex(name) {
    const hit = INDEX_SECTOR_RULES.find(([re]) => re.test(name));
    return hit ? hit[1] : "";
}

function hexToRgba(hex, a) {
    const n = parseInt(hex.slice(1), 16);
    return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`;
}

export function renderTicker(indices) {
    const el = document.getElementById('indexTicker');
    if (!el) return;

    const itemHtml = (row) => {
        const isUp = row.percentChange >= 0;
        const arrow = isUp ? '▲' : '▼';
        const sign = isUp ? '+' : '';
        const key = sectorKeyForIndex(row.name);
        const c = SECTOR_COLORS[key] || SECTOR_COLOR_DEFAULT;
        const icon = SECTOR_ICONS[key] || SECTOR_ICON_DEFAULT;
        const gradient = `linear-gradient(155deg, ${hexToRgba(c, .22)} 0%, ${hexToRgba(c, .07)} 55%, transparent 100%)`;
        return `
            <a class="ticker-item" href="${row.screenerUrl}" target="_blank" rel="noopener noreferrer" style="background:${gradient}">
                <span class="ticker-name" style="color:${c}">
                    <svg class="ticker-name-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${icon}</svg>
                    ${row.name}
                </span>
                <span class="ticker-last">${row.last.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                <span class="ticker-change ${isUp ? 'up' : 'down'}">
                    ${arrow} ${sign}${row.percentChange.toFixed(2)}%
                </span>
            </a>
        `;
    };

    // duplicate once so translateX loops seamlessly
    const singlePass = indices.map(itemHtml).join('');
    el.innerHTML = singlePass + singlePass;

    // Measuring scrollWidth too early (before web fonts finish loading) can
    // make the two halves end up slightly different widths, so the loop
    // visibly jumps/stutters at the seam. Wait for fonts + a settled layout
    // pass, then drive the animation off an exact pixel distance (not a
    // "-50%" transform, which is only as accurate as the two halves are
    // identical) and keep it in sync if the layout changes later (e.g. a
    // late-loading font or a window resize).
    const measureAndSync = () => {
        const singlePassWidth = el.scrollWidth / 2;
        if (!singlePassWidth) return;
        const PIXELS_PER_SECOND = 40;
        el.style.setProperty('--ticker-shift', `${singlePassWidth}px`);
        el.style.animationDuration = `${(singlePassWidth / PIXELS_PER_SECOND).toFixed(2)}s`;
    };

    const settle = () => requestAnimationFrame(() => requestAnimationFrame(measureAndSync));
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(settle);
    } else {
        settle();
    }

    if (!el._tickerResizeObserver && 'ResizeObserver' in window) {
        el._tickerResizeObserver = new ResizeObserver(() => measureAndSync());
        el._tickerResizeObserver.observe(el);
    }
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