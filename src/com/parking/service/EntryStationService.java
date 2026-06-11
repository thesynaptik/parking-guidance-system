package com.parking.service;

import com.parking.model.ParkingSpot;
import com.parking.storage.DataStore;

import java.util.List;

public class EntryStationService extends BaseService implements Reportable {

    private final ParkingService parkingService;

    public EntryStationService(ParkingService parkingService) {
        super(null); 
        this.parkingService = parkingService;
    }

    @Override
    public String getServiceName() {
        return "EntryStationService";
    }

    @Override
    public String buildReport() {
        int total = parkingService.getTotalSpots();
        int free  = parkingService.getFreeSpots().size();
        return log("Total spots: " + total + " | Free: " + free + " | Occupied: " + (total - free));
    }

    public List<ParkingSpot> monitorFreeSpots() {
        return parkingService.getFreeSpots();
    }

    public String adviseCustomerWithFreeSpot() {
        return parkingService.getFirstFreeSpot()
                .map(spot -> "Advise customer to go to Spot #" + spot.getId())
                .orElse("No free spots available right now.");
    }
}
