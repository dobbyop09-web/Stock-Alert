// Single source of truth for the six change% ranges. Order matters — each
// row falls into exactly one bucket, first match wins.
export const BUCKETS = [
    { id: "lt-5", label: "< -5%", color: "#7f1d1d", test: c => c <= -5 },
    { id: "-5to-2", label: "-5% to -2%", color: "#b91c1c", test: c => c <= -2 },
    { id: "-2to0", label: "-2% to 0%", color: "#ef4444", test: c => c < 0 },
    { id: "0to2", label: "0% to 2%", color: "#22c55e", test: c => c < 2 },
    { id: "2to5", label: "2% to 5%", color: "#16a34a", test: c => c < 5 },
    { id: "gt5", label: "> 5%", color: "#166534", test: () => true },
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