export function renderTicker(indices) {
    const el = document.getElementById('indexTicker');
    if (!el) return;

    el.innerHTML = indices.map(row => {
        const isUp = row.percentChange >= 0;
        const arrow = isUp ? '▲' : '▼';
        const sign = isUp ? '+' : '';
        const screenerUrl = row.screenerUrl;

        return `
            <a class="ticker-item" href="${screenerUrl}" target="_blank" rel="noopener noreferrer">
                <span class="ticker-name">${row.name}</span>
                <span class="ticker-last">${row.last.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                <span class="ticker-change ${isUp ? 'up' : 'down'}">
                    ${arrow} ${sign}${row.percentChange.toFixed(2)}%
                </span>
            </a>
        `;
    }).join('');
}

export function enableTickerDragScroll() {
    const scrollEl = document.querySelector('.ticker-scroll');
    if (!scrollEl) return;

    // --- mouse wheel (vertical wheel -> horizontal scroll) ---
    scrollEl.addEventListener('wheel', (e) => {
        if (e.deltaY === 0) return; // let native horizontal trackpad swipes pass through
        e.preventDefault();
        scrollEl.scrollLeft += e.deltaY;
    }, { passive: false });

    // --- click-and-drag ---
    let isDown = false;
    let didDrag = false;
    let startX;
    let scrollLeft;

    scrollEl.addEventListener('mousedown', (e) => {
        isDown = true;
        didDrag = false;
        scrollEl.classList.add('dragging');
        startX = e.pageX - scrollEl.offsetLeft;
        scrollLeft = scrollEl.scrollLeft;
    });

    scrollEl.addEventListener('mouseleave', () => {
        isDown = false;
        scrollEl.classList.remove('dragging');
    });

    scrollEl.addEventListener('mouseup', () => {
        isDown = false;
        scrollEl.classList.remove('dragging');
    });

    scrollEl.addEventListener('mousemove', (e) => {
        if (!isDown) return;
        e.preventDefault();
        const x = e.pageX - scrollEl.offsetLeft;
        const walk = (x - startX) * 1.5;
        if (Math.abs(walk) > 5) didDrag = true; // treat as a drag past this threshold
        scrollEl.scrollLeft = scrollLeft - walk;
    });

    // suppress the click-through-to-link if the user was actually dragging
    scrollEl.addEventListener('click', (e) => {
        if (didDrag) {
            e.preventDefault();
            e.stopPropagation();
        }
    }, true); // capture phase, so it runs before the <a> tag's own navigation
}
async function loadIndices() {
    try {
            // cache-bust so the browser doesn't serve a stale copy of the static file
           const res = await fetch(`./dashboard-indices.json?t=${Date.now()}`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            renderTicker(data);
        } catch (err) {
            console.error('Failed to load index data:', err);
        }
}

enableTickerDragScroll();
loadIndices();