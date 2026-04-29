package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class AuctionDetailsController {

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
}