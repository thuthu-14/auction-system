package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.util.DialogUtil;
import common.AuctionStatus;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.model.User;
import util.JsonUtil;
import util.LoggerUtil;

import java.io.File;
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
    private static final String STATUS_ALL = "Tất cả trạng thái";
    private static final String TIME_ALL = "Tất cả thời gian";

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
    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeController;
    private boolean contextInjected;

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
                    STATUS_ALL, "Đang đấu", "Trúng thầu", "Trượt", "Đã kết thúc"));
            statusFilter.getSelectionModel().selectFirst();
            statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (timeFilter != null) {
            timeFilter.setItems(FXCollections.observableArrayList(
                    TIME_ALL, "Tháng này", "Tháng trước", "3 tháng gần đây"));
            timeFilter.getSelectionModel().selectFirst();
            timeFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        Platform.runLater(() -> {
            if (!contextInjected && visibleRows.isEmpty()) {
                loadHistoryFromLocalFiles(List.of(), false);
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
            loadHistoryFromLocalFiles(List.of(), false);
            return;
        }
        if (clientSocket == null || !clientSocket.isConnected()) {
            loadHistoryFromLocalFiles(List.of(), true);
            return;
        }

        new Thread(() -> {
            try {
                Message response = clientSocket.sendAndReceive(
                        new Message(MessageType.GET_USER_BIDS, currentUser.getUserId(), currentUser.getUsername()));
                List<Bid> bids = extractBidList(response != null ? response.getData() : null);
                if (bids.isEmpty()) {
                    loadHistoryFromLocalFiles(List.of(), true);
                    return;
                }
                Map<String, Auction> auctionCache = new HashMap<>();
                List<HistoryRow> rows = new ArrayList<>();

                for (Bid bid : bids) {
                    if (bid == null || bid.getAuctionId() == null || bid.getAuctionId().isBlank()) {
                        continue;
                    }

                    Auction auction = auctionCache.computeIfAbsent(bid.getAuctionId(), this::fetchAuction);
                    if (auction != null) {
                        rows.add(new HistoryRow(bid, auction));
                    }
                }

                rows.sort(Comparator.comparingLong((HistoryRow row) -> row.bid.getBidTime()).reversed());
                Platform.runLater(() -> {
                    allRows.clear();
                    allRows.addAll(rows);
                    applyFilters();
                });
            } catch (Exception e) {
                LoggerUtil.error("Load auction history failed: " + e.getMessage());
                loadHistoryFromLocalFiles(List.of(), true);
            }
        }, "AuctionHistoryLoader").start();
    }

    private void loadHistoryFromLocalFiles(List<Bid> preferredBids, boolean filterByCurrentUser) {
        new Thread(() -> {
            try {
                List<Bid> bids = preferredBids == null || preferredBids.isEmpty()
                        ? loadLocalBids(filterByCurrentUser)
                        : preferredBids;
                if (bids.isEmpty() && filterByCurrentUser) {
                    bids = loadLocalBids(false);
                }
                Map<String, Auction> auctionCache = loadLocalAuctionsById();
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

                rows.sort(Comparator.comparingLong((HistoryRow row) -> row.bid.getBidTime()).reversed());
                Platform.runLater(() -> {
                    allRows.clear();
                    allRows.addAll(rows);
                    applyFilters();
                });
                LoggerUtil.info("Auction history local rows loaded: " + rows.size());
            } catch (Exception e) {
                LoggerUtil.error("Load local auction history failed: " + e.getMessage());
                Platform.runLater(() -> DialogUtil.showAlert(
                        Alert.AlertType.ERROR,
                        "Lỗi",
                        null,
                        "Không thể tải lịch sử đấu giá",
                        historyTable));
            }
        }, "AuctionHistoryLocalLoader").start();
    }

    private Auction fetchAuction(String auctionId) {
        try {
            Message response = clientSocket.sendAndReceive(
                    new Message(MessageType.GET_AUCTION_DETAIL, auctionId, currentUser.getUsername()));
            if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() instanceof Auction auction) {
                return auction;
            }
        } catch (Exception e) {
            LoggerUtil.error("Fetch auction failed: " + e.getMessage());
        }
        return null;
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
            case "Tháng này" -> bidDate.getYear() == today.getYear()
                    && bidDate.getMonthValue() == today.getMonthValue();
            case "Tháng trước" -> {
                LocalDate previousMonth = today.minusMonths(1);
                yield bidDate.getYear() == previousMonth.getYear()
                        && bidDate.getMonthValue() == previousMonth.getMonthValue();
            }
            case "3 tháng gần đây" -> !bidDate.isBefore(today.minusMonths(3));
            default -> true;
        };
    }

    private void showSellerContact(Auction auction) {
        if (auction == null || clientSocket == null || currentUser == null) {
            return;
        }

        new Thread(() -> {
            try {
                Message response = clientSocket.sendAndReceive(
                        new Message(MessageType.GET_SELLER_CONTACT, auction.getSellerId(), currentUser.getUsername()));
                if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() instanceof Map<?, ?> data) {
                    String name = asText(data.get("name"), auction.getSellerName());
                    String email = asText(data.get("email"), "Chưa cập nhật");
                    String phone = asText(data.get("phone"), "Chưa cập nhật");
                    Platform.runLater(() -> DialogUtil.showAlert(
                            Alert.AlertType.INFORMATION,
                            "Liên hệ người bán",
                            null,
                            "Tên: " + name + "\nEmail: " + email + "\nSĐT: " + phone,
                            historyTable));
                } else {
                    Platform.runLater(() -> DialogUtil.showAlert(
                            Alert.AlertType.WARNING,
                            "Liên hệ người bán",
                            null,
                            response != null ? response.getMessage() : "Chưa có thông tin liên hệ",
                            historyTable));
                }
            } catch (Exception e) {
                LoggerUtil.error("Load seller contact failed: " + e.getMessage());
                Platform.runLater(() -> DialogUtil.showAlert(
                        Alert.AlertType.ERROR,
                        "Lỗi",
                        null,
                        "Không thể tải thông tin liên hệ",
                        historyTable));
            }
        }, "SellerContactLoader").start();
    }

    private List<Bid> extractBidList(Object rawData) {
        if (!(rawData instanceof List<?> rawList)) {
            return List.of();
        }

        List<Bid> bids = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Bid bid) {
                bids.add(bid);
            }
        }
        return bids;
    }

    private List<Bid> loadLocalBids(boolean filterByCurrentUser) {
        File bidsFile = findDataFile("bids.json");
        if (!bidsFile.exists()) {
            LoggerUtil.warn("Local bids.json not found from: " + new File(".").getAbsolutePath());
            return List.of();
        }

        try {
            List<Bid> allBids = JsonUtil.loadListFromJson(bidsFile.getPath(), Bid.class);
            List<Bid> userBids = new ArrayList<>();
            for (Bid bid : allBids != null ? allBids : List.<Bid>of()) {
                if (!filterByCurrentUser || isBidFromCurrentUser(bid)) {
                    userBids.add(bid);
                }
            }
            userBids.sort(Comparator.comparingLong(Bid::getBidTime).reversed());
            LoggerUtil.info("Local auction history bids loaded: " + userBids.size());
            return userBids;
        } catch (Exception e) {
            LoggerUtil.error("Local user bids load failed: " + e.getMessage());
            return List.of();
        }
    }

    private boolean isBidFromCurrentUser(Bid bid) {
        if (bid == null || currentUser == null) {
            return false;
        }
        String userId = currentUser.getUserId();
        String username = currentUser.getUsername();
        return (userId != null && userId.equals(bid.getBidderId()))
                || (username != null && username.equalsIgnoreCase(bid.getBidderName()));
    }

    private Map<String, Auction> loadLocalAuctionsById() {
        File auctionsFile = findDataFile("auctions.json");
        if (!auctionsFile.exists()) {
            LoggerUtil.warn("Local auctions.json not found from: " + new File(".").getAbsolutePath());
            return Map.of();
        }

        try {
            List<Auction> auctions = JsonUtil.loadListFromJson(auctionsFile.getPath(), Auction.class);
            Map<String, Auction> auctionsById = new HashMap<>();
            for (Auction auction : auctions != null ? auctions : List.<Auction>of()) {
                if (auction != null && auction.getAuctionId() != null) {
                    auctionsById.put(auction.getAuctionId(), auction);
                }
            }
            return auctionsById;
        } catch (Exception e) {
            LoggerUtil.error("Local auctions load failed: " + e.getMessage());
            return Map.of();
        }
    }

    private File findDataFile(String fileName) {
        File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && current != null; i++) {
            File candidate = new File(current, "data/json/" + fileName);
            if (candidate.exists()) {
                return candidate;
            }
            current = current.getParentFile();
        }
        return new File("data/json/" + fileName);
    }

    private String asText(Object value, String fallback) {
        String text = value != null ? value.toString() : "";
        return text.isBlank() ? fallback : text;
    }

    private String formatDate(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    private String formatVnd(double amount) {
        return VND_FORMATTER.format(Math.round(amount)) + " VND";
    }

    private class ActionCell extends TableCell<HistoryRow, Void> {
        private final Button button = new Button();

        private ActionCell() {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            HistoryRow row = getTableView().getItems().get(getIndex());
            if (row.isActive()) {
                button.setText(row.isOutbid() ? "Trả giá tiếp" : "Đấu giá ngay");
                button.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
                button.setOnAction(event -> homeController.loadAuctionDetailView(row.auction, false));
            } else if (row.isWinner()) {
                button.setText("Liên hệ");
                button.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
                button.setOnAction(event -> showSellerContact(row.auction));
            } else {
                button.setText("Xem chi tiết");
                button.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
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
            return item != null && item.getName() != null ? item.getName() : "Sản phẩm";
        }

        private String statusText() {
            if (isActive()) {
                return "Đang đấu";
            }
            if (isWinner()) {
                return "Trúng thầu";
            }
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.CANCELLED || status == AuctionStatus.CLOSED || status == AuctionStatus.FINISHED) {
                return "Trượt";
            }
            return "Đã kết thúc";
        }

        private boolean isActive() {
            AuctionStatus status = auction.getStatus();
            return auction.getTimeRemainingSeconds() > 0
                    && (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING);
        }

        private boolean isWinner() {
            if (currentUser == null) {
                return false;
            }
            return !isActive()
                    && auction.getHighestBidderId() != null
                    && auction.getHighestBidderId().equals(currentUser.getUserId());
        }

        private boolean isOutbid() {
            if (currentUser == null) {
                return false;
            }
            return isActive()
                    && auction.getHighestBidderId() != null
                    && !auction.getHighestBidderId().equals(currentUser.getUserId());
        }
    }
}
