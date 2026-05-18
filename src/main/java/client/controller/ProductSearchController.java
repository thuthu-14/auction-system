package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.DashboardClientService;
import common.AuctionStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import server.model.Auction;
import server.model.Item;
import server.model.User;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProductSearchController {

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private FlowPane resultsFlowPane;

    private final DashboardClientService dashboardClientService = new DashboardClientService();
    private HomeScreenController homeScreenController;
    private User currentUser;
    private ClientSocket clientSocket;
    private String keyword = "";

    @FXML
    public void initialize() {
        showEmptyState("Nhập từ khóa trên thanh tìm kiếm để xem sản phẩm.");
    }

    public void setHomeScreenController(HomeScreenController homeScreenController) {
        this.homeScreenController = homeScreenController;
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

    public void search(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
        if (titleLabel != null) {
            titleLabel.setText(this.keyword.isBlank()
                    ? "Tìm kiếm sản phẩm"
                    : "Kết quả tìm kiếm: \"" + this.keyword + "\"");
        }
        if (this.keyword.isBlank()) {
            showEmptyState("Nhập từ khóa trên thanh tìm kiếm để xem sản phẩm.");
            return;
        }
        loadSearchResults();
    }

    private void loadSearchResults() {
        ClientSocket socket = getActiveSocket();
        if (resultsFlowPane == null || socket == null || !socket.isConnected()) {
            showEmptyState("Không thể tải dữ liệu do thiếu kết nối.");
            return;
        }

        setSummary("Đang tìm kiếm...");
        new Thread(() -> {
            try {
                List<Auction> auctions = dashboardClientService.fetchAllAuctions(socket);
                Platform.runLater(() -> renderResults(auctions));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Search products", e);
                Platform.runLater(() -> showEmptyState("Không thể tải kết quả tìm kiếm."));
            }
        }, "ProductSearchLoader").start();
    }

    private void renderResults(List<Auction> auctions) {
        if (resultsFlowPane == null) {
            return;
        }

        resultsFlowPane.getChildren().clear();
        String normalizedKeyword = normalize(keyword);
        List<Auction> results = auctions == null ? List.of() : auctions.stream()
                .filter(auction -> auction != null && auction.getItem() != null)
                .filter(this::isVisibleAuction)
                .filter(auction -> auction.getTimeRemainingSeconds() > 0)
                .filter(auction -> matchesKeyword(auction, normalizedKeyword))
                .sorted(Comparator.comparingLong(Auction::getEndTime)
                        .thenComparing(auction -> normalize(auction.getItem().getName())))
                .toList();

        if (results.isEmpty()) {
            showEmptyState("Không tìm thấy sản phẩm phù hợp.");
            return;
        }

        setSummary(results.size() + " sản phẩm đang đấu giá phù hợp");
        for (Auction auction : results) {
            VBox card = createAuctionCard(auction);
            if (card != null) {
                resultsFlowPane.getChildren().add(card);
            }
        }
    }

    private boolean matchesKeyword(Auction auction, String normalizedKeyword) {
        if (normalizedKeyword.isBlank()) {
            return true;
        }
        Item item = auction.getItem();
        String searchableText = normalize(String.join(" ",
                safe(item.getName()),
                item.getCategory() != null ? item.getCategory().name() : "",
                safe(auction.getSellerName())));
        return searchableText.contains(normalizedKeyword);
    }

    private VBox createAuctionCard(Auction auction) {
        try {
            URL resource = getClass().getResource("/fxml/BidderView/AuctionCard.fxml");
            if (resource == null) {
                ClientExceptionHandler.handle(ClientErrorType.NAVIGATION,
                        "Load auction card in search",
                        new IllegalStateException("/fxml/BidderView/AuctionCard.fxml"));
                return null;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();
            AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                cardController.setHomeScreenController(homeScreenController);
                cardController.setAuctionData(auction);
            }

            if (cardNode instanceof VBox box) {
                return box;
            }
            return new VBox(cardNode);
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.NAVIGATION, "Load auction card in search", e);
            return null;
        }
    }

    private boolean isVisibleAuction(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING;
    }

    private void showEmptyState(String message) {
        if (resultsFlowPane == null) {
            setSummary(message);
            return;
        }
        resultsFlowPane.getChildren().clear();
        setSummary(message);

        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(560);
        box.setMinHeight(150);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-padding: 24;");

        Label title = new Label(message);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label subtitle = new Label("Thử từ khóa khác hoặc quay lại trang chủ.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        box.getChildren().addAll(title, subtitle);
        resultsFlowPane.getChildren().add(box);
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

    private void setSummary(String text) {
        if (summaryLabel != null) {
            summaryLabel.setText(text);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
