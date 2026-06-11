package com.parking.service;

import com.parking.model.Ticket;
import com.parking.storage.DataStore;

import java.time.LocalDateTime;
import java.util.List;

public class ExitStationService extends BaseService implements Reportable {

    private final ParkingService parkingService;

    public ExitStationService(DataStore dataStore, ParkingService parkingService) {
        super(dataStore);
        this.parkingService = parkingService;
    }

    @Override
    public String getServiceName() {
        return "ExitStationService";
    }

    @Override
    public String buildReport() {
        long exited = dataStore.getTickets().stream()
                .filter(t -> "EXITED".equalsIgnoreCase(t.getStatus()))
                .count();
        return log("Total completed exits: " + exited);
    }

    public Ticket calculateParkingHours(int ticketId) {
        return dataStore.getTickets().stream()
                .filter(t -> t.getTicketId() == ticketId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));
    }

    public Ticket completeExit(int ticketId) {
        List<Ticket> tickets = dataStore.getTickets();
        for (Ticket ticket : tickets) {
            if (ticket.getTicketId() == ticketId) {
                if (!ticket.isPaid()) {
                    throw new IllegalStateException("Ticket must be paid before exit.");
                }
                if ("EXITED".equalsIgnoreCase(ticket.getStatus())) {
                    throw new IllegalStateException("Car already exited.");
                }
                if (ticket.getExitTime() == null) {
                    ticket.setExitTime(LocalDateTime.now());
                }
                ticket.setStatus("EXITED");
                dataStore.saveTickets(tickets);
                parkingService.releaseSpot(ticket.getSpotId());
                return ticket;
            }
        }
        throw new IllegalArgumentException("Ticket not found.");
    }
}