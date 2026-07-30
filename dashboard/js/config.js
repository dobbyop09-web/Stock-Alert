export const CLIENT_ID = "105417196420-ln76t9mgghpdpa1d9sfrash34gdoarde.apps.googleusercontent.com";
export const SPREADSHEET_ID = "1AQ93bxs1qthy6WSqtkV-DhMYjAMaZrsQLLabk_KGgyQ";
export const REFRESH_WORKER_URL = "https://tiny-art-8473.dobbyop09.workers.dev/refresh";
// NOTE: this key is shipped to every browser that loads the page, so it only
// works as a light rate-limit deterrent, not real authentication. If this
// endpoint needs real protection, move the trigger behind a server route
// that holds the secret instead.
export const REFRESH_TRIGGER_KEY = "97c15c54-40bf-479a-a4e2-8c470506502c";
export const COOLDOWN_SECONDS = 120;
export const GOOGLE_SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets";
