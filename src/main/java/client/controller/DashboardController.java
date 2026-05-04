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
import javafx.scene.layout.*;
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
    @FXML private Button prevBannerBtn;
    @FXML private Button nextBannerBtn;
    @FXML private GridPane endingSoonGrid;
    @FXML private HBox suggestedHBox;

    private List<Image> bannerImages;
    private int currentImageIndex = 0;

    private User currentUser;
    private ClientSocket clientSocket;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupBanner();

        if (ConnectionManager.getInstance().getClientSocket() != null) {
            loadAuctionsFromServer();
        }
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
                socket.sendMessage(request);

                Message response = socket.receiveMessage();

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

        int col = 0;
        int row = 0;

        for (Auction auction : auctions) {
            if (auction.getStatus() != AuctionStatus.CLOSED && auction.getTimeRemainingSeconds() > 0) {

                VBox card = createAuctionCard(auction);

                if (card != null) {
                    endingSoonGrid.add(card, col, row);
                    col++;
                    if (col > 1) { // Thiết kế 2 cột
                        col = 0;
                        row++;
                    }
                }
            }
        }
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

    public void refreshAuctions() {
        // Load lại danh sách từ server
        initialize(null, null);  // hoặc gọi method load data
    }
}