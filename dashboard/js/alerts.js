import { SPREADSHEET_ID } from "./config.js";
import { state } from "./state.js";
import { render } from "./render.js";
import { loadDashboard } from "./data-loader.js";
import { showModal } from "./modal.js";

export function editAlert(symbol, btn) {
    document.getElementById("alertHeader").innerHTML = "✏️ Editing";
    const row = btn.closest("tr");
    const stock = state.allRows.find(s => s.symbol === symbol);

    row.cells[2].style.position = "relative";
    row.cells[2].innerHTML = `
        <div class="alert-edit-row">
            <span class="fib-label">Alert</span>
            <input id="edit-${symbol}" class="edit-cell" value="${stock.alertPrice}">
            <span class="fib-label">Fib</span>
            <input id="edit-fib-${symbol}" class="edit-cell" value="${stock.fib ?? ''}">
        </div>
    `;
row.cells[6].innerHTML = `
    <button class="icon-btn save" onclick="saveAlert('${symbol}')">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z" />
            <path d="M17 21v-8H7v8M7 3v5h8" />
        </svg>
    </button>
`;
}

async function findRow(sheetName, symbol) {
    const url = `https://sheets.googleapis.com/v4/spreadsheets/${SPREADSHEET_ID}/values/${encodeURIComponent(sheetName)}!A:A`;

    const res = await fetch(url, {
        headers: { Authorization: "Bearer " + state.accessToken }
    });
    const data = await res.json();
    const rows = data.values || [];

    for (let i = 0; i < rows.length; i++) {
        if (rows[i][0] === symbol) return i + 1;
    }
    return -1;
}

export async function saveAlert(symbol) {
    const input = document.getElementById("edit-" + symbol);
    const fibInput = document.getElementById("edit-fib-" + symbol);

    const newAlert = input.value;
    const newFib = fibInput.value;

    const stock = state.allRows.find(s => s.symbol === symbol);
    const row = await findRow(stock.sheet, symbol);

    if (row === -1) {
        document.getElementById("alertHeader").textContent = "Alert";
        loadDashboard();
        showModal({ type: "error", title: "Stock not found", message: `${symbol} could not be located in the sheet.` });
        return;
    }

    const url = `https://sheets.googleapis.com/v4/spreadsheets/${SPREADSHEET_ID}/values:batchUpdate`;
    const body = {
        valueInputOption: "USER_ENTERED",
        data: [
            { range: `${stock.sheet}!C${row}`, values: [[newAlert]] },
            { range: `${stock.sheet}!D${row}`, values: [[newFib]] }
        ]
    };

    const res = await fetch(url, {
        method: "POST",
        headers: {
            Authorization: "Bearer " + state.accessToken,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    });

    if (!res.ok) {
        document.getElementById("alertHeader").textContent = "Alert";
        loadDashboard();
        showModal({ type: "error", title: "Failed to save", message: "The Google Sheets update did not go through. Please try again." });
        return;
    }

    stock.alertPrice = Number(newAlert);
    stock.fib = newFib;
    document.getElementById("alertHeader").textContent = "Alert";
    render();

    showModal({ type: "success", title: "Saved!", message: "Your changes have been saved successfully." });
}