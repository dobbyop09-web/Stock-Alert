import { CLIENT_ID, GOOGLE_SHEETS_SCOPE } from "./config.js";
import { state } from "./state.js";

const loginBtn = document.getElementById("loginBtn");
const authStatus = document.getElementById("authStatus");

function handleAuthResponse(response) {
    if (response.error) {
        console.error("Google auth error:", response);
        authStatus.textContent = "Sign-in failed: " + response.error;
        return;
    }
    state.accessToken = response.access_token;
    sessionStorage.setItem("accessToken", state.accessToken);
    loginBtn.textContent = "✅ Connected";
    authStatus.textContent = "";
}

function initGoogleAuth() {
    try {
        state.tokenClient = google.accounts.oauth2.initTokenClient({
            client_id: CLIENT_ID,
            scope: GOOGLE_SHEETS_SCOPE,
            callback: handleAuthResponse,
            error_callback: (err) => {
                console.error("Google auth error_callback:", err);
                authStatus.textContent = "Sign-in error: " + (err && err.type ? err.type : "unknown");
            }
        });

        loginBtn.disabled = false;
        loginBtn.textContent = state.accessToken ? "✅ Connected" : "Sign in with Google";
        authStatus.textContent = "";
    } catch (e) {
        console.error("Failed to initialize Google Identity Services:", e);
        loginBtn.disabled = true;
        loginBtn.textContent = "Sign-in unavailable";
        authStatus.textContent = "Google script failed to load — check network/adblock.";
    }
}

function waitForGoogleIdentity(attemptsLeft) {
    if (window.google?.accounts?.oauth2) {
        initGoogleAuth();
        return;
    }
    if (attemptsLeft <= 0) {
        loginBtn.disabled = true;
        loginBtn.textContent = "Sign-in unavailable";
        authStatus.textContent = "Could not load Google Identity Services. Check your connection or ad blocker.";
        return;
    }
    setTimeout(() => waitForGoogleIdentity(attemptsLeft - 1), 200);
}

export function initAuth() {
    loginBtn.addEventListener("click", () => {
        if (!state.tokenClient) {
            authStatus.textContent = "Still loading, try again in a moment.";
            return;
        }
        state.tokenClient.requestAccessToken({
            prompt: state.accessToken ? "" : "consent"
        });
    });

    waitForGoogleIdentity(50);
}
