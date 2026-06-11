package com.parking.service;

import com.parking.model.ParkingSpot;
import com.parking.model.Ticket;
import com.parking.storage.DataStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CustomerService extends BaseService implements Reportable {

    private static final double HOURLY_RATE = 10.0;
    private final ParkingService parkingService;

    public CustomerService(DataStore dataStore, ParkingService parkingService) {
        super(dataStore);
        this.parkingService = parkingService;
    }

    @Override
    public String getServiceName() {
        return "CustomerService";
    }

    @Override
    public String buildReport() {
        long active = dataStore.getTickets().stream()
                .filter(t -> !"EXITED".equalsIgnoreCase(t.getStatus()))
                .count();
        return log("Active tickets: " + active + " | Hourly rate: $" + HOURLY_RATE);
    }

    public Ticket printTicket(String plateNumber) {
        Optional<ParkingSpot> freeSpot = parkingService.getFirstFreeSpot();
        if (freeSpot.isEmpty()) {
            throw new IllegalStateException("No free spots available.");
        }

        ParkingSpot spot = freeSpot.get();
        parkingService.occupySpot(spot.getId(), plateNumber);

        List<Ticket> tickets = dataStore.getTickets();
        Ticket ticket = new Ticket(
                dataStore.nextTicketId(),
                dataStore.nextEntryId(),
                plateNumber,
                spot.getId(),
                LocalDateTime.now(),
                null,
                false,
                0.0,
                "PARKED"
        );
        tickets.add(ticket);
        dataStore.saveTickets(tickets);
        return ticket;
    }

    public Ticket findActiveTicketByEntryId(int entryId) {
        return dataStore.getTickets().stream()
                .filter(t -> t.getEntryId() == entryId && !"EXITED".equalsIgnoreCase(t.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Active ticket not found for entry ID: " + entryId));
    }

    public double calculateAmountByEntryId(int entryId) {
        Ticket ticket = findActiveTicketByEntryId(entryId);
        return ticket.calculateHours() * HOURLY_RATE;
    }

    public Ticket payAtExitByEntryId(int entryId) {
        List<Ticket> tickets = dataStore.getTickets();
        for (Ticket ticket : tickets) {
            if (ticket.getEntryId() == entryId && !"EXITED".equalsIgnoreCase(ticket.getStatus())) {
                ticket.setExitTime(LocalDateTime.now());
                double amount = ticket.calculateHours() * HOURLY_RATE;
                ticket.setAmountPaid(amount);
                ticket.setPaid(true);
                ticket.setStatus("PAID");
                dataStore.saveTickets(tickets);
                dataStore.addPaymentForTicket(ticket, amount);
                return ticket;
            }
        }
        throw new IllegalArgumentException("Entry ID not found or already exited.");
    }
}
