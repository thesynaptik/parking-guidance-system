package com.parking.service;

import com.parking.model.PaymentRecord;
import com.parking.model.Role;
import com.parking.model.ShiftRecord;
import com.parking.model.Ticket;
import com.parking.model.User;
import com.parking.storage.DataStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminService extends BaseService implements Reportable {

    private final ParkingService parkingService;

    public AdminService(DataStore dataStore, ParkingService parkingService) {
        super(dataStore);
        this.parkingService = parkingService;
    }

    @Override
    public String getServiceName() {
        return "AdminService";
    }

    @Override
    public String buildReport() {
        return buildShiftsReport();
    }

    public int addParkingSpot() {
        return parkingService.addSpot().getId();
    }

    public int viewTotalSpots() {
        return parkingService.getTotalSpots();
    }

    public User addUser(String username, String password, Role role) {
        List<User> users = dataStore.getUsers();
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        if (exists) {
            throw new IllegalArgumentException("Username already exists.");
        }
        User user = new User(dataStore.nextUserId(), username, password, role);
        users.add(user);
        dataStore.saveUsers(users);
        return user;
    }

    public User updateUser(int id, String username, String password, Role role) {
        List<User> users = dataStore.getUsers();
        for (User user : users) {
            if (user.getId() == id) {
                user.setUsername(username);
                user.setPassword(password);
                user.setRole(role);
                dataStore.saveUsers(users);
                return user;
            }
        }
        throw new IllegalArgumentException("User not found.");
    }

    public boolean deleteUser(int id) {
        List<User> users = dataStore.getUsers();
        boolean removed = users.removeIf(u -> u.getId() == id);
        if (removed) dataStore.saveUsers(users);
        return removed;
    }

    public List<User> getAllUsers() {
        return dataStore.getUsers();
    }

    public List<ShiftRecord> buildShiftRecords() {
        List<PaymentRecord> payments = dataStore.getPayments();
        List<User> users = dataStore.getUsers();

        Map<LocalDate, Double> totalsByDate = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentTime().toLocalDate(),
                        Collectors.summingDouble(PaymentRecord::getAmount)
                ));

        List<User> exitOperators = users.stream()
                .filter(u -> u.getRole() == Role.EXIT_OPERATOR)
                .toList();

        List<ShiftRecord> records = new ArrayList<>();

        totalsByDate.forEach((date, total) -> {
            if (exitOperators.isEmpty()) {

                records.add(new ShiftRecord("(admin)", Role.EXIT_OPERATOR, date, total));
            } else {

                double share = total / exitOperators.size();
                for (User op : exitOperators) {
                    records.add(new ShiftRecord(op.getUsername(), Role.EXIT_OPERATOR, date, share));
                }
            }
        });

        return records;
    }

    public String buildShiftsReport() {
        List<ShiftRecord> records = buildShiftRecords();

        StringBuilder sb = new StringBuilder();
        sb.append("==== Shifts Report With Payment ====\n");

        if (records.isEmpty()) {
            sb.append("No payments recorded yet.\n");
            return sb.toString();
        }

        records.forEach(r ->
            sb.append("Date: ").append(r.getDate())
              .append(" | Operator: ").append(r.getOperatorUsername())
              .append(" | Role: ").append(r.getRole())
              .append(" | Total: $").append(String.format("%.2f", r.getTotalPayments()))
              .append("\n")
        );

        double grandTotal = records.stream().mapToDouble(ShiftRecord::getTotalPayments).sum();
        sb.append("────────────────────────────────────\n");
        sb.append("Grand Total: $").append(String.format("%.2f", grandTotal)).append("\n");

        return sb.toString();
    }

    public String buildParkedCarsReport() {
        List<Ticket> activeTickets = dataStore.getTickets().stream()
                .filter(t -> !"EXITED".equalsIgnoreCase(t.getStatus()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("==== Parked Cars Report ====\n");
        sb.append("Current parked cars: ").append(activeTickets.size()).append("\n");
        for (Ticket ticket : activeTickets) {
            sb.append("Ticket ID: ").append(ticket.getTicketId())
                    .append(" | Entry ID: ").append(ticket.getEntryId())
                    .append(" | Plate: ").append(ticket.getPlateNumber())
                    .append(" | Spot: ").append(ticket.getSpotId())
                    .append(" | Status: ").append(ticket.getStatus())
                    .append("\n");
        }
        return sb.toString();
    }
}
