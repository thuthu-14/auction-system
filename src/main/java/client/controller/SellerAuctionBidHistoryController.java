package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import navigation.NavigationManager;
import server.model.Bid;
import server.model.User;
import util.LoggerUtil;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SellerAuctionBidHistoryController {

    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @FXML private Label summaryLabel;
    @FXML private TableView<Bid> bidTable;
    @FXML private TableColumn<Bid, String> colBidder;
    @FXML private TableColumn<Bid, String> colAmount;
    @FXML private TableColumn<Bid, String> colTime;
    @FXML private TableColumn<Bid, String> colStatus;

    private final SellerClientService sellerClientService = new SellerClientService();
    private String auctionId;
    private User currentUser;
    private ClientSocket clientSocket;
    private SellerHomeController sellerHomeController;

    @FXML
    public void initialize() {
        currentUser = NavigationManager.getInstance().getCurrentUser();
        clientSocket = getActiveSocket();
        setupTable();
        setText(summaryLabel, "Chưa chọn phiên đấu giá.");
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
            loadAuctionBidHistory(auctionId);
        }
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    public void loadAuctionBidHistory(String auctionId) {
        String targetAuctionId = resolveAuctionId(auctionId);
        this.auctionId = targetAuctionId;
        if (targetAuctionId == null || targetAuctionId.isBlank() || "--".equals(targetAuctionId)) {
            setText(summaryLabel, "Chưa chọn phiên đấu giá.");
            if (bidTable != null) {
                bidTable.getItems().clear();
            }
            LoggerUtil.warn("Seller bid history: missing auction id");
            return;
        }
        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = getActiveSocket();
        if (user == null || socket == null || !socket.isConnected()) {
            setText(summaryLabel, "Không thể tải dữ liệu do thiếu kết nối.");
            LoggerUtil.warn("Seller bid history: missing user or socket");
            return;
        }

        setText(summaryLabel, "Đang tải lịch sử đặt giá...");
        new Thread(() -> {
            try {
                List<Bid> bids = sellerClientService.fetchBidHistoryWithFallback(socket, user, targetAuctionId);
                Platform.runLater(() -> renderBidHistory(bids));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load seller auction bid history failed: " + e.getMessage())));
                Platform.runLater(() -> {
                    if (bidTable != null) {
                        bidTable.getItems().clear();
                    }
                    setText(summaryLabel, "Không thể tải lịch sử đặt giá.");
                });
            }
        }, "SellerAuctionBidHistoryLoadThread").start();
    }

    private void setupTable() {
        if (colBidder != null) {
            colBidder.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getBidderName())));
        }
        if (colAmount != null) {
            colAmount.setCellValueFactory(cell -> new SimpleStringProperty(formatVnd(cell.getValue().getAmount())));
        }
        if (colTime != null) {
            colTime.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getBidTime())));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getStatus())));
        }
    }

    private void renderBidHistory(List<Bid> bids) {
        List<Bid> safeBids = bids != null ? bids : List.of();
        if (bidTable != null) {
            bidTable.setItems(FXCollections.observableArrayList(safeBids));
        }
        setText(summaryLabel, "Tổng cộng " + safeBids.size() + " lượt đặt giá.");
    }

    @FXML
    private void handleDetails() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerAuctionDetailsView(auctionId);
        }
    }

    @FXML
    private void handleBidHistory() {
        loadAuctionBidHistory(auctionId);
    }

    @FXML
    private void handleWinnerInfo() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerWinnerInfoView(auctionId);
        }
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

    private String resolveAuctionId(String requestedAuctionId) {
        if (requestedAuctionId != null && !requestedAuctionId.isBlank() && !"--".equals(requestedAuctionId)) {
            return requestedAuctionId;
        }
        if (auctionId != null && !auctionId.isBlank() && !"--".equals(auctionId)) {
            return auctionId;
        }
        return sellerHomeController != null ? sellerHomeController.getSelectedSellerAuctionId() : null;
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String formatVnd(double amount) {
        return VND_FORMATTER.format(amount) + " đ";
    }

    private String formatDate(long timestamp) {
        return timestamp <= 0 ? "--" : DATE_FORMATTER.format(new Date(timestamp));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }
}

