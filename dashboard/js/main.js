import { initAuth } from "./auth.js";
import { initFilters } from "./filters.js";
import { initRefresh } from "./refresh.js";
import { loadDashboard } from "./data-loader.js";
import { editAlert, saveAlert } from "./alerts.js";
import { renderTicker } from './ticker.js';


// editAlert/saveAlert are invoked via inline onclick="" attributes generated
// in render.js template strings, so they must exist on window.
window.editAlert = editAlert;
window.saveAlert = saveAlert;

initAuth();
initFilters();
initRefresh();
loadDashboard();