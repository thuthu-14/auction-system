package client.controller;

import client.network.ConnectionManager;
import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import navigation.NavigationManager;
import server.model.User;
import util.LoggerUtil;

// --- THÊM IMPORT CHO THỜI GIAN ---
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import util.DateTimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SellerManagementController {

    // --- Các ID từ FXML ---
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

    private String currentTab = "all";
    private ObservableList<AuctionItem> allItems = FXCollections.observableArrayList();
    private List<HBox> allTabs;

    // THÊM BIẾN ĐẾM NGƯỢC CHO BẢNG
    private Timeline countdownTimer;

    @FXML
    public void initialize() {
        allTabs = Arrays.asList(tabAll, tabActive, tabPending, tabEnded, tabCancelled);
        setupTableColumns();

        if (sortCombo != null) sortCombo.setItems(FXCollections.observableArrayList("Mới nhất", "Cũ nhất", "Giá cao", "Giá thấp"));
        if (categoryCombo != null) categoryCombo.setItems(FXCollections.observableArrayList("Tất cả", "Điện tử", "Thời trang", "Gia dụng"));

        refreshAuctions();

        // --- GỌI TIMELINE KHI KHỞI TẠO ---
        startTableCountdown();
    }

    private void setupTableColumns() {
        colSelect.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        auctionTable.setEditable(true);

        colProduct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AuctionItem ai = getTableView().getItems().get(getIndex());
                VBox box = new VBox(2);
                Label name = new Label(ai.getName());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                Label id = new Label("ID: " + ai.getId());
                id.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
                box.getChildren().addAll(name, id);
                setGraphic(box);
            }
        });

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String p, boolean empty) {
                super.updateItem(p, empty);
                if (empty) { setGraphic(null); return; }
                AuctionItem ai = getTableView().getItems().get(getIndex());
                VBox box = new VBox(2);
                Label cur = new Label(formatVnd(ai.getCurrentPrice()));
                cur.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                Label start = new Label("Gốc: " + formatVnd(ai.getStartPrice()));
                start.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
                box.getChildren().addAll(cur, start);
                setGraphic(box);
            }
        });

        colBids.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getBids())));

        // --- SỬA LẠI LINK CỘT THỜI GIAN VỚI PROPERTY TỰ ĐỘNG CẬP NHẬT ---
        colTimeLeft.setCellValueFactory(cell -> cell.getValue().timeLeftStrProperty());

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                String status = getTableView().getItems().get(getIndex()).getStatus().toUpperCase();
                Label l = new Label(status);
                String style = "-fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold;";
                if(status.equals("OPEN")) style += "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
                else if(status.equals("CLOSED")) style += "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;";
                else style += "-fx-background-color: #fff7ed; -fx-text-fill: #9a3412;";
                l.setStyle(style);
                setGraphic(l);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Button btn = new Button("Chi tiết");
                btn.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-cursor: hand;");
                setGraphic(btn);
            }
        });
    }

    public void refreshAuctions() {
        User user = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        if (user == null || socket == null) return;

        new Thread(() -> {
            try {
                Message req = new Message(MessageType.GET_SELLER_AUCTIONS, null, user.getUsername());
                socket.sendMessage(req);
                Message res = socket.receiveMessage();

                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    Object data = res.getData();

                    final List<server.model.Auction> auctionList = new java.util.ArrayList<>();

                    if (data instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<server.model.Auction> dataList = (List<server.model.Auction>) data;
                        auctionList.addAll(dataList);
                    } else if (data instanceof server.model.Auction) {
                        auctionList.add((server.model.Auction) data);
                    }

                    Platform.runLater(() -> {
                        allItems.clear();
                        for (server.model.Auction a : auctionList) {
                            // --- TRUYỀN a.getEndTime() THAY VÌ getTimeRemainingSeconds() ---
                            allItems.add(new AuctionItem(a.getAuctionId(), a.getItem().getName(),
                                    (long)a.getItem().getStartingPrice(), (long)a.getCurrentPrice(),
                                    a.getBidIds().size(), a.getStatus().toString(), a.getEndTime()));
                        }
                        updateStats();
                        refreshTable();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // =========================================
    // --- THÊM HÀM CHẠY NGẦM ĐỂ CẬP NHẬT TỪNG DÒNG TRONG BẢNG ---
    // =========================================
    private void startTableCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            for (AuctionItem item : allItems) {
                item.updateTime(); // Cập nhật thời gian từng đối tượng
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }
    // =========================================

    private void updateStats() {
        statTotal.setText(String.valueOf(allItems.size()));
        countAll.setText(String.valueOf(allItems.size()));

        long active = allItems.stream().filter(i -> i.getStatus().equalsIgnoreCase("OPEN")).count();
        statActive.setText(String.valueOf(active));
        countActive.setText(String.valueOf(active));

        countPending.setText(String.valueOf(allItems.stream().filter(i -> i.getStatus().equalsIgnoreCase("PENDING")).count()));
        countEnded.setText(String.valueOf(allItems.stream().filter(i -> i.getStatus().equalsIgnoreCase("CLOSED")).count()));
        countCancelled.setText(String.valueOf(allItems.stream().filter(i -> i.getStatus().equalsIgnoreCase("CANCELLED")).count()));
        pageInfoLabel.setText("Hiển thị " + allItems.size() + " phiên của bạn");
    }

    private void refreshTable() {
        List<AuctionItem> filtered = allItems.stream()
                .filter(i -> "all".equals(currentTab) || i.getStatus().equalsIgnoreCase(currentTab))
                .collect(Collectors.toList());
        auctionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN TỪ FXML GIỮ NGUYÊN ---
    @FXML private void handleTabAll() { selectTab("all", tabAll); }
    @FXML private void handleTabActive() { selectTab("OPEN", tabActive); }
    @FXML private void handleTabPending() { selectTab("PENDING", tabPending); }
    @FXML private void handleTabEnded() { selectTab("CLOSED", tabEnded); }
    @FXML private void handleTabCancelled() { selectTab("CANCELLED", tabCancelled); }

    private void selectTab(String status, HBox selectedTab) {
        this.currentTab = status;
        allTabs.forEach(t -> t.getStyleClass().remove("auction-tab-active"));
        selectedTab.getStyleClass().add("auction-tab-active");
        refreshTable();
    }

    @FXML private void handleSearch() { refreshTable(); }
    @FXML private void handleApplyFilter() { refreshTable(); }
    @FXML private void handleResetFilter() { if(searchField != null) searchField.clear(); refreshTable(); }
    @FXML private void handleExportData() { LoggerUtil.info("Exporting data..."); }
    @FXML private void handleCreateAuction() { LoggerUtil.info("Opening create auction form..."); }

    @FXML private void handlePrevPage() { LoggerUtil.info("Trang trước"); }
    @FXML private void handleNextPage() { LoggerUtil.info("Trang sau"); }
    @FXML private void handlePage1() { LoggerUtil.info("Trang 1"); }
    @FXML private void handlePage2() { LoggerUtil.info("Trang 2"); }
    @FXML private void handlePage3() { LoggerUtil.info("Trang 3"); }

    private String formatVnd(long a) { return String.format("%,d đ", a).replace(',', '.'); }

    // Đã bỏ hàm formatTime() thủ công vì chuyển sang dùng DateTimeUtil

    public static class AuctionItem {
        private String id, name, status;
        private long startPrice, currentPrice, endTime; // Đổi timeLeftSeconds thành endTime
        private int bids;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        private final SimpleStringProperty timeLeftStr = new SimpleStringProperty(""); // Biến giữ thời gian đã format

        public AuctionItem(String id, String name, long sp, long cp, int b, String s, long endTime) {
            this.id = id; this.name = name; this.startPrice = sp;
            this.currentPrice = cp; this.bids = b; this.status = s; this.endTime = endTime;
            updateTime();
        }

        // --- HÀM CẬP NHẬT THỜI GIAN THEO CHUẨN DATETIMEUTIL ---
        public void updateTime() {
            if (DateTimeUtil.isExpired(endTime)) {
                timeLeftStr.set("Hết");
            } else {
                long remainingMs = endTime - System.currentTimeMillis();
                timeLeftStr.set(DateTimeUtil.formatTimestamp(remainingMs));
            }
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public long getStartPrice() { return startPrice; }
        public long getCurrentPrice() { return currentPrice; }
        public int getBids() { return bids; }
        public long getEndTime() { return endTime; }
        public SimpleBooleanProperty selectedProperty() { return selected; }
        public SimpleStringProperty timeLeftStrProperty() { return timeLeftStr; } // Cung cấp cho TableColumn
    }
}