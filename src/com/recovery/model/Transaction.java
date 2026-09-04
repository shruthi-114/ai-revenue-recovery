package com.recovery.model;

/**
 * A single failed payment transaction.
 *
 * This is just a plain data holder (no fancy annotations, no Lombok,
 * nothing magic) so it's easy to read for anyone reviewing the code.
 */
public class Transaction {

    private String id;
    private String customerName;
    private double amount;              // amount in rupees
    private String failureReason;       // e.g. insufficient_funds, network_error, bank_declined, otp_timeout
    private int pastSuccessfulPayments; // how many times this customer has paid successfully before
    private int retryAttemptsSoFar;     // how many times we already tried to retry this payment
    private String failedAtHour;        // hour of day the payment failed, e.g. "14" for 2 PM (24 hr format, as string)

    // recovery fields - filled in AFTER the engine scores the transaction
    private int recoveryScore;          // 0 to 100
    private String decision;            // RETRY_NOW, RETRY_LATER, SEND_REMINDER, DO_NOT_RETRY
    private String suggestedRetryTime;  // human readable suggestion
    private boolean recovered;          // did the retry actually succeed (simulated)

    public Transaction(String id, String customerName, double amount, String failureReason,
                        int pastSuccessfulPayments, int retryAttemptsSoFar, String failedAtHour) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.failureReason = failureReason;
        this.pastSuccessfulPayments = pastSuccessfulPayments;
        this.retryAttemptsSoFar = retryAttemptsSoFar;
        this.failedAtHour = failedAtHour;
    }

    // ---- getters ----
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public double getAmount() { return amount; }
    public String getFailureReason() { return failureReason; }
    public int getPastSuccessfulPayments() { return pastSuccessfulPayments; }
    public int getRetryAttemptsSoFar() { return retryAttemptsSoFar; }
    public String getFailedAtHour() { return failedAtHour; }
    public int getRecoveryScore() { return recoveryScore; }
    public String getDecision() { return decision; }
    public String getSuggestedRetryTime() { return suggestedRetryTime; }
    public boolean isRecovered() { return recovered; }

    // ---- setters used by the recovery engine ----
    public void setRecoveryScore(int recoveryScore) { this.recoveryScore = recoveryScore; }
    public void setDecision(String decision) { this.decision = decision; }
    public void setSuggestedRetryTime(String suggestedRetryTime) { this.suggestedRetryTime = suggestedRetryTime; }
    public void setRecovered(boolean recovered) { this.recovered = recovered; }

    /**
     * Turns this object into a JSON string by hand.
     * I avoided pulling in an external JSON library (like Gson or Jackson)
     * on purpose - the data shape here is small and fixed, so writing it
     * out manually keeps the whole project dependency-free. You can just
     * javac + java it, no Maven/Gradle setup needed.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(id).append("\",");
        sb.append("\"customerName\":\"").append(customerName).append("\",");
        sb.append("\"amount\":").append(amount).append(",");
        sb.append("\"failureReason\":\"").append(failureReason).append("\",");
        sb.append("\"pastSuccessfulPayments\":").append(pastSuccessfulPayments).append(",");
        sb.append("\"retryAttemptsSoFar\":").append(retryAttemptsSoFar).append(",");
        sb.append("\"failedAtHour\":\"").append(failedAtHour).append("\",");
        sb.append("\"recoveryScore\":").append(recoveryScore).append(",");
        sb.append("\"decision\":\"").append(decision == null ? "" : decision).append("\",");
        sb.append("\"suggestedRetryTime\":\"").append(suggestedRetryTime == null ? "" : suggestedRetryTime).append("\",");
        sb.append("\"recovered\":").append(recovered);
        sb.append("}");
        return sb.toString();
    }
}
