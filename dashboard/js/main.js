import { initAuth } from "./auth.js";
import { initFilters } from "./filters.js";
import { initRefresh } from "./refresh.js";
import { loadDashboard } from "./data-loader.js";
import { renderTicker } from './ticker.js';
import { initHistoricalAlerts } from "./historical-alerts.js";
import { initProvider } from "./provider.js";

// editAlert/saveAlert have been moved to stock-config.html.
// They are no longer rendered in index.html rows, so no window globals needed.

initAuth();
initFilters();
initRefresh();
initHistoricalAlerts();
initProvider();
loadDashboard();