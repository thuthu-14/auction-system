package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class SellerAuctionDetailsController {

    @FXML private Label productName, productId, productDesc;
    @FXML private ImageView productImage, topBidderAvatar;
    @FXML private Label startPrice, currentPrice;
    @FXML private Label startTime, endTime, timeLeft;
    @FXML private Label status, bidCount;
    @FXML private Label topBidderName, topBidderId, topBidderRating, topBidderPhone;
    @FXML private Label topBidAmount;
    @FXML private TableView bidsTable;

    private String auctionId;

    @FXML
    public void initialize() {
        // Load data when modal opens
    }

    public void loadAuctionDetails(String auctionId) {
        this.auctionId = auctionId;

        // Gọi API để lấy dữ liệu
        // AuctionService.getAuctionDetails(auctionId, response -> {
        //     productName.setText(response.getProductName());
        //     currentPrice.setText(formatCurrency(response.getCurrentPrice()));
        //     bidCount.setText(response.getBidCount() + " lượt");
        //
        //     // Load bid history
        //     loadBidHistory(auctionId);
        //
        //     // Load top bidder
        //     loadTopBidder(auctionId);
        // });
    }

    private void loadBidHistory(String auctionId) {
        // Gọi API để lấy lịch sử đặt giá
        // AuctionService.getBidHistory(auctionId, bids -> {
        //     bidsTable.setItems(FXCollections.observableArrayList(bids));
        // });
    }

    private void loadTopBidder(String auctionId) {
        // Gọi API để lấy người đặt giá cao nhất
        // AuctionService.getTopBidder(auctionId, bidder -> {
        //     topBidderName.setText(bidder.getName());
        //     topBidAmount.setText(formatCurrency(bidder.getAmount()));
        // });
    }

    @FXML
    private void handleEdit() {
        System.out.println("Edit auction: " + auctionId);
    }

    @FXML
    private void handlePause() {
        System.out.println("Pause auction: " + auctionId);
    }

    @FXML
    private void handleCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setContentText("Bạn có chắc muốn hủy phiên này?");
        alert.getDialogPane().getStyleClass().add("my-custom-alert");

        alert.showAndWait();
    }

    private String formatCurrency(double amount) {
        return String.format("%.0f đ", amount);
    }

    public void loadSellerAuctionDetails(String auctionId) {
        this.auctionId = auctionId;
        System.out.println("Đang tải dữ liệu chi tiết cho phiên đấu giá: " + auctionId);

        // =======================================================
        // 1. DỮ LIỆU GIẢ (MOCK DATA) ĐỂ TEST GIAO DIỆN HIỂN THỊ
        // =======================================================
        // Thông tin sản phẩm
        productId.setText("Mã: " + (auctionId != null ? auctionId : "AU-2024-001"));
        productName.setText("Đồng hồ Seiko 5 Sports");
        productDesc.setText("Đồng hồ thể thao chính hãng");

        // Thông tin giá
        startPrice.setText("1.200.000 đ");
        currentPrice.setText("2.850.000 đ");

        // Thông tin thời gian
        startTime.setText("10/04/2026 14:30");
        endTime.setText("12/04/2026 16:45");
        timeLeft.setText("1 ngày 5 giờ");

        // Trạng thái và lượt đặt
        status.setText("Đang diễn ra");
        bidCount.setText("18 lượt");

        // Chạy tiếp các hàm load lịch sử và người trúng thầu
        loadBidHistory(auctionId);
        loadTopBidder(auctionId);

        // =======================================================
        // 2. CODE GỌI API THẬT (Mở comment khi có Backend)
        // =======================================================
        /*
        AuctionService.getAuctionDetails(auctionId, response -> {
            // Lưu ý: Khi gọi API (chạy ở luồng mạng), phải dùng Platform.runLater
            // để cập nhật giao diện (chạy ở luồng UI) tránh bị lỗi Not on FX application thread.
            Platform.runLater(() -> {
                productName.setText(response.getProductName());
                productId.setText("Mã: " + response.getProductId());
                productDesc.setText(response.getDescription());

                startPrice.setText(formatCurrency(response.getStartPrice()));
                currentPrice.setText(formatCurrency(response.getCurrentPrice()));

                startTime.setText(response.getStartTime());
                endTime.setText(response.getEndTime());
                timeLeft.setText(response.getTimeLeft());

                status.setText(response.getStatus());
                bidCount.setText(response.getBidCount() + " lượt");
            });
        });
        */
    }
}