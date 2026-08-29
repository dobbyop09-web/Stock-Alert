import { CLIENT_ID, GOOGLE_SHEETS_SCOPE } from "./config.js";
import { state } from "./state.js";

const loginBtn = document.getElementById("loginBtn");
const loginBtnText = document.getElementById("loginBtnText");
const authStatus = document.getElementById("authStatus");
const authMenu = document.getElementById("authMenu");
const authDropdown = document.getElementById("authDropdown");
const signOutBtn = document.getElementById("signOutGoogleBtn");

function closeDropdown() {
    authDropdown.hidden = true;
    authMenu.classList.remove("open");
}

function openDropdown() {
    authDropdown.hidden = false;
    authMenu.classList.add("open");
}

function setConnectedUI() {
    loginBtn.classList.add("connected");
    loginBtnText.textContent = "Connected";
    loginBtn.title = "Signed in with Google — click to manage";
}

function setSignedOutUI() {
    loginBtn.classList.remove("connected");
    loginBtnText.textContent = "Sign in with Google";
    loginBtn.title = "";
    closeDropdown();
}

function handleAuthResponse(response) {
    if (response.error) {
        console.error("Google auth error:", response);
        authStatus.textContent = "Sign-in failed: " + response.error;
        return;
    }
    state.accessToken = response.access_token;
    sessionStorage.setItem("accessToken", state.accessToken);
    setConnectedUI();
    authStatus.textContent = "";
}

function signOut() {
    const token = state.accessToken;
    closeDropdown();

    const finish = () => {
        state.accessToken = null;
        sessionStorage.removeItem("accessToken");
        setSignedOutUI();
    };

    if (token && window.google?.accounts?.oauth2?.revoke) {
        google.accounts.oauth2.revoke(token, finish);
    } else {
        finish();
    }
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
        if (state.accessToken) setConnectedUI(); else setSignedOutUI();
        authStatus.textContent = "";
    } catch (e) {
        console.error("Failed to initialize Google Identity Services:", e);
        loginBtn.disabled = true;
        loginBtnText.textContent = "Sign-in unavailable";
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
        loginBtnText.textContent = "Sign-in unavailable";
        authStatus.textContent = "Could not load Google Identity Services. Check your connection or ad blocker.";
        return;
    }
    setTimeout(() => waitForGoogleIdentity(attemptsLeft - 1), 200);
}

export function initAuth() {
    loginBtn.addEventListener("click", () => {
        // Already connected: this pill now opens a small "Sign out" menu
        // instead of silently re-requesting a token.
        if (state.accessToken) {
            authDropdown.hidden ? openDropdown() : closeDropdown();
            return;
        }
        if (!state.tokenClient) {
            authStatus.textContent = "Still loading, try again in a moment.";
            return;
        }
        state.tokenClient.requestAccessToken({ prompt: "consent" });
    });

    signOutBtn.addEventListener("click", signOut);

    document.addEventListener("click", (e) => {
        if (!authMenu.contains(e.target)) closeDropdown();
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeDropdown();
    });

    waitForGoogleIdentity(50);
}