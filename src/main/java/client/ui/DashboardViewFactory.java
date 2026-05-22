package client.ui;

import client.controller.HomeScreenController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import server.model.Auction;

public class DashboardViewFactory {

    private final AuctionCardFactory auctionCardFactory;

    public DashboardViewFactory(AuctionCardFactory auctionCardFactory) {
        this.auctionCardFactory = auctionCardFactory;
    }

    public VBox createEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(140);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-padding: 24;");

        Label title = new Label("Chua co phien dau gia phu hop");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Cac phien dang dien ra se hien thi o day.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    public VBox createAuctionCard(Auction auction, HomeScreenController homeScreenController, String context) {
        VBox card = auctionCardFactory.create(auction, homeScreenController, context);
        return card != null ? card : new VBox();
    }

    public void applyCompactCardSize(VBox card) {
        applyCardSize(card, 238, 216, 204, 362, 216, 204, 14, 32, 16, 8, 11);
        resizeMetaRow(card, 216, 27, 12, 12);
    }

    public void applySuggestionCardSize(VBox card) {
        applyCardSize(card, 218, 188, 188, 336, 198, 188, 13, 28, 14, 7, 10);
        resizeMetaRow(card, 198, 25, 11, 11);
    }

    private void applyCardSize(
            VBox card,
            double cardWidth,
            double imageWidth,
            double imageHeight,
            double cardHeight,
            double contentWidth,
            double imageViewHeight,
            double titleFontSize,
            double buttonHeight,
            double buttonRadius,
            double spacing,
            double padding
    ) {
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(spacing);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #eef2f7; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 3); -fx-padding: " + padding + ";");
        setRegionWidth(card, cardWidth);
        card.setMinHeight(cardHeight);
        card.setPrefHeight(cardHeight);
        card.setMaxHeight(cardHeight);

        javafx.scene.Node imageContainer = card.lookup("#imageContainer");
        if (imageContainer instanceof Region region) {
            region.setMinSize(imageWidth, imageHeight);
            region.setPrefSize(imageWidth, imageHeight);
            region.setMaxSize(imageWidth, imageHeight);
            region.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
            VBox.setMargin(region, new Insets(0, 0, 0, 0));
            if (region instanceof StackPane stackPane) {
                stackPane.setAlignment(Pos.CENTER);
            }
        }

        javafx.scene.Node productImage = card.lookup("#productImageView");
        if (productImage instanceof ImageView imageView) {
            imageView.setFitWidth(imageWidth);
            imageView.setFitHeight(imageViewHeight);
        }

        javafx.scene.Node productName = card.lookup("#productNameLabel");
        if (productName instanceof Label label) {
            setRegionWidth(label, contentWidth);
            label.setMinHeight(40);
            label.setPrefHeight(40);
            label.setMaxHeight(40);
            label.setStyle("-fx-text-fill: #113254; -fx-font-size: " + titleFontSize + "px; -fx-font-weight: bold;");
            VBox.setMargin(label, new Insets(4, 0, 0, 0));
        }

        javafx.scene.Node bidButton = card.lookup("#bidButton");
        if (bidButton instanceof Button button) {
            setRegionWidth(button, contentWidth);
            button.setMinHeight(buttonHeight);
            button.setPrefHeight(buttonHeight);
            button.setMaxHeight(buttonHeight);
            button.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: " + buttonRadius + "; -fx-cursor: hand; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0;");
        }
    }

    private void resizeMetaRow(VBox card, double rowWidth, double rowHeight, double timerFontSize, double priceFontSize) {
        javafx.scene.Node metaRow = card.lookup("#metaRow");
        if (metaRow instanceof HBox row) {
            setRegionWidth(row, rowWidth);
            row.setMinHeight(rowHeight);
            row.setPrefHeight(rowHeight);
            row.setMaxHeight(rowHeight);
        }

        javafx.scene.Node timer = card.lookup("#timerLabel");
        if (timer instanceof Label label) {
            label.setMinWidth(Region.USE_COMPUTED_SIZE);
            label.setPrefWidth(Region.USE_COMPUTED_SIZE);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-text-fill: #e53e3e; -fx-font-size: " + timerFontSize + "px; -fx-font-weight: bold;");
        }

        javafx.scene.Node price = card.lookup("#priceLabel");
        if (price instanceof Label label) {
            label.setMinWidth(Region.USE_COMPUTED_SIZE);
            label.setPrefWidth(Region.USE_COMPUTED_SIZE);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setStyle("-fx-text-fill: #000000; -fx-font-size: " + priceFontSize + "px; -fx-font-weight: bold;");
        }
    }

    private void setRegionWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }
}
