// src/main/java/client/controller/SellerPanelController.java
package client.controller;

import client.network.ClientSocket;
import client.util.AlertUtil;
import server.model.Auction;
import util.ItemValidationUtil;
import util.LoggerUtil;
import util.DateTimeUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import java.io.IOException;
import java.util.*;


public class SellerPanelController {

    // ==================== SELLER AUCTIONS TABLE ====================
    @FXML private TableView<Auction> sellerAuctionsTable;
    @FXML private TableColumn<Auction, String> itemNameColumn;
    @FXML private TableColumn<Auction, String> categoryColumn;
    @FXML private TableColumn<Auction, String> startPriceColumn;
    @FXML private TableColumn<Auction, String> currentPriceColumn;
    @FXML private TableColumn<Auction, String> highestBidderColumn;
    @FXML private TableColumn<Auction, String> statusColumn;

    // ==================== BUTTONS ====================
    @FXML private Button createAuctionButton;
    @FXML private Button editAuctionButton;
    @FXML private Button deleteAuctionButton;
    @FXML private Button viewBidsButton;
    @FXML private Button refreshButton;

    // ==================== LABELS ====================
    @FXML private Label messageLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // ==================== PROPERTIES ====================
    private ClientSocket clientSocket;
    private server.model.User currentUser;
    private HomeScreenController parentController;
    private Timer refreshTimer;

    /**
     * Initialize controller
     */
    @FXML
    public void initialize() {
        initializeSellerAuctionsTable();

        createAuctionButton.setOnAction(e -> handleCreateAuction());
        editAuctionButton.setOnAction(e -> handleEditAuction());
        deleteAuctionButton.setOnAction(e -> handleDeleteAuction());
        viewBidsButton.setOnAction(e -> handleViewBids());
        refreshButton.setOnAction(e -> loadSellerAuctions());

        progressIndicator.setVisible(false);

        LoggerUtil.info("✓ SellerPanelController initialized");
    }

    /**
     * Khởi tạo seller auctions table
     */
    private void initializeSellerAuctionsTable() {
        itemNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getItem() != null ? cellData.getValue().getItem().getName() : "N/A"
                ));

        categoryColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getItem() != null ? cellData.getValue().getItem().getCategory().name() : "N/A"
                ));

        startPriceColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        "$" + String.format("%.2f",
                                cellData.getValue().getItem() != null ? cellData.getValue().getItem().getStartingPrice() : 0)
                ));

        currentPriceColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        "$" + String.format("%.2f", cellData.getValue().getCurrentPrice())
                ));

        highestBidderColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getHighestBidderName() != null ?
                                cellData.getValue().getHighestBidderName() : "Chưa có"
                ));

        statusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus().name()
                ));
    }

    /**
     * Set user data
     */
    public void setUserData(server.model.User user, ClientSocket socket, HomeScreenController parent) {
        this.currentUser = user;
        this.clientSocket = socket;
        this.parentController = parent;

        loadSellerAuctions();
        startAutoRefresh();

        LoggerUtil.info("✓ SellerPanel set for user: " + user.getUsername());
    }

    /**
     * Load auctions của seller
     */
    private void loadSellerAuctions() {
        progressIndicator.setVisible(true);
        messageLabel.setText("Đang tải...");

        new Thread(() -> {
            try {
                Message message = new Message(MessageType.GET_SELLER_AUCTIONS, null, currentUser.getUserId());
                clientSocket.sendMessage(message);

                Message response = clientSocket.receiveMessage();

                if (response != null && response.getStatus().equals("SUCCESS")) {
                    @SuppressWarnings("unchecked")
                    List<Auction> auctions = (List<Auction>) response.getData();

                    Platform.runLater(() -> {
                        sellerAuctionsTable.getItems().clear();
                        sellerAuctionsTable.getItems().addAll(auctions);

                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("✓ Tải " + auctions.size() + " phiên của bạn");
                        statusLabel.setText("Cập nhật: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));

                        LoggerUtil.info("✓ Loaded " + auctions.size() + " seller auctions");
                    });
                } else {
                    showError("Không thể tải danh sách phiên của bạn");
                }

            } catch (IOException | ClassNotFoundException e) {
                LoggerUtil.error("Load seller auctions error: " + e.getMessage());
                showError("Lỗi kết nối: " + e.getMessage());
            } finally {
                Platform.runLater(() -> progressIndicator.setVisible(false));
            }
        }).start();
    }

    /**
     * Xử lý tạo auction mới
     */
    @FXML
    private void handleCreateAuction() {
        showCreateAuctionDialog();
    }

    /**
     * Hiển thị dialog tạo auction
     */
    private void showCreateAuctionDialog() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("📝 Tạo phiên đấu giá mới");
        dialog.setHeaderText("Điền thông tin sản phẩm");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Category selection
        ComboBox<ItemCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ItemCategory.values());
        categoryCombo.setValue(ItemCategory.ELECTRONICS);
        categoryCombo.setPrefWidth(200);

        // Basic fields
        TextField nameField = new TextField();
        nameField.setPromptText("Tên sản phẩm");
        nameField.setPrefWidth(300);

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Mô tả sản phẩm");
        descriptionArea.setPrefWidth(300);
        descriptionArea.setPrefHeight(80);
        descriptionArea.setWrapText(true);

        TextField priceField = new TextField();
        priceField.setPromptText("Giá khởi điểm");
        priceField.setPrefWidth(200);

        TextField durationField = new TextField();
        durationField.setPromptText("Thời gian (phút)");
        durationField.setPrefWidth(200);

        // Dynamic fields container
        VBox dynamicFieldsVBox = new VBox(10);

        // Update dynamic fields when category changes
        categoryCombo.setOnAction(e -> {
            ItemCategory category = categoryCombo.getValue();
            dynamicFieldsVBox.getChildren().clear();

            switch (category) {
                case ELECTRONICS:
                    addFieldToVBox(dynamicFieldsVBox, "Hãng sản xuất:", "brand", 300);
                    addFieldToVBox(dynamicFieldsVBox, "Thời gian bảo hành:", "warranty", 300);
                    break;
                case ART:
                    addFieldToVBox(dynamicFieldsVBox, "Tên họa sĩ:", "creator", 300);
                    addFieldToVBox(dynamicFieldsVBox, "Chất liệu:", "material", 300);
                    break;
                case VEHICLE:
                    addFieldToVBox(dynamicFieldsVBox, "Đời xe:", "model", 300);
                    addFieldToVBox(dynamicFieldsVBox, "Số km đã đi:", "odometer", 300);
                    break;
                case FASHION:
                    addFieldToVBox(dynamicFieldsVBox, "Hãng:", "brand", 300);
                    addFieldToVBox(dynamicFieldsVBox, "Chất liệu:", "material", 300);
                    break;
                case JEWELRY:
                    addFieldToVBox(dynamicFieldsVBox, "Chất liệu:", "material", 300);
                    addFieldToVBox(dynamicFieldsVBox, "Trọng lượng (g):", "weight", 300);
                    break;
                default:
                    break;
            }
        });

        // Trigger initial dynamic fields
        categoryCombo.getOnAction().handle(null);

        // Add to grid
        grid.add(new Label("Loại sản phẩm:"), 0, 0);
        grid.add(categoryCombo, 1, 0);

        grid.add(new Label("Tên sản phẩm:"), 0, 1);
        grid.add(nameField, 1, 1);

        grid.add(new Label("Mô tả:"), 0, 2);
        grid.add(descriptionArea, 1, 2);

        grid.add(new Label("Giá khởi điểm ($):"), 0, 3);
        grid.add(priceField, 1, 3);

        grid.add(new Label("Thời gian (phút):"), 0, 4);
        grid.add(durationField, 1, 4);

        grid.add(new Label("Thông tin riêng:"), 0, 5);
        grid.add(dynamicFieldsVBox, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

// Convert dữ liệu khi bấm OK
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    String description = descriptionArea.getText().trim();
                    double price = Double.parseDouble(priceField.getText().trim());
                    int duration = Integer.parseInt(durationField.getText().trim());
                    ItemCategory category = categoryCombo.getValue();

                    return createAuctionDataMap(
                            category, name, description, price, duration, dynamicFieldsVBox
                    );
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

// Handle OK
        Optional<Map<String, Object>> result = dialog.showAndWait();
        if (result.isPresent()) {
            sendCreateAuctionToServer(result.get());
        } else {
            AlertUtil.showError("Lỗi", "❌ Vui lòng nhập dữ liệu hợp lệ!");
        }

    }

    /**
     * Thêm field vào VBox
     */
    private void addFieldToVBox(VBox vbox, String label, String fieldId, int width) {
        HBox hbox = new HBox(10);
        Label fieldLabel = new Label(label);
        fieldLabel.setPrefWidth(150);

        TextField textField = new TextField();
        textField.setId(fieldId);
        textField.setPrefWidth(width);

        hbox.getChildren().addAll(fieldLabel, textField);
        vbox.getChildren().add(hbox);
    }

    /**
     * Tạo map dữ liệu auction từ form
     */
    private Map<String, Object> createAuctionDataMap(
            ItemCategory category, String name, String description,
            double price, int duration, VBox dynamicFieldsVBox) {

        Map<String, Object> auctionData = new HashMap<>();
        auctionData.put("itemType", category.name());
        auctionData.put("name", name);
        auctionData.put("description", description);
        auctionData.put("price", price);
        auctionData.put("duration", duration);

        // Lấy dynamic fields
        for (javafx.scene.Node node : dynamicFieldsVBox.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (javafx.scene.Node child : hbox.getChildren()) {
                    if (child instanceof TextField) {
                        TextField tf = (TextField) child;
                        auctionData.put(tf.getId(), tf.getText());
                    }
                }
            }
        }

        return auctionData;
    }

    /**
     * Gửi tạo auction tới server
     */
    private void sendCreateAuctionToServer(Map<String, Object> auctionData) {
        new Thread(() -> {
            try {
                Message message = new Message(MessageType.CREATE_SELLER_ITEM, auctionData, currentUser.getUserId());
                clientSocket.sendMessage(message);

                Message response = clientSocket.receiveMessage();

                if (response != null && response.getStatus().equals("SUCCESS")) {
                    Platform.runLater(() -> {
                        AlertUtil.showSuccess("Thành công",
                                "✓ Tạo phiên đấu giá thành công!");

                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("✓ Phiên mới được tạo!");

                        loadSellerAuctions();
                    });
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                    AlertUtil.showError("Lỗi", "❌ " + errorMsg);
                }

            } catch (IOException | ClassNotFoundException e) {
                LoggerUtil.error("Create auction error: " + e.getMessage());
                AlertUtil.showError("Lỗi", "Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Xử lý edit auction
     */
    @FXML
    private void handleEditAuction() {
        Auction selectedAuction = sellerAuctionsTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn phiên đấu giá!");
            return;
        }

        AlertUtil.showWarning("Thông báo", "Tính năng sửa chỉnh sẽ được cập nhật!");
    }

    /**
     * Xử lý xóa auction
     */
    @FXML
    private void handleDeleteAuction() {
        Auction selectedAuction = sellerAuctionsTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn phiên đấu giá!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.DRAFT &&
                selectedAuction.getStatus() != AuctionStatus.OPEN) {
            AlertUtil.showError("Lỗi", "Chỉ có thể xóa phiên ở trạng thái DRAFT hoặc OPEN!");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xác nhận");
        confirmation.setHeaderText("Xóa phiên đấu giá");
        confirmation.setContentText("Bạn có chắc chắn muốn xóa phiên này?\n" +
                (selectedAuction.getItem() != null ? selectedAuction.getItem().getName() : "N/A"));

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            deleteAuctionFromServer(selectedAuction.getAuctionId());
        }
    }

    /**
     * Xóa auction từ server
     */
    private void deleteAuctionFromServer(String auctionId) {
        new Thread(() -> {
            try {
                Message message = new Message(MessageType.DELETE_SELLER_ITEM, auctionId, currentUser.getUserId());
                clientSocket.sendMessage(message);

                Message response = clientSocket.receiveMessage();

                if (response != null && response.getStatus().equals("SUCCESS")) {
                    Platform.runLater(() -> {
                        AlertUtil.showSuccess("Thành công", "✓ Xóa phiên đấu giá thành công!");
                        loadSellerAuctions();
                    });
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                    AlertUtil.showError("Lỗi", "❌ " + errorMsg);
                }

            } catch (IOException | ClassNotFoundException e) {
                LoggerUtil.error("Delete auction error: " + e.getMessage());
                AlertUtil.showError("Lỗi", "Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Xử lý xem bids
     */
    @FXML
    private void handleViewBids() {
        Auction selectedAuction = sellerAuctionsTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn phiên đấu giá!");
            return;
        }

        StringBuilder bidInfo = new StringBuilder();
        bidInfo.append("📋 BID HISTORY:\n");
        bidInfo.append("Phiên: ").append(selectedAuction.getItem() != null ? selectedAuction.getItem().getName() : "N/A").append("\n\n");

        bidInfo.append("👤 Người dẫn đầu: ").append(
                selectedAuction.getHighestBidderName() != null ? selectedAuction.getHighestBidderName() : "Chưa có").append("\n");
        bidInfo.append("💰 Giá hiện tại: $").append(String.format("%.2f", selectedAuction.getCurrentPrice())).append("\n");
        bidInfo.append("📊 Tổng số bid: ").append(selectedAuction.getBidIds().size()).append("\n");

        Alert bidAlert = new Alert(Alert.AlertType.INFORMATION);
        bidAlert.setTitle("Lịch sử bid");
        bidAlert.setHeaderText("Chi tiết bid");
        bidAlert.setContentText(bidInfo.toString());
        bidAlert.showAndWait();
    }

    /**
     * Auto-refresh
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadSellerAuctions());
            }
        }, 10000, 10000); // Refresh every 10 seconds
    }

    /**
     * Hiển thị error
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("❌ " + message);
        });
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
