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
     * TÃ¡ÂºÂ£i chi tiÃ¡ÂºÂ¿t phiÃƒÂªn Ã„â€˜Ã¡ÂºÂ¥u giÃƒÂ¡ tÃ¡Â»Â« Server
     */
    public void loadSellerAuctionDetails(String auctionId) {
        this.auctionId = auctionId;
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        User user = NavigationManager.getInstance().getCurrentUser();

        if (socket == null || user == null) {
            LoggerUtil.error("Socket hoÃ¡ÂºÂ·c User bÃ¡Â»â€¹ null trong loadSellerAuctionDetails");
            return;
        }

        new Thread(() -> {
            try {
                Auction auction = sellerClientService.fetchAuctionDetail(socket, user, auctionId);
                Platform.runLater(() -> updateUI(auction));
            } catch (Exception e) {
                LoggerUtil.error("LÃ¡Â»â€”i kÃ¡ÂºÂ¿t nÃ¡Â»â€˜i khi lÃ¡ÂºÂ¥y chi tiÃ¡ÂºÂ¿t phiÃƒÂªn: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * CÃ¡ÂºÂ­p nhÃ¡ÂºÂ­t thÃƒÂ´ng tin lÃƒÂªn giao diÃ¡Â»â€¡n
     */
    private void updateUI(Auction auction) {
        if (auction == null) return;

        // ThÃƒÂ´ng tin sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m cÃ†Â¡ bÃ¡ÂºÂ£n (KiÃ¡Â»Æ’m tra null cho tÃ¡Â»Â«ng Label Ã„â€˜Ã¡Â»Æ’ trÃƒÂ¡nh NullPointerException)
        if (productName != null) productName.setText(auction.getItem() != null ? auction.getItem().getName() : "N/A");
        if (productId != null) productId.setText("MÃƒÂ£ phiÃƒÂªn: " + auction.getAuctionId());
        if (productDesc != null) productDesc.setText(auction.getItem() != null ? auction.getItem().getDescription() : "");

        // GiÃƒÂ¡ cÃ¡ÂºÂ£
        if (startPrice != null) startPrice.setText(auction.getItem() != null ? formatVnd(auction.getItem().getStartingPrice()) : "0 Ã„â€˜");
        if (currentPrice != null) currentPrice.setText(formatVnd(auction.getCurrentPrice()));

        // ThÃ¡Â»Âi gian
        if (startTime != null) startTime.setText(formatDate(auction.getStartTime()));
        if (endTime != null) endTime.setText(formatDate(auction.getEndTime()));

        if (timeLeft != null) {
            long remain = auction.getTimeRemainingSeconds();
            if (remain > 0) {
                timeLeft.setText(formatRemain(remain));
                timeLeft.setStyle("-fx-text-fill: #185fa5;"); // MÃƒÂ u xanh nÃ¡ÂºÂ¿u cÃƒÂ²n thÃ¡Â»Âi gian
            } else {
                timeLeft.setText("Ã„ÂÃƒÂ£ kÃ¡ÂºÂ¿t thÃƒÂºc");
                timeLeft.setStyle("-fx-text-fill: #dc2626;"); // MÃƒÂ u Ã„â€˜Ã¡Â»Â nÃ¡ÂºÂ¿u Ã„â€˜ÃƒÂ£ hÃ¡ÂºÂ¿t
            }
        }

        // TrÃ¡ÂºÂ¡ng thÃƒÂ¡i
        if (status != null) {
            String statusStr = auction.getStatus() != null ? auction.getStatus().toString() : "UNKNOWN";
            status.setText(statusStr);
            // ThÃƒÂªm mÃƒÂ u sÃ¡ÂºÂ¯c cho trÃ¡ÂºÂ¡ng thÃƒÂ¡i sinh Ã„â€˜Ã¡Â»â„¢ng hÃ†Â¡n
            if (statusStr.equals("ACTIVE")) status.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 10; -fx-background-radius: 15;");
            else status.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 10; -fx-background-radius: 15;");
        }

        // LÃ†Â°Ã¡Â»Â£t Ã„â€˜Ã¡ÂºÂ·t vÃƒÂ  ngÃ†Â°Ã¡Â»Âi dÃ¡ÂºÂ«n Ã„â€˜Ã¡ÂºÂ§u
        if (bidCount != null) bidCount.setText((auction.getBidIds() != null ? auction.getBidIds().size() : 0) + " lÃ†Â°Ã¡Â»Â£t");
        if (topBidderName != null) topBidderName.setText(auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "ChÃ†Â°a cÃƒÂ³");
        if (topBidAmount != null) topBidAmount.setText(formatVnd(auction.getCurrentPrice()));
    }

    @FXML private void handleEdit() { LoggerUtil.info("ChÃ¡Â»â€°nh sÃ¡Â»Â­a: " + auctionId); }
    @FXML private void handlePause() { LoggerUtil.info("TÃ¡ÂºÂ¡m dÃ¡Â»Â«ng: " + auctionId); }
    @FXML private void handleCancel() { LoggerUtil.info("HÃ¡Â»Â§y phiÃƒÂªn: " + auctionId); }

    private String formatVnd(double a) { return String.format("%,.0f Ã„â€˜", a).replace(',', '.'); }
    private String formatDate(long m) { return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(m)); }
    private String formatRemain(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) return h + " giÃ¡Â»Â " + m + " phÃƒÂºt";
        return m + " phÃƒÂºt";
    }
}
