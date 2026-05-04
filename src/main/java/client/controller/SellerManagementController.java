package client.controller;

import client.controller.SellerAuctionDetailsController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SellerManagementController {

    // ── Stats ─────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statSuccessRate;

    // ── Tabs ───────────────────────────────────────────────────
    @FXML private HBox tabAll, tabActive, tabPending, tabEnded, tabCancelled;
    @FXML private Label countAll, countActive, countPending, countEnded, countCancelled;

    // ── Filter bar ────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;

    // ── Table ─────────────────────────────────────────────────
    @FXML private TableView<AuctionItem> auctionTable;
    @FXML private TableColumn<AuctionItem, Boolean>  colSelect;
    @FXML private TableColumn<AuctionItem, String>   colProduct;
    @FXML private TableColumn<AuctionItem, String>   colId;
    @FXML private TableColumn<AuctionItem, String>   colPrice;
    @FXML private TableColumn<AuctionItem, String>   colBids;
    @FXML private TableColumn<AuctionItem, String>   colTimeLeft;
    @FXML private TableColumn<AuctionItem, String>   colStatus;
    @FXML private TableColumn<AuctionItem, Void>     colActions;

    // ── Pagination ────────────────────────────────────────────
    @FXML private Label  pageInfoLabel;
    @FXML private Button btnPrevPage, btnPage1, btnPage2, btnPage3, btnNextPage;

    // ── Buttons ───────────────────────────────────────────────
    @FXML private Button btnCreateAuction;

    // ── State ─────────────────────────────────────────────────
    private static final int PAGE_SIZE = 8;
    private int currentPage = 1;
    private String currentTab = "all";
    private ObservableList<AuctionItem> allItems;
    private List<HBox> allTabs;
    private Timeline countdownTimer;

    // ══════════════════════════════════════════════════════════
    //  initialize
    // ══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        allTabs = Arrays.asList(tabAll, tabActive, tabPending, tabEnded, tabCancelled);
        setupComboBoxes();
        setupTableColumns();
        loadSampleData();
        refreshTable();
        startCountdownTimer();
    }

    private void setupComboBoxes() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Mới nhất", "Cũ nhất", "Giá cao nhất", "Giá thấp nhất", "Lượt đặt giá nhiều nhất"
        ));
        categoryCombo.setItems(FXCollections.observableArrayList(
                "Tất cả danh mục", "Điện tử", "Thời trang", "Đồ sưu tầm", "Đồ gia dụng"
        ));
    }

    private void setupTableColumns() {
        colSelect.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        auctionTable.setEditable(true);

        colProduct.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        colProduct.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); return; }
                AuctionItem item = getTableView().getItems().get(getIndex());
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label(item.getIcon());
                icon.setStyle("-fx-font-size: 20px; -fx-min-width:36; -fx-min-height:36; " +
                        "-fx-background-color:#f3f4f6; -fx-background-radius:6; -fx-alignment:center;");
                VBox texts = new VBox(2);
                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
                Label skuLabel = new Label(item.getId());
                skuLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                texts.getChildren().addAll(nameLabel, skuLabel);
                box.getChildren().addAll(icon, texts);
                setGraphic(box);
            }
        });

        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));

        colPrice.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) { setGraphic(null); return; }
                AuctionItem item = getTableView().getItems().get(getIndex());
                VBox box = new VBox(2);
                Label current = new Label(formatVnd(item.getCurrentPrice()));
                current.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
                Label start = new Label("Khởi điểm: " + formatVnd(item.getStartPrice()));
                start.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                box.getChildren().addAll(current, start);
                setGraphic(box);
            }
        });

        colBids.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getBids())));
        colTimeLeft.setCellValueFactory(cell -> new SimpleStringProperty(formatTime(cell.getValue().getTimeLeftSeconds())));

        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(statusLabel(status));
                pill.setStyle("-fx-background-radius: 20; -fx-padding: 3 9; -fx-font-size: 11px; -fx-font-weight: bold; " + statusStyle(status));
                setGraphic(pill);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                AuctionItem item = getTableView().getItems().get(getIndex());
                setGraphic(buildActionButtons(item));
            }
        });
    }

    private void updateStats() {
        if (statTotal != null) statTotal.setText(String.valueOf(allItems.size()));

        long activeCount = allItems.stream().filter(i -> "active".equals(i.getStatus())).count();
        if (statActive != null) statActive.setText(String.valueOf(activeCount));

        if (statSuccessRate != null) statSuccessRate.setText("87%");
    }

    // ── Tab Handling ───────────────────────────────────────────
    @FXML private void handleTabAll()       { selectTab("all",       tabAll); }
    @FXML private void handleTabActive()    { selectTab("active",    tabActive); }
    @FXML private void handleTabPending()   { selectTab("pending",   tabPending); }
    @FXML private void handleTabEnded()     { selectTab("ended",     tabEnded); }
    @FXML private void handleTabCancelled() { selectTab("cancelled", tabCancelled); }

    private void selectTab(String tab, HBox selectedHBox) {
        currentTab = tab;
        currentPage = 1;
        String activeStyle   = "-fx-padding: 12 16; -fx-border-color: transparent transparent #111827 transparent; -fx-border-width: 0 0 2 0; -fx-background-color: white; -fx-cursor: hand;";
        String inactiveStyle = "-fx-padding: 12 16; -fx-border-color: transparent; -fx-background-color: white; -fx-cursor: hand;";

        for (HBox t : allTabs) {
            if (t == null) continue;
            t.setStyle(t == selectedHBox ? activeStyle : inactiveStyle);
        }
        refreshTable();
    }

    private void refreshTable() {
        String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<AuctionItem> filtered = allItems.stream()
                .filter(item -> {
                    if (!"all".equals(currentTab) && !item.getStatus().equals(currentTab)) return false;
                    return search.isEmpty() || item.getName().toLowerCase().contains(search) || item.getId().toLowerCase().contains(search);
                }).collect(Collectors.toList());

        int total = filtered.size();

        // Bắt lỗi an toàn khi không tìm thấy dữ liệu
        if (total == 0) {
            auctionTable.setItems(FXCollections.observableArrayList());
            if (pageInfoLabel != null) pageInfoLabel.setText("Không có phiên nào");
            updatePaginationButtons(0);
            return;
        }

        int fromIdx = (currentPage - 1) * PAGE_SIZE;
        if (fromIdx >= total) {
            currentPage = 1;
            fromIdx = 0;
        }
        int toIdx = Math.min(fromIdx + PAGE_SIZE, total);

        auctionTable.setItems(FXCollections.observableArrayList(filtered.subList(fromIdx, toIdx)));
        if (pageInfoLabel != null) pageInfoLabel.setText("Hiển thị " + (fromIdx + 1) + "–" + toIdx + " trong " + total + " phiên");
        updatePaginationButtons(total);
    }

    private void updatePaginationButtons(int total) {
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages || totalPages == 0);
        highlightActivePage();
    }

    private void highlightActivePage() {
        String active = "-fx-background-color:#111827; -fx-text-fill:white; -fx-border-radius:6; -fx-min-width:28;";
        String inactive = "-fx-background-color:white; -fx-border-color:#d1d5db; -fx-border-radius:6; -fx-min-width:28;";
        if (btnPage1 != null) btnPage1.setStyle(currentPage == 1 ? active : inactive);
        if (btnPage2 != null) btnPage2.setStyle(currentPage == 2 ? active : inactive);
        if (btnPage3 != null) btnPage3.setStyle(currentPage == 3 ? active : inactive);
    }

    private void loadSampleData() {
        allItems = FXCollections.observableArrayList(
                new AuctionItem("AU-2024-001", "Đồng hồ Seiko 5 Sports", "⌚", 1200000, 2850000, 18, "active", 7200),
                new AuctionItem("AU-2024-002", "Túi da bò handmade", "👜", 800000, 1450000, 9, "active", 18000),
                new AuctionItem("AU-2024-007", "Figurine Luffy", "🗿", 500000, 1100000, 31, "ended", 0)
        );
        updateTabCounts();
        updateStats();
    }

    private void updateTabCounts() {
        if (countAll != null) countAll.setText(String.valueOf(allItems.size()));
        if (countActive != null) countActive.setText(String.valueOf(allItems.stream().filter(i -> "active".equals(i.getStatus())).count()));
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            allItems.forEach(item -> {
                if ("active".equals(item.getStatus()) && item.getTimeLeftSeconds() > 0) {
                    item.setTimeLeftSeconds(item.getTimeLeftSeconds() - 1);
                    if (item.getTimeLeftSeconds() == 0) item.setStatus("ended");
                }
            });
            Platform.runLater(() -> auctionTable.refresh());
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    // ── Helpers & Action Wiring ──────────────────────────────────
    private HBox buildActionButtons(AuctionItem item) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        Button btnView = new Button("Xem");
        btnView.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");

        // Đã kết nối nút Xem với hàm mở Modal
        btnView.setOnAction(e -> handleViewAuctionDetails(item.getId()));

        box.getChildren().add(btnView);
        return box;
    }

    private static String formatVnd(long amount) { return String.format("%,d đ", amount).replace(',', '.'); }
    private static String formatTime(long seconds) {
        if (seconds <= 0) return "—";
        return (seconds / 3600) + "g " + ((seconds % 3600) / 60) + "p";
    }
    private static String statusLabel(String s) {
        return switch (s) {
            case "active" -> "● Đang diễn ra";
            case "ended" -> "● Đã kết thúc";
            case "pending" -> "● Chờ bắt đầu";
            default -> "● Đã hủy";
        };
    }
    private static String statusStyle(String s) {
        return switch (s) {
            case "active" -> "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
            case "pending" -> "-fx-background-color: #faeeda; -fx-text-fill: #854f0b;";
            default -> "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;";
        };
    }

    @FXML private void handleSearch(KeyEvent e) { currentPage = 1; refreshTable(); }
    @FXML private void handleApplyFilter() { refreshTable(); }
    @FXML private void handleResetFilter() { searchField.clear(); refreshTable(); }
    @FXML private void handleCreateAuction() { System.out.println("Tạo mới"); }
    @FXML private void handleExportData() { System.out.println("Xuất dữ liệu"); }
    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; refreshTable(); } }
    @FXML private void handleNextPage() { currentPage++; refreshTable(); }
    @FXML private void handlePage1() { currentPage = 1; refreshTable(); }
    @FXML private void handlePage2() { currentPage = 2; refreshTable(); }
    @FXML private void handlePage3() { currentPage = 3; refreshTable(); }

    @FXML
    private void handleViewAuctionDetails(String auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SellerAuctionDetails.fxml"));
            VBox modalContent = loader.load();

            SellerAuctionDetailsController controller = loader.getController();
            controller.loadSellerAuctionDetails(auctionId);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Chi tiết phiên đấu giá");
            dialog.setDialogPane(new DialogPane() {
                {
                    setContent(modalContent);
                    setPrefWidth(700);
                    setPrefHeight(600);
                }
            });

            dialog.getDialogPane().getStyleClass().add("my-custom-alert");
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class AuctionItem {
        private final String id, name, icon;
        private final long startPrice;
        private long currentPrice, timeLeftSeconds;
        private int bids;
        private String status;
        private final javafx.beans.property.BooleanProperty selected = new SimpleBooleanProperty(false);

        public AuctionItem(String id, String name, String icon, long sp, long cp, int b, String s, long t) {
            this.id = id; this.name = name; this.icon = icon; this.startPrice = sp;
            this.currentPrice = cp; this.bids = b; this.status = s; this.timeLeftSeconds = t;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public long getStartPrice() { return startPrice; }
        public long getCurrentPrice() { return currentPrice; }
        public int getBids() { return bids; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public long getTimeLeftSeconds() { return timeLeftSeconds; }
        public void setTimeLeftSeconds(long t) { this.timeLeftSeconds = t; }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
    }
}