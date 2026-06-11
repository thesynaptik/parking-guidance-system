package com.parking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int paymentId;
    private final int ticketId;
    private final int entryId;
    private final String plateNumber;
    private final double amount;
    private final LocalDateTime paymentTime;

    public PaymentRecord(int paymentId, int ticketId, int entryId, String plateNumber, double amount, LocalDateTime paymentTime) {
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.entryId = entryId;
        this.plateNumber = plateNumber;
        this.amount = amount;
        this.paymentTime = paymentTime;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public int getEntryId() {
        return entryId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public String toCsv() {
        return paymentId + "," + ticketId + "," + entryId + "," + plateNumber + "," + amount + "," + paymentTime.format(FORMATTER);
    }

    public static PaymentRecord fromCsv(String line) {
        String[] p = line.split(",");
        return new PaymentRecord(
                Integer.parseInt(p[0]),
                Integer.parseInt(p[1]),
                Integer.parseInt(p[2]),
                p[3],
                Double.parseDouble(p[4]),
                LocalDateTime.parse(p[5], FORMATTER)
        );
    }
}
