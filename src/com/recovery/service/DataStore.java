package com.recovery.service;

import com.recovery.model.Transaction;
import java.util.ArrayList;
import java.util.List;

/**
 * DataStore
 * ----------
 * Holds the failed transactions in memory. In a real production system
 * this would obviously be a database (Postgres/MySQL), but for this
 * hackathon build I kept it as a simple in-memory list so the project
 * runs instantly with zero setup - no DB install, no connection strings,
 * nothing to configure. Swapping this for a real DB later is a small,
 * isolated change (see README "Next Steps" section).
 */
public class DataStore {

    private final List<Transaction> transactions = new ArrayList<>();

    public DataStore() {
        seedSampleData();
    }

    public List<Transaction> getAll() {
        return transactions;
    }

    /**
     * A realistic looking set of failed payments, mixing different
     * failure reasons, customer histories and retry counts so the
     * dashboard actually shows a variety of decisions.
     */
    private void seedSampleData() {
        transactions.add(new Transaction("TXN1001", "Ramesh Iyer", 1499.00, "network_error", 12, 0, "23"));
        transactions.add(new Transaction("TXN1002", "Priya Nair", 799.00, "otp_timeout", 4, 0, "20"));
        transactions.add(new Transaction("TXN1003", "Ankit Sharma", 2999.00, "insufficient_funds", 8, 0, "10"));
        transactions.add(new Transaction("TXN1004", "Neha Gupta", 499.00, "bank_declined", 0, 1, "15"));
        transactions.add(new Transaction("TXN1005", "Suresh Kumar", 5999.00, "network_error", 2, 0, "02"));
        transactions.add(new Transaction("TXN1006", "Fatima Sheikh", 1200.00, "otp_timeout", 15, 1, "18"));
        transactions.add(new Transaction("TXN1007", "Rahul Verma", 349.00, "insufficient_funds", 1, 2, "11"));
        transactions.add(new Transaction("TXN1008", "Divya Menon", 8999.00, "bank_declined", 20, 0, "13"));
        transactions.add(new Transaction("TXN1009", "Karthik Raja", 699.00, "network_error", 0, 0, "04"));
        transactions.add(new Transaction("TXN1010", "Sneha Reddy", 1999.00, "otp_timeout", 6, 3, "09"));
        transactions.add(new Transaction("TXN1011", "Vikram Singh", 3499.00, "bank_declined", 3, 0, "16"));
        transactions.add(new Transaction("TXN1012", "Anjali Desai", 899.00, "network_error", 9, 1, "22"));
        transactions.add(new Transaction("TXN1013", "Manoj Pillai", 249.00, "insufficient_funds", 0, 0, "14"));
        transactions.add(new Transaction("TXN1014", "Kavya Krishnan", 4599.00, "otp_timeout", 11, 0, "19"));
        transactions.add(new Transaction("TXN1015", "Arjun Malhotra", 1599.00, "network_error", 5, 2, "01"));
    }
}
