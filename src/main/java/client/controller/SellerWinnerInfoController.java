package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClientService;
import common.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Bid;
import server.model.User;
import util.LoggerUtil;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerWinnerInfoController {

    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private Label winnerName;
    @FXML private Label winnerUsername;
    @FXML private Label winnerPhone;
    @FXML private Label winnerEmail;
    @FXML private Label winningPrice;
    @FXML private Label paymentStatus;
    @FXML private Label shippingAddress;
    @FXML private Label noWinnerLabel;

    private final SellerClientService sellerClientService = new SellerClientService();
    private User currentUser;
    private ClientSocket clientSocket;
    private SellerHomeController sellerHomeController;
    private String auctionId;

    @FXML
    public void initialize() {
        currentUser = NavigationManager.getInstance().getCurrentUser();
        clientSocket = getActiveSocket();
        showEmptyWinnerState("Chưa chọn phiên đấu giá.");
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
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
        if (auctionId != null && !auctionId.isBlank()) {
            loadWinnerInfo(auctionId);
        }
    }

    public void loadWinnerInfo(String auctionId) {
        String targetAuctionId = resolveAuctionId(auctionId);
        this.auctionId = targetAuctionId;
        if (targetAuctionId == null || targetAuctionId.isBlank() || "--".equals(targetAuctionId)) {
            showEmptyWinnerState("Chưa chọn phiên đấu giá.");
            LoggerUtil.warn("Seller winner info: missing auction id");
            return;
        }
        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = getActiveSocket();
        if (user == null || socket == null || !socket.isConnected()) {
            showEmptyWinnerState("Không thể tải dữ liệu do thiếu kết nối.");
            LoggerUtil.warn("Seller winner info: missing user or socket");
            return;
        }

        new Thread(() -> {
            try {
                Auction auction = sellerClientService.fetchAuctionDetail(socket, user, targetAuctionId);
                List<Bid> bids = sellerClientService.fetchBidHistoryWithFallback(socket, user, targetAuctionId);
                String winnerId = resolveWinnerId(auction, bids);
                Map<String, String> contact = winnerId == null || winnerId.isBlank()
                        ? Map.of()
                        : sellerClientService.fetchUserContact(socket, user, winnerId);
                Platform.runLater(() -> renderWinnerInfo(auction, bids, contact));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load winner info failed: " + e.getMessage())));
                Platform.runLater(() -> showEmptyWinnerState("Không thể tải thông tin người trúng."));
            }
        }, "SellerWinnerInfoLoadThread").start();
    }

    private void renderWinnerInfo(Auction auction, List<Bid> bids, Map<String, String> contact) {
        Bid winningBid = findWinningBid(auction, bids);
        boolean auctionEnded = auction != null
                && (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.CLOSED
                || auction.getTimeRemainingSeconds() <= 0);

        if (winningBid == null && (auction == null || auction.getHighestBidderName() == null || auction.getHighestBidderName().isBlank())) {
            showEmptyWinnerState(auctionEnded ? "Phiên đã kết thúc nhưng chưa có người trúng." : "Đang chờ người trúng giải...");
            return;
        }

        String bidderName = winningBid != null
                ? safe(winningBid.getBidderName(), "Người thắng")
                : safe(auction.getHighestBidderName(), "Người thắng");
        String bidderId = winningBid != null
                ? safe(winningBid.getBidderId(), "--")
                : safe(auction.getHighestBidderId(), "--");
        double price = winningBid != null ? winningBid.getAmount() : auction.getCurrentPrice();

        setText(winnerName, contactValue(contact, "name", bidderName));
        setText(winnerUsername, bidderId);
        setText(winnerPhone, contactValue(contact, "phone", "Chưa cập nhật"));
        setText(winnerEmail, contactValue(contact, "email", "Chưa cập nhật"));
        setText(winningPrice, formatVnd(price));
        setText(paymentStatus, auctionEnded ? "CHỜ THANH TOÁN" : "ĐANG DIỄN RA");
        setText(shippingAddress, contactValue(contact, "address", "Chưa cập nhật địa chỉ giao hàng."));
        setVisible(noWinnerLabel, false);
    }

    private String resolveWinnerId(Auction auction, List<Bid> bids) {
        Bid winningBid = findWinningBid(auction, bids);
        if (winningBid != null && winningBid.getBidderId() != null && !winningBid.getBidderId().isBlank()) {
            return winningBid.getBidderId();
        }
        return auction != null ? auction.getHighestBidderId() : null;
    }

    private Bid findWinningBid(Auction auction, List<Bid> bids) {
        if (bids == null || bids.isEmpty()) {
            return null;
        }
        if (auction != null && auction.getHighestBidderId() != null) {
            return bids.stream()
                    .filter(bid -> auction.getHighestBidderId().equals(bid.getBidderId()))
                    .max(Comparator.comparingDouble(Bid::getAmount))
                    .orElse(null);
        }
        return bids.stream().max(Comparator.comparingDouble(Bid::getAmount)).orElse(null);
    }

    private void showEmptyWinnerState(String message) {
        setText(winnerName, "--");
        setText(winnerUsername, "");
        setText(winnerPhone, "--");
        setText(winnerEmail, "--");
        setText(winningPrice, "--");
        setText(paymentStatus, "--");
        setText(shippingAddress, "--");
        setText(noWinnerLabel, message);
        setVisible(noWinnerLabel, true);
    }

    @FXML
    private void handleDetails() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerAuctionDetailsView(resolveAuctionId(auctionId));
        }
    }

    @FXML
    private void handleBidHistory() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerAuctionBidHistoryView(resolveAuctionId(auctionId));
        }
    }

    @FXML
    private void handleWinnerInfo() {
        loadWinnerInfo(auctionId);
    }

    @FXML
    private void handleMessageBuyer() {
        showInfo("Chức năng nhắn tin cho người mua chưa được triển khai.");
        LoggerUtil.info("Message buyer for auction: " + auctionId);
    }

    @FXML
    private void handleConfirmShipping() {
        showInfo("Chức năng xác nhận giao hàng chưa được triển khai.");
        LoggerUtil.info("Confirm shipping for auction: " + auctionId);
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void setVisible(Label label, boolean visible) {
        if (label != null) {
            label.setVisible(visible);
            label.setManaged(visible);
        }
    }

    private String formatVnd(double amount) {
        return VND_FORMATTER.format(amount) + " đ";
    }

    private String resolveAuctionId(String requestedAuctionId) {
        if (requestedAuctionId != null && !requestedAuctionId.isBlank() && !"--".equals(requestedAuctionId)) {
            return requestedAuctionId;
        }
        if (auctionId != null && !auctionId.isBlank() && !"--".equals(auctionId)) {
            return auctionId;
        }
        return sellerHomeController != null ? sellerHomeController.getSelectedSellerAuctionId() : null;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String contactValue(Map<String, String> contact, String key, String fallback) {
        if (contact == null) {
            return fallback;
        }
        String value = contact.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}

