package client.controller.auctiondetail;

import client.logic.AuctionImageGalleryLogic;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.ImageDownloadService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import navigation.NavigationManager;
import server.model.Item;
import util.LoggerUtil;

import java.io.ByteArrayInputStream;
import java.util.List;

public class AuctionImageGalleryRenderer {
    private final ImageView mainImageView;
    private final HBox thumbnailBox;
    private final AuctionImageGalleryLogic galleryLogic;

    public AuctionImageGalleryRenderer(ImageView mainImageView, HBox thumbnailBox) {
        this(mainImageView, thumbnailBox, new AuctionImageGalleryLogic());
    }

    AuctionImageGalleryRenderer(ImageView mainImageView, HBox thumbnailBox, AuctionImageGalleryLogic galleryLogic) {
        this.mainImageView = mainImageView;
        this.thumbnailBox = thumbnailBox;
        this.galleryLogic = galleryLogic;
    }

    public void loadMainImage(Item item) {
        List<String> imageIds = galleryLogic.imageIdsFor(item);
        if (mainImageView == null || imageIds.isEmpty()) {
            if (mainImageView != null) {
                mainImageView.setImage(null);
            }
            if (thumbnailBox != null) {
                thumbnailBox.getChildren().clear();
            }
            return;
        }

        renderImageGallery(imageIds);
    }

    private void renderImageGallery(List<String> imageIds) {
        if (mainImageView == null || imageIds == null || imageIds.isEmpty()) {
            return;
        }

        if (thumbnailBox != null) {
            thumbnailBox.getChildren().clear();
            thumbnailBox.setAlignment(Pos.CENTER_LEFT);
        }

        loadAndDisplayImage(galleryLogic.firstImageId(imageIds), mainImageView);

        if (thumbnailBox == null) {
            return;
        }

        for (String imageId : imageIds) {
            StackPane thumbnailFrame = createThumbnailFrame(imageId);
            thumbnailBox.getChildren().add(thumbnailFrame);
        }
    }

    private StackPane createThumbnailFrame(String imageId) {
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitWidth(78);
        thumbnailView.setFitHeight(78);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);

        StackPane thumbnailFrame = new StackPane(thumbnailView);
        thumbnailFrame.setPrefSize(88, 88);
        thumbnailFrame.setMinSize(88, 88);
        thumbnailFrame.setMaxSize(88, 88);
        thumbnailFrame.setStyle(getThumbnailStyle());

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = getActiveImageSocket();
                if (socket == null || !socket.isConnected()) {
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image thumbnailImage = new Image(new ByteArrayInputStream(imageBytes), 90, 90, true, true);
                Image fullImage = new Image(new ByteArrayInputStream(imageBytes));

                Platform.runLater(() -> {
                    thumbnailView.setImage(thumbnailImage);
                    thumbnailFrame.setOnMouseEntered(event -> mainImageView.setImage(fullImage));
                    thumbnailFrame.setOnMouseClicked(event -> mainImageView.setImage(fullImage));
                });
            } catch (Exception e) {
                LoggerUtil.error("Failed to load thumbnail " + imageId + ": " + e.getMessage());
            }
        });

        return thumbnailFrame;
    }

    private void loadAndDisplayImage(String imageId, ImageView imageView) {
        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = getActiveImageSocket();
                if (socket == null || !socket.isConnected()) {
                    LoggerUtil.error("Socket not connected");
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                Platform.runLater(() -> imageView.setImage(image));
            } catch (Exception e) {
                LoggerUtil.error("Failed to load image " + imageId + ": " + e.getMessage());
            }
        });
    }

    private ClientSocket getActiveImageSocket() {
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        if (socket != null && socket.isConnected()) {
            return socket;
        }
        socket = NavigationManager.getInstance().getClientSocket();
        if (socket != null && socket.isConnected()) {
            return socket;
        }
        return null;
    }

    private String getThumbnailStyle() {
        return "-fx-background-color: white;"
                + "-fx-border-color: #e5e7eb;"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4;"
                + "-fx-cursor: hand;";
    }
}
