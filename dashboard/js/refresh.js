import { REFRESH_WORKER_URL, REFRESH_TRIGGER_KEY, COOLDOWN_SECONDS } from "./config.js";

const refreshBtn = document.getElementById("refreshBtn");
const msgEl = document.getElementById("refreshMsg");
let cooldownInterval = null;

function startCooldown(seconds) {
    let remaining = seconds;
    refreshBtn.disabled = true;
    refreshBtn.textContent = `Wait ${remaining}s`;

    clearInterval(cooldownInterval);
    cooldownInterval = setInterval(() => {
        remaining--;
        if (remaining <= 0) {
            clearInterval(cooldownInterval);
            refreshBtn.disabled = false;
            refreshBtn.textContent = "Refresh Now";
        } else {
            refreshBtn.textContent = `Wait ${remaining}s`;
        }
    }, 1000);
}

function failRefresh(message) {
    msgEl.textContent = message;
    refreshBtn.disabled = false;
    refreshBtn.textContent = "Failed — try again";
    setTimeout(() => { refreshBtn.textContent = "Refresh Now"; }, 3000);
}

async function triggerRefresh() {
    refreshBtn.disabled = true;
    refreshBtn.textContent = "Triggering...";
    msgEl.textContent = "";

    try {
        const res = await fetch(REFRESH_WORKER_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "x-trigger-key": REFRESH_TRIGGER_KEY
            }
        });

        let data = {};
        try { data = await res.json(); } catch { /* no body */ }

        if (res.status === 429) {
            const wait = data.retryAfter || COOLDOWN_SECONDS;
            msgEl.textContent = `Rate limited — please wait ${Math.ceil(wait / 60)} min.`;
            refreshBtn.textContent = "Refresh Now";
            startCooldown(wait);
            return;
        }

        if (res.ok) {
            msgEl.textContent = "";
            startCooldown(COOLDOWN_SECONDS);
        } else {
            failRefresh(data.error || "Something went wrong.");
        }
    } catch {
        failRefresh("Network error.");
    }
}

export function initRefresh() {
    refreshBtn.addEventListener("click", triggerRefresh);
}
