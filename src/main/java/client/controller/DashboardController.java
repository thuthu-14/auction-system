package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
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

    // --- UI Components Mapping từ FXML ---
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

    /**
     * Nhận dữ liệu người dùng và socket.
     */
    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        System.out.println("Dashboard kết nối thành công: " + (user != null ? user.getUsername() : "Guest"));

        // Ưu tiên load dữ liệu ngay khi có socket
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
                applyLabelColor(label, text.contains("\u0110\u00e3") || text.contains("Ä") ? "#6b7280" : "#e53e3e");
            } else if ("priceLabel".equals(id)) {
                applyLabelColor(label, "#111827");
            } else if (text.contains("V\u1eeba xong") || text.contains("Vá") || text.contains("xong")) {
                applyLabelColor(label, "#9ca3af");
            } else if (text.contains("H\u00e0ng ng\u00e0n")
                    || text.contains("H\u1ec7 th\u1ed1ng")
                    || text.contains("HÃ")
                    || text.contains("thá")) {
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
     * Gửi yêu cầu lấy danh sách đấu giá mới nhất từ Server (ĐÃ SỬA LỖI CRASH)
     */
    public void loadAuctionsFromServer() {
        ClientSocket socket = (clientSocket != null) ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        if (socket == null) return;

        new Thread(() -> {
            try {
                Message request = new Message(MessageType.GET_ALL_AUCTIONS, null, "client");
                Message response = socket.sendAndReceive(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    Object rawData = response.getData();
                    List<Auction> auctions = new ArrayList<>();

                    // KIỂM TRA ÉP KIỂU AN TOÀN TRÁNH CLASS CAST EXCEPTION
                    if (rawData instanceof List) {
                        auctions = (List<Auction>) rawData;
                    } else if (rawData instanceof Auction) {
                        auctions.add((Auction) rawData);
                    }

                    // Lưu ý: Cập nhật UI phải chạy trên Platform.runLater
                    final List<Auction> finalAuctions = auctions;
                    Platform.runLater(() -> renderProductCards(finalAuctions));
                }
            } catch (Exception e) {
                System.err.println("❌ LỖI ĐỒNG BỘ DASHBOARD: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Xóa các mẫu cũ và vẽ lại các thẻ sản phẩm mới
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
                    if (col > 1) { // Thiết kế 2 cột
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
     * Tạo một ô sản phẩm BẰNG CÁCH GỌI FILE FXML (ĐÃ SỬA ĐỂ NÚT BẤM CÓ TÁC DỤNG)
     */
    private VBox createAuctionCard(Auction auction) {
        try {
            // Thử đường dẫn 1
            String cardPath = "/client/view/AuctionCard.fxml";
            URL resource = getClass().getResource(cardPath);

            // Thử đường dẫn 2 nếu đường dẫn 1 sai
            if (resource == null) {
                resource = getClass().getResource("/fxml/AuctionCard.fxml");
            }

            if (resource == null) {
                System.err.println("❌ LỖI: Không tìm thấy file AuctionCard.fxml!");
                return new VBox();
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent cardNode = loader.load();

            // Lấy Controller và truyền dữ liệu sang Card
            client.controller.AuctionCardController cardController = loader.getController();
            if (cardController != null) {
                cardController.setAuctionData(auction);
                if (homeScreenController != null) {
                    cardController.setHomeScreenController(homeScreenController);
                }
            }

            // Nếu root của AuctionCard.fxml là VBox thì ép kiểu trực tiếp
            if (cardNode instanceof VBox) {
                return (VBox) cardNode;
            } else {
                // Nếu là AnchorPane hoặc thứ khác, gói nó vào một VBox để không bị lỗi hàm
                VBox wrapper = new VBox();
                wrapper.getChildren().add(cardNode);
                return wrapper;
            }

        } catch (Exception e) {
            System.err.println("❌ LỖI LOAD THẺ AUCTION CARD BÊN TRONG DASHBOARD: " + e.getMessage());
            e.printStackTrace();
            return new VBox(); // Trả về Vbox rỗng để không bị chết Grid
        }
    }

    // ==============================================================
    // CÁC HÀM XỬ LÝ ẢNH BANNER BÊN TRÊN (GIỮ NGUYÊN NHƯ CŨ CỦA BẠN)
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
            System.out.println("Lỗi load ảnh banner: " + e.getMessage());
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
