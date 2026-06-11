package com.parking.storage;

import com.parking.model.ParkingSpot;
import com.parking.model.PaymentRecord;
import com.parking.model.Role;
import com.parking.model.Ticket;
import com.parking.model.User;
import com.parking.util.FileUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataStore {
    private final Path basePath;
    private final Path usersFile;
    private final Path spotsFile;
    private final Path ticketsFile;
    private final Path paymentsFile;

    public DataStore(String baseFolder) {
        this.basePath = Path.of(baseFolder);
        this.usersFile = basePath.resolve("users.txt");
        this.spotsFile = basePath.resolve("spots.txt");
        this.ticketsFile = basePath.resolve("tickets.txt");
        this.paymentsFile = basePath.resolve("payments.txt");
        seedDefaults();
    }

    private void seedDefaults() {
        if (getUsers().isEmpty()) {
            List<User> defaults = new ArrayList<>();
            defaults.add(new User(1, "admin", "admin123", Role.ADMIN));
            defaults.add(new User(2, "entry1", "entry123", Role.ENTRY_OPERATOR));
            defaults.add(new User(3, "exit1", "exit123", Role.EXIT_OPERATOR));
            saveUsers(defaults);
        }
        if (getParkingSpots().isEmpty()) {
            List<ParkingSpot> defaults = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                defaults.add(new ParkingSpot(i, true, ""));
            }
            saveParkingSpots(defaults);
        }
    }

    public List<User> getUsers() {
        return FileUtil.readLines(usersFile).stream()
                .filter(line -> !line.isBlank())
                .map(User::fromCsv)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void saveUsers(List<User> users) {
        FileUtil.writeLines(usersFile, users.stream().map(User::toCsv).toList());
    }

    public List<ParkingSpot> getParkingSpots() {
        return FileUtil.readLines(spotsFile).stream()
                .filter(line -> !line.isBlank())
                .map(ParkingSpot::fromCsv)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void saveParkingSpots(List<ParkingSpot> spots) {
        FileUtil.writeLines(spotsFile, spots.stream().map(ParkingSpot::toCsv).toList());
    }

    public List<Ticket> getTickets() {
        return FileUtil.readLines(ticketsFile).stream()
                .filter(line -> !line.isBlank())
                .map(Ticket::fromCsv)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void saveTickets(List<Ticket> tickets) {
        FileUtil.writeLines(ticketsFile, tickets.stream().map(Ticket::toCsv).toList());
    }

    public List<PaymentRecord> getPayments() {
        return FileUtil.readLines(paymentsFile).stream()
                .filter(line -> !line.isBlank())
                .map(PaymentRecord::fromCsv)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void savePayments(List<PaymentRecord> payments) {
        FileUtil.writeLines(paymentsFile, payments.stream().map(PaymentRecord::toCsv).toList());
    }

    public int nextUserId() {
        return getUsers().stream().mapToInt(User::getId).max().orElse(0) + 1;
    }

    public int nextSpotId() {
        return getParkingSpots().stream().mapToInt(ParkingSpot::getId).max().orElse(0) + 1;
    }

    public int nextTicketId() {
        return getTickets().stream().mapToInt(Ticket::getTicketId).max().orElse(0) + 1;
    }

    public int nextEntryId() {
        return getTickets().stream().mapToInt(Ticket::getEntryId).max().orElse(1000) + 1;
    }

    public int nextPaymentId() {
        return getPayments().stream().mapToInt(PaymentRecord::getPaymentId).max().orElse(0) + 1;
    }

    public Optional<User> authenticate(String username, String password) {
        return getUsers().stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst();
    }

    public void addPaymentForTicket(Ticket ticket, double amount) {
        List<PaymentRecord> payments = getPayments();
        payments.add(new PaymentRecord(nextPaymentId(), ticket.getTicketId(), ticket.getEntryId(),
                ticket.getPlateNumber(), amount, LocalDateTime.now()));
        savePayments(payments);
    }
}
