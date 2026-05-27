package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.logic.AuctionFilterStrategy;
import client.logic.AuctionFilters;
import client.logic.CategorySortOption;
import client.ui.AuctionCardFactory;
import client.util.RecommendationTracker;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuctionQueryClient;
import client.service.DashboardClientService;
import client.service.ImageDownloadService;
import common.ItemCategory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import server.model.Auction;
import server.model.Item;
import server.model.User;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryController {

    @FXML private ScrollPane categoryRoot;
    @FXML private FlowPane productsFlowPane;
    @FXML private FlowPane electronicsFlowPane;
    @FXML private FlowPane fashionFlowPane;
    @FXML private FlowPane jewelryFlowPane;
    @FXML private FlowPane vehiclesFlowPane;
    @FXML private FlowPane otherFlowPane;
    @FXML private Button backBtn;
    @FXML private Button heroBidButton;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label heroTitleLabel;
    @FXML private Label heroDescriptionLabel;
    @FXML private ImageView heroImageView;
    @FXML private Label heroIconLabel;
    @FXML private Label productCountLabel;

    private final AuctionQueryClient auctionQueryClient;
    private final AuctionCardFactory auctionCardFactory;
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final double HERO_IMAGE_SIZE = 190.0;
    private static final double HERO_IMAGE_RADIUS = 24.0;
    private static final Map<ItemCategory, CategorySortOption> DEFAULT_SORT_BY_CATEGORY = createDefaultSortByCategory();
    private static final Map<ItemCategory, String> HERO_DESCRIPTION_COLOR_BY_CATEGORY = createHeroDescriptionColors();

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeScreenController;
    private Auction heroAuction;
    private List<Auction> currentAuctions = List.of();

    public CategoryController() {
        this(new DashboardClientService(), new AuctionCardFactory());
    }

    CategoryController(AuctionQueryClient auctionQueryClient, AuctionCardFactory auctionCardFactory) {
        this.auctionQueryClient = auctionQueryClient;
        this.auctionCardFactory = auctionCardFactory;
    }

    @FXML
    public void initialize() {
        if (backBtn != null) {
            backBtn.setOnAction(event -> handleBack());
        }
        if (heroBidButton != null) {
            heroBidButton.setOnAction(event -> handleHeroBid());
        }
        configureSortCombo();
        installHeroImageClip();
        Platform.runLater(this::forceCategoryTextColors);
        Platform.runLater(this::loadAuctionsFromServer);
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        loadAuctionsFromServer();
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    private void loadAuctionsFromServer() {
        FlowPane flowPane = getActiveFlowPane();
        ClientSocket socket = clientSocket != null ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        if (flowPane == null || socket == null) {
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Auction> auctions = auctionQueryClient.fetchAllAuctions(socket);
                Platform.runLater(() -> {
                    currentAuctions = auctions == null ? List.of() : auctions;
                    renderAuctions(currentAuctions);
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Load category auctions", e);
            }
        });
    }

    private void renderAuctions(List<Auction> auctions) {
        currentAuctions = auctions == null ? List.of() : auctions;
        FlowPane flowPane = getActiveFlowPane();
        ItemCategory category = getCurrentCategory();
        if (flowPane == null || category == null) {
            return;
        }

        flowPane.getChildren().clear();
        AuctionFilterStrategy filter = AuctionFilters.visibleActive()
                .and(AuctionFilters.hasTimeRemaining())
                .and(AuctionFilters.category(category));
        List<Auction> filteredAuctions = auctions == null ? List.of() : auctions.stream()
                .filter(filter::accepts)
                .sorted(getAuctionComparator())
                .toList();
        updateProductCountLabel(countCurrentCategoryAuctions(auctions, filter));

        if (filteredAuctions.isEmpty()) {
            flowPane.getChildren().add(createEmptyState());
            return;
        }

        heroAuction = filteredAuctions.stream()
                .max(Comparator.comparingInt(this::getBidCount)
                        .thenComparingDouble(Auction::getCurrentPrice)
                        .thenComparingLong(Auction::getEndTime))
                .orElse(filteredAuctions.get(0));
        updateHero(heroAuction);
        for (Auction auction : filteredAuctions) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                flowPane.getChildren().add(card);
            }
        }
    }

    private void configureSortCombo() {
        if (sortCombo == null) {
            return;
        }
        sortCombo.getItems().setAll(CategorySortOption.labels());
        sortCombo.getSelectionModel().select(getDefaultSortOption());
        sortCombo.setOnAction(event -> renderAuctions(currentAuctions));
    }

    private String getDefaultSortOption() {
        return DEFAULT_SORT_BY_CATEGORY
                .getOrDefault(getCurrentCategory(), CategorySortOption.ENDING_SOON)
                .label();
    }

    private Comparator<Auction> getAuctionComparator() {
        String selected = sortCombo != null && sortCombo.getValue() != null
                ? sortCombo.getValue()
                : getDefaultSortOption();
        return CategorySortOption.fromLabel(selected).comparator(this::getBidCount);
    }

    private void updateHero(Auction auction) {
        if (auction == null || auction.getItem() == null) {
            return;
        }
        if (heroTitleLabel != null) {
            heroTitleLabel.setText(auction.getItem().getName());
            heroTitleLabel.setTextFill(Color.web("#1a1a1a"));
            heroTitleLabel.setStyle("-fx-text-fill: #1a1a1a; -fx-font-weight: bold; -fx-font-size: 32px;");
        }
        if (heroDescriptionLabel != null) {
            heroDescriptionLabel.setText("Giá hiện tại: "
                    + VND_FORMATTER.format(Math.round(auction.getCurrentPrice()))
                    + " d - "
                    + getBidCount(auction)
                    + " lượt đặt giá");
            heroDescriptionLabel.setTextFill(Color.web(getHeroDescriptionColor()));
            heroDescriptionLabel.setStyle("-fx-text-fill: " + getHeroDescriptionColor() + "; -fx-font-size: 15px;");
        }
        updateHeroImage(auction.getItem());
        forceCategoryTextColors();
    }

    private void forceCategoryTextColors() {
        if (heroTitleLabel != null) {
            applyLabelColor(heroTitleLabel, "#1a1a1a");
        }
        if (heroDescriptionLabel != null) {
            applyLabelColor(heroDescriptionLabel, getHeroDescriptionColor());
        }
        if (heroIconLabel != null) {
            applyLabelColor(heroIconLabel, getHeroDescriptionColor());
        }

        if (categoryRoot == null) {
            return;
        }
        applyCategoryTextColors(categoryRoot.getContent());
    }

    private void applyCategoryTextColors(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof Label label) {
            if (label == heroTitleLabel || label == heroDescriptionLabel || label == heroIconLabel) {
                return;
            }

            String style = label.getStyle() == null ? "" : label.getStyle();
            String text = label.getText() == null ? "" : label.getText();
            if (style.contains("-fx-background-color")) {
                applyLabelColor(label, "#ffffff");
            } else if (text.contains("s\u1ea3n ph\u1ea9m") || text.contains("S\u1ea3n ph\u1ea9m")
                    || text.contains("Danh s\u00e1ch")) {
                applyLabelColor(label, "#718096");
            } else if (!text.isBlank()) {
                applyLabelColor(label, "#1a1a1a");
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyCategoryTextColors(child);
            }
        }
    }

    private void applyLabelColor(Label label, String color) {
        label.setTextFill(Color.web(color));
        label.setOpacity(1.0);

        String style = label.getStyle() == null ? "" : label.getStyle();
        style = style.replaceAll("-fx-text-fill\\s*:\\s*[^;]+;?", "").trim();
        if (!style.isEmpty() && !style.endsWith(";")) {
            style += ";";
        }
        label.setStyle(style + " -fx-text-fill: " + color + ";");
    }

    private void updateHeroImage(Item item) {
        if (heroImageView == null || item == null || item.getImages() == null || item.getImages().isEmpty()) {
            showHeroIcon();
            return;
        }

        String imageId = item.getImages().get(0);
        if (imageId == null || imageId.isBlank()) {
            showHeroIcon();
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = getActiveSocket();
                if (socket == null || !socket.isConnected()) {
                    Platform.runLater(this::showHeroIcon);
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes), HERO_IMAGE_SIZE * 2, HERO_IMAGE_SIZE * 2, true, true);
                Platform.runLater(() -> showHeroImage(image));
            } catch (Exception e) {
                Platform.runLater(this::showHeroIcon);
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Load category hero image", e);
            }
        });
    }

    private ClientSocket getActiveSocket() {
        if (clientSocket != null && clientSocket.isConnected()) {
            return clientSocket;
        }
        ClientSocket managerSocket = ConnectionManager.getInstance().getClientSocket();
        if (managerSocket != null && managerSocket.isConnected()) {
            clientSocket = managerSocket;
            return managerSocket;
        }
        return null;
    }

    private void showHeroImage(Image image) {
        if (image == null || image.isError()) {
            showHeroIcon();
            return;
        }
        heroImageView.setFitWidth(HERO_IMAGE_SIZE);
        heroImageView.setFitHeight(HERO_IMAGE_SIZE);
        heroImageView.setImage(image);
        heroImageView.setVisible(true);
        heroImageView.setManaged(true);
        if (heroIconLabel != null) {
            heroIconLabel.setVisible(false);
            heroIconLabel.setManaged(false);
        }
    }

    private void installHeroImageClip() {
        if (heroImageView == null) {
            return;
        }
        heroImageView.setFitWidth(HERO_IMAGE_SIZE);
        heroImageView.setFitHeight(HERO_IMAGE_SIZE);
        Rectangle clip = new Rectangle(HERO_IMAGE_SIZE, HERO_IMAGE_SIZE);
        clip.setArcWidth(HERO_IMAGE_RADIUS * 2);
        clip.setArcHeight(HERO_IMAGE_RADIUS * 2);
        heroImageView.setClip(clip);
    }

    private void showHeroIcon() {
        if (heroImageView != null) {
            heroImageView.setImage(null);
            heroImageView.setVisible(false);
            heroImageView.setManaged(false);
        }
        if (heroIconLabel != null) {
            heroIconLabel.setVisible(true);
            heroIconLabel.setManaged(true);
        }
    }

    private int getBidCount(Auction auction) {
        if (auction == null || auction.getBidIds() == null) {
            return 0;
        }
        return auction.getBidIds().size();
    }

    private int countCurrentCategoryAuctions(List<Auction> auctions, AuctionFilterStrategy filter) {
        if (auctions == null || filter == null) {
            return 0;
        }
        return (int) auctions.stream().filter(filter::accepts).count();
    }

    private void updateProductCountLabel(int count) {
        if (productCountLabel != null) {
            String currentText = productCountLabel.getText();
            String prefix = "Tất cả sản phẩm";
            int countStart = currentText != null ? currentText.lastIndexOf('(') : -1;
            if (countStart > 0) {
                prefix = currentText.substring(0, countStart).trim();
            }
            productCountLabel.setText(prefix + " (" + count + ")");
        }
    }

    private boolean isVisibleAuction(Auction auction) {
        return AuctionFilters.visibleActive().accepts(auction);
    }

    private String getHeroDescriptionColor() {
        return HERO_DESCRIPTION_COLOR_BY_CATEGORY.getOrDefault(getCurrentCategory(), "#374151");
    }

    private VBox createAuctionCard(Auction auction) {
        return auctionCardFactory.create(auction, homeScreenController, "category");
    }

    private VBox createEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(560);
        box.setMinHeight(140);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-padding: 24;");

        Label title = new Label("Chưa có phiên đấu giá phù hợp");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Các phiên đang diễn ra sẽ hiển thị ở đây.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private FlowPane getActiveFlowPane() {
        if (productsFlowPane != null) return productsFlowPane;
        if (electronicsFlowPane != null) return electronicsFlowPane;
        if (fashionFlowPane != null) return fashionFlowPane;
        if (jewelryFlowPane != null) return jewelryFlowPane;
        if (vehiclesFlowPane != null) return vehiclesFlowPane;
        return otherFlowPane;
    }

    private ItemCategory getCurrentCategory() {
        if (productsFlowPane != null) return ItemCategory.ART;
        if (electronicsFlowPane != null) return ItemCategory.ELECTRONICS;
        if (fashionFlowPane != null) return ItemCategory.FASHION;
        if (jewelryFlowPane != null) return ItemCategory.JEWELRY;
        if (vehiclesFlowPane != null) return ItemCategory.VEHICLE;
        if (otherFlowPane != null) return ItemCategory.OTHER;
        return null;
    }

    private void handleBack() {
        if (homeScreenController != null) {
            homeScreenController.loadDashboardView();
        }
    }

    private void handleHeroBid() {
        if (heroAuction != null && homeScreenController != null) {
            RecommendationTracker.recordAuctionClick(heroAuction);
            homeScreenController.loadAuctionDetailView(heroAuction);
        }
    }

    private static Map<ItemCategory, CategorySortOption> createDefaultSortByCategory() {
        Map<ItemCategory, CategorySortOption> options = new EnumMap<>(ItemCategory.class);
        options.put(ItemCategory.ART, CategorySortOption.NEWEST);
        options.put(ItemCategory.ELECTRONICS, CategorySortOption.HIGHEST_PRICE);
        options.put(ItemCategory.FASHION, CategorySortOption.NEWEST);
        options.put(ItemCategory.JEWELRY, CategorySortOption.MOST_BIDS);
        options.put(ItemCategory.OTHER, CategorySortOption.MOST_BIDS);
        return options;
    }

    private static Map<ItemCategory, String> createHeroDescriptionColors() {
        Map<ItemCategory, String> colors = new EnumMap<>(ItemCategory.class);
        colors.put(ItemCategory.ART, "#78350f");
        colors.put(ItemCategory.ELECTRONICS, "#1e3a8a");
        colors.put(ItemCategory.FASHION, "#881337");
        colors.put(ItemCategory.JEWELRY, "#581c87");
        colors.put(ItemCategory.VEHICLE, "#14532d");
        colors.put(ItemCategory.OTHER, "#115e59");
        return colors;
    }
}
