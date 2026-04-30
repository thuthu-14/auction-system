package client.controller;



import client.network.ClientSocket;

import client.util.AlertUtil;

import common.AuctionStatus;
import common.Message;
import common.MessageType;
import server.model.Auction;

import server.model.Bid;

import util.LoggerUtil;

import util.DateTimeUtil;

import javafx.application.Platform;

import javafx.fxml.FXML;

import javafx.scene.control.*;

import javafx.geometry.Insets;

import javafx.scene.layout.VBox;

import java.io.IOException;

import java.util.*;



/**

 * BidderPanelController - Tab Người mua (Bidder)

 * MVC Pattern: Controller

 *

 * Chức năng:

 * 1. Xem danh sách phiên đấu giá đang hoạt động

 * 2. Tham gia đặt giá

 * 3. Xem lịch sử bid của mình

 * 4. Real-time update giá

 */

public class BidderPanelController {



// ==================== AUCTIONS TABLE ====================

    @FXML private TableView<Auction> auctionsTable;

    @FXML private TableColumn<Auction, String> itemNameColumn;

    @FXML private TableColumn<Auction, String> categoryColumn;

    @FXML private TableColumn<Auction, String> currentPriceColumn;

    @FXML private TableColumn<Auction, String> timeRemainingColumn;

    @FXML private TableColumn<Auction, String> highestBidderColumn;

    @FXML private TableColumn<Auction, String> statusColumn;



// ==================== BID HISTORY TABLE ====================

    @FXML private TableView<Bid> bidHistoryTable;

    @FXML private TableColumn<Bid, String> bidItemColumn;

    @FXML private TableColumn<Bid, String> bidAmountColumn;

    @FXML private TableColumn<Bid, String> bidTimeColumn;

    @FXML private TableColumn<Bid, String> bidStatusColumn;



// ==================== BUTTONS ====================

    @FXML private Button placeBidButton;

    @FXML private Button viewDetailsButton;

    @FXML private Button refreshButton;

    @FXML private Button myBidsButton;



// ==================== LABELS ====================

    @FXML private Label messageLabel;

    @FXML private Label statusLabel;

    @FXML private ProgressIndicator progressIndicator;



// ==================== PROPERTIES ====================

    private ClientSocket clientSocket;

    private server.model.User currentUser;

    private HomeScreenController parentController;

    private Timer refreshTimer;

    private boolean showingBidHistory;



    /**

     * Initialize controller

     */

    @FXML

    public void initialize() {

        initializeAuctionsTable();

        initializeBidHistoryTable();



        placeBidButton.setOnAction(e -> handlePlaceBid());

        viewDetailsButton.setOnAction(e -> handleViewDetails());

        refreshButton.setOnAction(e -> loadAuctions());

        myBidsButton.setOnAction(e -> toggleBidHistory());



        progressIndicator.setVisible(false);

        showingBidHistory = false;



        LoggerUtil.info("✓ BidderPanelController initialized");

    }



    /**

     * Khởi tạo auctions table

     */

    private void initializeAuctionsTable() {

        itemNameColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getItem() != null ? cellData.getValue().getItem().getName() : "N/A"

                ));



        categoryColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getItem() != null ? cellData.getValue().getItem().getCategory().name() : "N/A"

                ));



        currentPriceColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        "$" + String.format("%.2f", cellData.getValue().getCurrentPrice())

                ));



        timeRemainingColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        DateTimeUtil.formatTimeRemaining(cellData.getValue().getEndTime())

                ));



        highestBidderColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getHighestBidderName() != null ?

                                cellData.getValue().getHighestBidderName() : "Chưa có ai bid"

                ));



        statusColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getStatus().name()

                ));

    }



    /**

     * Khởi tạo bid history table

     */

    private void initializeBidHistoryTable() {

        bidItemColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getAuctionId()

                ));



        bidAmountColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        "$" + String.format("%.2f", cellData.getValue().getAmount())

                ));



        bidTimeColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(cellData.getValue().getBidTime()))

                ));



        bidStatusColumn.setCellValueFactory(cellData ->

                new javafx.beans.property.SimpleStringProperty(

                        cellData.getValue().getStatus()

                ));

    }



    /**

     * Set user data

     */

    public void setUserData(server.model.User user, ClientSocket socket, HomeScreenController parent) {

        this.currentUser = user;

        this.clientSocket = socket;

        this.parentController = parent;



        loadAuctions();

        startAutoRefresh();



        LoggerUtil.info("✓ BidderPanel set for user: " + user.getUsername());

    }



    /**

     * Load tất cả auctions đang hoạt động

     */

    private void loadAuctions() {

        progressIndicator.setVisible(true);

        messageLabel.setText("Đang tải...");



        new Thread(() -> {

            try {

                Message message = new Message(MessageType.GET_AUCTIONS, null, currentUser.getUserId());

                clientSocket.sendMessage(message);



                Message response = clientSocket.receiveMessage();



                if (response != null && response.getStatus().equals("SUCCESS")) {

                    @SuppressWarnings("unchecked")

                    List<Auction> auctions = (List<Auction>) response.getData();



                    Platform.runLater(() -> {

                        auctionsTable.getItems().clear();

                        auctionsTable.getItems().addAll(auctions);



                        messageLabel.setStyle("-fx-text-fill: green;");

                        messageLabel.setText("✓ Tải " + auctions.size() + " phiên đấu giá");

                        statusLabel.setText("Cập nhật: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));



                        LoggerUtil.info("✓ Loaded " + auctions.size() + " auctions");

                    });

                } else {

                    showError("Không thể tải danh sách đấu giá");

                }



            } catch (IOException | ClassNotFoundException e) {

                LoggerUtil.error("Load auctions error: " + e.getMessage());

                showError("Lỗi kết nối: " + e.getMessage());

            } finally {

                Platform.runLater(() -> progressIndicator.setVisible(false));

            }

        }).start();

    }



    /**

     * Xử lý đặt giá

     */

    @FXML

    private void handlePlaceBid() {

        Auction selectedAuction = auctionsTable.getSelectionModel().getSelectedItem();



        if (selectedAuction == null) {

            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn phiên đấu giá!");

            return;

        }



// Kiểm tra auction còn mở không

        if (selectedAuction.getStatus() != AuctionStatus.OPEN &&

                selectedAuction.getStatus() != AuctionStatus.RUNNING) {

            AlertUtil.showError("Lỗi", "Phiên đấu giá này đã kết thúc!");

            return;

        }



// Kiểm tra không phải seller

        if (currentUser.getUserId().equals(selectedAuction.getSellerId())) {

            AlertUtil.showError("Lỗi", "Bạn không thể bid trên phiên đấu giá của chính mình!");

            return;

        }



// Show bid dialog

        showBidDialog(selectedAuction);

    }



    /**

     * Hiển thị bid dialog

     */
    /**
     * Hiển thị bid dialog
     * ← SỬA: Hiển thị khoảng bid tối thiểu
     */
    private void showBidDialog(Auction auction) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("💰 Đặt giá");
        dialog.setHeaderText("Phiên đấu giá: " + (auction.getItem() != null ? auction.getItem().getName() : "N/A"));

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        // Info labels
        Label currentPriceLabel = new Label("💵 Giá hiện tại: $" + String.format("%.2f", auction.getCurrentPrice()));
        currentPriceLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label highestBidderLabel = new Label("👤 Người dẫn đầu: " +
                (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Chưa có"));
        highestBidderLabel.setStyle("-fx-font-size: 12;");

        Label timeRemainingLabel = new Label("⏱ Thời gian còn lại: " +
                DateTimeUtil.formatTimeRemaining(auction.getEndTime()));
        timeRemainingLabel.setStyle("-fx-font-size: 12;");

        // ← THÊM: Hiển thị khoảng bid tối thiểu
        common.ItemCategory category = auction.getItem().getCategory();
        double increment = category.getMinimumBidIncrement();
        double minimumBid = auction.getCurrentPrice() + increment;

        Label incrementLabel = new Label("📈 Khoảng bid tối thiểu: $" + String.format("%.2f", increment));
        incrementLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #2196F3;");

        // Bid amount input
        Label bidLabel = new Label("Nhập giá đặt:");
        bidLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

        TextField bidAmountField = new TextField();
        bidAmountField.setPromptText("VD: " + String.format("%.2f", minimumBid));
        bidAmountField.setStyle("-fx-font-size: 14; -fx-padding: 8;");

        // Min bid label
        Label minBidLabel = new Label("⚠ Giá tối thiểu: $" + String.format("%.2f", minimumBid) +
                " (Hiện tại: $" + String.format("%.2f", auction.getCurrentPrice()) +
                " + Increment: $" + String.format("%.2f", increment) + ")");
        minBidLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #FF6B6B;");

        content.getChildren().addAll(
                currentPriceLabel,
                highestBidderLabel,
                timeRemainingLabel,
                incrementLabel,
                new Separator(),
                bidLabel,
                bidAmountField,
                minBidLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    return Double.parseDouble(bidAmountField.getText().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });


        // Handle OK button
        Optional<Double> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double bidAmount = result.get();

                // ← SỬA: Validate dựa trên increment
                if (bidAmount < minimumBid) {
                    AlertUtil.showError("Lỗi", "❌ Giá phải >= $" + String.format("%.2f", minimumBid) +
                            " (Hiện tại: $" + String.format("%.2f", auction.getCurrentPrice()) +
                            " + Increment: $" + String.format("%.2f", increment) + ")");
                    return;
                }

                if (bidAmount > currentUser.getWallet()) {
                    AlertUtil.showError("Lỗi", "❌ Bạn không có đủ tiền! Tài khoản hiện có: $" +
                            String.format("%.2f", currentUser.getWallet()));
                    return;
                }

                // Gửi bid tới server
                sendBidToServer(auction, bidAmount);

            } catch (NumberFormatException e) {
                AlertUtil.showError("Lỗi", "❌ Vui lòng nhập giá hợp lệ!");
            }
        }
    }



    private void sendBidToServer(Auction auction, double bidAmount) {

        new Thread(() -> {

            try {

                Map<String, Object> bidData = new HashMap<>();

                bidData.put("auctionId", auction.getAuctionId());

                bidData.put("amount", bidAmount);



                Message message = new Message(MessageType.PLACE_BID, bidData, currentUser.getUserId());

                clientSocket.sendMessage(message);



                Message response = clientSocket.receiveMessage();



                if (response != null && response.getStatus().equals("SUCCESS")) {

                    Platform.runLater(() -> {

                        AlertUtil.showSuccess("Thành công",

                                "✓ Bạn đã đặt giá $" + String.format("%.2f", bidAmount) +

                                        " cho: " + (auction.getItem() != null ? auction.getItem().getName() : "N/A"));



                        messageLabel.setStyle("-fx-text-fill: green;");

                        messageLabel.setText("✓ Đặt giá thành công!");



                        loadAuctions();

                        loadMyBids();

                    });

                } else {

                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";

                    AlertUtil.showError("Lỗi", "❌ " + errorMsg);

                }



            } catch (IOException | ClassNotFoundException e) {

                LoggerUtil.error("Bid error: " + e.getMessage());

                AlertUtil.showError("Lỗi", "Lỗi kết nối: " + e.getMessage());

            }

        }).start();

    }



    /**

     * Xử lý xem chi tiết auction

     */

    @FXML

    private void handleViewDetails() {

        Auction selectedAuction = auctionsTable.getSelectionModel().getSelectedItem();



        if (selectedAuction == null) {

            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn phiên đấu giá!");

            return;

        }



        StringBuilder details = new StringBuilder();

        details.append("📦 TÊN SẢN PHẨM:\n");

        details.append(selectedAuction.getItem() != null ? selectedAuction.getItem().getName() : "N/A").append("\n\n");



        details.append("📝 MÔ TẢ:\n");

        details.append(selectedAuction.getItem() != null ? selectedAuction.getItem().getDescription() : "N/A").append("\n\n");



        details.append("🏷 CHI TIẾT:\n");

        details.append(selectedAuction.getItem() != null ? selectedAuction.getItem().getDetailedInfo() : "N/A").append("\n\n");



        details.append("💰 THÔNG TIN GIÁ:\n");

        details.append("Giá khởi điểm: $").append(String.format("%.2f",

                selectedAuction.getItem() != null ? selectedAuction.getItem().getStartingPrice() : 0)).append("\n");

        details.append("Giá hiện tại: $").append(String.format("%.2f", selectedAuction.getCurrentPrice())).append("\n\n");



        details.append("👤 NGƯỜI BÁN:\n");

        details.append(selectedAuction.getSellerName()).append("\n\n");



        details.append("👥 NGƯỜI DẪN ĐẦU:\n");

        details.append(selectedAuction.getHighestBidderName() != null ?

                selectedAuction.getHighestBidderName() : "Chưa có").append("\n\n");



        details.append("⏱ THỜI GIAN:\n");

        details.append("Thời gian còn lại: ").append(DateTimeUtil.formatTimeRemaining(selectedAuction.getEndTime())).append("\n");



        details.append("📊 TRẠNG THÁI:\n");

        details.append(selectedAuction.getStatus().name()).append("\n");



        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);

        detailsAlert.setTitle("Chi tiết phiên đấu giá");

        detailsAlert.setHeaderText(selectedAuction.getItem() != null ? selectedAuction.getItem().getName() : "Phiên đấu giá");

        detailsAlert.setContentText(details.toString());

        detailsAlert.getDialogPane().setPrefWidth(500);

        detailsAlert.showAndWait();

    }



    /**

     * Toggle giữa auctions và bid history

     */

    @FXML

    private void toggleBidHistory() {

        if (!showingBidHistory) {

            loadMyBids();

        } else {

            loadAuctions();

        }

        showingBidHistory = !showingBidHistory;

    }



    /**

     * Load lịch sử bid của user

     */

    private void loadMyBids() {

        progressIndicator.setVisible(true);

        messageLabel.setText("Đang tải lịch sử bid...");



        new Thread(() -> {

            try {

                Message message = new Message(MessageType.GET_BID_HISTORY, currentUser.getUserId(), currentUser.getUserId());

                clientSocket.sendMessage(message);



                Message response = clientSocket.receiveMessage();



                if (response != null && response.getStatus().equals("SUCCESS")) {

                    @SuppressWarnings("unchecked")

                    List<Bid> bids = (List<Bid>) response.getData();



                    Platform.runLater(() -> {

                        auctionsTable.setVisible(false);

                        bidHistoryTable.setVisible(true);



                        bidHistoryTable.getItems().clear();

                        bidHistoryTable.getItems().addAll(bids);



                        messageLabel.setStyle("-fx-text-fill: green;");

                        messageLabel.setText("✓ Tải " + bids.size() + " lần bid của bạn");



                        myBidsButton.setText("← Quay lại Auctions");



                        LoggerUtil.info("✓ Loaded " + bids.size() + " bids");

                    });

                } else {

                    showError("Không thể tải lịch sử bid");

                }



            } catch (IOException | ClassNotFoundException e) {

                LoggerUtil.error("Load bids error: " + e.getMessage());

                showError("Lỗi kết nối: " + e.getMessage());

            } finally {

                Platform.runLater(() -> progressIndicator.setVisible(false));

            }

        }).start();

    }



    /**

     * Auto-refresh auctions

     */

    private void startAutoRefresh() {

        refreshTimer = new Timer();

        refreshTimer.scheduleAtFixedRate(new TimerTask() {

            @Override

            public void run() {

                if (!showingBidHistory) {

                    Platform.runLater(() -> loadAuctions());

                }

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