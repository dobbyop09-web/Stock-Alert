import { BUCKETS } from "./utils.js";

export const state = {
    allRows: [],
    sectorFilter: "",   // "" | sector name — set by clicking the sidebar list
    sortMode: "marketCap",
    activeBuckets: new Set(BUCKETS.map(b => b.id)),   // all ranges shown by default
};