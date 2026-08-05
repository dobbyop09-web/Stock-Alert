// ── Theme-color helpers ─────────────────────────────────────────────
// Reads the actual accent colors from variables.css at runtime, so the
// heatmap always matches whatever the theme currently defines — change
// --watch / --triggered once, and the treemap follows automatically.
function getCSSVar(name, fallback) {
    const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return v || fallback;
}

function hexToRgb(hex) {
    const h = hex.replace("#", "");
    const full = h.length === 3 ? h.split("").map(c => c + c).join("") : h;
    const num = parseInt(full, 16);
    return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 };
}

function rgbToHex({ r, g, b }) {
    return "#" + [r, g, b]
        .map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, "0"))
        .join("");
}

// Mix a color toward black by `amount` (0 = unchanged, 1 = black) —
// used to make bigger moves render as deeper/darker tiles.
function darken(hex, amount) {
    const { r, g, b } = hexToRgb(hex);
    return rgbToHex({
        r: r * (1 - amount),
        g: g * (1 - amount),
        b: b * (1 - amount),
    });
}

const ACCENT_UP = getCSSVar("--watch", "#2DD4BF");
const ACCENT_DOWN = getCSSVar("--triggered", "#FF5C6C");

// Single source of truth for the six change% ranges. Order matters — each
// row falls into exactly one bucket, first match wins. Colors form a
// gradient off the theme accents: closer to 0% is brighter, bigger moves
// are darker — same shape as before, just theme-colored instead of
// stock-market red/green.
export const BUCKETS = [
    { id: "lt-5", label: "< -5%", color: darken(ACCENT_DOWN, 0.55), test: c => c <= -5 },
    { id: "-5to-2", label: "-5% to -2%", color: darken(ACCENT_DOWN, 0.3), test: c => c <= -2 },
    { id: "-2to0", label: "-2% to 0%", color: ACCENT_DOWN, test: c => c < 0 },
    { id: "0to2", label: "0% to 2%", color: ACCENT_UP, test: c => c < 2 },
    { id: "2to5", label: "2% to 5%", color: darken(ACCENT_UP, 0.3), test: c => c < 5 },
    { id: "gt5", label: "> 5%", color: darken(ACCENT_UP, 0.55), test: () => true },
];

export function getBucket(change) {
    return BUCKETS.find(b => b.test(change));
}

export function color(change) {
    return getBucket(change).color;
}

export function fmtNum(n) {
    if (n === undefined || n === null || isNaN(n)) return "-";
    if (Math.abs(n) >= 1e7) return (n / 1e7).toFixed(2) + "Cr";
    if (Math.abs(n) >= 1e5) return (n / 1e5).toFixed(2) + "L";
    return Number(n).toLocaleString("en-IN");
}

export function truncateToWidth(text, maxWidth, fontSize) {
    const avgCharWidth = fontSize * 0.6;
    const maxChars = Math.max(1, Math.floor(maxWidth / avgCharWidth));
    if (text.length <= maxChars) return text;
    return text.slice(0, Math.max(1, maxChars - 1)) + "…";
}