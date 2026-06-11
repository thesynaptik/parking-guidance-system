package com.parking.model;

public class ParkingSpot {
    private final int id;
    private boolean free;
    private String currentPlateNumber;

    public ParkingSpot(int id, boolean free, String currentPlateNumber) {
        this.id = id;
        this.free = free;
        this.currentPlateNumber = currentPlateNumber;
    }

    public int getId() {
        return id;
    }

    public boolean isFree() {
        return free;
    }

    public void occupy(String plateNumber) {
        this.free = false;
        this.currentPlateNumber = plateNumber;
    }

    public void release() {
        this.free = true;
        this.currentPlateNumber = "";
    }

    public String getCurrentPlateNumber() {
        return currentPlateNumber;
    }

    public String toCsv() {
        return id + "," + free + "," + (currentPlateNumber == null ? "" : currentPlateNumber);
    }

    public static ParkingSpot fromCsv(String line) {
        String[] parts = line.split(",", -1);
        return new ParkingSpot(
                Integer.parseInt(parts[0]),
                Boolean.parseBoolean(parts[1]),
                parts.length > 2 ? parts[2] : ""
        );
    }
}
