package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.StageStyle;
import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ManageAuctionsController {

    // ── Stats ──────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statRevenue;
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

    // ── ComboBox setup ────────────────────────────────────────
    private void setupComboBoxes() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Mới nhất", "Cũ nhất", "Giá cao nhất", "Giá thấp nhất", "Lượt đặt giá nhiều nhất"
        ));
        categoryCombo.setItems(FXCollections.observableArrayList(
                "Tất cả danh mục", "Điện tử", "Thời trang", "Đồ sưu tầm", "Đồ gia dụng"
        ));
    }

    // ── Table columns ─────────────────────────────────────────
    private void setupTableColumns() {
        // Checkbox
        colSelect.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);
        auctionTable.setEditable(true);

        // Sản phẩm — icon + tên + mã
        colProduct.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName()));
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
                setText(null);
            }
        });

        // Mã phiên
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) { setText(null); return; }
                setText(id);
                setStyle("-fx-font-size:12px; -fx-text-fill:#6b7280;");
            }
        });

        // Giá
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
                setText(null);
            }
        });

        // Lượt đặt
        colBids.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getBids())));
        colBids.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String bids, boolean empty) {
                super.updateItem(bids, empty);
                if (empty || bids == null) { setGraphic(null); return; }
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                Label num = new Label(bids);
                num.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
                Label unit = new Label("lượt");
                unit.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");
                box.getChildren().addAll(num, unit);
                setGraphic(box);
                setText(null);
            }
        });

        // Thời gian còn lại
        colTimeLeft.setCellValueFactory(cell ->
                new SimpleStringProperty(formatTime(cell.getValue().getTimeLeftSeconds())));
        colTimeLeft.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) { setText(null); return; }
                setText(time);
                boolean ended = time.equals("—");
                setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:"
                        + (ended ? "#9ca3af" : "#854f0b") + ";");
            }
        });

        // Trạng thái
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(statusLabel(status));
                pill.setStyle("-fx-background-radius: 20; -fx-padding: 3 9; -fx-font-size: 11px; -fx-font-weight: bold; "
                        + statusStyle(status));
                setGraphic(pill);
                setText(null);
            }
        });

        // Thao tác
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                AuctionItem item = getTableView().getItems().get(getIndex());
                HBox btns = buildActionButtons(item);
                setGraphic(btns);
            }
        });
    }

    // ── Build action buttons per row ──────────────────────────
    private HBox buildActionButtons(AuctionItem item) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        String status = item.getStatus();

        Button btnView = smallButton("Xem", false);
        btnView.setOnAction(e -> handleViewAuction(item));

        if ("active".equals(status) || "pending".equals(status)) {
            Button btnEdit = smallButton("Chỉnh sửa", false);
            Button btnCancel = smallButton("Hủy", true);
            btnEdit.setOnAction(e -> handleEditAuction(item));
            btnCancel.setOnAction(e -> handleCancelAuction(item));
            box.getChildren().addAll(btnView, btnEdit, btnCancel);
        } else if ("ended".equals(status)) {
            Button btnRepost = smallButton("Đăng lại", false);
            btnRepost.setOnAction(e -> handleRepostAuction(item));
            box.getChildren().addAll(btnView, btnRepost);
        } else if ("cancelled".equals(status)) {
            Button btnRecreate = smallButton("Tạo lại", false);
            btnRecreate.setOnAction(e -> handleRecreateAuction(item));
            box.getChildren().addAll(btnView, btnRecreate);
        } else {
            box.getChildren().add(btnView);
        }
        return box;
    }

    private Button smallButton(String text, boolean danger) {
        Button btn = new Button(text);
        if (danger) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: #f7c1c1; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #a32d2d; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; " +
                    "-fx-border-radius: 6; -fx-background-radius: 6; " +
                    "-fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #374151; -fx-cursor: hand;");
        }
        return btn;
    }

    // ── Sample data ───────────────────────────────────────────
    private void loadSampleData() {
        allItems = FXCollections.observableArrayList(
                new AuctionItem("AU-2024-001", "Đồng hồ Seiko 5 Sports",   "⌚", 1_200_000, 2_850_000, 18, "active",    7200),
                new AuctionItem("AU-2024-002", "Túi da bò thật handmade",   "👜", 800_000,  1_450_000,  9, "active",   18000),
                new AuctionItem("AU-2024-003", "Lens Canon 50mm f/1.8",     "📷", 2_000_000,3_200_000, 24, "active",    3600),
                new AuctionItem("AU-2024-004", "Bộ bài tarot vintage",      "🃏", 350_000,    350_000,  0, "pending",  86400),
                new AuctionItem("AU-2024-005", "Máy pha cà phê Delonghi",   "☕", 4_500_000,4_500_000,  0, "pending", 172800),
                new AuctionItem("AU-2024-006", "Giày Nike Air Max 90",      "👟", 900_000,    900_000,  0, "pending",  43200),
                new AuctionItem("AU-2024-007", "Figurine One Piece Luffy",  "🗿", 500_000,  1_100_000, 31, "ended",       0),
                new AuctionItem("AU-2024-008", "Đèn bàn Xiaomi",            "💡", 300_000,    680_000, 12, "ended",       0),
                new AuctionItem("AU-2024-009", "Áo khoác denim vintage",    "🧥", 600_000,    950_000,  7, "ended",       0),
                new AuctionItem("AU-2024-010", "Bộ lego Architecture",      "🏗", 1_500_000,1_500_000,  0, "cancelled",   0),
                new AuctionItem("AU-2024-011", "Tai nghe Sony WH-1000XM5",  "🎧", 5_000_000,7_200_000, 43, "ended",       0),
                new AuctionItem("AU-2024-012", "Nồi cơm điện Cuckoo",       "🍚", 1_200_000,1_200_000,  0, "cancelled",   0),
                new AuctionItem("AU-2024-013", "Sách cổ 1975",              "📚", 200_000,    440_000,  8, "ended",       0),
                new AuctionItem("AU-2024-014", "iPad Pro 12.9 M2",          "📱",18_000_000,24_500_000,56,"ended",       0)
        );
        updateTabCounts();
        updateStats();
    }

    // ── Refresh table with current tab + search ───────────────
    private void refreshTable() {
        String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        List<AuctionItem> filtered = allItems.stream()
                .filter(item -> {
                    if (!"all".equals(currentTab) && !item.getStatus().equals(currentTab)) return false;
                    if (!search.isEmpty()) {
                        return item.getName().toLowerCase().contains(search)
                                || item.getId().toLowerCase().contains(search);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIdx = (currentPage - 1) * PAGE_SIZE;
        int toIdx   = Math.min(fromIdx + PAGE_SIZE, total);

        if (fromIdx > total) { currentPage = 1; fromIdx = 0; toIdx = Math.min(PAGE_SIZE, total); }

        List<AuctionItem> page = filtered.subList(fromIdx, toIdx);
        auctionTable.setItems(FXCollections.observableArrayList(page));

        if (pageInfoLabel != null) {
            pageInfoLabel.setText(total == 0
                    ? "Không có phiên nào"
                    : "Hiển thị " + (fromIdx + 1) + "–" + toIdx + " trong " + total + " phiên");
        }

        updatePaginationButtons(total);
    }

    private void updatePaginationButtons(int total) {
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        if (btnPage1 != null) btnPage1.setDisable(totalPages < 1);
        if (btnPage2 != null) btnPage2.setDisable(totalPages < 2);
        if (btnPage3 != null) btnPage3.setDisable(totalPages < 3);
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages || totalPages == 0);
        highlightActivePage();
    }

    private void highlightActivePage() {
        String active   = "-fx-background-color:#111827; -fx-text-fill:white; -fx-border-radius:6; -fx-background-radius:6; -fx-min-width:28; -fx-min-height:28; -fx-font-size:12px; -fx-cursor:hand;";
        String inactive = "-fx-background-color:white; -fx-border-color:#d1d5db; -fx-border-radius:6; -fx-background-radius:6; -fx-min-width:28; -fx-min-height:28; -fx-font-size:12px; -fx-cursor:hand;";
        if (btnPage1 != null) btnPage1.setStyle(currentPage == 1 ? active : inactive);
        if (btnPage2 != null) btnPage2.setStyle(currentPage == 2 ? active : inactive);
        if (btnPage3 != null) btnPage3.setStyle(currentPage == 3 ? active : inactive);
    }

    private void updateTabCounts() {
        long cAll       = allItems.size();
        long cActive    = allItems.stream().filter(i -> "active".equals(i.getStatus())).count();
        long cPending   = allItems.stream().filter(i -> "pending".equals(i.getStatus())).count();
        long cEnded     = allItems.stream().filter(i -> "ended".equals(i.getStatus())).count();
        long cCancelled = allItems.stream().filter(i -> "cancelled".equals(i.getStatus())).count();
        if (countAll       != null) countAll.setText(String.valueOf(cAll));
        if (countActive    != null) countActive.setText(String.valueOf(cActive));
        if (countPending   != null) countPending.setText(String.valueOf(cPending));
        if (countEnded     != null) countEnded.setText(String.valueOf(cEnded));
        if (countCancelled != null) countCancelled.setText(String.valueOf(cCancelled));
    }

    private void updateStats() {
        if (statTotal != null) statTotal.setText(String.valueOf(allItems.size()));
        long activeCount = allItems.stream().filter(i -> "active".equals(i.getStatus())).count();
        if (statActive != null) statActive.setText(String.valueOf(activeCount));
    }

    // ── Tab selection ─────────────────────────────────────────
    private void selectTab(String tab, HBox selectedHBox) {
        currentTab = tab;
        currentPage = 1;
        String activeStyle   = "-fx-padding: 12 16; -fx-border-color: transparent transparent #111827 transparent; -fx-border-width: 0 0 2 0; -fx-background-color: white; -fx-cursor: hand;";
        String inactiveStyle = "-fx-padding: 12 16; -fx-border-color: transparent; -fx-background-color: white; -fx-cursor: hand;";
        String activeLabelStyle   = "-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#111827;";
        String inactiveLabelStyle = "-fx-font-size:13px; -fx-text-fill:#6b7280;";

        for (HBox t : allTabs) {
            if (t == null) continue;
            t.setStyle(t == selectedHBox ? activeStyle : inactiveStyle);
            t.getChildren().stream()
                    .filter(n -> n instanceof Label && !((Label)n).getText().matches("\\d+"))
                    .forEach(n -> ((Label)n).setStyle(t == selectedHBox ? activeLabelStyle : inactiveLabelStyle));
        }
        refreshTable();
    }

    @FXML private void handleTabAll()       { selectTab("all",       tabAll); }
    @FXML private void handleTabActive()    { selectTab("active",    tabActive); }
    @FXML private void handleTabPending()   { selectTab("pending",   tabPending); }
    @FXML private void handleTabEnded()     { selectTab("ended",     tabEnded); }
    @FXML private void handleTabCancelled() { selectTab("cancelled", tabCancelled); }

    // ── Filter / search ───────────────────────────────────────
    @FXML
    private void handleSearch(KeyEvent event) {
        currentPage = 1;
        refreshTable();
    }

    @FXML
    private void handleApplyFilter() {
        currentPage = 1;
        String sort = sortCombo.getValue();
        if (sort != null) {
            switch (sort) {
                case "Mới nhất"              -> allItems.sort((a, b) -> b.getId().compareTo(a.getId()));
                case "Cũ nhất"               -> allItems.sort((a, b) -> a.getId().compareTo(b.getId()));
                case "Giá cao nhất"          -> allItems.sort((a, b) -> Long.compare(b.getCurrentPrice(), a.getCurrentPrice()));
                case "Giá thấp nhất"         -> allItems.sort((a, b) -> Long.compare(a.getCurrentPrice(), b.getCurrentPrice()));
                case "Lượt đặt giá nhiều nhất" -> allItems.sort((a, b) -> Integer.compare(b.getBids(), a.getBids()));
            }
        }
        refreshTable();
    }

    @FXML
    private void handleResetFilter() {
        if (searchField  != null) searchField.clear();
        if (sortCombo    != null) sortCombo.setValue(null);
        if (categoryCombo!= null) categoryCombo.setValue(null);
        currentPage = 1;
        refreshTable();
    }

    // ── Pagination ────────────────────────────────────────────
    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; refreshTable(); } }
    @FXML private void handleNextPage() { currentPage++; refreshTable(); }
    @FXML private void handlePage1()    { currentPage = 1; refreshTable(); }
    @FXML private void handlePage2()    { currentPage = 2; refreshTable(); }
    @FXML private void handlePage3()    { currentPage = 3; refreshTable(); }

    // ── Row actions ───────────────────────────────────────────
    @FXML
    private void handleCreateAuction() {
        System.out.println("→ Mở form tạo phiên đấu giá mới");
        // TODO: FXMLLoader → CreateAuctionView
    }

    private void handleViewAuction(AuctionItem item) {
        System.out.println("→ Xem chi tiết phiên: " + item.getId());
        // TODO: FXMLLoader → AuctionDetailView
    }

    private void handleEditAuction(AuctionItem item) {
        System.out.println("→ Chỉnh sửa phiên: " + item.getId());
        // TODO: FXMLLoader → EditAuctionView
    }

    @FXML
    private void handleCancelAuction(AuctionItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.TRANSPARENT);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn hủy phiên này?\nĐộng hộ Seiko 5 Sports?");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("confirm-dialog");

        // Set icon
        Label graphic = new Label("❓");
        graphic.setStyle("-fx-font-size: 32px; -fx-text-fill: #3b82f6;");
        alert.setGraphic(graphic);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Cancel auction logic
            System.out.println("Auction cancelled");
        }
    }

    private void handleRepostAuction(AuctionItem item) {
        System.out.println("→ Đăng lại phiên: " + item.getId());
        // TODO: clone item → pending
    }

    private void handleRecreateAuction(AuctionItem item) {
        System.out.println("→ Tạo lại từ phiên: " + item.getId());
        // TODO: prefill CreateAuctionView with item data
    }

    // ── Countdown timer ───────────────────────────────────────
    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            allItems.forEach(item -> {
                if ("active".equals(item.getStatus()) && item.getTimeLeftSeconds() > 0) {
                    item.setTimeLeftSeconds(item.getTimeLeftSeconds() - 1);
                    if (item.getTimeLeftSeconds() == 0) {
                        item.setStatus("ended");
                        updateTabCounts();
                    }
                }
            });
            Platform.runLater(() -> auctionTable.refresh());
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    // ── Helpers ───────────────────────────────────────────────
    private static String formatVnd(long amount) {
        return String.format("%,d đ", amount).replace(',', '.');
    }

    private static String formatTime(long seconds) {
        if (seconds <= 0) return "—";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h >= 24) return (h / 24) + "ng " + (h % 24) + "g";
        if (h > 0)   return h + "g " + m + "p";
        return m + "p " + s + "s";
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "active"    -> "● Đang diễn ra";
            case "pending"   -> "● Chờ bắt đầu";
            case "ended"     -> "● Đã kết thúc";
            case "cancelled" -> "● Đã hủy";
            default -> status;
        };
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case "active"    -> "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
            case "pending"   -> "-fx-background-color: #faeeda; -fx-text-fill: #854f0b;";
            case "ended"     -> "-fx-background-color: #f1efe8; -fx-text-fill: #5f5e5a;";
            case "cancelled" -> "-fx-background-color: #fcebeb; -fx-text-fill: #a32d2d;";
            default          -> "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;";
        };
    }

    // ══════════════════════════════════════════════════════════
    //  Inner model class
    // ══════════════════════════════════════════════════════════
    public static class AuctionItem {
        private final String id;
        private final String name;
        private final String icon;
        private final long startPrice;
        private long currentPrice;
        private int bids;
        private String status;
        private long timeLeftSeconds;
        private final javafx.beans.property.BooleanProperty selected =
                new javafx.beans.property.SimpleBooleanProperty(false);

        public AuctionItem(String id, String name, String icon,
                           long startPrice, long currentPrice,
                           int bids, String status, long timeLeftSeconds) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.startPrice = startPrice;
            this.currentPrice = currentPrice;
            this.bids = bids;
            this.status = status;
            this.timeLeftSeconds = timeLeftSeconds;
        }

        public String getId()                { return id; }
        public String getName()              { return name; }
        public String getIcon()              { return icon; }
        public long getStartPrice()          { return startPrice; }
        public long getCurrentPrice()        { return currentPrice; }
        public void setCurrentPrice(long v)  { this.currentPrice = v; }
        public int getBids()                 { return bids; }
        public void setBids(int v)           { this.bids = v; }
        public String getStatus()            { return status; }
        public void setStatus(String s)      { this.status = s; }
        public long getTimeLeftSeconds()     { return timeLeftSeconds; }
        public void setTimeLeftSeconds(long v){ this.timeLeftSeconds = v; }
        public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
    }

    @FXML
    private void handleExportData() {
        // TODO: Implement export functionality
        System.out.println("Export data clicked");
    }

    @FXML
    private void handleViewAuctionDetails(String auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionDetailsModal.fxml"));
            VBox modalContent = loader.load();

            AuctionDetailsController controller = loader.getController();
            controller.loadAuctionDetails(auctionId);

            // Tạo Dialog
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
}