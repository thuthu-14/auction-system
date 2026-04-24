package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private ImageView bannerImageView;

    @FXML
    private Button prevBannerBtn;

    @FXML
    private Button nextBannerBtn;

    private List<Image> bannerImages;
    private int currentImageIndex = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(30.0);
        clip.setArcHeight(30.0);
        clip.widthProperty().bind(bannerImageView.fitWidthProperty());
        clip.heightProperty().bind(bannerImageView.fitHeightProperty());

        bannerImageView.setClip(clip);
        bannerImages = new ArrayList<>();

        try {
            String dashboardUrl = Objects.requireNonNull(getClass().getResource("/CSS/dashboard.png")).toExternalForm();
            String flashBidUrl = Objects.requireNonNull(getClass().getResource("/CSS/flashBid.png")).toExternalForm();
            bannerImages.add(new Image(dashboardUrl));
            bannerImages.add(new Image(flashBidUrl));
        } catch (Exception e) {
            System.out.println("Lỗi load ảnh: " + e.getMessage());
            e.printStackTrace();
        }

        if (!bannerImages.isEmpty()) {
            bannerImageView.setImage(bannerImages.get(0));
        }

        prevBannerBtn.setOnAction(event -> showPreviousImage());
        nextBannerBtn.setOnAction(event -> showNextImage());
    }

    private void showNextImage() {
        if (bannerImages == null || bannerImages.isEmpty()) return;
        currentImageIndex = (currentImageIndex + 1) % bannerImages.size();
        bannerImageView.setImage(bannerImages.get(currentImageIndex));
    }

    private void showPreviousImage() {
        if (bannerImages == null || bannerImages.isEmpty()) return;
        currentImageIndex = (currentImageIndex - 1 + bannerImages.size()) % bannerImages.size();
        bannerImageView.setImage(bannerImages.get(currentImageIndex));
    }
}

