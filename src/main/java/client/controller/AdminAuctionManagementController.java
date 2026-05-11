package client.controller;

import client.network.ClientSocket;
import client.service.AdminClientService;
import common.AuctionStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import server.model.Auction;
import server.model.Item;
import server.model.User;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AdminAuctionManagementController {
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VND_FORMATTER =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private Button btnRefresh;
    @FXML private TextField searchBidInput;
    @FXML private ComboBox<String> filterStatus;
    @FXML private DatePicker filterStartDate;
    @FXML private DatePicker filterEndDate;
    @FXML private ComboBox<String> sortOptions;
    @FXML private TableView<AdminAuctionRow> tableBids;
    @FXML private TableColumn<AdminAuctionRow, String> colBidId;
    @FXML private TableColumn<AdminAuctionRow, String> colAuctionId;
    @FXML private TableColumn<AdminAuctionRow, String> colBidderId;
    @FXML private TableColumn<AdminAuctionRow, String> colBidderName;
    @FXML private TableColumn<AdminAuctionRow, String> colAmount;
    @FXML private TableColumn<AdminAuctionRow, String> colBidTime;
    @FXML private TableColumn<AdminAuctionRow, String> colStatus;
    @FXML private Label totalLabel;

    private final ObservableList<AdminAuctionRow> allRows = FXCollections.observableArrayList();
    private final AdminClientService adminClientService = new AdminClientService();
    private User currentUser;
    private ClientSocket clientSocket;

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        setupEvents();
    }

    public void setContext(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        loadAuctions();
    }

    private void setupColumns() {
        colBidId.setCellValueFactory(cell -> cell.getValue().auctionIdProperty());
        colAuctionId.setCellValueFactory(cell -> cell.getValue().itemIdProperty());
        colBidderId.setCellValueFactory(cell -> cell.getValue().sellerIdProperty());
        colBidderName.setCellValueFactory(cell -> cell.getValue().itemNameProperty());
        colAmount.setCellValueFactory(cell -> cell.getValue().currentPriceProperty());
        colBidTime.setCellValueFactory(cell -> cell.getValue().endTimeProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
    }

    private void setupFilters() {
        filterStatus.setItems(FXCollections.observableArrayList(
                "Tất cả", "DRAFT", "OPEN", "RUNNING", "FINISHED", "CANCELLED"
        ));
        filterStatus.setValue("Tất cả");

        sortOptions.setItems(FXCollections.observableArrayList(
                "Mới nhất trước", "Cũ nhất trước", "Giá cao nhất", "Giá thấp nhất", "Sắp kết thúc"
        ));
        sortOptions.setValue("Mới nhất trước");
    }

    private void setupEvents() {
        btnRefresh.setOnAction(event -> loadAuctions());
        searchBidInput.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterStatus.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterStartDate.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterEndDate.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        sortOptions.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void loadAuctions() {
        new Thread(() -> {
            try {
                List<Auction> auctions = adminClientService.fetchAllAuctions(clientSocket, currentUser);
                Platform.runLater(() -> {
                    allRows.setAll(auctions.stream()
                            .map(AdminAuctionRow::fromAuction)
                            .collect(Collectors.toList()));
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    tableBids.setItems(FXCollections.observableArrayList());
                    updateTotal(0);
                });
            }
        }, "AdminLoadAuctionsThread").start();
    }

    private void applyFilters() {
        String keyword = searchBidInput.getText() == null
                ? ""
                : searchBidInput.getText().trim().toLowerCase();
        String status = filterStatus.getValue();
        LocalDate startDate = filterStartDate.getValue();
        LocalDate endDate = filterEndDate.getValue();

        List<AdminAuctionRow> filtered = allRows.stream()
                .filter(row -> keyword.isEmpty() || row.matches(keyword))
                .filter(row -> status == null || "Tất cả".equals(status) || row.getRawStatus().equals(status))
                .filter(row -> startDate == null || !row.getCreatedDate().isBefore(startDate))
                .filter(row -> endDate == null || !row.getCreatedDate().isAfter(endDate))
                .sorted(getComparator())
                .collect(Collectors.toList());

        tableBids.setItems(FXCollections.observableArrayList(filtered));
        updateTotal(filtered.size());
    }

    private Comparator<AdminAuctionRow> getComparator() {
        String sort = sortOptions.getValue();
        if ("Cũ nhất trước".equals(sort)) {
            return Comparator.comparingLong(AdminAuctionRow::getCreatedAt);
        }
        if ("Giá cao nhất".equals(sort)) {
            return Comparator.comparingDouble(AdminAuctionRow::getRawCurrentPrice).reversed();
        }
        if ("Giá thấp nhất".equals(sort)) {
            return Comparator.comparingDouble(AdminAuctionRow::getRawCurrentPrice);
        }
        if ("Sắp kết thúc".equals(sort)) {
            return Comparator.comparingLong(AdminAuctionRow::getEndTimeMillis);
        }
        return Comparator.comparingLong(AdminAuctionRow::getCreatedAt).reversed();
    }

    private void updateTotal(int total) {
        if (totalLabel != null) {
            totalLabel.setText("Tổng số: " + total + " phiên đấu giá");
        }
    }

    public static class AdminAuctionRow {
        private final String auctionId;
        private final String itemId;
        private final String sellerId;
        private final String sellerName;
        private final String itemName;
        private final double rawCurrentPrice;
        private final String currentPrice;
        private final String endTime;
        private final String rawStatus;
        private final String status;
        private final long createdAt;
        private final long endTimeMillis;

        private AdminAuctionRow(Auction auction) {
            Item item = auction.getItem();
            AuctionStatus auctionStatus = auction.getStatus();

            this.auctionId = valueOrDash(auction.getAuctionId());
            this.itemId = valueOrDash(auction.getItemId());
            this.sellerId = valueOrDash(auction.getSellerId());
            this.sellerName = valueOrDash(auction.getSellerName());
            this.itemName = item != null ? valueOrDash(item.getName()) : "-";
            this.rawCurrentPrice = auction.getCurrentPrice();
            this.currentPrice = VND_FORMATTER.format(Math.round(auction.getCurrentPrice())) + " VNĐ";
            this.endTimeMillis = auction.getEndTime();
            this.endTime = formatDateTime(auction.getEndTime());
            this.rawStatus = auctionStatus != null ? auctionStatus.name() : "-";
            this.status = auctionStatus != null ? auctionStatus.name() : "-";
            this.createdAt = auction.getCreatedAt();
        }

        private static AdminAuctionRow fromAuction(Auction auction) {
            return new AdminAuctionRow(auction);
        }

        private boolean matches(String keyword) {
            return auctionId.toLowerCase().contains(keyword)
                    || itemId.toLowerCase().contains(keyword)
                    || sellerId.toLowerCase().contains(keyword)
                    || sellerName.toLowerCase().contains(keyword)
                    || itemName.toLowerCase().contains(keyword)
                    || rawStatus.toLowerCase().contains(keyword);
        }

        private static String formatDateTime(long millis) {
            if (millis <= 0) {
                return "-";
            }
            return Instant.ofEpochMilli(millis).atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
        }

        private static String valueOrDash(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }

        public SimpleStringProperty auctionIdProperty() {
            return new SimpleStringProperty(auctionId);
        }

        public SimpleStringProperty itemIdProperty() {
            return new SimpleStringProperty(itemId);
        }

        public SimpleStringProperty sellerIdProperty() {
            return new SimpleStringProperty(sellerId);
        }

        public SimpleStringProperty itemNameProperty() {
            return new SimpleStringProperty(itemName);
        }

        public SimpleStringProperty currentPriceProperty() {
            return new SimpleStringProperty(currentPrice);
        }

        public SimpleStringProperty endTimeProperty() {
            return new SimpleStringProperty(endTime);
        }

        public SimpleStringProperty statusProperty() {
            return new SimpleStringProperty(status);
        }

        public String getRawStatus() {
            return rawStatus;
        }

        public double getRawCurrentPrice() {
            return rawCurrentPrice;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getEndTimeMillis() {
            return endTimeMillis;
        }

        public LocalDate getCreatedDate() {
            return Instant.ofEpochMilli(createdAt).atZone(ZONE_ID).toLocalDate();
        }
    }
}
