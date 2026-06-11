package com.parking.service;

import com.parking.model.ParkingSpot;
import com.parking.storage.DataStore;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParkingService extends BaseService {

    public ParkingService(DataStore dataStore) {
        super(dataStore);
    }

    @Override
    public String getServiceName() {
        return "ParkingService";
    }

    public List<ParkingSpot> getAllSpots() {
        return dataStore.getParkingSpots();
    }

    public List<ParkingSpot> getFreeSpots() {
        return getAllSpots().stream().filter(ParkingSpot::isFree).collect(Collectors.toList());
    }

    public Optional<ParkingSpot> getFirstFreeSpot() {
        return getAllSpots().stream().filter(ParkingSpot::isFree).findFirst();
    }

    public ParkingSpot addSpot() {
        List<ParkingSpot> spots = dataStore.getParkingSpots();
        ParkingSpot newSpot = new ParkingSpot(dataStore.nextSpotId(), true, "");
        spots.add(newSpot);
        dataStore.saveParkingSpots(spots);
        return newSpot;
    }

    public boolean occupySpot(int spotId, String plateNumber) {
        List<ParkingSpot> spots = dataStore.getParkingSpots();
        for (ParkingSpot spot : spots) {
            if (spot.getId() == spotId && spot.isFree()) {
                spot.occupy(plateNumber);
                dataStore.saveParkingSpots(spots);
                return true;
            }
        }
        return false;
    }

    public boolean releaseSpot(int spotId) {
        List<ParkingSpot> spots = dataStore.getParkingSpots();
        for (ParkingSpot spot : spots) {
            if (spot.getId() == spotId) {
                spot.release();
                dataStore.saveParkingSpots(spots);
                return true;
            }
        }
        return false;
    }

    public int getTotalSpots() {
        return dataStore.getParkingSpots().size();
    }
}
