package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClient;
import client.service.SellerClientService;
import common.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Item;
import server.model.User;
import util.DateTimeUtil;
import util.LoggerUtil;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SellerManagementController {

    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private Label statTotal, statActive, statSuccessRate;
    @FXML private HBox tabAll, tabActive, tabPending, tabEnded, tabCancelled;
    @FXML private Label countAll, countActive, countPending, countEnded, countCancelled;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo, categoryCombo;
    @FXML private TableView<AuctionItem> auctionTable;
    @FXML private TableColumn<AuctionItem, Boolean> colSelect;
    @FXML private TableColumn<AuctionItem, String> colProduct, colId, colPrice, colBids, colTimeLeft, colStatus;
    @FXML private TableColumn<AuctionItem, Void> colActions;
    @FXML private Label pageInfoLabel;

    private final ObservableList<AuctionItem> allItems = FXCollections.observableArrayList();
    private final SellerClient sellerClientService;

    private List<HBox> allTabs;
    private String currentTab = "all";
    private User currentUser;
    private ClientSocket clientSocket;
    private Timeline countdownTimer;
    private SellerHomeController sellerHomeController;

    public SellerManagementController() {
        this(new SellerClientService());
    }

    SellerManagementController(SellerClient sellerClientService) {
        this.sellerClientService = sellerClientService;
    }

    @FXML
    public void initialize() {
        allTabs = Arrays.asList(tabAll, tabActive, tabPending, tabEnded, tabCancelled);
        setupFilters();
        setupTableColumns();
        startTableCountdown();

        User navigationUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket navigationSocket = getActiveSocket();
        if (navigationUser != null && navigationSocket != null) {
            setUserData(navigationUser, navigationSocket);
        } else {
            updateStats();
            refreshTable();
        }
    }

    public void setUserData(User user, ClientSocket socket) {
        currentUser = user;
        clientSocket = socket;
        if (user != null) {
            NavigationManager.getInstance().setCurrentUser(user);
        }
        if (socket != null) {
            NavigationManager.getInstance().setClientSocket(socket);
        }
        refreshAuctions();
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    private void setupFilters() {
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Mới nhất", "Cũ nhất", "Giá cao", "Giá thấp"));
            sortCombo.getSelectionModel().selectFirst();
        }
        if (categoryCombo != null) {
            categoryCombo.setItems(FXCollections.observableArrayList("Tất cả", "Điện tử", "Thời trang", "Trang sức", "Sưu tầm", "Xe cộ", "Khác"));
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private void setupTableColumns() {
        if (auctionTable == null) {
            return;
        }

        colSelect.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        auctionTable.setEditable(true);

        colProduct.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colProduct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AuctionItem row = getTableView().getItems().get(getIndex());
                VBox box = new VBox(2);
                Label name = new Label(row.getName());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                Label id = new Label("ID: " + row.getId());
                id.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
                box.getChildren().addAll(name, id);
                setGraphic(box);
            }
        });

        colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        colPrice.setCellValueFactory(cell -> cell.getValue().priceProperty());
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AuctionItem row = getTableView().getItems().get(getIndex());
                VBox box = new VBox(2);
                Label current = new Label(formatVnd(row.getCurrentPrice()));
                current.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                Label start = new Label("Gốc: " + formatVnd(row.getStartPrice()));
                start.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
                box.getChildren().addAll(current, start);
                setGraphic(box);
            }
        });

        colBids.setCellValueFactory(cell -> cell.getValue().bidsProperty());
        colTimeLeft.setCellValueFactory(cell -> cell.getValue().timeLeftStrProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusTextProperty());
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AuctionItem row = getTableView().getItems().get(getIndex());
                Label label = new Label(row.getStatusText());
                label.setStyle(statusStyle(row.getStatusKey()));
                setGraphic(label);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Button button = new Button("Chi tiết");
                button.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-cursor: hand;");
                button.setOnAction(event -> {
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        return;
                    }
                    AuctionItem row = getTableView().getItems().get(getIndex());
                    if (sellerHomeController != null) {
                        sellerHomeController.loadSellerAuctionDetailsView(row.getId());
                    } else {
                        LoggerUtil.warn("SellerHomeController is null, cannot open auction details: " + row.getId());
                    }
                });
                setGraphic(button);
            }
        });
    }

    public void refreshAuctions() {
        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = getActiveSocket();
        if (user == null || socket == null || !socket.isConnected()) {
            LoggerUtil.warn("Seller auctions table: missing user or socket");
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Auction> auctionList = sellerClientService.fetchSellerAuctions(socket, user);
                Platform.runLater(() -> {
                    allItems.setAll(auctionList.stream()
                            .filter(auction -> auction != null && auction.getItem() != null)
                            .map(AuctionItem::new)
                            .collect(Collectors.toList()));
                    updateStats();
                    refreshTable();
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load seller auctions table failed: " + e.getMessage())));
                Platform.runLater(() -> {
                    allItems.clear();
                    updateStats();
                    refreshTable();
                });
            }
        });
    }

    private ClientSocket getActiveSocket() {
        if (clientSocket != null && clientSocket.isConnected()) {
            return clientSocket;
        }
        ClientSocket navigationSocket = NavigationManager.getInstance().getClientSocket();
        if (navigationSocket != null && navigationSocket.isConnected()) {
            clientSocket = navigationSocket;
            return navigationSocket;
        }
        ClientSocket managerSocket = ConnectionManager.getInstance().getClientSocket();
        if (managerSocket != null && managerSocket.isConnected()) {
            clientSocket = managerSocket;
            return managerSocket;
        }
        return null;
    }

    private void startTableCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            for (AuctionItem item : allItems) {
                item.updateTime();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateStats() {
        int total = allItems.size();
        long active = allItems.stream().filter(AuctionItem::isActive).count();
        long pending = allItems.stream().filter(AuctionItem::isPending).count();
        long ended = allItems.stream().filter(AuctionItem::isEnded).count();
        long cancelled = allItems.stream().filter(AuctionItem::isCancelled).count();
        long successful = allItems.stream().filter(item -> item.isEnded() && item.getBids() > 0).count();

        setText(statTotal, String.valueOf(total));
        setText(statActive, String.valueOf(active));
        setText(statSuccessRate, ended == 0 ? "0.0%" : String.format(Locale.US, "%.1f%%", successful * 100.0 / ended));

        setText(countAll, String.valueOf(total));
        setText(countActive, String.valueOf(active));
        setText(countPending, String.valueOf(pending));
        setText(countEnded, String.valueOf(ended));
        setText(countCancelled, String.valueOf(cancelled));
        setText(pageInfoLabel, "Hiển thị " + total + " phiên");
    }

    private void refreshTable() {
        if (auctionTable == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryCombo == null || categoryCombo.getValue() == null
                ? "Tất cả"
                : categoryCombo.getValue();

        List<AuctionItem> filtered = allItems.stream()
                .filter(item -> matchesTab(item))
                .filter(item -> keyword.isBlank()
                        || item.getName().toLowerCase(Locale.ROOT).contains(keyword)
                        || item.getId().toLowerCase(Locale.ROOT).contains(keyword))
                .filter(item -> "Tất cả".equals(category) || item.getCategoryLabel().equals(category))
                .sorted(selectedComparator())
                .collect(Collectors.toList());

        auctionTable.setItems(FXCollections.observableArrayList(filtered));
        setText(pageInfoLabel, "Hiển thị " + filtered.size() + " / " + allItems.size() + " phiên");
    }

    private boolean matchesTab(AuctionItem item) {
        return switch (currentTab) {
            case "active" -> item.isActive();
            case "pending" -> item.isPending();
            case "ended" -> item.isEnded();
            case "cancelled" -> item.isCancelled();
            default -> true;
        };
    }

    private Comparator<AuctionItem> selectedComparator() {
        String sort = sortCombo == null ? null : sortCombo.getValue();
        if ("Cũ nhất".equals(sort)) {
            return Comparator.comparingLong(AuctionItem::getCreatedAt);
        }
        if ("Giá cao".equals(sort)) {
            return Comparator.comparingLong(AuctionItem::getCurrentPrice).reversed();
        }
        if ("Giá thấp".equals(sort)) {
            return Comparator.comparingLong(AuctionItem::getCurrentPrice);
        }
        return Comparator.comparingLong(AuctionItem::getCreatedAt).reversed();
    }

    @FXML private void handleTabAll() { selectTab("all", tabAll); }
    @FXML private void handleTabActive() { selectTab("active", tabActive); }
    @FXML private void handleTabPending() { selectTab("pending", tabPending); }
    @FXML private void handleTabEnded() { selectTab("ended", tabEnded); }
    @FXML private void handleTabCancelled() { selectTab("cancelled", tabCancelled); }

    private void selectTab(String status, HBox selectedTab) {
        currentTab = status;
        for (HBox tab : allTabs) {
            if (tab != null) {
                tab.getStyleClass().remove("auction-tab-active");
            }
        }
        if (selectedTab != null && !selectedTab.getStyleClass().contains("auction-tab-active")) {
            selectedTab.getStyleClass().add("auction-tab-active");
        }
        refreshTable();
    }

    @FXML private void handleSearch() { refreshTable(); }
    @FXML private void handleApplyFilter() { refreshTable(); }

    @FXML
    private void handleResetFilter() {
        if (searchField != null) {
            searchField.clear();
        }
        if (sortCombo != null) {
            sortCombo.getSelectionModel().selectFirst();
        }
        if (categoryCombo != null) {
            categoryCombo.getSelectionModel().selectFirst();
        }
        refreshTable();
    }

    @FXML private void handleExportData() { LoggerUtil.info("Exporting seller auction data..."); }
    @FXML
    private void handleCreateAuction() {
        if (sellerHomeController != null) {
            sellerHomeController.loadAddAuctionProductView();
        } else {
            LoggerUtil.info("Opening create auction form...");
        }
    }
    @FXML private void handlePrevPage() { LoggerUtil.info("Trang trước"); }
    @FXML private void handleNextPage() { LoggerUtil.info("Trang sau"); }
    @FXML private void handlePage1() { LoggerUtil.info("Trang 1"); }
    @FXML private void handlePage2() { LoggerUtil.info("Trang 2"); }
    @FXML private void handlePage3() { LoggerUtil.info("Trang 3"); }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String formatVnd(long amount) {
        return VND_FORMATTER.format(amount) + " đ";
    }

    private String statusStyle(String statusKey) {
        String style = "-fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        return switch (statusKey) {
            case "active" -> style + "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
            case "pending" -> style + "-fx-background-color: #e6f1fb; -fx-text-fill: #185fa5;";
            case "ended" -> style + "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563;";
            case "cancelled" -> style + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            default -> style + "-fx-background-color: #fff7ed; -fx-text-fill: #9a3412;";
        };
    }

    public static class AuctionItem {
        private final String id;
        private final String itemId;
        private final String name;
        private final String categoryLabel;
        private AuctionStatus status;
        private final long startPrice;
        private final long currentPrice;
        private final long startTime;      // ← THÊM DÒNG NÀY
        private final long endTime;
        private final long createdAt;
        private final int bids;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        private final SimpleStringProperty timeLeftStr = new SimpleStringProperty("");

        public AuctionItem(Auction auction) {
            Item item = auction.getItem();
            itemId = safe(auction.getItemId() != null && !auction.getItemId().isBlank()
                    ? auction.getItemId()
                    : item.getItemId());
            id = safe(auction.getAuctionId());
            name = safe(item.getName());
            categoryLabel = categoryLabel(item.getCategory());
            status = auction.getStatus();
            startPrice = Math.round(item.getStartingPrice());
            currentPrice = Math.round(auction.getCurrentPrice());
            startTime = auction.getStartTime();  // ← THÊM DÒNG NÀY
            endTime = auction.getEndTime();
            createdAt = auction.getCreatedAt();
            bids = auction.getBidIds() == null ? 0 : auction.getBidIds().size();
            updateTime();
        }

        public void updateTime() {
            long now = System.currentTimeMillis();

            if (status == AuctionStatus.WAITING && now >= startTime && now < endTime) {
                status = AuctionStatus.OPEN;
            }

            if (isEnded() || isCancelled()) {
                timeLeftStr.set(isCancelled() ? "Đã hủy" : "Đã kết thúc");
            } else if (endTime <= 0) {
                timeLeftStr.set("--");
            } else if (isPending() && now < startTime) {
                long waitingMs = Math.max(0, startTime - now);
                timeLeftStr.set("Bắt đầu trong: " + DateTimeUtil.formatTimestamp(waitingMs));
            } else {
                long remainingMs = Math.max(0, endTime - now);
                timeLeftStr.set(DateTimeUtil.formatTimestamp(remainingMs));
            }
        }

        public boolean isActive() {
            return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
        }

        public boolean isPending() {
            return status == AuctionStatus.WAITING;
        }

        public boolean isEnded() {
            return status == AuctionStatus.FINISHED
                    || status == AuctionStatus.CLOSED
                    || (endTime > 0 && endTime <= System.currentTimeMillis() && status != AuctionStatus.CANCELLED);
        }

        public boolean isCancelled() {
            return status == AuctionStatus.CANCELLED;
        }

        public String getStatusKey() {
            if (isActive()) return "active";
            if (isPending()) return "pending";
            if (isEnded()) return "ended";
            if (isCancelled()) return "cancelled";
            return "other";
        }

        public String getStatusText() {
            return switch (getStatusKey()) {
                case "active" -> "Đang diễn ra";
                case "pending" -> "Chờ bắt đầu";
                case "ended" -> "Đã kết thúc";
                case "cancelled" -> "Đã hủy";
                default -> status == null ? "--" : status.name();
            };
        }

        public String getId() { return id; }
        public String getItemId() { return itemId; }
        public String getName() { return name; }
        public String getCategoryLabel() { return categoryLabel; }
        public long getStartPrice() { return startPrice; }
        public long getCurrentPrice() { return currentPrice; }
        public long getCreatedAt() { return createdAt; }
        public int getBids() { return bids; }

        public SimpleBooleanProperty selectedProperty() { return selected; }
        public SimpleStringProperty nameProperty() { return new SimpleStringProperty(name); }
        public SimpleStringProperty idProperty() { return new SimpleStringProperty(id); }
        public SimpleStringProperty priceProperty() { return new SimpleStringProperty(String.valueOf(currentPrice)); }
        public SimpleStringProperty bidsProperty() { return new SimpleStringProperty(String.valueOf(bids)); }
        public SimpleStringProperty statusTextProperty() { return new SimpleStringProperty(getStatusText()); }
        public SimpleStringProperty timeLeftStrProperty() { return timeLeftStr; }

        private static String safe(String value) {
            return value == null || value.isBlank() ? "--" : value;
        }

        private static String categoryLabel(common.ItemCategory category) {
            if (category == null) {
                return "Khác";
            }
            return switch (category) {
                case ELECTRONICS -> "Điện tử";
                case FASHION -> "Thời trang";
                case JEWELRY -> "Trang sức";
                case ART -> "Sưu tầm";
                case VEHICLE -> "Xe cộ";
                case OTHER -> "Khác";
            };
        }
    }
}

