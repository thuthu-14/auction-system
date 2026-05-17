package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.util.RecommendationTracker;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.DashboardClientService;
import common.AuctionStatus;
import common.ItemCategory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import server.model.Auction;
import server.model.Item;
import server.model.User;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CategoryController {

    @FXML private FlowPane productsFlowPane;
    @FXML private FlowPane electronicsFlowPane;
    @FXML private FlowPane fashionFlowPane;
    @FXML private FlowPane jewelryFlowPane;
    @FXML private FlowPane vehiclesFlowPane;
    @FXML private FlowPane otherFlowPane;
    @FXML private Button backBtn;
    @FXML private Button heroBidButton;
    @FXML private Label heroTitleLabel;
    @FXML private Label heroDescriptionLabel;
    @FXML private ImageView heroImageView;
    @FXML private Label heroIconLabel;

    private final DashboardClientService dashboardClientService = new DashboardClientService();
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeScreenController;
    private Auction heroAuction;

    @FXML
    public void initialize() {
        if (backBtn != null) {
            backBtn.setOnAction(event -> handleBack());
        }
        if (heroBidButton != null) {
            heroBidButton.setOnAction(event -> handleHeroBid());
        }
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

        new Thread(() -> {
            try {
                List<Auction> auctions = dashboardClientService.fetchAllAuctions(socket);
                Platform.runLater(() -> renderAuctions(auctions));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Load category auctions", e);
            }
        }).start();
    }

    private void renderAuctions(List<Auction> auctions) {
        FlowPane flowPane = getActiveFlowPane();
        ItemCategory category = getCurrentCategory();
        if (flowPane == null || category == null) {
            return;
        }

        flowPane.getChildren().clear();
        List<Auction> filteredAuctions = auctions == null ? List.of() : auctions.stream()
                .filter(auction -> auction != null && auction.getItem() != null)
                .filter(this::isVisibleAuction)
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .filter(auction -> category == auction.getItem().getCategory())
                .sorted(Comparator.comparingLong(Auction::getEndTime))
                .toList();

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
            heroDescriptionLabel.setText("Gia hien tai: "
                    + VND_FORMATTER.format(Math.round(auction.getCurrentPrice()))
                    + " d - "
                    + getBidCount(auction)
                    + " luot dat gia");
            heroDescriptionLabel.setTextFill(Color.web(getHeroDescriptionColor()));
            heroDescriptionLabel.setStyle("-fx-text-fill: " + getHeroDescriptionColor() + "; -fx-font-size: 15px;");
        }
        updateHeroImage(auction.getItem());
    }

    private void updateHeroImage(Item item) {
        if (heroImageView == null || item == null || item.getImages() == null || item.getImages().isEmpty()) {
            showHeroIcon();
            return;
        }

        try {
            String rawPath = item.getImages().get(0);
            Image image = null;
            if (rawPath != null && (rawPath.startsWith("file:") || rawPath.startsWith("http"))) {
                image = new Image(rawPath, 320, 320, true, true);
            } else if (rawPath != null) {
                File file = resolveImageFile(rawPath);
                if (file.exists()) {
                    image = new Image(file.toURI().toURL().toString(), 320, 320, true, true);
                }
            }

            if (image == null || image.isError()) {
                showHeroIcon();
                return;
            }

            heroImageView.setImage(image);
            heroImageView.setVisible(true);
            heroImageView.setManaged(true);
            if (heroIconLabel != null) {
                heroIconLabel.setVisible(false);
                heroIconLabel.setManaged(false);
            }
        } catch (Exception e) {
            showHeroIcon();
            ClientExceptionHandler.handle(ClientErrorType.DATA, "Load category hero image", e);
        }
    }

    private File resolveImageFile(String rawPath) {
        String normalizedPath = rawPath.replace("\\", "/");
        File file = new File(normalizedPath);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), normalizedPath);
            if (!file.exists() && normalizedPath.startsWith("uploads/")) {
                file = new File(System.getProperty("user.dir"), "data/" + normalizedPath);
            }
        }
        return file;
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

    private boolean isVisibleAuction(Auction auction) {
        if (auction == null || auction.getStatus() == null) {
            return false;
        }
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
    }

    private String getHeroDescriptionColor() {
        ItemCategory category = getCurrentCategory();
        if (category == ItemCategory.ART) return "#78350f";
        if (category == ItemCategory.ELECTRONICS) return "#1e3a8a";
        if (category == ItemCategory.FASHION) return "#881337";
        if (category == ItemCategory.JEWELRY) return "#581c87";
        if (category == ItemCategory.VEHICLE) return "#14532d";
        if (category == ItemCategory.OTHER) return "#115e59";
        return "#374151";
    }

    private VBox createAuctionCard(Auction auction) {
        try {
            URL resource = getClass().getResource("/fxml/BidderView/AuctionCard.fxml");
            if (resource == null) {
                ClientExceptionHandler.handle(ClientErrorType.NAVIGATION,
                        "Load auction card in category",
                        new IllegalStateException("/fxml/BidderView/AuctionCard.fxml"));
                return null;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();
            AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                cardController.setAuctionData(auction);
                if (homeScreenController != null) {
                    cardController.setHomeScreenController(homeScreenController);
                }
            }

            if (cardNode instanceof VBox box) {
                return box;
            }
            VBox wrapper = new VBox(cardNode);
            wrapper.setPrefWidth(280);
            return wrapper;
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION, "Load auction card in category", e);
            return null;
        }
    }

    private VBox createEmptyState() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(560);
        box.setMinHeight(140);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-padding: 24;");

        Label title = new Label("Chua co phien dau gia phu hop");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Cac phien dang dien ra se hien thi o day.");
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
}
