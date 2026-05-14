package client.controller;

import client.network.ClientSocket;
import server.model.User;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

public class SellerDashboardController {

    @FXML private VBox cardTaoPhien;
    @FXML private VBox cardQuanLyPhien;
    @FXML private VBox cardThongKe;
    @FXML private VBox cardThongBao;
    @FXML private VBox cardViTien;

    // ===== THÊM ĐỂ LINK SERVER =====
    private User currentUser;
    private ClientSocket clientSocket;
    private SellerHomeController sellerHomeController;
    private final Map<String, Runnable> cardActions = new HashMap<>();

    @FXML
    private void initialize() {
        cardActions.put("cardTaoPhien", () -> { if (sellerHomeController != null) sellerHomeController.loadAddAuctionProductView(); else loadView("/fxml/SellerView/AddAuctionProduct.fxml"); });
        cardActions.put("cardQuanLyPhien", () -> { if (sellerHomeController != null) sellerHomeController.loadManageAuctionsView(); else loadView("/fxml/SellerView/SellerManageAuctions.fxml"); });
        cardActions.put("cardThongKe", () -> { if (sellerHomeController != null) sellerHomeController.loadSellerStatisticsView(); else loadView("/fxml/SellerView/SellerStatistics.fxml"); });
        cardActions.put("cardThongBao", () -> { if (sellerHomeController != null) sellerHomeController.loadSellerNotificationsView(); else loadView("/fxml/SellerView/SellerNotifications.fxml"); });
        cardActions.put("cardViTien", () -> { if (sellerHomeController != null) sellerHomeController.loadWalletView(); else loadView("/fxml/BidderView/WalletView.fxml"); });
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    private void loadView(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent newNode = loader.load();

            // ===== BƠM DỮ LIỆU SERVER SANG MÀN HÌNH MỚI =====
            Object controller = loader.getController();
            if (controller != null) {
                if (controller instanceof WalletController) {
                    ((WalletController) controller).setUserData(currentUser, clientSocket);
                } else if (controller instanceof SellerManagementController) {
                    ((SellerManagementController) controller).refreshAuctions();
                } else if (controller instanceof SellerStatisticsController) {
                    ((SellerStatisticsController) controller).setUserData(currentUser, clientSocket);
                } else if (controller instanceof SellerNotificationsController) {
                    ((SellerNotificationsController) controller).setUserData(currentUser, clientSocket);
                }
            }

            javafx.scene.layout.Pane contentArea = (javafx.scene.layout.Pane) cardTaoPhien.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(newNode);
            } else {
                LoggerUtil.error("Lỗi: Không tìm thấy fx:id='contentArea' ở trang chính!");
            }
        } catch (Exception e) {
            LoggerUtil.error("Không thể load file FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCardClick(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        String id = source.getId();

        Runnable action = cardActions.get(id);
        if (action != null) {
            action.run();
        }
    }

    @FXML
    private void onCardEnter(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        source.setStyle("-fx-cursor: hand;");
    }

    @FXML
    private void onCardExit(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        source.setScaleX(1.0);
        source.setScaleY(1.0);
        source.setStyle("");
    }
}
