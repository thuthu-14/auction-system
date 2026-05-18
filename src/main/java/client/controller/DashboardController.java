package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.util.RecommendationTracker;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.DashboardClientService;
import common.AuctionStatus;
import common.ItemCategory;
import server.model.Auction;
import server.model.User;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import util.LoggerUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public class DashboardController implements Initializable {

    
    @FXML private ImageView bannerImageView;
    @FXML private ScrollPane dashboardRoot;
    @FXML private Button prevBannerBtn;
    @FXML private Button nextBannerBtn;
    @FXML private ScrollPane endingSoonScroll;
    @FXML private HBox endingSoonHBox;
    @FXML private Button prevEndingBtn;
    @FXML private Button nextEndingBtn;
    @FXML private HBox suggestedHBox;

    private List<Image> bannerImages;
    private int currentImageIndex = 0;

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeScreenController;
    private final DashboardClientService dashboardClientService = new DashboardClientService();

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        LoggerUtil.info("Dashboard connected: " + (user != null ? user.getUsername() : "Guest"));
        loadAuctionsFromServer();
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupBanner();
        setupEndingSoonControls();
        Platform.runLater(() -> {
            forceDashboardTextColors();
            setupCategoryHoverEffects();
        });
    }

    private void setupCategoryHoverEffects() {
        if (dashboardRoot == null) {
            return;
        }

        for (Node categoryCard : dashboardRoot.lookupAll(".dashboard-category-card")) {
            categoryCard.setOnMouseEntered(event -> animateCategoryCard(categoryCard, 1.06));
            categoryCard.setOnMouseExited(event -> animateCategoryCard(categoryCard, 1.0));
        }
    }

    private void animateCategoryCard(Node categoryCard, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(120), categoryCard);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private void forceDashboardTextColors() {
        if (dashboardRoot == null) {
            return;
        }

        for (javafx.scene.Node node : dashboardRoot.lookupAll(".label")) {
            if (!(node instanceof Label label)) {
                continue;
            }

            String text = label.getText() != null ? label.getText() : "";
            String id = label.getId() != null ? label.getId() : "";

            if (label.getStyleClass().contains("category-icon")) {
                applyLabelColor(label, "#ffffff");
            } else if ("productNameLabel".equals(id)) {
                applyLabelColor(label, "#113254");
            } else if ("timerLabel".equals(id)) {
                applyLabelColor(label, text.contains("\u0110\u00e3") || text.contains("K\u1ebeT TH\u00daC") ? "#6b7280" : "#e53e3e");
            } else if ("priceLabel".equals(id)) {
                applyLabelColor(label, "#111827");
            } else if (text.contains("V\u1eeba xong") || text.contains("V\u1eeba") || text.contains("xong")) {
                applyLabelColor(label, "#9ca3af");
            } else if (text.contains("H\u00e0ng ng\u00e0n") || text.contains("H\u1ec7 th\u1ed1ng") || text.contains("th\u00e1ng")) {
                applyLabelColor(label, "#4a5568");
            } else if (!text.isBlank()) {
                applyLabelColor(label, text.length() <= 4 ? "#94a3b8" : "#111827");
            }
        }
    }

    private void applyLabelColor(Label label, String color) {
        label.setTextFill(Color.web(color));
        label.setOpacity(1.0);

        String style = label.getStyle() != null ? label.getStyle() : "";
        style = style.replaceAll("-fx-text-fill\\s*:\\s*[^;]+;?", "").trim();
        if (!style.isEmpty() && !style.endsWith(";")) {
            style += ";";
        }
        label.setStyle(style + " -fx-text-fill: " + color + ";");
    }

    public void loadAuctionsFromServer() {
        ClientSocket socket = (clientSocket != null) ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        if (socket == null) return;

        new Thread(() -> {
            try {
                List<Auction> auctions = dashboardClientService.fetchAllAuctions(socket);
                Platform.runLater(() -> renderProductCards(auctions));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Dashboard load auctions", e);
            }
        }).start();
    }

    private void renderProductCards(List<Auction> auctions) {
        if (endingSoonHBox == null) return;

        endingSoonHBox.getChildren().clear();
        List<Auction> safeAuctions = auctions != null ? auctions : List.of();

        List<Auction> endingSoonAuctions = safeAuctions.stream()
                .filter(Objects::nonNull)
                .filter(auction -> auction.getItem() != null)
                .filter(this::isVisibleAuction)
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .sorted(Comparator.comparingLong(Auction::getTimeRemainingSeconds))
                .limit(10)
                .toList();

        for (Auction auction : endingSoonAuctions) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                applyCompactCardSize(card);
                endingSoonHBox.getChildren().add(card);
            }
        }

        if (endingSoonHBox.getChildren().isEmpty()) {
            endingSoonHBox.getChildren().add(createEmptyState());
        }
        updateEndingSoonButtons();
        renderSuggestedAuctions(safeAuctions);
        forceDashboardTextColors();
        Platform.runLater(this::forceDashboardTextColors);
    }

    private void renderSuggestedAuctions(List<Auction> auctions) {
        if (suggestedHBox == null) {
            return;
        }
        suggestedHBox.getChildren().clear();

        List<Auction> visibleAuctions = auctions.stream()
                .filter(Objects::nonNull)
                .filter(auction -> auction.getItem() != null)
                .filter(this::isVisibleAuction)
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .toList();

        List<String> recentAuctionIds = RecommendationTracker.getRecentAuctionIds();
        Map<ItemCategory, Integer> categoryWeights = RecommendationTracker.getCategoryWeights();
        Set<String> recentIdSet = new HashSet<>(recentAuctionIds);
        Map<String, Integer> auctionRank = new HashMap<>();
        for (int i = 0; i < recentAuctionIds.size(); i++) {
            auctionRank.put(recentAuctionIds.get(i), recentAuctionIds.size() - i);
        }

        List<Auction> suggestions = visibleAuctions.stream()
                .filter(auction -> auction.getAuctionId() == null || !recentIdSet.contains(auction.getAuctionId()))
                .sorted(Comparator.comparingInt((Auction auction) -> recommendationScore(auction, categoryWeights, auctionRank)).reversed()
                        .thenComparingInt(this::getBidCount).reversed()
                        .thenComparingDouble(Auction::getCurrentPrice).reversed()
                        .thenComparingLong(Auction::getEndTime))
                .limit(8)
                .toList();

        if (!RecommendationTracker.hasSignals() || suggestions.isEmpty()) {
            suggestions = visibleAuctions.stream()
                    .sorted(Comparator.comparingInt(this::getBidCount).reversed()
                            .thenComparingDouble(Auction::getCurrentPrice).reversed()
                            .thenComparingLong(Auction::getEndTime))
                    .limit(8)
                    .toList();
        }

        for (Auction auction : suggestions) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                applySuggestionCardSize(card);
                suggestedHBox.getChildren().add(card);
            }
        }
    }

    private int recommendationScore(Auction auction, Map<ItemCategory, Integer> categoryWeights, Map<String, Integer> auctionRank) {
        int score = 0;
        if (auction == null) {
            return score;
        }
        if (auction.getAuctionId() != null) {
            score += auctionRank.getOrDefault(auction.getAuctionId(), 0) * 20;
        }
        if (auction.getItem() != null) {
            score += categoryWeights.getOrDefault(auction.getItem().getCategory(), 0) * 12;
        }
        score += Math.min(getBidCount(auction), 10);
        return score;
    }

    private int getBidCount(Auction auction) {
        return auction != null && auction.getBidIds() != null ? auction.getBidIds().size() : 0;
    }

    private void setupEndingSoonControls() {
        if (prevEndingBtn != null) {
            prevEndingBtn.setOnAction(event -> scrollEndingSoon(-1));
        }
        if (nextEndingBtn != null) {
            nextEndingBtn.setOnAction(event -> scrollEndingSoon(1));
        }
        if (endingSoonScroll != null) {
            endingSoonScroll.setHvalue(0);
            endingSoonScroll.hvalueProperty().addListener((obs, oldValue, newValue) -> updateEndingSoonButtons());
        }
    }

    private boolean isVisibleAuction(Auction auction) {
        if (auction == null || auction.getStatus() == null) {
            return false;
        }
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
    }

    private void scrollEndingSoon(int direction) {
        if (endingSoonScroll == null) {
            return;
        }
        double step = 0.38;
        double next = Math.max(0, Math.min(1, endingSoonScroll.getHvalue() + direction * step));
        endingSoonScroll.setHvalue(next);
    }

    private void updateEndingSoonButtons() {
        if (prevEndingBtn == null || nextEndingBtn == null || endingSoonHBox == null) {
            return;
        }
        boolean hasOverflow = endingSoonHBox.getChildren().size() > 4;
        prevEndingBtn.setVisible(hasOverflow);
        prevEndingBtn.setManaged(hasOverflow);
        nextEndingBtn.setVisible(hasOverflow);
        nextEndingBtn.setManaged(hasOverflow);
        prevEndingBtn.toFront();
        nextEndingBtn.toFront();
    }

    private void applyCompactCardSize(VBox card) {
        double cardWidth = 218;
        double contentWidth = 198;
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(7);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #eef2f7; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 3); -fx-padding: 10;");
        card.setMinWidth(cardWidth);
        card.setPrefWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setMinHeight(336);
        card.setPrefHeight(336);
        card.setMaxHeight(336);

        javafx.scene.Node imageContainer = card.lookup("#imageContainer");
        if (imageContainer instanceof Region region) {
            region.setMinSize(188, 188);
            region.setPrefSize(188, 188);
            region.setMaxSize(188, 188);
            region.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
            VBox.setMargin(region, new javafx.geometry.Insets(0, 0, 0, 0));
            if (region instanceof StackPane stackPane) {
                stackPane.setAlignment(Pos.CENTER);
            }
        }

        javafx.scene.Node productImage = card.lookup("#productImageView");
        if (productImage instanceof ImageView imageView) {
            imageView.setFitWidth(188);
            imageView.setFitHeight(188);
        }

        javafx.scene.Node productName = card.lookup("#productNameLabel");
        if (productName instanceof Label label) {
            setRegionWidth(label, contentWidth);
            label.setMinHeight(40);
            label.setPrefHeight(40);
            label.setMaxHeight(40);
            label.setStyle("-fx-text-fill: #113254; -fx-font-size: 13px; -fx-font-weight: bold;");
            VBox.setMargin(label, new javafx.geometry.Insets(4, 0, 0, 0));
        }

        
        resizeMetaRow(card, contentWidth, 25, 11, 11);

        javafx.scene.Node bidButton = card.lookup("#bidButton");
        if (bidButton instanceof Button button) {
            setRegionWidth(button, contentWidth);
            button.setMinHeight(28);
            button.setPrefHeight(28);
            button.setMaxHeight(28);
            button.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 14; -fx-cursor: hand; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0;");
        }
    }

    private void applySuggestionCardSize(VBox card) {
        double cardWidth = 218;
        double contentWidth = 198;
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(7);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #eef2f7; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 3); -fx-padding: 10;");

        card.setMinWidth(cardWidth);
        card.setPrefWidth(cardWidth);
        card.setMaxWidth(cardWidth);
        card.setMinHeight(336);
        card.setPrefHeight(336);
        card.setMaxHeight(336);

        javafx.scene.Node imageContainer = card.lookup("#imageContainer");
        if (imageContainer instanceof Region region) {
            region.setMinSize(188, 188);
            region.setPrefSize(188, 188);
            region.setMaxSize(188, 188);
            region.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
            VBox.setMargin(region, new javafx.geometry.Insets(0, 0, 0, 0));
            if (region instanceof StackPane stackPane) {
                stackPane.setAlignment(Pos.CENTER);
            }
        }

        javafx.scene.Node productImage = card.lookup("#productImageView");
        if (productImage instanceof ImageView imageView) {
            imageView.setFitWidth(188);
            imageView.setFitHeight(188);
        }

        javafx.scene.Node productName = card.lookup("#productNameLabel");
        if (productName instanceof Label label) {
            setRegionWidth(label, contentWidth);
            label.setMinHeight(40);
            label.setPrefHeight(40);
            label.setMaxHeight(40);
            label.setStyle("-fx-text-fill: #113254; -fx-font-size: 13px; -fx-font-weight: bold;");
            VBox.setMargin(label, new javafx.geometry.Insets(4, 0, 0, 0));
        }

        resizeMetaRow(card, contentWidth, 25, 11, 11);

        javafx.scene.Node bidButton = card.lookup("#bidButton");
        if (bidButton instanceof Button button) {
            setRegionWidth(button, contentWidth);
            button.setMinHeight(28);
            button.setPrefHeight(28);
            button.setMaxHeight(28);
            button.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 14; -fx-cursor: hand; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0;");
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

    private VBox createEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(140);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-padding: 24;");

        Label title = new Label("Ch\u01b0a c\u00f3 phi\u00ean \u0111\u1ea5u gi\u00e1 ph\u00f9 h\u1ee3p");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("C\u00e1c phi\u00ean \u0111ang di\u1ec5n ra s\u1ebd hi\u1ec3n th\u1ecb \u1edf \u0111\u00e2y.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private VBox createAuctionCard(Auction auction) {
        try {
            
            String cardPath = "/client/view/AuctionCard.fxml";
            URL resource = getClass().getResource(cardPath);

            
            if (resource == null) {
                resource = getClass().getResource("/fxml/BidderView/AuctionCard.fxml");
            }

            if (resource == null) {
                ClientExceptionHandler.handle(ClientErrorType.NAVIGATION, "Load auction card in dashboard", new IllegalStateException("/fxml/BidderView/AuctionCard.fxml"));
                return new VBox();
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();

            
            client.controller.AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                if (homeScreenController != null) {
                    cardController.setHomeScreenController(homeScreenController);
                }
                cardController.setAuctionData(auction);
            }

            
            if (cardNode instanceof VBox) {
                return (VBox) cardNode;
            } else {
                
                VBox wrapper = new VBox();
                wrapper.getChildren().add(cardNode);
                return wrapper;
            }

        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION, "Load auction card in dashboard", e);
            return new VBox(); 
        }
    }

    // ==============================================================
    
    // ==============================================================

    private void setupBanner() {
        if (bannerImageView == null) {
            return;
        }
        Rectangle clip = new Rectangle();
        clip.setArcWidth(30.0);
        clip.setArcHeight(30.0);
        clip.widthProperty().bind(bannerImageView.fitWidthProperty());
        clip.heightProperty().bind(bannerImageView.fitHeightProperty());
        bannerImageView.setClip(clip);

        bannerImages = new ArrayList<>();
        try {
            URL banner1 = getClass().getResource("/CSS/dashboard.png");
            if (banner1 != null) bannerImages.add(new Image(banner1.toExternalForm()));

            URL banner2 = getClass().getResource("/CSS/flashBid.png");
            if (banner2 != null) bannerImages.add(new Image(banner2.toExternalForm()));
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.DATA, "Load dashboard banner images", e);
        }

        if (!bannerImages.isEmpty() && bannerImages.get(0) != null) {
            bannerImageView.setImage(bannerImages.get(0));
        }

        if (prevBannerBtn != null) {
            prevBannerBtn.setOnAction(event -> showPreviousImage());
        }
        if (nextBannerBtn != null) {
            nextBannerBtn.setOnAction(event -> showNextImage());
        }
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

    public void refreshAuctions() {
        loadAuctionsFromServer();
    }

    @FXML
    private void openCollectiblesCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.ART);
        openCategory("/fxml/BidderView/CategoryCollectibles.fxml");
    }

    @FXML
    private void openElectronicsCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.ELECTRONICS);
        openCategory("/fxml/BidderView/CategoryElectronics.fxml");
    }

    @FXML
    private void openFashionCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.FASHION);
        openCategory("/fxml/BidderView/CategoryFashion.fxml");
    }

    @FXML
    private void openJewelryCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.JEWELRY);
        openCategory("/fxml/BidderView/CategoryJewelry.fxml");
    }

    @FXML
    private void openVehiclesCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.VEHICLE);
        openCategory("/fxml/BidderView/CategoryVehicles.fxml");
    }

    @FXML
    private void openOtherCategory() {
        RecommendationTracker.recordCategoryClick(ItemCategory.OTHER);
        openCategory("/fxml/BidderView/CategoryOther.fxml");
    }

    private void openCategory(String fxmlPath) {
        if (homeScreenController != null) {
            homeScreenController.loadCategoryView(fxmlPath);
        } else {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION,
                    "Open category without HomeScreenController",
                    new IllegalStateException(fxmlPath));
        }
    }
}
