package com.parking.ui;

import com.parking.model.Role;
import com.parking.model.Ticket;
import com.parking.model.User;
import com.parking.model.ParkingSpot;
import com.parking.model.PaymentRecord;
import com.parking.service.AdminService;
import com.parking.service.CustomerService;
import com.parking.service.EntryStationService;
import com.parking.service.ExitStationService;
import com.parking.storage.DataStore;
import com.parking.service.ParkingService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ParkingFXApp extends Application {

    private DataStore dataStore;
    private ParkingService parkingService;
    private CustomerService customerService;
    private EntryStationService entryStationService;
    private ExitStationService exitStationService;
    private AdminService adminService;

    private User loggedInUser = null;

    private StackPane root;
    private StackPane contentArea;

    private static final String BG_MAIN      = "#f4f7f6";
    private static final String BG_CARD      = "#ffffff";
    private static final String BG_SIDEBAR   = "#2c3e50";
    private static final String BG_SIDEBAR_H = "#34495e";
    private static final String TXT_PRIMARY  = "#2c3e50";
    private static final String TXT_SUB      = "#7f8c8d";
    private static final String ACCENT_BLUE  = "#3498db";
    private static final String SUCCESS      = "#27ae60";
    private static final String DANGER       = "#e74c3c";
    private static final String DANGER_SOFT  = "#ff7675";
    private static final String FIELD_BG     = "#fcfcfc";

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void start(Stage stage) {
        String dataPath = "data";
        dataStore          = new DataStore(dataPath);
        parkingService     = new ParkingService(dataStore);
        customerService    = new CustomerService(dataStore, parkingService);
        entryStationService = new EntryStationService(parkingService);
        exitStationService = new ExitStationService(dataStore, parkingService);
        adminService       = new AdminService(dataStore, parkingService);

        root = new StackPane();
        root.setStyle("-fx-background-color: " + BG_MAIN + ";");

        showLoginScreen();

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("🚗  Parking Guidance System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private void showLoginScreen() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);
        card.setPadding(new Insets(40));
        card.setStyle(cardStyle());

        Label icon  = new Label("🅿");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Parking Guidance");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");

        Label sub = new Label("Sign in to your account");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TXT_SUB + ";");

        TextField userField = styledTextField("Username");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(fieldStyle());

        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill: " + DANGER + "; -fx-font-size: 12px;");

        Button loginBtn = primaryButton("Sign In", SUCCESS);
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                errLabel.setText("⚠  Username and password are required.");
                return;
            }
            Optional<User> opt = dataStore.authenticate(u, p);
            if (opt.isEmpty()) {
                errLabel.setText("⚠  Invalid credentials.");
                return;
            }
            loggedInUser = opt.get();
            showDashboard();
        });

        card.getChildren().addAll(icon, title, sub,
                sectionLabel("Username *"), userField,
                sectionLabel("Password *"), passField,
                errLabel, loginBtn);

        StackPane loginRoot = new StackPane(card);
        loginRoot.setStyle("-fx-background-color: " + BG_MAIN + ";");
        root.getChildren().setAll(loginRoot);
    }

    private void showDashboard() {
        BorderPane dashboard = new BorderPane();

        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: " + BG_SIDEBAR + ";");
        sidebar.setPadding(new Insets(0, 0, 20, 0));

        VBox logoBox = new VBox(4);
        logoBox.setPadding(new Insets(28, 20, 24, 20));
        logoBox.setStyle("-fx-background-color: #243342;");
        Label logoIcon  = new Label("🅿  ParkSys");
        logoIcon.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label userBadge = new Label("👤  " + loggedInUser.getUsername()
                + "  •  " + loggedInUser.getRole().name().replace("_", " "));
        userBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        logoBox.getChildren().addAll(logoIcon, userBadge);
        sidebar.getChildren().add(logoBox);

        sidebar.getChildren().add(navSectionLabel("NAVIGATION"));

        Role role = loggedInUser.getRole();

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: " + BG_MAIN + ";");

        addNavBtn(sidebar, "🎫  Print Ticket",       () -> contentArea.getChildren().setAll(buildCustomerTicketPane()));
        addNavBtn(sidebar, "💳  Pay at Exit",         () -> contentArea.getChildren().setAll(buildCustomerPayPane()));

        if (role == Role.ENTRY_OPERATOR || role == Role.ADMIN) {
            sidebar.getChildren().add(navSectionLabel("ENTRY STATION"));
            addNavBtn(sidebar, "🟢  Monitor Free Spots", () -> contentArea.getChildren().setAll(buildMonitorSpotsPane()));
            addNavBtn(sidebar, "📍  Advise Customer",    () -> contentArea.getChildren().setAll(buildAdvisePane()));
        }

        if (role == Role.EXIT_OPERATOR || role == Role.ADMIN) {
            sidebar.getChildren().add(navSectionLabel("EXIT STATION"));
            addNavBtn(sidebar, "🕐  Calculate Hours",  () -> contentArea.getChildren().setAll(buildCalcHoursPane()));
            addNavBtn(sidebar, "🚪  Complete Exit",    () -> contentArea.getChildren().setAll(buildCompleteExitPane()));
        }

        if (role == Role.ADMIN) {
            sidebar.getChildren().add(navSectionLabel("ADMIN"));
            addNavBtn(sidebar, "🏢  Manage Spots",   () -> contentArea.getChildren().setAll(buildManageSpotsPane()));
            addNavBtn(sidebar, "👥  Manage Users",   () -> contentArea.getChildren().setAll(buildManageUsersPane()));
            addNavBtn(sidebar, "📊  Shifts Report",  () -> contentArea.getChildren().setAll(buildShiftsReportPane()));
            addNavBtn(sidebar, "🚗  Parked Cars",    () -> contentArea.getChildren().setAll(buildParkedCarsPane()));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);
        sidebar.getChildren().add(navSectionLabel("SESSION"));
        addNavBtn(sidebar, "🔓  Logout", () -> {
            loggedInUser = null;
            showLoginScreen();
        });

        contentArea.getChildren().setAll(buildHomePane());

        dashboard.setLeft(sidebar);
        dashboard.setCenter(contentArea);

        root.getChildren().setAll(dashboard);
    }

    private Node buildHomePane() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(36));
        box.setAlignment(Pos.TOP_LEFT);

        Label title = pageTitle("Welcome, " + loggedInUser.getUsername() + " 👋");
        Label sub   = subLabel("Use the sidebar to navigate between modules.");

        HBox stats = new HBox(16);
        int total = parkingService.getTotalSpots();
        int free  = parkingService.getFreeSpots().size();
        int parked = total - free;
        long payments = dataStore.getPayments().size();

        stats.getChildren().addAll(
                statCard("🅿  Total Spots",   String.valueOf(total),   ACCENT_BLUE),
                statCard("🟢  Free Spots",    String.valueOf(free),    SUCCESS),
                statCard("🚗  Occupied",      String.valueOf(parked),  "#e67e22"),
                statCard("💳  Payments",      String.valueOf(payments), "#9b59b6")
        );

        box.getChildren().addAll(title, sub, new Separator(), stats);
        return box;
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setPrefWidth(160);
        card.setStyle(cardStyle() + "-fx-border-left: 4px solid " + color + ";");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TXT_SUB + ";");
        card.getChildren().addAll(v, l);
        return card;
    }

    private Node buildCustomerTicketPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));

        page.getChildren().add(pageTitle("🎫  Print Entry Ticket"));

        VBox card = formCard();
        GridPane grid = formGrid();

        Label plateLbl = fieldLabel("Plate Number *");
        TextField plateField = styledTextField("e.g. ABC-1234");

        grid.add(plateLbl,   0, 0); grid.add(plateField, 1, 0);

        Label resultLabel = new Label("");
        resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + SUCCESS + "; -fx-wrap-text: true;");
        resultLabel.setWrapText(true);

        Button printBtn = primaryButton("🖨  Print Ticket", SUCCESS);
        printBtn.setDisable(true);

        plateField.textProperty().addListener((obs, oldVal, newVal) -> {
            String text = newVal.trim();
            if (text.isEmpty()) {
                plateField.setStyle(fieldStyle());
                printBtn.setDisable(true);
            } else if (text.matches("^[a-zA-Z]{1,3}-[0-9]{1,4}$")) {
                plateField.setStyle(fieldStyle() + "-fx-border-color: " + SUCCESS + ";");
                printBtn.setDisable(false);
            } else if (text.matches("^[a-zA-Z]{1,3}(-([0-9]{1,4})?)?$")) {
                plateField.setStyle(fieldStyle());
                printBtn.setDisable(true);
            } else {
                plateField.setStyle(fieldStyle() + "-fx-border-color: " + DANGER + ";");
                printBtn.setDisable(true);
            }
        });

        printBtn.setOnAction(e -> {
            String plate = plateField.getText().trim();
            if (plate.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Plate Number is required.");
                return;
            }
            try {
                Ticket t = customerService.printTicket(plate);
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + SUCCESS + ";");
                resultLabel.setText(
                        "✅  Ticket Created!\n" +
                                "  Ticket ID  : " + t.getTicketId() + "\n" +
                                "  Entry ID   : " + t.getEntryId() + "\n" +
                                "  Plate      : " + t.getPlateNumber() + "\n" +
                                "  Spot       : #" + t.getSpotId() + "\n" +
                                "  Entry Time : " + t.getEntryTime().format(DT_FMT)
                );
                plateField.clear();
            } catch (Exception ex) {
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DANGER + ";");
                resultLabel.setText("❌  " + ex.getMessage());
            }
        });

        card.getChildren().addAll(grid, printBtn, resultLabel);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildCustomerPayPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("💳  Pay at Exit Station"));

        VBox card = formCard();
        GridPane grid = formGrid();

        Label entryLbl   = fieldLabel("Entry ID *");
        TextField entryField = styledTextField("Numeric Entry ID");

        grid.add(entryLbl,   0, 0); grid.add(entryField, 1, 0);

        Label previewLabel = new Label("");
        previewLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + ACCENT_BLUE + ";");

        Button calcBtn = primaryButton("🔍  Calculate Amount", ACCENT_BLUE);
        Button payBtn  = primaryButton("✅  Confirm Payment", SUCCESS);
        payBtn.setDisable(true);

        calcBtn.setOnAction(e -> {
            String raw = entryField.getText().trim();
            if (!raw.matches("\\d+")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Entry ID must be numeric.");
                return;
            }
            try {
                int entryId = Integer.parseInt(raw);
                double amount = customerService.calculateAmountByEntryId(entryId);
                previewLabel.setText("💰  Amount Due: $" + String.format("%.2f", amount));
                payBtn.setDisable(false);
            } catch (Exception ex) {
                previewLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DANGER + ";");
                previewLabel.setText("❌  " + ex.getMessage());
                payBtn.setDisable(true);
            }
        });

        payBtn.setOnAction(e -> {
            try {
                int entryId = Integer.parseInt(entryField.getText().trim());
                Ticket t = customerService.payAtExitByEntryId(entryId);
                showAlert(Alert.AlertType.INFORMATION, "Payment Successful",
                        "✅  Payment recorded!\nTicket ID: " + t.getTicketId()
                                + "\nAmount Paid: $" + String.format("%.2f", t.getAmountPaid()));
                entryField.clear();
                previewLabel.setText("");
                payBtn.setDisable(true);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        HBox btnRow = new HBox(12, calcBtn, payBtn);
        card.getChildren().addAll(grid, btnRow, previewLabel);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildMonitorSpotsPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("🟢  Monitor Free Spots"));

        VBox card = formCard();

        Button refreshBtn = primaryButton("🔄  Refresh", ACCENT_BLUE);

        FlowPane spotsFlow = new FlowPane(10, 10);
        spotsFlow.setPadding(new Insets(10, 0, 0, 0));

        Runnable refresh = () -> {
            spotsFlow.getChildren().clear();
            List<ParkingSpot> all = parkingService.getAllSpots();
            if (all.isEmpty()) {
                spotsFlow.getChildren().add(emptyState("No spots configured."));
                return;
            }
            for (ParkingSpot s : all) {
                VBox chip = new VBox(4);
                chip.setAlignment(Pos.CENTER);
                chip.setPadding(new Insets(12, 16, 12, 16));
                String bg = s.isFree() ? "#e8f8f5" : "#fdecea";
                String fg = s.isFree() ? SUCCESS : DANGER;
                chip.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 6, 0, 0, 2);");
                Label idLbl = new Label("Spot #" + s.getId());
                idLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + TXT_PRIMARY + ";");
                Label statusLbl = new Label(s.isFree() ? "✅ FREE" : "🔴 OCCUPIED");
                statusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + fg + ";");
                if (!s.isFree() && !s.getCurrentPlateNumber().isBlank()) {
                    Label plateLbl2 = new Label(s.getCurrentPlateNumber());
                    plateLbl2.setStyle("-fx-font-size: 10px; -fx-text-fill: " + TXT_SUB + ";");
                    chip.getChildren().addAll(idLbl, statusLbl, plateLbl2);
                } else {
                    chip.getChildren().addAll(idLbl, statusLbl);
                }
                spotsFlow.getChildren().add(chip);
            }
        };

        refreshBtn.setOnAction(e -> refresh.run());
        refresh.run();

        card.getChildren().addAll(refreshBtn, spotsFlow);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildAdvisePane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("📍  Advise Customer"));

        VBox card = formCard();
        Label adviceLabel = new Label("");
        adviceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + SUCCESS + "; -fx-wrap-text: true;");
        adviceLabel.setWrapText(true);

        Button adviseBtn = primaryButton("📍  Get Advice", ACCENT_BLUE);
        adviseBtn.setOnAction(e -> {
            String msg = entryStationService.adviseCustomerWithFreeSpot();
            adviceLabel.setText("💬  " + msg);
        });

        card.getChildren().addAll(adviseBtn, adviceLabel);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildCalcHoursPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("🕐  Calculate Parking Hours"));

        VBox card = formCard();
        GridPane grid = formGrid();

        TextField ticketField = styledTextField("Numeric Ticket ID");
        grid.add(fieldLabel("Ticket ID *"), 0, 0);
        grid.add(ticketField, 1, 0);

        Label resultLabel = new Label("");
        resultLabel.setWrapText(true);
        resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TXT_PRIMARY + ";");

        Button calcBtn = primaryButton("🔍  Calculate", ACCENT_BLUE);
        calcBtn.setOnAction(e -> {
            String raw = ticketField.getText().trim();
            if (!raw.matches("\\d+")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Ticket ID must be numeric.");
                return;
            }
            try {
                Ticket t = exitStationService.calculateParkingHours(Integer.parseInt(raw));
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TXT_PRIMARY + ";");
                resultLabel.setText(
                        "🎫  Ticket ID  : " + t.getTicketId() + "\n" +
                                "🚗  Plate      : " + t.getPlateNumber() + "\n" +
                                "🕐  Entry Time : " + t.getEntryTime().format(DT_FMT) + "\n" +
                                "⏱  Hours      : " + t.calculateHours() + " hr(s)\n" +
                                "💳  Paid       : " + (t.isPaid() ? "✅ Yes" : "❌ No") + "\n" +
                                "📌  Status     : " + t.getStatus()
                );
            } catch (Exception ex) {
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DANGER + ";");
                resultLabel.setText("❌  " + ex.getMessage());
            }
        });

        card.getChildren().addAll(grid, calcBtn, resultLabel);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildCompleteExitPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("🚪  Complete Car Exit"));

        VBox card = formCard();
        GridPane grid = formGrid();

        TextField ticketField = styledTextField("Numeric Ticket ID");
        grid.add(fieldLabel("Ticket ID *"), 0, 0);
        grid.add(ticketField, 1, 0);

        Label resultLabel = new Label("");
        resultLabel.setWrapText(true);

        Button exitBtn = primaryButton("🚪  Complete Exit", DANGER);
        exitBtn.setOnAction(e -> {
            String raw = ticketField.getText().trim();
            if (!raw.matches("\\d+")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Ticket ID must be numeric.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Exit");
            confirm.setHeaderText(null);
            confirm.setContentText("Complete exit for Ticket ID " + raw + "?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        Ticket t = exitStationService.completeExit(Integer.parseInt(raw));
                        resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + SUCCESS + ";");
                        resultLabel.setText("✅  Exit completed for plate: " + t.getPlateNumber()
                                + "\nSpot #" + t.getSpotId() + " is now FREE.");
                        ticketField.clear();
                    } catch (Exception ex) {
                        resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + DANGER + ";");
                        resultLabel.setText("❌  " + ex.getMessage());
                    }
                }
            });
        });

        card.getChildren().addAll(grid, exitBtn, resultLabel);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildManageSpotsPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("🏢  Manage Parking Spots"));

        VBox card = formCard();

        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");

        Button addBtn     = primaryButton("➕  Add New Spot", SUCCESS);
        Button refreshBtn = primaryButton("🔄  Refresh", ACCENT_BLUE);

        TableView<ParkingSpot> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);
        styleTable(table);

        TableColumn<ParkingSpot, Integer> idCol = new TableColumn<>("Spot ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<ParkingSpot, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isFree() ? "✅ Free" : "🔴 Occupied"));

        TableColumn<ParkingSpot, String> plateCol = new TableColumn<>("Current Plate");
        plateCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getCurrentPlateNumber()));

        table.getColumns().addAll(idCol, statusCol, plateCol);

        Runnable refresh = () -> {
            List<ParkingSpot> spots = parkingService.getAllSpots();
            table.setItems(FXCollections.observableArrayList(spots));
            totalLabel.setText("Total Spots: " + spots.size()
                    + "   |   Free: " + spots.stream().filter(ParkingSpot::isFree).count()
                    + "   |   Occupied: " + spots.stream().filter(s -> !s.isFree()).count());
            if (spots.isEmpty()) table.setPlaceholder(emptyState("No spots configured."));
        };

        addBtn.setOnAction(e -> {
            int newId = adminService.addParkingSpot();
            showAlert(Alert.AlertType.INFORMATION, "Success", "✅  New Spot #" + newId + " added.");
            refresh.run();
        });
        refreshBtn.setOnAction(e -> refresh.run());
        refresh.run();

        HBox btnRow = new HBox(12, addBtn, refreshBtn);
        card.getChildren().addAll(totalLabel, btnRow, table);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildManageUsersPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("👥  Manage Users"));

        VBox addCard = formCard();
        Label addHeader = new Label("Add New User");
        addHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");

        GridPane grid = formGrid();

        TextField unField = styledTextField("No spaces allowed");
        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Password");
        pwField.setStyle(fieldStyle());

        ComboBox<Role> roleBox = new ComboBox<>();
        roleBox.setItems(FXCollections.observableArrayList(Role.values()));
        roleBox.setPromptText("Select Role...");
        roleBox.setStyle(fieldStyle());
        roleBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(fieldLabel("Username *"),  0, 0); grid.add(unField,  1, 0);
        grid.add(fieldLabel("Password *"),  0, 1); grid.add(pwField,  1, 1);
        grid.add(fieldLabel("Role *"),      0, 2); grid.add(roleBox,  1, 2);

        TableView<User> usersTable = buildUsersTable();

        Button addBtn = primaryButton("➕  Add User", SUCCESS);
        addBtn.setOnAction(e -> {
            String un = unField.getText().trim();
            String pw = pwField.getText().trim();
            Role r  = roleBox.getValue();

            if (un.isEmpty() || pw.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Username and password are required.");
                return;
            }
            if (un.contains(" ")) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Username must not contain spaces.");
                return;
            }
            if (r == null) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please select a role.");
                return;
            }
            try {
                User created = adminService.addUser(un, pw, r);
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "✅  User '" + created.getUsername() + "' added with ID " + created.getId());
                unField.clear(); pwField.clear(); roleBox.setValue(null);
                refreshUserTable(usersTable);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        addCard.getChildren().addAll(addHeader, grid, addBtn);

        VBox tableCard = formCard();
        Label tblHeader = new Label("All Users");
        tblHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");

        tableCard.getChildren().addAll(tblHeader, usersTable);
        page.getChildren().addAll(addCard, tableCard);
        return scrollWrap(page);
    }

    private TableView<User> buildUsersTable() {
        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(320);
        styleTable(table);

        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<User, String> unCol = new TableColumn<>("Username");
        unCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole().name()));

        TableColumn<User, Void> actCol = new TableColumn<>("Actions");
        actCol.setCellFactory(buildUserActionCellFactory(table));

        table.getColumns().addAll(idCol, unCol, roleCol, actCol);
        refreshUserTable(table);
        return table;
    }

    private void refreshUserTable(TableView<User> table) {
        table.setItems(FXCollections.observableArrayList(adminService.getAllUsers()));
        if (adminService.getAllUsers().isEmpty()) table.setPlaceholder(emptyState("No users found."));
    }

    private Callback<TableColumn<User, Void>, TableCell<User, Void>> buildUserActionCellFactory(TableView<User> table) {
        return col -> new TableCell<>() {
            final Button editBtn = dangerButton("✏  Edit", ACCENT_BLUE);
            final Button delBtn  = dangerButton("🗑  Delete", DANGER);
            final HBox box = new HBox(8, editBtn, delBtn);

            {
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showEditUserDialog(u, table);
                });
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Delete");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Delete user '" + u.getUsername() + "'?");
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            adminService.deleteUser(u.getId());
                            refreshUserTable(table);
                        }
                    });
                });
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private void showEditUserDialog(User user, TableView<User> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User — " + user.getUsername());
        dialog.setHeaderText(null);

        GridPane grid = formGrid();
        TextField unF = styledTextField(user.getUsername());
        unF.setText(user.getUsername());
        PasswordField pwF = new PasswordField();
        pwF.setText(user.getPassword());
        pwF.setStyle(fieldStyle());
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleBox.setValue(user.getRole());
        roleBox.setStyle(fieldStyle());

        grid.add(fieldLabel("Username *"), 0, 0); grid.add(unF,     1, 0);
        grid.add(fieldLabel("Password *"), 0, 1); grid.add(pwF,     1, 1);
        grid.add(fieldLabel("Role *"),     0, 2); grid.add(roleBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String newUn = unF.getText().trim();
                String newPw = pwF.getText().trim();
                Role newRole = roleBox.getValue();
                if (newUn.isEmpty() || newPw.isEmpty() || newRole == null) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "All fields are required.");
                    return;
                }
                if (newUn.contains(" ")) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "Username must not contain spaces.");
                    return;
                }
                adminService.updateUser(user.getId(), newUn, newPw, newRole);
                refreshUserTable(table);
            }
        });
    }

    private Node buildShiftsReportPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("📊  Shifts Report"));

        VBox card = formCard();

        TableView<PaymentRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(380);
        styleTable(table);

        TableColumn<PaymentRecord, Integer> pidCol = new TableColumn<>("Payment ID");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("paymentId"));

        TableColumn<PaymentRecord, Integer> tidCol = new TableColumn<>("Ticket ID");
        tidCol.setCellValueFactory(new PropertyValueFactory<>("ticketId"));

        TableColumn<PaymentRecord, String> plateCol = new TableColumn<>("Plate");
        plateCol.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));

        TableColumn<PaymentRecord, String> amtCol = new TableColumn<>("Amount ($)");
        amtCol.setCellValueFactory(cd ->
                new SimpleStringProperty(String.format("%.2f", cd.getValue().getAmount())));

        TableColumn<PaymentRecord, String> timeCol = new TableColumn<>("Payment Time");
        timeCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getPaymentTime().format(DT_FMT)));

        table.getColumns().addAll(pidCol, tidCol, plateCol, amtCol, timeCol);

        List<PaymentRecord> payments = dataStore.getPayments();
        table.setItems(FXCollections.observableArrayList(payments));
        if (payments.isEmpty()) table.setPlaceholder(emptyState("No payments recorded yet."));

        double total = payments.stream().mapToDouble(PaymentRecord::getAmount).sum();
        Label totalLabel = new Label("Total Revenue: $" + String.format("%.2f", total));
        totalLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + SUCCESS + ";");

        card.getChildren().addAll(totalLabel, table);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private Node buildParkedCarsPane() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(36));
        page.getChildren().add(pageTitle("🚗  Parked Cars Report"));

        VBox card = formCard();

        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(380);
        styleTable(table);

        TableColumn<Ticket, Integer> tidCol = new TableColumn<>("Ticket ID");
        tidCol.setCellValueFactory(new PropertyValueFactory<>("ticketId"));

        TableColumn<Ticket, Integer> eidCol = new TableColumn<>("Entry ID");
        eidCol.setCellValueFactory(new PropertyValueFactory<>("entryId"));

        TableColumn<Ticket, String> plateCol = new TableColumn<>("Plate");
        plateCol.setCellValueFactory(new PropertyValueFactory<>("plateNumber"));

        TableColumn<Ticket, Integer> spotCol = new TableColumn<>("Spot");
        spotCol.setCellValueFactory(new PropertyValueFactory<>("spotId"));

        TableColumn<Ticket, String> entryCol = new TableColumn<>("Entry Time");
        entryCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getEntryTime().format(DT_FMT)));

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> {
            String s = cd.getValue().getStatus();
            String display = switch (s.toUpperCase()) {
                case "PARKED" -> "⏳ Parked";
                case "PAID"   -> "✅ Paid";
                case "EXITED" -> "❌ Exited";
                default       -> s;
            };
            return new SimpleStringProperty(display);
        });

        TableColumn<Ticket, String> hrsCol = new TableColumn<>("Hours");
        hrsCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().calculateHours() + " hr(s)"));

        table.getColumns().addAll(tidCol, eidCol, plateCol, spotCol, entryCol, hrsCol, statusCol);

        List<Ticket> active = dataStore.getTickets().stream()
                .filter(t -> !"EXITED".equalsIgnoreCase(t.getStatus()))
                .toList();

        table.setItems(FXCollections.observableArrayList(active));
        if (active.isEmpty()) table.setPlaceholder(emptyState("No cars currently parked."));

        Label countLabel = new Label("Currently Parked: " + active.size() + " vehicle(s)");
        countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");

        card.getChildren().addAll(countLabel, table);
        page.getChildren().add(card);
        return scrollWrap(page);
    }

    private String cardStyle() {
        return "-fx-background-color: " + BG_CARD + "; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.10), 10, 0, 0, 5);";
    }

    private VBox formCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setStyle(cardStyle());
        return card;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        return tf;
    }

    private String fieldStyle() {
        return "-fx-background-color: " + FIELD_BG + "; " +
                "-fx-border-color: #000000; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 8;";
    }

    private Button primaryButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 30 10 30; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );
        btn.setCursor(Cursor.HAND);
        String hoverColor = darken(color);
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(color, hoverColor)));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle().replace(hoverColor, color)));
        return btn;
    }

    private Button dangerButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-padding: 5 14 5 14; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand;"
        );
        btn.setCursor(Cursor.HAND);
        return btn;
    }

    private String darken(String hex) {
        return switch (hex) {
            case "#27ae60" -> "#1e8449";
            case "#3498db" -> "#2471a3";
            case "#e74c3c" -> "#c0392b";
            case "#9b59b6" -> "#7d3c98";
            case "#e67e22" -> "#ca6f1e";
            default -> hex;
        };
    }

    private void addNavBtn(VBox sidebar, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setCursor(Cursor.HAND);
        btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-font-size: 13px; " +
                        "-fx-alignment: CENTER_LEFT;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + BG_SIDEBAR_H + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-alignment: CENTER_LEFT;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #ecf0f1; " +
                        "-fx-font-size: 13px; " +
                        "-fx-alignment: CENTER_LEFT;"
        ));
        btn.setOnAction(e -> action.run());
        sidebar.getChildren().add(btn);
    }

    private Label navSectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: #7f8c8d; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 16 20 4 20;"
        );
        return l;
    }

    private Label pageTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");
        return l;
    }

    private Label subLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TXT_SUB + ";");
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TXT_PRIMARY + ";");
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TXT_SUB + ";");
        return l;
    }

    private Label emptyState(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-style: italic; -fx-text-fill: " + TXT_SUB + "; -fx-font-size: 13px;");
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private ScrollPane scrollWrap(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private <T> void styleTable(TableView<T> table) {
        table.setStyle(
                "-fx-background-color: " + BG_CARD + "; " +
                        "-fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 6, 0, 0, 2);"
        );
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}