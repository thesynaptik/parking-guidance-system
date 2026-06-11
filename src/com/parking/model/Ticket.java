package com.parking.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int ticketId;
    private final int entryId;
    private final String plateNumber;
    private final int spotId;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private boolean paid;
    private double amountPaid;
    private String status;

    public Ticket(int ticketId, int entryId, String plateNumber, int spotId, LocalDateTime entryTime,
                  LocalDateTime exitTime, boolean paid, double amountPaid, String status) {
        this.ticketId = ticketId;
        this.entryId = entryId;
        this.plateNumber = plateNumber;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.paid = paid;
        this.amountPaid = amountPaid;
        this.status = status;
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

    public int getSpotId() {
        return spotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long calculateHours() {
        LocalDateTime end = exitTime == null ? LocalDateTime.now() : exitTime;
        long minutes = Duration.between(entryTime, end).toMinutes();
        long hours = minutes / 60;
        if (minutes % 60 != 0 || hours == 0) {
            hours++;
        }
        return hours;
    }

    public String toCsv() {
        return ticketId + "," + entryId + "," + plateNumber + "," + spotId + "," +
                entryTime.format(FORMATTER) + "," +
                (exitTime == null ? "" : exitTime.format(FORMATTER)) + "," +
                paid + "," + amountPaid + "," + status;
    }

    public static Ticket fromCsv(String line) {
        String[] p = line.split(",", -1);
        return new Ticket(
                Integer.parseInt(p[0]),
                Integer.parseInt(p[1]),
                p[2],
                Integer.parseInt(p[3]),
                LocalDateTime.parse(p[4], FORMATTER),
                p[5].isBlank() ? null : LocalDateTime.parse(p[5], FORMATTER),
                Boolean.parseBoolean(p[6]),
                Double.parseDouble(p[7]),
                p[8]
        );
    }
}
