package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.User;
import util.LoggerUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SellerAuctionDetailsController {

    @FXML private Label productName, productId, productDesc;
    @FXML private Label startPrice, currentPrice;
    @FXML private Label startTime, endTime, timeLeft;
    @FXML private Label status, bidCount;
    @FXML private Label topBidderName, topBidAmount;
    @FXML private ImageView productImage;

    private String auctionId;
    private final SellerClientService sellerClientService = new SellerClientService();

    /**
     * Tải chi tiết phiên đấu giá từ Server
     */
    public void loadSellerAuctionDetails(String auctionId) {
        this.auctionId = auctionId;
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        User user = NavigationManager.getInstance().getCurrentUser();

        if (socket == null || user == null) {
            LoggerUtil.error("Socket hoặc User bị null trong loadSellerAuctionDetails");
            return;
        }

        new Thread(() -> {
            try {
                Auction auction = sellerClientService.fetchAuctionDetail(socket, user, auctionId);
                Platform.runLater(() -> updateUI(auction));
            } catch (Exception e) {
                LoggerUtil.error("Lỗi kết nối khi lấy chi tiết phiên: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Cập nhật thông tin lên giao diện
     */
    private void updateUI(Auction auction) {
        if (auction == null) return;

        // Thông tin sản phẩm cơ bản (Kiểm tra null cho từng Label để tránh NullPointerException)
        if (productName != null) productName.setText(auction.getItem() != null ? auction.getItem().getName() : "N/A");
        if (productId != null) productId.setText("Mã phiên: " + auction.getAuctionId());
        if (productDesc != null) productDesc.setText(auction.getItem() != null ? auction.getItem().getDescription() : "");

        // Giá cả
        if (startPrice != null) startPrice.setText(auction.getItem() != null ? formatVnd(auction.getItem().getStartingPrice()) : "0 d");
        if (currentPrice != null) currentPrice.setText(formatVnd(auction.getCurrentPrice()));

        // Thời gian
        if (startTime != null) startTime.setText(formatDate(auction.getStartTime()));
        if (endTime != null) endTime.setText(formatDate(auction.getEndTime()));

        if (timeLeft != null) {
            long remain = auction.getTimeRemainingSeconds();
            if (remain > 0) {
                timeLeft.setText(formatRemain(remain));
                timeLeft.setStyle("-fx-text-fill: #185fa5;"); // Màu xanh nếu còn thời gian
            } else {
                timeLeft.setText("Đã kết thúc");
                timeLeft.setStyle("-fx-text-fill: #dc2626;"); // Màu đỏ nếu đã hết
            }
        }

        // Trạng thái
        if (status != null) {
            String statusStr = auction.getStatus() != null ? auction.getStatus().toString() : "UNKNOWN";
            status.setText(statusStr);
            // Thêm màu sắc cho trạng thái sinh động hơn
            if (statusStr.equals("ACTIVE")) status.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 10; -fx-background-radius: 15;");
            else status.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 10; -fx-background-radius: 15;");
        }

        // Lượt đặt và người dẫn đầu
        if (bidCount != null) bidCount.setText((auction.getBidIds() != null ? auction.getBidIds().size() : 0) + " lượt");
        if (topBidderName != null) topBidderName.setText(auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Chưa có");
        if (topBidAmount != null) topBidAmount.setText(formatVnd(auction.getCurrentPrice()));
    }

    @FXML private void handleEdit() { LoggerUtil.info("Chỉnh sửa: " + auctionId); }
    @FXML private void handlePause() { LoggerUtil.info("Tạm dừng: " + auctionId); }
    @FXML private void handleCancel() { LoggerUtil.info("Hủy phiên: " + auctionId); }

    private String formatVnd(double a) { return String.format("%,.0f d", a).replace(',', '.'); }
    private String formatDate(long m) { return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(m)); }
    private String formatRemain(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) return h + " giá " + m + " phút";
        return m + " phút";
    }
}
