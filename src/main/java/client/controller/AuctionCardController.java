package client.controller;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import server.model.Auction;
import server.model.Item;

import util.DateTimeUtil; // CHỈ THÊM IMPORT NÀY

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

    @FXML
    public void initialize() {
        // KHÔNG set handler ở đây, sẽ set sau khi có homeScreenController

        if (imageContainer != null && productImageView != null) {
            imageContainer.prefHeightProperty().bind(imageContainer.widthProperty());
            productImageView.fitWidthProperty().bind(imageContainer.widthProperty());
            productImageView.fitHeightProperty().bind(imageContainer.heightProperty());

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
        priceLabel.setText(VND_FORMATTER.format(Math.round(auction.getCurrentPrice())) + " đ");

        // --- PHẦN LOAD ẢNH CỰC KỲ AN TOÀN (GIỮ NGUYÊN) ---
        if (productImageView != null) {
            List<String> images = item.getImages();
            if (images != null && !images.isEmpty()) {
                String rawPath = images.get(0);
                try {
                    Image img = null;

                    // 1. Nếu đường dẫn đã có định dạng URL (bắt đầu bằng file: hoặc http:)
                    if (rawPath.startsWith("file:") || rawPath.startsWith("http")) {
                        img = new Image(rawPath, 250, 0, true, true);
                    }
                    // 2. Nếu là đường dẫn ổ đĩa thuần (C:\...)
                    else {
                        File file = new File(rawPath);
                        if (file.exists()) {
                            String imageUrl = file.toURI().toURL().toString();
                            img = new Image(imageUrl, 250, 0, true, true);
                        }
                    }

                    if (img != null) {
                        productImageView.setImage(img);
                        updateImageViewport();
                    } else {
                        System.err.println("Card: Không thể load ảnh từ: " + rawPath);
                    }

                } catch (Exception e) {
                    System.err.println("Card: Lỗi hệ thống khi load ảnh: " + e.getMessage());
                }
            }
        }
        // ------------------------------------

        startCountdown();
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
        // Set handler của button KHI homeScreenController đã được truyền vào
        if (bidButton != null) {
            bidButton.setOnAction(event -> openAuctionDetail());
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

    // =========================================
    // ĐÃ SỬA LẠI: CHỈ LINK VỚI DATETIMEUTIL VÀ AUCTION
    // =========================================
    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            long endTime = currentAuction.getEndTime(); // Lấy thời gian kết thúc (miligiây) từ model

            // Link với DateTimeUtil để kiểm tra hết giờ
            if (!DateTimeUtil.isExpired(endTime)) {
                // Tính khoảng cách (miligiây) và link với formatTimestamp để hiển thị
                long remainingMs = endTime - System.currentTimeMillis();
                timerLabel.setText("⏱ Còn " + DateTimeUtil.formatTimestamp(remainingMs));
            } else {
                timerLabel.setText("⏱ Đã kết thúc");
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

        // Nếu có reference đến HomeScreenController, load vào contentArea
        if (homeScreenController != null) {
            homeScreenController.loadAuctionDetailView(currentAuction);
        } else {
            // Fallback: Mở popup nếu không có reference (cho trường hợp debug)
            try {
                String fxmlPath = "/fxml/AuctionDetail.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();

                AuctionDetailController detailController = loader.getController();
                detailController.loadAuctionData(currentAuction);

                Stage stage = new Stage();
                stage.setTitle("Chi tiết: " + currentAuction.getItem().getName());

                stage.setScene(new Scene(root, 1000, 700));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setResizable(false);
                stage.show();

            } catch (Exception e) {
                System.err.println("LỖI KHI MỞ CHI TIẾT: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // =========================================
    // CÁC HÀM HỖ TRỢ REAL-TIME (GIỮ NGUYÊN)
    // =========================================

    /**
     * Hàm này được Dashboard gọi khi nhận được tín hiệu có người đặt giá mới
     */
    public void updatePrice(double newPrice) {
        if (currentAuction != null) {
            currentAuction.setCurrentPrice(newPrice); // Cập nhật dữ liệu ngầm
        }

        // Cập nhật giao diện (Bắt buộc chạy trong Platform.runLater đối với JavaFX)
        Platform.runLater(() -> {
            priceLabel.setText(VND_FORMATTER.format(Math.round(newPrice)) + " đ");
        });
    }

    /**
     * Hàm này giúp Dashboard biết thẻ này đang hiển thị sản phẩm nào
     */
    public String getCurrentAuctionId() {
        return currentAuction != null ? currentAuction.getAuctionId() : "";
    }
}
