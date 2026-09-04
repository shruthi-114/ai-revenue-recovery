// Plain vanilla JavaScript - no React, no build step, just fetch() and the DOM.
// This talks to the Java backend running on the same server.

const loadBtn = document.getElementById("loadBtn");
const retryBtn = document.getElementById("retryBtn");
const txnBody = document.getElementById("txnBody");

let currentTransactions = [];

loadBtn.addEventListener("click", loadTransactions);
retryBtn.addEventListener("click", runRetries);

async function loadTransactions() {
  loadBtn.textContent = "Loading...";
  const response = await fetch("/api/transactions");
  currentTransactions = await response.json();
  renderTable(currentTransactions);
  updateStats(currentTransactions);
  loadBtn.textContent = "Reload Failed Payments";
  retryBtn.disabled = false;
}

async function runRetries() {
  retryBtn.textContent = "Running Smart Retry...";
  const response = await fetch("/api/retry", { method: "POST" });
  currentTransactions = await response.json();
  renderTable(currentTransactions);
  updateStats(currentTransactions);
  retryBtn.textContent = "Run Smart Retry Again";
}

function renderTable(transactions) {
  txnBody.innerHTML = "";

  transactions.forEach(t => {
    const row = document.createElement("tr");

    row.innerHTML = `
      <td>${t.id}</td>
      <td>${t.customerName}</td>
      <td>₹${t.amount.toFixed(2)}</td>
      <td>${formatReason(t.failureReason)}</td>
      <td>${t.recoveryScore}/100</td>
      <td>${decisionBadge(t.decision)}</td>
      <td>${t.suggestedRetryTime}</td>
      <td>${resultLabel(t)}</td>
    `;

    txnBody.appendChild(row);
  });
}

function formatReason(reason) {
  return reason.replace(/_/g, " ");
}

function decisionBadge(decision) {
  const map = {
    "RETRY_NOW": ["badge-retry-now", "Retry Now"],
    "RETRY_LATER": ["badge-retry-later", "Retry Later"],
    "SEND_REMINDER": ["badge-reminder", "Send Reminder"],
    "DO_NOT_RETRY": ["badge-no-retry", "Do Not Retry"]
  };
  const [cls, label] = map[decision] || ["badge-no-retry", decision];
  return `<span class="badge ${cls}">${label}</span>`;
}

function resultLabel(t) {
  // If we haven't run the retry simulation yet, "recovered" will just be false
  // for everyone and there's no point showing a misleading "Failed" - so we
  // check the decision to know whether a retry was even attempted.
  if (t.decision === "DO_NOT_RETRY" || t.decision === "SEND_REMINDER") {
    return `<span class="result-pending">Not attempted</span>`;
  }
  if (t.recovered) {
    return `<span class="result-yes">Recovered ✓</span>`;
  }
  return `<span class="result-pending">Pending</span>`;
}

function updateStats(transactions) {
  const totalFailed = transactions.reduce((sum, t) => sum + t.amount, 0);
  const recoveredAmount = transactions
    .filter(t => t.recovered)
    .reduce((sum, t) => sum + t.amount, 0);
  const rate = totalFailed === 0 ? 0 : (recoveredAmount / totalFailed) * 100;

  document.getElementById("totalFailed").textContent = "₹" + totalFailed.toFixed(2);
  document.getElementById("recoveredAmount").textContent = "₹" + recoveredAmount.toFixed(2);
  document.getElementById("recoveryRate").textContent = rate.toFixed(1) + "%";
  document.getElementById("txnCount").textContent = transactions.length;
}
