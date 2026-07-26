import { SPREADSHEET_ID } from "./config.js";
import { state } from "./state.js";
import { render } from "./render.js";
import { loadDashboard } from "./data-loader.js";

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

    row.cells[6].innerHTML = `<button onclick="saveAlert('${symbol}')">💾</button>`;
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
        alert("Stock not found.");
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
        alert("Failed to save");
        return;
    }

    stock.alertPrice = Number(newAlert);
    stock.fib = newFib;
    document.getElementById("alertHeader").textContent = "Alert";
    render();

    alert("Saved!");
}
