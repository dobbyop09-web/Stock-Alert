export function fmtPct(n) {
    return (n > 0 ? "+" : "") + Number(n).toFixed(1) + "%";
}

export function timeAgo(date) {
    const diff = Math.floor((Date.now() - date.getTime()) / 1000);

    if (diff < 60) return "just now";
    if (diff < 3600) return `${Math.floor(diff / 60)} min ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)} hr ago`;
    return `${Math.floor(diff / 86400)} day ago`;
}
