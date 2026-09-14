"use strict";

const resultEl = document.getElementById("result");
const errorEl = document.getElementById("error");

function showResult(text) {
    errorEl.hidden = true;
    errorEl.textContent = "";
    resultEl.textContent = text;
    resultEl.hidden = false;
}

function showError(text) {
    resultEl.hidden = true;
    resultEl.textContent = "";
    errorEl.textContent = text;
    errorEl.hidden = false;
}

function setLoading(button, isLoading, loadingLabel) {
    if (isLoading) {
        button.dataset.originalLabel = button.textContent;
        button.textContent = loadingLabel || "Loading...";
        button.disabled = true;
    } else {
        button.textContent = button.dataset.originalLabel || button.textContent;
        button.disabled = false;
    }
}

/**
 * Calls one of the server's JSON services and returns the parsed body.
 * Network failures (server unreachable) and HTTP error responses (4xx/5xx)
 * are kept distinct on purpose, so the two failure modes required by the
 * lab produce visibly different messages instead of one generic "error".
 */
async function callService(url, button, loadingLabel) {
    setLoading(button, true, loadingLabel);
    let response;
    try {
        response = await fetch(url);
    } catch (networkError) {
        setLoading(button, false);
        throw new Error("Network error: could not reach the server. Check your connection and try again.");
    }

    let body = null;
    try {
        body = await response.json();
    } catch (parseError) {
        body = null;
    }
    setLoading(button, false);

    if (!response.ok) {
        const message = body && body.error ? body.error : `Request failed with status ${response.status}`;
        throw new Error(message);
    }
    return body;
}

const greetingForm = document.getElementById("greeting-form");
greetingForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = greetingForm.querySelector("button");
    const name = document.getElementById("name-input").value;
    try {
        const data = await callService(`/greeting?name=${encodeURIComponent(name)}`, button);
        showResult(data.message);
    } catch (err) {
        showError(err.message);
    }
});

const squareForm = document.getElementById("square-form");
squareForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = squareForm.querySelector("button");
    const number = document.getElementById("number-input").value;
    try {
        const data = await callService(`/square?number=${encodeURIComponent(number)}`, button);
        showResult(`${data.input} squared is ${data.square}`);
    } catch (err) {
        showError(err.message);
    }
});

const timeButton = document.getElementById("time-button");
timeButton.addEventListener("click", async () => {
    try {
        const data = await callService("/time", timeButton);
        showResult(`Server time: ${data.serverTime}`);
    } catch (err) {
        showError(err.message);
    }
});

const slowButton = document.getElementById("slow-button");
slowButton.addEventListener("click", async () => {
    try {
        const startedAt = performance.now();
        const data = await callService("/slow", slowButton, "Waiting (5s)...");
        const elapsedMs = Math.round(performance.now() - startedAt);
        showResult(`${data.message} (took ${elapsedMs} ms). Try clicking another button in a second window while this one is loading.`);
    } catch (err) {
        showError(err.message);
    }
});

(async function checkHealth() {
    const statusEl = document.getElementById("status-badge");
    try {
        const response = await fetch("/health");
        const data = await response.json();
        statusEl.textContent = response.ok ? `Server status: ${data.status}` : "Server status: unknown";
    } catch (err) {
        statusEl.textContent = "Server status: unreachable";
    }
})();
