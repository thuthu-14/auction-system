package client.controller.auctiondetail;

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

import java.util.ArrayList;
import java.util.List;

public class AuctionImageGalleryRenderer {
    private final ImageView mainImageView;
    private final HBox thumbnailBox;

    public AuctionImageGalleryRenderer(ImageView mainImageView, HBox thumbnailBox) {
        this.mainImageView = mainImageView;
        this.thumbnailBox = thumbnailBox;
    }

    public void loadMainImage(Item item) {
        if (mainImageView == null || item == null || item.getImages() == null || item.getImages().isEmpty()) {
            if (thumbnailBox != null) {
                thumbnailBox.getChildren().clear();
            }
            return;
        }

        renderImageGallery(item.getImages());
    }

    private void renderImageGallery(List<String> imageIds) {
        if (mainImageView == null || imageIds == null || imageIds.isEmpty()) {
            return;
        }

        if (thumbnailBox != null) {
            thumbnailBox.getChildren().clear();
            thumbnailBox.setAlignment(Pos.CENTER_LEFT);
        }

        List<StackPane> thumbnailFrames = new ArrayList<>();
        loadAndDisplayImage(imageIds.get(0), mainImageView);

        if (thumbnailBox == null) {
            return;
        }

        for (int i = 0; i < imageIds.size(); i++) {
            StackPane thumbnailFrame = createThumbnailFrame(imageIds.get(i), i);
            thumbnailFrames.add(thumbnailFrame);
            thumbnailBox.getChildren().add(thumbnailFrame);
        }
    }

    private StackPane createThumbnailFrame(String imageId, int index) {
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitWidth(78);
        thumbnailView.setFitHeight(78);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);

        StackPane thumbnailFrame = new StackPane(thumbnailView);
        thumbnailFrame.setPrefSize(88, 88);
        thumbnailFrame.setMinSize(88, 88);
        thumbnailFrame.setMaxSize(88, 88);
        thumbnailFrame.setStyle(getThumbnailStyle(index == 0));

        new Thread(() -> {
            try {
                ClientSocket socket = getActiveImageSocket();
                if (socket == null || !socket.isConnected()) {
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image thumbnailImage = new Image(new java.io.ByteArrayInputStream(imageBytes), 90, 90, true, true);

                Platform.runLater(() -> {
                    thumbnailView.setImage(thumbnailImage);
                    thumbnailFrame.setOnMouseEntered(event -> {
                        loadAndDisplayImage(imageId, mainImageView);
                        thumbnailFrame.setStyle(getThumbnailStyle(true));
                    });
                });
            } catch (Exception e) {
                LoggerUtil.error("Failed to load thumbnail " + imageId + ": " + e.getMessage());
            }
        }, "LoadThumbnailThread-" + index).start();

        return thumbnailFrame;
    }

    private void loadAndDisplayImage(String imageId, ImageView imageView) {
        new Thread(() -> {
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
        }, "LoadImageThread").start();
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

    private String getThumbnailStyle(boolean selected) {
        String borderColor = selected ? "#ef4444" : "#e5e7eb";
        int borderWidth = selected ? 3 : 1;
        return "-fx-background-color: white;"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: " + borderWidth + ";"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4;"
                + "-fx-cursor: hand;";
    }
}
