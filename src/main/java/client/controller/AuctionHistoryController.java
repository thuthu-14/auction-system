package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuctionHistoryClient;
import client.service.AuctionHistoryClientService;
import client.ui.AuctionHistoryViewFactory;
import client.util.DialogUtil;
import common.AuctionStatus;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.model.User;
import util.LoggerUtil;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuctionHistoryController {
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String STATUS_ALL = "T\u1ea5t c\u1ea3 tr\u1ea1ng th\u00e1i";
    private static final String TIME_ALL = "T\u1ea5t c\u1ea3 th\u1eddi gian";

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private TableView<HistoryRow> historyTable;
    @FXML private TableColumn<HistoryRow, String> dateColumn;
    @FXML private TableColumn<HistoryRow, String> productNameColumn;
    @FXML private TableColumn<HistoryRow, String> bidAmountColumn;
    @FXML private TableColumn<HistoryRow, String> statusColumn;
    @FXML private TableColumn<HistoryRow, Void> actionColumn;

    private final ObservableList<HistoryRow> visibleRows = FXCollections.observableArrayList();
    private final List<HistoryRow> allRows = new ArrayList<>();
    private final AuctionHistoryViewFactory viewFactory = new AuctionHistoryViewFactory();
    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeController;
    private boolean contextInjected;
    private final AuctionHistoryClient historyService;

    public AuctionHistoryController() {
        this(new AuctionHistoryClientService());
    }

    AuctionHistoryController(AuctionHistoryClient historyService) {
        this.historyService = historyService;
    }

    @FXML
    private void initialize() {
        if (dateColumn != null) {
            dateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().bid.getBidTime())));
        }
        if (productNameColumn != null) {
            productNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().productName()));
        }
        if (bidAmountColumn != null) {
            bidAmountColumn.setCellValueFactory(data -> new SimpleStringProperty(formatVnd(data.getValue().bid.getAmount())));
        }
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statusText()));
        }
        if (actionColumn != null) {
            actionColumn.setCellFactory(column -> new ActionCell());
        }
        if (historyTable != null) {
            historyTable.setItems(visibleRows);
        }

        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    STATUS_ALL, "\u0110ang \u0111\u1ea5u", "Tr\u00fang th\u1ea7u", "Tr\u01b0\u1ee3t", "\u0110\u00e3 k\u1ebft th\u00fac"));
            statusFilter.getSelectionModel().selectFirst();
            statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (timeFilter != null) {
            timeFilter.setItems(FXCollections.observableArrayList(
                    TIME_ALL, "Th\u00e1ng n\u00e0y", "Th\u00e1ng tr\u01b0\u1edbc", "3 th\u00e1ng g\u1ea7n \u0111\u00e2y"));
            timeFilter.getSelectionModel().selectFirst();
            timeFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        Platform.runLater(() -> {
            if (!contextInjected && visibleRows.isEmpty()) {
                loadHistoryFromLocalFiles(List.of(), true);
            }
        });
    }

    public void setContext(User user, ClientSocket socket, HomeScreenController homeController) {
        this.contextInjected = true;
        this.currentUser = user != null ? user : NavigationManager.getInstance().getCurrentUser();
        this.clientSocket = socket != null ? socket : ConnectionManager.getInstance().getClientSocket();
        this.homeController = homeController;
        loadHistory();
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    private void loadHistory() {
        if (currentUser == null) {
            LoggerUtil.warn("Auction history has no current user");
            loadHistoryFromLocalFiles(List.of(), true);
            return;
        }
        if (clientSocket == null || !clientSocket.isConnected()) {
            loadHistoryFromLocalFiles(List.of(), true);
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Bid> bids = historyService.fetchUserBids(clientSocket, currentUser);
                if (bids.isEmpty()) {
                    loadHistoryFromLocalFiles(List.of(), true);
                    return;
                }
                Map<String, Auction> auctionCache = new HashMap<>();
                Map<String, Auction> localAuctionsById = historyService.loadLocalAuctionsById();
                List<HistoryRow> rows = new ArrayList<>();

                for (Bid bid : bids) {
                    if (bid == null || bid.getAuctionId() == null || bid.getAuctionId().isBlank()) {
                        continue;
                    }
                    if (!isBidFromCurrentUser(bid)) {
                        continue;
                    }

                    Auction auction = auctionCache.computeIfAbsent(bid.getAuctionId(), this::fetchAuction);
                    if (auction == null) {
                        auction = localAuctionsById.get(bid.getAuctionId());
                    }
                    if (auction != null) {
                        rows.add(new HistoryRow(bid, auction));
                    }
                }
                addWonAuctionRows(rows, localAuctionsById, historyService.loadLocalBids(currentUser, false));

                rows.sort(Comparator.comparingLong((HistoryRow row) -> row.bid.getBidTime()).reversed());
                Platform.runLater(() -> {
                    allRows.clear();
                    allRows.addAll(rows);
                    applyFilters();
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load auction history failed: " + e.getMessage())));
                loadHistoryFromLocalFiles(List.of(), true);
            }
        });
    }

    private void loadHistoryFromLocalFiles(List<Bid> preferredBids, boolean filterByCurrentUser) {
        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Bid> bids = preferredBids == null || preferredBids.isEmpty()
                        ? historyService.loadLocalBids(currentUser, filterByCurrentUser)
                        : preferredBids;
                Map<String, Auction> auctionCache = historyService.loadLocalAuctionsById();
                List<Bid> allLocalBids = historyService.loadLocalBids(currentUser, false);
                List<HistoryRow> rows = new ArrayList<>();

                for (Bid bid : bids) {
                    if (bid == null || bid.getAuctionId() == null || bid.getAuctionId().isBlank()) {
                        continue;
                    }

                    Auction auction = auctionCache.get(bid.getAuctionId());
                    if (auction == null && clientSocket != null && clientSocket.isConnected()) {
                        auction = fetchAuction(bid.getAuctionId());
                    }
                    if (auction != null) {
                        rows.add(new HistoryRow(bid, auction));
                    }
                }
                addWonAuctionRows(rows, auctionCache, allLocalBids);

                rows.sort(Comparator.comparingLong((HistoryRow row) -> row.bid.getBidTime()).reversed());
                Platform.runLater(() -> {
                    allRows.clear();
                    allRows.addAll(rows);
                    applyFilters();
                });
                LoggerUtil.info("Auction history local rows loaded: " + rows.size());
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load local auction history failed: " + e.getMessage())));
                Platform.runLater(() -> DialogUtil.showAlert(
                        Alert.AlertType.ERROR,
                        "L\u1ed7i",
                        null,
                        "Kh\u00f4ng th\u1ec3 t\u1ea3i l\u1ecbch s\u1eed \u0111\u1ea5u gi\u00e1",
                        historyTable));
            }
        });
    }

    private Auction fetchAuction(String auctionId) {
        try {
            return historyService.fetchAuction(clientSocket, currentUser, auctionId);
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Fetch auction failed: " + e.getMessage())));
            return null;
        }
    }

    private void applyFilters() {
        String selectedStatus = statusFilter != null ? statusFilter.getValue() : STATUS_ALL;
        String selectedTime = timeFilter != null ? timeFilter.getValue() : TIME_ALL;
        String keyword = searchField != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";

        visibleRows.setAll(allRows.stream()
                .filter(row -> STATUS_ALL.equals(selectedStatus) || row.statusText().equals(selectedStatus))
                .filter(row -> TIME_ALL.equals(selectedTime) || matchesTimeFilter(row.bid.getBidTime(), selectedTime))
                .filter(row -> keyword.isBlank() || row.productName().toLowerCase(Locale.ROOT).contains(keyword))
                .toList());
    }

    private boolean matchesTimeFilter(long bidTime, String selectedTime) {
        LocalDate bidDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(bidTime), ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        return switch (selectedTime) {
            case "Th\u00e1ng n\u00e0y" -> bidDate.getYear() == today.getYear()
                    && bidDate.getMonthValue() == today.getMonthValue();
            case "Th\u00e1ng tr\u01b0\u1edbc" -> {
                LocalDate previousMonth = today.minusMonths(1);
                yield bidDate.getYear() == previousMonth.getYear()
                        && bidDate.getMonthValue() == previousMonth.getMonthValue();
            }
            case "3 th\u00e1ng g\u1ea7n \u0111\u00e2y" -> !bidDate.isBefore(today.minusMonths(3));
            default -> true;
        };
    }

    private void showSellerContact(Auction auction) {
        if (auction == null || currentUser == null) {
            return;
        }

        Map<String, String> localContact = historyService.findLocalSellerContact(auction);
        if (localContact != null && hasUsefulContact(localContact)) {
            showContactDialog(localContact, auction);
            return;
        }

        if (clientSocket == null || !clientSocket.isConnected()) {
            showContactDialog(localContact, auction);
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            Map<String, String> contact = historyService.fetchSellerContact(clientSocket, currentUser, auction);
            Platform.runLater(() -> showContactDialog(contact != null ? contact : localContact, auction));
        });
    }

    private void showContactDialog(Map<String, String> contact, Auction auction) {
        String name = contact != null ? contact.get("name") : null;
        String email = contact != null ? contact.get("email") : null;
        String phone = contact != null ? contact.get("phone") : null;
        String address = contact != null ? contact.get("address") : null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Li\u00ean h\u1ec7 ng\u01b0\u1eddi b\u00e1n");
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("seller-contact-dialog");
        pane.getButtonTypes().add(ButtonType.OK);
        pane.setHeaderText("Li\u00ean h\u1ec7 ng\u01b0\u1eddi b\u00e1n");
        pane.setContent(viewFactory.createContactContent(
                safeContact(name, auction != null ? auction.getSellerName() : "Ng\u01b0\u1eddi b\u00e1n"),
                safeContact(email, "Ch\u01b0a c\u1eadp nh\u1eadt"),
                safeContact(phone, "Ch\u01b0a c\u1eadp nh\u1eadt"),
                safeContact(address, "Ch\u01b0a c\u1eadp nh\u1eadt")));
        DialogUtil.prepareDialog(dialog, historyTable);
        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("OK");
            okButton.getStyleClass().add("seller-contact-ok-button");
        }
        dialog.showAndWait();
    }

    private boolean hasUsefulContact(Map<String, String> contact) {
        return contact != null
                && (!"Ch\u01b0a c\u1eadp nh\u1eadt".equals(contact.get("phone"))
                || !"Ch\u01b0a c\u1eadp nh\u1eadt".equals(contact.get("email")));
    }

    private VBox createContactContent(String name, String email, String phone, String address) {
        return viewFactory.createContactContent(name, email, phone, address);
    }

    private String safeContact(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private boolean isBidFromCurrentUser(Bid bid) {
        return historyService.isBidFromUser(bid, currentUser);
    }

    private void addWonAuctionRows(List<HistoryRow> rows, Map<String, Auction> auctionsById, List<Bid> allBids) {
        if (currentUser == null || auctionsById == null || auctionsById.isEmpty()) {
            return;
        }

        for (Auction auction : auctionsById.values()) {
            if (!isAuctionWonByCurrentUser(auction) || hasRowForAuction(rows, auction.getAuctionId())) {
                continue;
            }

            Bid winningBid = findLatestUserBidForAuction(auction.getAuctionId(), allBids);
            if (winningBid == null) {
                winningBid = createWinningBidFromAuction(auction);
            }
            rows.add(new HistoryRow(winningBid, auction));
        }
    }

    private boolean hasRowForAuction(List<HistoryRow> rows, String auctionId) {
        if (auctionId == null) {
            return false;
        }
        for (HistoryRow row : rows) {
            if (row != null && row.auction != null && auctionId.equals(row.auction.getAuctionId())) {
                return true;
            }
        }
        return false;
    }

    private Bid findLatestUserBidForAuction(String auctionId, List<Bid> allBids) {
        if (auctionId == null || allBids == null) {
            return null;
        }
        Bid latest = null;
        for (Bid bid : allBids) {
            if (bid != null && auctionId.equals(bid.getAuctionId()) && isBidFromCurrentUser(bid)) {
                if (latest == null || bid.getBidTime() > latest.getBidTime()) {
                    latest = bid;
                }
            }
        }
        return latest;
    }

    private Bid createWinningBidFromAuction(Auction auction) {
        Bid bid = new Bid();
        bid.setAuctionId(auction.getAuctionId());
        bid.setBidderId(auction.getHighestBidderId());
        bid.setBidderName(auction.getHighestBidderName());
        bid.setAmount(auction.getCurrentPrice());
        bid.setBidTime(auction.getEndTime() > 0 ? auction.getEndTime() : auction.getCreatedAt());
        bid.setStatus("ACTIVE");
        return bid;
    }

    private boolean isAuctionWonByCurrentUser(Auction auction) {
        if (auction == null || currentUser == null) {
            return false;
        }
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            return false;
        }
        String userId = currentUser.getUserId();
        return userId != null && userId.equals(auction.getHighestBidderId());
    }

    private String formatDate(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    private String formatVnd(double amount) {
        return VND_FORMATTER.format(Math.round(amount)) + " VND";
    }

    private class ActionCell extends TableCell<HistoryRow, Void> {
        private final Button button = viewFactory.createActionButton();

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            HistoryRow row = getTableView().getItems().get(getIndex());
            if (row.isActive()) {
                viewFactory.configureActiveButton(button, row.isOutbid());
                button.setOnAction(event -> homeController.loadAuctionDetailView(row.auction, false));
            } else if (row.isWinner()) {
                viewFactory.configureWinnerButton(button);
                button.setOnAction(event -> showSellerContact(row.auction));
            } else {
                viewFactory.configureEndedButton(button);
                button.setOnAction(event -> homeController.loadAuctionDetailView(row.auction, true));
            }
            setGraphic(button);
        }
    }

    private class HistoryRow {
        private final Bid bid;
        private final Auction auction;

        private HistoryRow(Bid bid, Auction auction) {
            this.bid = bid;
            this.auction = auction;
        }

        private String productName() {
            Item item = auction.getItem();
            return item != null && item.getName() != null ? item.getName() : "S\u1ea3n ph\u1ea9m";
        }

        private String statusText() {
            if (isActive()) {
                return "\u0110ang \u0111\u1ea5u";
            }
            if (isWinner()) {
                return "Tr\u00fang th\u1ea7u";
            }
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.CANCELLED || status == AuctionStatus.CLOSED || status == AuctionStatus.FINISHED) {
                return "Tr\u01b0\u1ee3t";
            }
            return "\u0110\u00e3 k\u1ebft th\u00fac";
        }

        private boolean isActive() {
            AuctionStatus status = auction.getStatus();
            return auction.getTimeRemainingSeconds() > 0
                    && (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING);
        }

        private boolean isWinner() {
            return !isActive() && isAuctionWonByCurrentUser(auction);
        }

        private boolean isOutbid() {
            if (currentUser == null) {
                return false;
            }
            return isActive()
                    && auction.getHighestBidderId() != null
                    && !isAuctionWonByCurrentUser(auction);
        }
    }
}

