package client.controller;

import client.network.ClientSocket;
import navigation.NavigationContextAware;
import server.model.User;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

public class SellerDashboardController implements NavigationContextAware {

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
        cardActions.put("cardTaoPhien", () -> sellerHomeController.loadAddAuctionProductView());
        cardActions.put("cardQuanLyPhien", () -> sellerHomeController.loadManageAuctionsView());
        cardActions.put("cardThongKe", () -> sellerHomeController.loadSellerStatisticsView());
        cardActions.put("cardThongBao", () -> sellerHomeController.loadSellerNotificationsView());
        cardActions.put("cardViTien", () -> sellerHomeController.loadWalletView());
    }

    @Override
    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    @FXML
    private void onCardClick(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        String id = source.getId();

        Runnable action = cardActions.get(id);
        if (action != null && sellerHomeController != null) {
            action.run();
        } else {
            LoggerUtil.warn("SellerDashboardController missing SellerHomeController for action: " + id);
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

