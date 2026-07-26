export function color(change) {
    if (change <= -5) return "#7f1d1d";
    if (change <= -2) return "#b91c1c";
    if (change < 0) return "#ef4444";
    if (change < 2) return "#22c55e";
    if (change < 5) return "#16a34a";
    return "#166534";
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
