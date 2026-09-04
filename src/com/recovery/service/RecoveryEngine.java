package com.recovery.service;

import com.recovery.model.Transaction;
import java.util.List;
import java.util.Random;

/**
 * RecoveryEngine
 * ----------------
 * This is the "brain" of the project. When a payment fails, Razorpay
 * (or any payment gateway) has to decide: is it even worth retrying this
 * payment, and if yes, when?
 *
 * Instead of a black-box machine learning model (which would need a
 * training dataset we don't have, and would be hard to explain to a
 * judge in 5 minutes), I built this as a transparent, rule based
 * SCORING engine. Every failed transaction gets a "recovery score" from
 * 0 to 100 based on a few signals that actually matter in the real
 * world. This is sometimes called "explainable AI" - simple enough to
 * reason about, but it mimics exactly what a trained model would learn
 * to weigh anyway (I researched common reasons UPI/card payments fail
 * before deciding these weights).
 *
 * Signals used:
 *  1. Failure reason        - some failures are temporary (network glitch,
 *                              OTP timeout) and some are not (bank declined
 *                              due to fraud check). Temporary ones score higher.
 *  2. Customer history      - a customer who has paid successfully many
 *                              times before is low risk, so we push harder
 *                              to recover their payment.
 *  3. Retry attempts so far - if we've already retried 3 times and it kept
 *                              failing, retrying again has diminishing returns.
 *                              We reduce the score so we don't spam the customer.
 *  4. Time of day            - payments that failed late at night or very
 *                              early morning are more likely to be network
 *                              related blips, not real declines.
 */
public class RecoveryEngine {

    private final Random random = new Random();

    /**
     * Scores every transaction in the list and fills in the recovery
     * fields (score, decision, suggested retry time) directly on the
     * Transaction objects.
     */
    public void scoreAll(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            int score = calculateScore(t);
            t.setRecoveryScore(score);
            t.setDecision(decideAction(score, t));
            t.setSuggestedRetryTime(suggestRetryTime(t));
        }
    }

    /**
     * The actual scoring formula. Everything starts at a base of 50 and
     * moves up or down depending on the signals. Clamped to 0-100 at the end.
     */
    private int calculateScore(Transaction t) {
        int score = 50;

        // 1. Failure reason - the single biggest factor
        switch (t.getFailureReason()) {
            case "network_error":
                score += 30; // almost always safe to retry, nothing wrong with the customer
                break;
            case "otp_timeout":
                score += 22; // customer was probably just slow, easy win
                break;
            case "insufficient_funds":
                score += 5;  // needs a smarter retry time, not an instant retry
                break;
            case "bank_declined":
                score -= 20; // could be a real decline / fraud check, be careful
                break;
            default:
                score += 0;
        }

        // 2. Customer history - reward loyal, reliable customers
        if (t.getPastSuccessfulPayments() >= 10) {
            score += 15;
        } else if (t.getPastSuccessfulPayments() >= 3) {
            score += 8;
        } else if (t.getPastSuccessfulPayments() == 0) {
            score -= 5; // brand new customer, less data to trust
        }

        // 3. Retry attempts already made - diminishing returns
        score -= (t.getRetryAttemptsSoFar() * 12);

        // 4. Time of day - late night / early morning failures are
        //    usually connectivity blips, not real declines
        int hour = safeParseHour(t.getFailedAtHour());
        if (hour >= 23 || hour <= 5) {
            score += 8;
        }

        // clamp between 0 and 100
        if (score > 100) score = 100;
        if (score < 0) score = 0;
        return score;
    }

    private int safeParseHour(String h) {
        try {
            return Integer.parseInt(h);
        } catch (Exception e) {
            return 12; // default to noon if the value is weird
        }
    }

    /**
     * Turns a raw score into an actual business decision.
     */
    private String decideAction(int score, Transaction t) {
        if (t.getFailureReason().equals("insufficient_funds") && t.getRetryAttemptsSoFar() == 0) {
            // never instantly retry an insufficient-funds failure, wait for a better time
            return "SEND_REMINDER";
        }
        if (score >= 70) {
            return "RETRY_NOW";
        } else if (score >= 40) {
            return "RETRY_LATER";
        } else {
            return "DO_NOT_RETRY";
        }
    }

    /**
     * Suggests a human-readable retry window. This is intentionally
     * simple heuristics, not a full scheduling system - good enough to
     * demo the idea.
     */
    private String suggestRetryTime(Transaction t) {
        switch (t.getDecision()) {
            case "RETRY_NOW":
                return "Immediately";
            case "RETRY_LATER":
                return "In 3 hours";
            case "SEND_REMINDER":
                return "1st of next month (likely salary day)";
            default:
                return "Not recommended";
        }
    }

    /**
     * Simulates actually attempting the retries for transactions marked
     * RETRY_NOW or RETRY_LATER, so the dashboard has something to show.
     * The success chance is tied to the recovery score - higher score,
     * higher chance of success. This is a simulation since we don't have
     * a live payment gateway connected in this hackathon build.
     */
    public void simulateRetries(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            if (t.getDecision().equals("RETRY_NOW") || t.getDecision().equals("RETRY_LATER")) {
                double successChance = t.getRecoveryScore() / 100.0;
                boolean success = random.nextDouble() < successChance;
                t.setRecovered(success);
            } else {
                t.setRecovered(false);
            }
        }
    }
}
