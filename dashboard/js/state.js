// Single source of truth for data + filter state. Other modules import
// `state` and mutate its fields directly (kept simple on purpose — no
// framework/store here), then call render() to reflect changes.
export const state = {
    rows: [],          // currently visible rows (search + sector + status applied)
    allRows: [],        // full dataset as loaded from dashboard-data.json
    contextRows: [],    // allRows filtered by search + sector, but NOT by status
    statusFilter: "",   // "" | "Triggered" | "Near" | "Watch"
    sectorFilter: "",   // "" | sector name
    accessToken: sessionStorage.getItem("accessToken"),
    tokenClient: null,
    marketCapAscending: false,
    dayChangeAscending: false,
};
