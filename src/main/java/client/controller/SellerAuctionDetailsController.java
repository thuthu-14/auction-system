package client.controller;


import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.ImageDownloadService;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Item;
import server.model.User;
import server.repository.SqlAuctionRepository;
import util.LoggerUtil;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.scene.shape.Rectangle;

public class SellerAuctionDetailsController {

    @FXML private Label productName, productId, productDesc;
    @FXML private Label startPrice, currentPrice;
    @FXML private Label startTime, endTime, timeLeft;
    @FXML private Label status, bidCount;
    @FXML private Label topBidderName, topBidAmount;
    @FXML private Button cancelAuctionButton;
    @FXML private ImageView productImage;

    private String auctionId;
    private User currentUser;
    private ClientSocket clientSocket;
    private SellerHomeController sellerHomeController;
    private Auction currentAuction;
    private final SellerClientService sellerClientService = new SellerClientService();

    @FXML
    public void initialize() {
        setupProductImageClip();
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        if (user != null) {
            NavigationManager.getInstance().setCurrentUser(user);
        }
        if (socket != null) {
            NavigationManager.getInstance().setClientSocket(socket);
        }
    }

    private void setupProductImageClip() {
        if (productImage == null) {
            return;
        }
        Rectangle clip = new Rectangle();
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        clip.widthProperty().bind(productImage.fitWidthProperty());
        clip.heightProperty().bind(productImage.fitHeightProperty());
        productImage.setClip(clip);
    }

    /**
     * Tải chi tiết phiên đấu giá từ Server
     */
    public void loadSellerAuctionDetails(String auctionId) {
        String targetAuctionId = resolveAuctionId(auctionId);
        this.auctionId = targetAuctionId;
        if (targetAuctionId == null || targetAuctionId.isBlank() || "--".equals(targetAuctionId)) {
            LoggerUtil.warn("Cannot load seller auction details: missing auction id");
            if (productName != null) {
                productName.setText("Chưa chọn phiên đấu giá");
            }
            if (productId != null) {
                productId.setText("Mã phiên: --");
            }
            return;
        }
        ClientSocket socket = clientSocket != null ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();

        if (socket == null || user == null) {
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Socket hoặc User bị null trong loadSellerAuctionDetails")));
            loadSellerAuctionDetailsFromLocal(targetAuctionId);
            return;
        }

        new Thread(() -> {
            try {
                Auction auction = sellerClientService.fetchAuctionDetail(socket, user, targetAuctionId);
                if (auction == null) {
                    auction = new SqlAuctionRepository().getAuctionById(targetAuctionId);
                }
                Auction finalAuction = auction;
                Platform.runLater(() -> updateUI(finalAuction));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load seller auction detail failed: " + e.getMessage())));
                loadSellerAuctionDetailsFromLocal(targetAuctionId);
            }
        }, "SellerAuctionDetailLoadThread").start();
    }


    private void loadSellerAuctionDetailsFromLocal(String auctionId) {
        new Thread(() -> {
            try {
                Auction auction = new SqlAuctionRepository().getAuctionById(auctionId);
                Platform.runLater(() -> updateUI(auction));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Load local seller auction detail failed: " + e.getMessage())));
            }
        }, "SellerAuctionDetailLocalLoadThread").start();
    }
    /**
     * Cập nhật thông tin lên giao diện
     */
    private void updateUI(Auction auction) {
        if (auction == null) return;
        currentAuction = auction;
        if (auction.getAuctionId() != null && !auction.getAuctionId().isBlank()) {
            auctionId = auction.getAuctionId();
            if (sellerHomeController != null) {
                sellerHomeController.rememberSellerAuctionId(auction.getAuctionId());
            }
        } else if (auction.getItemId() != null && !auction.getItemId().isBlank()) {
            auctionId = auction.getItemId();
            if (sellerHomeController != null) {
                sellerHomeController.rememberSellerAuctionId(auction.getItemId());
            }
        } else if (auction.getItem() != null && auction.getItem().getItemId() != null && !auction.getItem().getItemId().isBlank()) {
            auctionId = auction.getItem().getItemId();
            if (sellerHomeController != null) {
                sellerHomeController.rememberSellerAuctionId(auction.getItem().getItemId());
            }
        }

        // Thông tin sản phẩm cơ bản (Kiểm tra null cho từng Label để tránh NullPointerException)
        if (productName != null) productName.setText(auction.getItem() != null ? auction.getItem().getName() : "N/A");
        if (productId != null) productId.setText("Mã phiên: " + displayAuctionKey(auction));
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
        updateCancelButtonState(auction);
        loadProductImage(auction.getItem());
    }

    private void loadProductImage(Item item) {
        if (productImage == null || item == null || item.getImages() == null || item.getImages().isEmpty()) {
            return;
        }

        String imageId = item.getImages().get(0);  // imageId bây giờ (không phải path)
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        // Download từ server
        new Thread(() -> {
            try {
                if (clientSocket == null) {
                    LoggerUtil.warn("ClientSocket is null");
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(clientSocket, imageId);
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));

                Platform.runLater(() -> {
                    if (productImage != null && image.getWidth() > 0) {
                        productImage.setImage(image);
                    }
                });

            } catch (Exception e) {
                LoggerUtil.warn("Failed to load image: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Fallback: Load ảnh từ file local
     */


    @FXML private void handleEdit() { LoggerUtil.info("Chỉnh sửa: " + auctionId); }
    @FXML private void handlePause() { LoggerUtil.info("Tạm dừng: " + auctionId); }
    @FXML
    private void handleCancel() {
        String targetAuctionId = resolveAuctionId(auctionId);
        LoggerUtil.info("Cancel auction resolved id: " + targetAuctionId
                + ", currentAuction=" + (currentAuction != null ? currentAuction.getAuctionId() + "/" + currentAuction.getItemId() : "null"));
        if (!isUsableKey(targetAuctionId) && currentAuction == null) {
            showAlert(Alert.AlertType.ERROR, "L\u1ed7i", "Kh\u00f4ng t\u00ecm th\u1ea5y phi\u00ean \u0111\u1ea5u gi\u00e1.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("H\u1ee7y phi\u00ean \u0111\u1ea5u gi\u00e1");
        confirm.setHeaderText("X\u00e1c nh\u1eadn h\u1ee7y phi\u00ean?");
        confirm.setContentText("H\u1ec7 th\u1ed1ng s\u1ebd k\u1ebft th\u00fac phi\u00ean v\u00e0 ho\u00e0n ti\u1ec1n cho t\u1ea5t c\u1ea3 ng\u01b0\u1eddi \u0111\u00e3 \u0111\u1eb7t gi\u00e1.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = clientSocket != null ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        if (user == null || socket == null || !socket.isConnected()) {
            showAlert(Alert.AlertType.ERROR, "L\u1ed7i", "M\u1ea5t k\u1ebft n\u1ed1i t\u1edbi server.");
            return;
        }

        if (cancelAuctionButton != null) {
            cancelAuctionButton.setDisable(true);
        }
        new Thread(() -> {
            try {
                Auction cancelledAuction = sellerClientService.cancelAuction(socket, user, targetAuctionId);
                Platform.runLater(() -> {
                    if (cancelledAuction != null) {
                        updateUI(cancelledAuction);
                    } else {
                        loadSellerAuctionDetails(targetAuctionId);
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Th\u00e0nh c\u00f4ng", "\u0110\u00e3 h\u1ee7y phi\u00ean v\u00e0 ho\u00e0n ti\u1ec1n cho ng\u01b0\u1eddi \u0111\u00e3 \u0111\u1eb7t gi\u00e1.");
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Cancel auction failed: " + e.getMessage())));
                Platform.runLater(() -> {
                    if (cancelAuctionButton != null) {
                        cancelAuctionButton.setDisable(false);
                    }
                    showAlert(Alert.AlertType.ERROR, "L\u1ed7i", "Kh\u00f4ng th\u1ec3 h\u1ee7y phi\u00ean: " + e.getMessage());
                });
            }
        }, "SellerCancelAuctionThread").start();
    }

    @FXML
    private void handleBidHistory() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerAuctionBidHistoryView(resolveAuctionId(auctionId));
        } else {
            LoggerUtil.warn("SellerHomeController is null, cannot open bid history: " + auctionId);
        }
    }

    @FXML
    private void handleWinnerInfo() {
        if (sellerHomeController != null) {
            sellerHomeController.loadSellerWinnerInfoView(resolveAuctionId(auctionId));
        } else {
            LoggerUtil.warn("SellerHomeController is null, cannot open winner info: " + auctionId);
        }
    }

    private String formatVnd(double a) { return String.format("%,.0f", a).replace(',', '.') + "\u0111"; }
    private String formatDate(long m) { return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(m)); }
    private String resolveAuctionId(String requestedAuctionId) {
        if (currentAuction != null && isUsableKey(currentAuction.getAuctionId())) {
            return currentAuction.getAuctionId();
        }
        if (currentAuction != null && isUsableKey(currentAuction.getItemId())) {
            return currentAuction.getItemId();
        }
        if (currentAuction != null && currentAuction.getItem() != null
                && isUsableKey(currentAuction.getItem().getItemId())) {
            return currentAuction.getItem().getItemId();
        }
        if (isUsableKey(requestedAuctionId)) {
            return requestedAuctionId;
        }
        if (isUsableKey(auctionId)) {
            return auctionId;
        }
        String labelId = extractIdFromProductLabel();
        if (labelId != null) {
            return labelId;
        }
        return sellerHomeController != null ? sellerHomeController.getSelectedSellerAuctionId() : null;
    }

    private String displayAuctionKey(Auction auction) {
        if (auction == null) {
            return "--";
        }
        if (isUsableKey(auction.getAuctionId())) {
            return auction.getAuctionId();
        }
        if (isUsableKey(auction.getItemId())) {
            return auction.getItemId();
        }
        if (auction.getItem() != null && isUsableKey(auction.getItem().getItemId())) {
            return auction.getItem().getItemId();
        }
        return "--";
    }

    private boolean isUsableKey(String value) {
        return value != null
                && !value.isBlank()
                && !"--".equals(value)
                && !"null".equalsIgnoreCase(value);
    }

    private String extractIdFromProductLabel() {
        if (productId == null || productId.getText() == null) {
            return null;
        }
        String text = productId.getText().trim();
        int separator = text.indexOf(':');
        String value = separator >= 0 ? text.substring(separator + 1).trim() : text;
        return isUsableKey(value) ? value : null;
    }
    private void updateCancelButtonState(Auction auction) {
        if (cancelAuctionButton == null || auction == null) {
            return;
        }
        boolean cancellable = auction.getStatus() == common.AuctionStatus.OPEN
                || auction.getStatus() == common.AuctionStatus.RUNNING;
        cancelAuctionButton.setDisable(!cancellable);
        cancelAuctionButton.setText(cancellable ? "H\u1ee7y phi\u00ean" : "\u0110\u00e3 k\u1ebft th\u00fac");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        client.util.DialogUtil.showAlert(type, title, null, content);
    }

    private String formatRemain(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) return h + " giá " + m + " phút";
        return m + " phút";
    }


}

