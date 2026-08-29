// Reusable "Saved! / Failed" status modal — replaces bare browser alert()
// calls so Google Sheets save results (js/alerts.js) get a styled popup
// instead of the native alert box.

const CHECK_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="#0B1114" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="5,13 10,18 19,7" /></svg>';
const CROSS_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="#0B1114" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="6" y1="6" x2="18" y2="18" /><line x1="18" y1="6" x2="6" y2="18" /></svg>';

let bound = false;

function bindOnce() {
    if (bound) return;
    bound = true;
    const overlay = document.getElementById("statusModal");
    const ok = document.getElementById("statusModalOk");
    if (!overlay || !ok) return;
    ok.addEventListener("click", hideModal);
    overlay.addEventListener("click", e => { if (e.target === overlay) hideModal(); });
    document.addEventListener("keydown", e => { if (e.key === "Escape") hideModal(); });
}

export function showModal({ type = "success", title, message } = {}) {
    bindOnce();
    const overlay = document.getElementById("statusModal");
    const box = document.getElementById("statusModalBox");
    const circle = document.getElementById("statusModalCircle");
    const titleEl = document.getElementById("statusModalTitle");
    const msgEl = document.getElementById("statusModalMsg");
    if (!overlay) { alert(title || message || "Done"); return; } // fallback safety net

    box.classList.toggle("is-error", type === "error");
    circle.innerHTML = type === "error" ? CROSS_ICON : CHECK_ICON;
    titleEl.textContent = title || (type === "error" ? "Something went wrong" : "Saved!");
    msgEl.textContent = message || (type === "error" ? "Please try again." : "Your changes have been saved successfully.");
    overlay.hidden = false;
    document.getElementById("statusModalOk").focus();
}

export function hideModal() {
    const overlay = document.getElementById("statusModal");
    if (overlay) overlay.hidden = true;
}