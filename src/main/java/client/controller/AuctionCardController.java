package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.ImageDownloadService;
import client.util.RecommendationTracker;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Item;

import util.DateTimeUtil;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AuctionCardController {

    @FXML private ImageView productImageView;
    @FXML private StackPane imageContainer;
    @FXML private Label productNameLabel;
    @FXML private Label timerLabel;
    @FXML private Label priceLabel;
    @FXML private Button bidButton;

    private Auction currentAuction;
    private Timeline countdownTimer;
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private HomeScreenController homeScreenController;
    private String pendingImageId;

    @FXML
    public void initialize() {
        if (bidButton != null) {
            bidButton.setText("\u0110\u1eb7t gi\u00e1");
            bidButton.setOnAction(event -> openAuctionDetail());
        }

        if (imageContainer != null && productImageView != null) {
            Rectangle clip = new Rectangle();
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            clip.widthProperty().bind(imageContainer.widthProperty());
            clip.heightProperty().bind(imageContainer.heightProperty());
            imageContainer.setClip(clip);

            imageContainer.widthProperty().addListener((obs, oldValue, newValue) -> updateImageViewport());
            imageContainer.heightProperty().addListener((obs, oldValue, newValue) -> updateImageViewport());
            productImageView.imageProperty().addListener((obs, oldValue, newValue) -> updateImageViewport());
        }
    }

    public void setAuctionData(Auction auction) {
        this.currentAuction = auction;
        if (auction == null || auction.getItem() == null) return;

        Item item = auction.getItem();
        productNameLabel.setText(item.getName());
        priceLabel.setText(VND_FORMATTER.format(Math.round(auction.getCurrentPrice())) + " \u0111");
        forceVisibleTextColors();
        Platform.runLater(this::forceVisibleTextColors);

        if (productImageView != null) {
            List<String> images = item.getImages();
            if (images != null && !images.isEmpty()) {
                String imagePath = images.get(0);
                pendingImageId = imagePath;

                // ✅ THÊM: Tải ảnh từ server
                loadImageFromServer(imagePath);
            }
        }
        // ------------------------------------

        startCountdown();
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
        if (bidButton != null) {
            bidButton.setOnAction(event -> openAuctionDetail());
        }
        if (pendingImageId != null && productImageView != null && productImageView.getImage() == null) {
            loadImageFromServer(pendingImageId);
        }
    }


    private void updateImageViewport() {
        if (productImageView == null || imageContainer == null) return;

        Image image = productImageView.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) return;

        double containerWidth = imageContainer.getWidth();
        double containerHeight = imageContainer.getHeight();
        if (containerWidth <= 0 || containerHeight <= 0) return;

        double imageRatio = image.getWidth() / image.getHeight();
        double containerRatio = containerWidth / containerHeight;

        double viewportWidth = image.getWidth();
        double viewportHeight = image.getHeight();
        double viewportX = 0;
        double viewportY = 0;

        if (imageRatio > containerRatio) {
            viewportWidth = image.getHeight() * containerRatio;
            viewportX = (image.getWidth() - viewportWidth) / 2.0;
        } else {
            viewportHeight = image.getWidth() / containerRatio;
            viewportY = (image.getHeight() - viewportHeight) / 2.0;
        }

        productImageView.setViewport(new Rectangle2D(viewportX, viewportY, viewportWidth, viewportHeight));
    }

    private void forceVisibleTextColors() {
        if (productNameLabel != null) {
            productNameLabel.setTextFill(Color.web("#113254"));
            productNameLabel.setOpacity(1.0);
            productNameLabel.setStyle("-fx-text-fill: #113254; -fx-font-weight: bold; -fx-font-size: 15px;");
        }
        if (timerLabel != null) {
            String timerColor = timerLabel.getText() != null && timerLabel.getText().contains("\u0110\u00e3") ? "#6b7280" : "#e53e3e";
            timerLabel.setTextFill(Color.web(timerColor));
            timerLabel.setOpacity(1.0);
            timerLabel.setStyle("-fx-text-fill: " + timerColor + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        }
        if (priceLabel != null) {
            priceLabel.setTextFill(Color.web("#111827"));
            priceLabel.setOpacity(1.0);
            priceLabel.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    // =========================================
    // =========================================
    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long endTime = currentAuction.getEndTime();

            if (!DateTimeUtil.isExpired(endTime)) {
                long remainingMs = endTime - System.currentTimeMillis();
                timerLabel.setText("\u23f1 C\u00f2n " + DateTimeUtil.formatTimestamp(remainingMs));
                timerLabel.setTextFill(Color.web("#e53e3e"));
                forceVisibleTextColors();
            } else {
                timerLabel.setText("\u23f1 \u0110\u00e3 k\u1ebft th\u00fac");
                timerLabel.setTextFill(Color.web("#6b7280"));
                timerLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-font-size: 12px;");
                if (productNameLabel != null) {
                    productNameLabel.setTextFill(Color.web("#113254"));
                    productNameLabel.setStyle("-fx-text-fill: #113254; -fx-font-weight: bold; -fx-font-size: 15px;");
                }
                if (priceLabel != null) {
                    priceLabel.setTextFill(Color.web("#111827"));
                    priceLabel.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
                bidButton.setDisable(true);
                countdownTimer.stop();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }
    // =========================================

    private void openAuctionDetail() {
        if (currentAuction == null) return;

        RecommendationTracker.recordAuctionClick(currentAuction);
        if (homeScreenController != null) {
            homeScreenController.loadAuctionDetailView(currentAuction);
        } else {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION,
                    "Open auction detail without HomeScreenController",
                    new IllegalStateException(currentAuction.getAuctionId()));
        }
    }

    // =========================================
    // CÁC HÀM H? TR? REAL-TIME
    // =========================================

    /**
     */
    public void updatePrice(double newPrice) {
        if (currentAuction != null) {
            currentAuction.setCurrentPrice(newPrice);
        }

        Platform.runLater(() -> {
            priceLabel.setText(VND_FORMATTER.format(Math.round(newPrice)) + " \u0111");
            forceVisibleTextColors();
        });
    }

    /**
     */
    public String getCurrentAuctionId() {
        return currentAuction != null ? currentAuction.getAuctionId() : "";
    }
    // ===== MỚI THÊM: Download ảnh từ server =====

    /**
     * Cần inject HomeScreenController để lấy transport
     * HomeScreenController có clientSocket
     */
    private void loadImageFromServer(String imagePath) {
        new Thread(() -> {
            try {
                // [1] Lấy clientSocket từ HomeScreenController
                ClientSocket clientSocket = getClientSocket();
                if (clientSocket == null) {
                    showPlaceholderImage();  // ← Đơn giản: nếu không có socket → placeholder
                    return;
                }

                // [2] Download ảnh từ server
                byte[] imageBytes = ImageDownloadService.downloadImage(
                        clientSocket,
                        imagePath  // ← Đây là imageId bây giờ
                );

                // [3] Convert byte[] → Image
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));

                // [4] Cập nhật UI trên Main Thread
                Platform.runLater(() -> {
                    if (image.getWidth() > 0 && image.getHeight() > 0) {
                        productImageView.setImage(image);
                        updateImageViewport();
                    } else {
                        showPlaceholderImage();
                    }
                });

            } catch (Exception e) {
                System.err.println("Failed to load image: " + e.getMessage());
                Platform.runLater(this::showPlaceholderImage);
            }
        }).start();
    }

    /**
     * Fallback: Load ảnh từ file local (khi client có bản local)
     */


    /**
     * Hiển thị ảnh placeholder nếu load thất bại
     */
    private void showPlaceholderImage() {
        try {
            // Tìm file placeholder trong resources
            java.net.URL placeholderUrl = getClass().getResource("/CSS/placeholder.png");
            if (placeholderUrl != null) {
                Image placeholder = new Image(placeholderUrl.toExternalForm());
                productImageView.setImage(placeholder);
            }
        } catch (Exception e) {
            System.err.println("Placeholder image not found");
        }
    }

    /**
     * Lấy ClientSocket từ HomeScreenController
     */
    private ClientSocket getClientSocket() {
        if (homeScreenController != null && homeScreenController.getClientSocket() != null
                && homeScreenController.getClientSocket().isConnected()) {
            return homeScreenController.getClientSocket();  // ← GỌI METHOD VỪA THÊM
        }
        ClientSocket navigationSocket = NavigationManager.getInstance().getClientSocket();
        if (navigationSocket != null && navigationSocket.isConnected()) {
            return navigationSocket;
        }
        ClientSocket managerSocket = ConnectionManager.getInstance().getClientSocket();
        if (managerSocket != null && managerSocket.isConnected()) {
            return managerSocket;
        }
        return null;
    }

// ===== HẾT THÊM MỚI =====
}

