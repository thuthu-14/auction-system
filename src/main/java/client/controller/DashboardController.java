package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.logic.DashboardLogic;
import client.util.RecommendationTracker;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuctionQueryClient;
import client.service.DashboardClientService;
import client.ui.AuctionCardFactory;
import client.ui.DashboardViewFactory;
import common.AuctionStatus;
import common.ItemCategory;
import server.model.Auction;
import server.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    
    @FXML private ImageView bannerImageView;
    @FXML private ScrollPane dashboardRoot;
    @FXML private Button prevBannerBtn;
    @FXML private Button nextBannerBtn;
    @FXML private ScrollPane endingSoonScroll;
    @FXML private HBox endingSoonHBox;
    @FXML private Button prevEndingBtn;
    @FXML private Button nextEndingBtn;
    @FXML private ScrollPane suggestedScroll;
    @FXML private HBox suggestedHBox;
    @FXML private Button prevSuggestedBtn;
    @FXML private Button nextSuggestedBtn;

    private List<Image> bannerImages;
    private int currentImageIndex = 0;
    private boolean bannerAnimating = false;

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeScreenController;
    private final AuctionQueryClient auctionQueryClient;
    private final DashboardLogic dashboardLogic;
    private final DashboardViewFactory dashboardViewFactory;

    public DashboardController() {
        this(new DashboardClientService(), new DashboardLogic(), new AuctionCardFactory());
    }

    DashboardController(AuctionQueryClient auctionQueryClient,
                        DashboardLogic dashboardLogic,
                        AuctionCardFactory auctionCardFactory) {
        this.auctionQueryClient = auctionQueryClient;
        this.dashboardLogic = dashboardLogic;
        this.dashboardViewFactory = new DashboardViewFactory(auctionCardFactory);
    }

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
        setupSuggestedControls();
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

        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Auction> auctions = auctionQueryClient.fetchAllAuctions(socket);
                Platform.runLater(() -> renderProductCards(auctions));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Dashboard load auctions", e);
            }
        });
    }

    private void renderProductCards(List<Auction> auctions) {
        if (endingSoonHBox == null) return;

        endingSoonHBox.getChildren().clear();
        List<Auction> safeAuctions = auctions != null ? auctions : List.of();

        List<Auction> endingSoonAuctions = dashboardLogic.endingSoonAuctions(safeAuctions, 10);

        for (Auction auction : endingSoonAuctions) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                dashboardViewFactory.applyCompactCardSize(card);
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

        List<String> recentAuctionIds = RecommendationTracker.getRecentAuctionIds();
        Map<ItemCategory, Integer> categoryWeights = RecommendationTracker.getCategoryWeights();
        List<Auction> suggestions = dashboardLogic.suggestedAuctions(auctions,
                recentAuctionIds,
                categoryWeights,
                RecommendationTracker.hasSignals(),
                8);

        for (Auction auction : suggestions) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                dashboardViewFactory.applySuggestionCardSize(card);
                suggestedHBox.getChildren().add(card);
            }
        }
        updateSuggestedButtons();
    }

    private int recommendationScore(Auction auction, Map<ItemCategory, Integer> categoryWeights, Map<String, Integer> auctionRank) {
        return dashboardLogic.recommendationScore(auction, categoryWeights, auctionRank);
    }

    private int getBidCount(Auction auction) {
        return dashboardLogic.bidCount(auction);
    }

    private void setupEndingSoonControls() {
        if (prevEndingBtn != null) {
            prevEndingBtn.setFocusTraversable(false);
            prevEndingBtn.setOnAction(event -> runWithoutVerticalScrollJump(() -> scrollEndingSoon(-1)));
        }
        if (nextEndingBtn != null) {
            nextEndingBtn.setFocusTraversable(false);
            nextEndingBtn.setOnAction(event -> runWithoutVerticalScrollJump(() -> scrollEndingSoon(1)));
        }
        if (endingSoonScroll != null) {
            endingSoonScroll.setHvalue(0);
            endingSoonScroll.hvalueProperty().addListener((obs, oldValue, newValue) -> updateEndingSoonButtons());
        }
    }

    private void setupSuggestedControls() {
        if (prevSuggestedBtn != null) {
            prevSuggestedBtn.setFocusTraversable(false);
            prevSuggestedBtn.setOnAction(event -> runWithoutVerticalScrollJump(() -> scrollSuggested(-1)));
        }
        if (nextSuggestedBtn != null) {
            nextSuggestedBtn.setFocusTraversable(false);
            nextSuggestedBtn.setOnAction(event -> runWithoutVerticalScrollJump(() -> scrollSuggested(1)));
        }
        if (suggestedScroll != null) {
            suggestedScroll.setHvalue(0);
            suggestedScroll.hvalueProperty().addListener((obs, oldValue, newValue) -> updateSuggestedButtons());
        }
    }

    private boolean isVisibleAuction(Auction auction) {
        return dashboardLogic.isVisibleAuction(auction);
    }

    private void scrollEndingSoon(int direction) {
        if (endingSoonScroll == null) {
            return;
        }
        scrollPaneSmoothly(endingSoonScroll, direction);
    }

    private void scrollSuggested(int direction) {
        if (suggestedScroll == null) {
            return;
        }
        scrollPaneSmoothly(suggestedScroll, direction);
    }

    private void scrollPaneSmoothly(ScrollPane scrollPane, int direction) {
        double step = 0.28;
        double next = Math.max(0, Math.min(1, scrollPane.getHvalue() + direction * step));
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(720),
                        new KeyValue(scrollPane.hvalueProperty(), next, Interpolator.SPLINE(0.22, 0.61, 0.36, 1.0)))
        );
        timeline.play();
    }

    private void runWithoutVerticalScrollJump(Runnable action) {
        if (action == null) {
            return;
        }
        if (dashboardRoot == null) {
            action.run();
            return;
        }

        double currentVvalue = dashboardRoot.getVvalue();
        action.run();
        dashboardRoot.setVvalue(currentVvalue);
        Platform.runLater(() -> dashboardRoot.setVvalue(currentVvalue));
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

    private void updateSuggestedButtons() {
        if (prevSuggestedBtn == null || nextSuggestedBtn == null || suggestedHBox == null) {
            return;
        }
        boolean hasOverflow = suggestedHBox.getChildren().size() > 4;
        prevSuggestedBtn.setVisible(hasOverflow);
        prevSuggestedBtn.setManaged(hasOverflow);
        nextSuggestedBtn.setVisible(hasOverflow);
        nextSuggestedBtn.setManaged(hasOverflow);
        prevSuggestedBtn.toFront();
        nextSuggestedBtn.toFront();
    }

    private VBox createEmptyState() {
        return dashboardViewFactory.createEmptyState();
    }

    private VBox createAuctionCard(Auction auction) {
        return dashboardViewFactory.createAuctionCard(auction, homeScreenController, "dashboard");
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
            prevBannerBtn.setFocusTraversable(false);
            prevBannerBtn.setOnAction(event -> runWithoutVerticalScrollJump(this::showPreviousImage));
        }
        if (nextBannerBtn != null) {
            nextBannerBtn.setFocusTraversable(false);
            nextBannerBtn.setOnAction(event -> runWithoutVerticalScrollJump(this::showNextImage));
        }
    }

    private void showNextImage() {
        if (bannerImages == null || bannerImages.isEmpty()) return;
        showBannerImage((currentImageIndex + 1) % bannerImages.size(), 1);
    }

    private void showPreviousImage() {
        if (bannerImages == null || bannerImages.isEmpty()) return;
        showBannerImage((currentImageIndex - 1 + bannerImages.size()) % bannerImages.size(), -1);
    }

    private void showBannerImage(int nextIndex, int direction) {
        if (bannerImages == null || bannerImages.isEmpty() || bannerImageView == null || bannerAnimating) {
            return;
        }
        if (nextIndex == currentImageIndex) {
            return;
        }

        bannerAnimating = true;
        setBannerButtonsDisabled(true);

        double slideDistance = 46.0 * direction;
        bannerImageView.setOpacity(0.0);
        bannerImageView.setTranslateX(slideDistance);
        currentImageIndex = nextIndex;
        bannerImageView.setImage(bannerImages.get(currentImageIndex));

        TranslateTransition slide = new TranslateTransition(Duration.millis(320), bannerImageView);
        slide.setFromX(slideDistance);
        slide.setToX(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(260), bannerImageView);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ParallelTransition transition = new ParallelTransition(slide, fade);
        transition.setOnFinished(event -> {
            bannerImageView.setTranslateX(0.0);
            bannerImageView.setOpacity(1.0);
            bannerAnimating = false;
            setBannerButtonsDisabled(false);
        });
        transition.play();
    }

    private void setBannerButtonsDisabled(boolean disabled) {
        if (prevBannerBtn != null) {
            prevBannerBtn.setDisable(disabled);
        }
        if (nextBannerBtn != null) {
            nextBannerBtn.setDisable(disabled);
        }
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
