package client.controller;

import client.network.ClientSocket;
import client.util.ResponsiveSceneUtil;
import server.model.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import util.LoggerUtil;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SellerHomeController {

    @FXML private HBox menuHome, menuAI, menuCreateAuctions, menuManageAuctions,
            menuAuctionStatistic, menuMsg, menuPay, menuDowngrade, menuSettings;

    private List<HBox> allMenus;

    @FXML private AnchorPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private TextField productSearchInput;
    @FXML private Button clearSearchBtn;

    private User currentUser;
    private ClientSocket clientSocket;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(
                menuHome, menuAI, menuCreateAuctions, menuManageAuctions,
                menuAuctionStatistic, menuMsg, menuPay, menuSettings, menuDowngrade
        );

        if (productSearchInput != null && clearSearchBtn != null) {
            productSearchInput.textProperty().addListener((observable, oldValue, newValue) ->
                    clearSearchBtn.setVisible(!newValue.trim().isEmpty())
            );
        } else {
            LoggerUtil.warn("Cảnh báo: productSearchInput hoặc clearSearchBtn bị null!");
        }

        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                KeyCombination ctrlK = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
                rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (ctrlK.match(event) && productSearchInput != null) {
                        productSearchInput.requestFocus();
                        event.consume();
                    }
                });
            }
        });

        // Load trang chủ mặc định
        loadSellerDashboardView();
    }

    // ================= DATA INJECTION =================

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        LoggerUtil.info("SellerHomeController đã nhận data cho user: " + (user != null ? user.getUsername() : "null"));
    }

    // ================= MENU ACTIONS =================

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox clickedMenu) {

            // Xử lý nút Chuyển về màn hình người mua
            if (clickedMenu == menuDowngrade) {
                LoggerUtil.info("Đang chuyển về màn hình Bidder (Home)");
                switchToBidderMode();
                return;
            }

            updateMenuSelection(clickedMenu);
        }
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null) continue;
            menu.getStyleClass().remove("menu-item-active");

            if (menu == selectedMenu) {
                if (!menu.getStyleClass().contains("menu-item-active")) {
                    menu.getStyleClass().add("menu-item-active");
                }
            }
        }
    }

    private void switchToBidderMode() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Parent root = loader.load();

            HomeScreenController controller = loader.getController();
            if (controller != null) {
                // Truyền lại user và socket để không bị rớt mạng
                controller.setUserData(currentUser, clientSocket);
            }

            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(ResponsiveSceneUtil.createScaledScene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (Exception e) {
            LoggerUtil.error("Lỗi khi chuyển về màn hình Bidder: " + e.getMessage());
        }
    }

    // ================= VIEW LOADERS =================

    @FXML
    public void loadSellerDashboardView() {
        loadView("/fxml/SellerDashboard.fxml", menuHome, null);
    }

    @FXML
    public void loadAddAuctionProductView() {
        loadView("/fxml/AddAuctionProduct.fxml", menuCreateAuctions, null);
    }

    @FXML
    public void loadSellerStatisticsView() {
        loadView("/fxml/SellerStatistics.fxml", menuAuctionStatistic, null);
    }

    @FXML
    public void loadManageAuctionsView() {
        loadView("/fxml/SellerManageAuctions.fxml", menuManageAuctions, controller -> {
            if (controller instanceof SellerManagementController) {
                ((SellerManagementController) controller).refreshAuctions();
            }
        });
    }

    @FXML
    public void loadSellerNotificationsView() {
        loadView("/fxml/SellerNotifications.fxml", menuMsg, null);
    }

    @FXML
    public void loadProfileView() {
        loadView("/fxml/ProfileView.fxml", menuHome, null); // Hoặc bạn có thể tạo menuProfile
    }

    @FXML
    public void loadWalletView() {
        loadView("/fxml/WalletView.fxml", menuPay, controller -> {
            if (controller instanceof WalletController && currentUser != null && clientSocket != null) {
                ((WalletController) controller).setUserData(currentUser, clientSocket);
            } else {
                LoggerUtil.warn("Không thể truyền data cho WalletController vì user/socket bị null");
            }
        });
    }

    /**
     * Hàm Helper dùng chung để load các file FXML vào contentArea
     */
    private void loadView(String fxmlPath, HBox menuToSelect, Consumer<Object> controllerSetup) {
        updateMenuSelection(menuToSelect);
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                LoggerUtil.error("Không tìm thấy file: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent node = loader.load();

            Object controller = loader.getController();
            if (controller instanceof SellerDashboardController) {
                ((SellerDashboardController) controller).setSellerHomeController(this);
                ((SellerDashboardController) controller).setUserData(currentUser, clientSocket);
            }

            if (controllerSetup != null) {
                controllerSetup.accept(controller);
            }

            prepareContentNode(node);
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            LoggerUtil.error("Lỗi load giao diện " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void prepareContentNode(Node node) {
        StackPane.setAlignment(node, Pos.TOP_LEFT);
        if (node instanceof Region region) {
            region.setMinWidth(0);
            region.setMinHeight(0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
        if (node instanceof ScrollPane scrollPane) {
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
    }

    // ================= UTILITIES =================

    @FXML
    private void handleProductSearch() {
        if (productSearchInput == null) return;
        String keyword = productSearchInput.getText().trim();
        if (!keyword.isEmpty()) {
            System.out.println("Searching for: " + keyword);
        }
    }

    @FXML
    private void clearSearch() {
        if (productSearchInput != null) {
            productSearchInput.clear();
            productSearchInput.requestFocus();
        }
    }
}
