package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.logic.ProductSearchLogic;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuctionQueryClient;
import client.service.DashboardClientService;
import client.ui.AuctionCardFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import server.model.Auction;
import server.model.User;

import java.util.List;

public class ProductSearchController {

    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private FlowPane resultsFlowPane;

    private final AuctionQueryClient auctionQueryClient;
    private final AuctionCardFactory auctionCardFactory;
    private HomeScreenController homeScreenController;
    private User currentUser;
    private ClientSocket clientSocket;
    private String keyword = "";

    public ProductSearchController() {
        this(new DashboardClientService(), new AuctionCardFactory());
    }

    ProductSearchController(AuctionQueryClient auctionQueryClient, AuctionCardFactory auctionCardFactory) {
        this.auctionQueryClient = auctionQueryClient;
        this.auctionCardFactory = auctionCardFactory;
    }

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
        client.util.ClientTaskRunner.run(() -> {
            try {
                List<Auction> auctions = auctionQueryClient.fetchAllAuctions(socket);
                Platform.runLater(() -> renderResults(auctions));
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.DATA, "Search products", e);
                Platform.runLater(() -> showEmptyState("Không thể tải kết quả tìm kiếm."));
            }
        });
    }

    private void renderResults(List<Auction> auctions) {
        if (resultsFlowPane == null) {
            return;
        }

        resultsFlowPane.getChildren().clear();
        List<Auction> results = ProductSearchLogic.filterVisibleMatches(auctions, keyword);

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

    private VBox createAuctionCard(Auction auction) {
        return auctionCardFactory.create(auction, homeScreenController, "search");
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

}
