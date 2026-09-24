const BASE_URL = "https://tiny-art-8473.dobbyop09.workers.dev";
const TOGGLE_URL = `${BASE_URL}/toggle`;

const segButtons = document.querySelectorAll(".provider-seg");

const modal = document.getElementById("providerModal");
const closeBtn = document.getElementById("providerCloseBtn");
const cancelBtn = document.getElementById("providerCancelBtn");
const saveBtn = document.getElementById("providerSaveBtn");
const modalMsg = document.getElementById("providerModalMsg");
const optionButtons = document.querySelectorAll(".provider-option");

let currentProvider = null; // value currently saved on the server / reflected in the topbar
let pendingProvider = null; // value selected inside the open modal, not yet confirmed

function paintToggle(provider) {
    segButtons.forEach(btn => {
        btn.classList.toggle("active", btn.dataset.value === provider);
    });
}

function paintModalSelection(provider) {
    optionButtons.forEach(btn => {
        btn.classList.toggle("selected", btn.dataset.value === provider);
    });
}

async function fetchProvider() {
    try {
        const res = await fetch(`${TOGGLE_URL}?t=${Date.now()}`);
        if (!res.ok) throw new Error(`Status ${res.status}`);
        const data = await res.json();
        currentProvider = data.marketDataProvider || null;
        paintToggle(currentProvider);
    } catch (e) {
        console.error("Failed to load market data provider", e);
        currentProvider = null;
        paintToggle(null);
    }
}

function openModal(targetProvider) {
    pendingProvider = targetProvider;
    modalMsg.textContent = "";
    paintModalSelection(pendingProvider);
    saveBtn.disabled = false;
    saveBtn.textContent = "Update";
    modal.hidden = false;
}

function closeModal() {
    modal.hidden = true;
    pendingProvider = null;
}

async function confirmSwitch() {
    if (!pendingProvider) return;
    const value = pendingProvider;

    saveBtn.disabled = true;
    saveBtn.textContent = "Updating...";
    modalMsg.textContent = "";

    try {
        const res = await fetch(TOGGLE_URL, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ marketDataProvider: value })
        });

        if (!res.ok) {
            let msg = "Something went wrong.";
            try {
                const data = await res.json();
                msg = data.error || msg;
            } catch { /* no body */ }
            throw new Error(msg);
        }

        currentProvider = value;
        paintToggle(currentProvider);
        closeModal();
    } catch (e) {
        console.error("Failed to save market data provider", e);
        modalMsg.textContent = e.message || "Failed to update. Try again.";
        saveBtn.disabled = false;
        saveBtn.textContent = "Update";
    }
}

export function initProvider() {
    segButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            const value = btn.dataset.value;
            if (value === currentProvider) return; // already active, nothing to confirm
            openModal(value);
        });
    });

    optionButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            pendingProvider = btn.dataset.value;
            paintModalSelection(pendingProvider);
        });
    });

    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);
    saveBtn.addEventListener("click", confirmSwitch);

    modal.addEventListener("click", (e) => {
        if (e.target === modal) closeModal();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modal.hidden) closeModal();
    });

    fetchProvider();
}