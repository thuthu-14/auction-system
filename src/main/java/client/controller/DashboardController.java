package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.DashboardClientService;
import common.AuctionStatus;
import server.model.Auction;
import server.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import util.LoggerUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // --- UI Components Mapping tÃ¡Â»Â« FXML ---
    @FXML private ImageView bannerImageView;
    @FXML private ScrollPane dashboardRoot;
    @FXML private Button prevBannerBtn;
    @FXML private Button nextBannerBtn;
    @FXML private GridPane endingSoonGrid;
    @FXML private HBox suggestedHBox;

    private List<Image> bannerImages;
    private int currentImageIndex = 0;

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeScreenController;
    private final DashboardClientService dashboardClientService = new DashboardClientService();

    /**
     * NhÃ¡ÂºÂ­n dÃ¡Â»Â¯ liÃ¡Â»â€¡u ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng vÃƒÂ  socket.
     */
    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        System.out.println("Dashboard kÃ¡ÂºÂ¿t nÃ¡Â»â€˜i thÃƒÂ nh cÃƒÂ´ng: " + (user != null ? user.getUsername() : "Guest"));

        // Ã†Â¯u tiÃƒÂªn load dÃ¡Â»Â¯ liÃ¡Â»â€¡u ngay khi cÃƒÂ³ socket
        loadAuctionsFromServer();
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupBanner();
        Platform.runLater(this::forceDashboardTextColors);
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

            if ("productNameLabel".equals(id)) {
                applyLabelColor(label, "#113254");
            } else if ("timerLabel".equals(id)) {
                applyLabelColor(label, text.contains("\u0110\u00e3") || text.contains("Ãƒâ€ž") ? "#6b7280" : "#e53e3e");
            } else if ("priceLabel".equals(id)) {
                applyLabelColor(label, "#111827");
            } else if (text.contains("V\u1eeba xong") || text.contains("VÃƒÂ¡") || text.contains("xong")) {
                applyLabelColor(label, "#9ca3af");
            } else if (text.contains("H\u00e0ng ng\u00e0n")
                    || text.contains("H\u1ec7 th\u1ed1ng")
                    || text.contains("HÃƒÆ’")
                    || text.contains("thÃƒÂ¡")) {
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

    /**
     * GÃ¡Â»Â­i yÃƒÂªu cÃ¡ÂºÂ§u lÃ¡ÂºÂ¥y danh sÃƒÂ¡ch Ã„â€˜Ã¡ÂºÂ¥u giÃƒÂ¡ mÃ¡Â»â€ºi nhÃ¡ÂºÂ¥t tÃ¡Â»Â« Server (Ã„ÂÃƒÆ’ SÃ¡Â»Â¬A LÃ¡Â»â€“I CRASH)
     */
    public void loadAuctionsFromServer() {
        ClientSocket socket = (clientSocket != null) ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        if (socket == null) return;

        new Thread(() -> {
            try {
                List<Auction> auctions = dashboardClientService.fetchAllAuctions(socket);
                Platform.runLater(() -> renderProductCards(auctions));
            } catch (Exception e) {
                LoggerUtil.error("Dashboard load failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * XÃƒÂ³a cÃƒÂ¡c mÃ¡ÂºÂ«u cÃ…Â© vÃƒÂ  vÃ¡ÂºÂ½ lÃ¡ÂºÂ¡i cÃƒÂ¡c thÃ¡ÂºÂ» sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m mÃ¡Â»â€ºi
     */
    private void renderProductCards(List<Auction> auctions) {
        if (endingSoonGrid == null) return;

        endingSoonGrid.getChildren().clear();
        List<Auction> safeAuctions = auctions != null ? auctions : List.of();

        int col = 0;
        int row = 0;
        int visibleCount = 0;

        for (Auction auction : safeAuctions) {
            if (auction == null || auction.getItem() == null || auction.getStatus() == null) {
                continue;
            }
            if (auction.getStatus() != AuctionStatus.CLOSED && auction.getTimeRemainingSeconds() > 0) {

                VBox card = createAuctionCard(auction);

                if (card != null) {
                    card.setMinWidth(280);
                    card.setPrefWidth(280);
                    card.setMaxWidth(280);
                    GridPane.setFillWidth(card, false);
                    GridPane.setHalignment(card, HPos.LEFT);
                    endingSoonGrid.add(card, col, row);
                    visibleCount++;
                    col++;
                    if (col > 1) { // ThiÃ¡ÂºÂ¿t kÃ¡ÂºÂ¿ 2 cÃ¡Â»â„¢t
                        col = 0;
                        row++;
                    }
                }
            }
        }

        if (visibleCount == 0) {
            endingSoonGrid.add(createEmptyState(), 0, 0, 2, 1);
        }
        forceDashboardTextColors();
        Platform.runLater(this::forceDashboardTextColors);
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

    /**
     * TÃ¡ÂºÂ¡o mÃ¡Â»â„¢t ÃƒÂ´ sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m BÃ¡ÂºÂ°NG CÃƒÂCH GÃ¡Â»Å’I FILE FXML (Ã„ÂÃƒÆ’ SÃ¡Â»Â¬A Ã„ÂÃ¡Â»â€š NÃƒÅ¡T BÃ¡ÂºÂ¤M CÃƒâ€œ TÃƒÂC DÃ¡Â»Â¤NG)
     */
    private VBox createAuctionCard(Auction auction) {
        try {
            // ThÃ¡Â»Â­ Ã„â€˜Ã†Â°Ã¡Â»Âng dÃ¡ÂºÂ«n 1
            String cardPath = "/client/view/AuctionCard.fxml";
            URL resource = getClass().getResource(cardPath);

            // ThÃ¡Â»Â­ Ã„â€˜Ã†Â°Ã¡Â»Âng dÃ¡ÂºÂ«n 2 nÃ¡ÂºÂ¿u Ã„â€˜Ã†Â°Ã¡Â»Âng dÃ¡ÂºÂ«n 1 sai
            if (resource == null) {
                resource = getClass().getResource("/fxml/AuctionCard.fxml");
            }

            if (resource == null) {
                System.err.println("Ã¢ÂÅ’ LÃ¡Â»â€“I: KhÃƒÂ´ng tÃƒÂ¬m thÃ¡ÂºÂ¥y file AuctionCard.fxml!");
                return new VBox();
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();

            // LÃ¡ÂºÂ¥y Controller vÃƒÂ  truyÃ¡Â»Ân dÃ¡Â»Â¯ liÃ¡Â»â€¡u sang Card
            client.controller.AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                cardController.setAuctionData(auction);
                if (homeScreenController != null) {
                    cardController.setHomeScreenController(homeScreenController);
                }
            }

            // NÃ¡ÂºÂ¿u root cÃ¡Â»Â§a AuctionCard.fxml lÃƒÂ  VBox thÃƒÂ¬ ÃƒÂ©p kiÃ¡Â»Æ’u trÃ¡Â»Â±c tiÃ¡ÂºÂ¿p
            if (cardNode instanceof VBox) {
                return (VBox) cardNode;
            } else {
                // NÃ¡ÂºÂ¿u lÃƒÂ  AnchorPane hoÃ¡ÂºÂ·c thÃ¡Â»Â© khÃƒÂ¡c, gÃƒÂ³i nÃƒÂ³ vÃƒÂ o mÃ¡Â»â„¢t VBox Ã„â€˜Ã¡Â»Æ’ khÃƒÂ´ng bÃ¡Â»â€¹ lÃ¡Â»â€”i hÃƒÂ m
                VBox wrapper = new VBox();
                wrapper.getChildren().add(cardNode);
                return wrapper;
            }

        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ LÃ¡Â»â€“I LOAD THÃ¡ÂºÂº AUCTION CARD BÃƒÅ N TRONG DASHBOARD: " + e.getMessage());
            e.printStackTrace();
            return new VBox(); // TrÃ¡ÂºÂ£ vÃ¡Â»Â Vbox rÃ¡Â»â€”ng Ã„â€˜Ã¡Â»Æ’ khÃƒÂ´ng bÃ¡Â»â€¹ chÃ¡ÂºÂ¿t Grid
        }
    }

    // ==============================================================
    // CÃƒÂC HÃƒâ‚¬M XÃ¡Â»Â¬ LÃƒÂ Ã¡ÂºÂ¢NH BANNER BÃƒÅ N TRÃƒÅ N (GIÃ¡Â»Â® NGUYÃƒÅ N NHÃ†Â¯ CÃ…Â¨ CÃ¡Â»Â¦A BÃ¡ÂºÂ N)
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
            System.out.println("LÃ¡Â»â€”i load Ã¡ÂºÂ£nh banner: " + e.getMessage());
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
}
