package com.parking.model;

import java.time.LocalDate;

public class ShiftRecord {
    private final String operatorUsername;
    private final Role role;
    private final LocalDate date;
    private final double totalPayments;

    public ShiftRecord(String operatorUsername, Role role, LocalDate date, double totalPayments) {
        this.operatorUsername = operatorUsername;
        this.role = role;
        this.date = date;
        this.totalPayments = totalPayments;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public Role getRole() {
        return role;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getTotalPayments() {
        return totalPayments;
    }
}
