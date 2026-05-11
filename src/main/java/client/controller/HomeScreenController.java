package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.util.ResponsiveSceneUtil;
import client.util.StageUtil;
import navigation.NavigationManager;
import server.model.RegularUser;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import util.LoggerUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class HomeScreenController {

    @FXML private HBox menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings;
    private List<HBox> allMenus;

    @FXML private StackPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private TextField productSearchInput;
    @FXML private Button clearSearchBtn;
    @FXML private Button themeToggleBtn;

    private boolean isNightMode = false;
    private User currentUser;
    private ClientSocket clientSocket;
    private Runnable onLogout;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings);

        // Mặc định load Dashboard
        loadDashboardView();

        // Xử lý thanh tìm kiếm
        if (productSearchInput != null && clearSearchBtn != null) {
            productSearchInput.textProperty().addListener((observable, oldValue, newValue) -> {
                clearSearchBtn.setVisible(!newValue.trim().isEmpty());
            });
        }

        // Phím tắt Ctrl + K để focus thanh tìm kiếm
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

        // LƯU Ý: Không gọi updateMenuBasedOnSellerStatus() ở đây vì currentUser đang là null!
    }

    // ================= DATA INJECTION =================

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        NavigationManager.getInstance().setCurrentUser(user);
        NavigationManager.getInstance().setClientSocket(socket);

        // Gọi ở đây mới đúng, vì lúc này user đã được truyền từ Login/Signup sang
        updateMenuBasedOnSellerStatus();
        loadDashboardView();
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    private void updateMenuBasedOnSellerStatus() {
        if (menuUpgrade == null || menuUpgrade.getChildren().isEmpty()) {
            return;
        }

        menuUpgrade.setVisible(true);
        menuUpgrade.setManaged(true);

        if (menuUpgrade.getChildren().get(0) instanceof Button upgradeButton) {
            if (currentUser instanceof RegularUser regularUser && regularUser.isSeller()) {
                upgradeButton.setText("👨‍🏫  Trở về Seller");
            } else {
                upgradeButton.setText("👨‍🏫  Trở thành Seller");
            }
        }
    }

    // ================= MENU ACTIONS =================

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox) {
            HBox clickedMenu = (HBox) event.getSource();
            if (clickedMenu == menuHome) {
                loadDashboardView();
            } else if (clickedMenu == menuRecent) {
                loadAuctionHistoryView();
            } else if (clickedMenu == menuMsg) {
                loadNotificationsView();
            } else if (clickedMenu == menuPay) {
                loadWalletView();
            } else if (clickedMenu == menuUpgrade) {
                if (currentUser instanceof RegularUser regularUser && regularUser.isSeller()) {
                    goToSellerHome();
                } else {
                    openBecomeSeller();
                }
            } else if (clickedMenu == menuSettings) {
                handleLogout();
            } else {
                updateMenuSelection(clickedMenu);
            }
        }
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null || menu.getChildren().isEmpty()) continue;
            menu.getStyleClass().remove("menu-item-active");

            try {
                Button btn = (Button) menu.getChildren().get(0);
                if (menu == selectedMenu) {
                    if (!menu.getStyleClass().contains("menu-item-active")) {
                        menu.getStyleClass().add("menu-item-active");
                    }
                    menu.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
                    btn.setStyle("-fx-background-color: transparent;");
                } else {
                    menu.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
                    if (menu == menuUpgrade || menu == menuSettings) {
                        btn.setStyle("-fx-background-color: transparent;");
                    } else {
                        btn.setStyle("-fx-background-color: transparent;");
                    }
                }
            } catch (ClassCastException e) {
                LoggerUtil.error("Element trong menu không phải là Button");
            }
        }
    }

    // ================= VIEW LOADERS =================

    @FXML
    public void loadDashboardView() {
        updateMenuSelection(menuHome);
        loadView("/fxml/Dashboard.fxml", controller -> {
            // ← THÊM: Refresh data khi load
            if (controller instanceof DashboardController && currentUser != null && clientSocket != null) {
                DashboardController dashCtrl = (DashboardController) controller;
                dashCtrl.setHomeScreenController(this);  // ← Truyền reference của Home screen
                dashCtrl.setUserData(currentUser, clientSocket);
            }
        });
    }



    public void loadProfileView() {
        updateMenuSelection(null);
        loadView("/fxml/ProfileView.fxml", null);
    }

    public void loadNotificationsView() {
        updateMenuSelection(menuMsg);
        loadView("/fxml/Notifications.fxml", controller -> {
            if (controller instanceof NotificationsController notificationsController) {
                notificationsController.setHomeController(this);
                notificationsController.setUserData(currentUser, clientSocket);
            }
        });
    }

    public void loadWalletView() {
        updateMenuSelection(menuPay);
        loadView("/fxml/WalletView.fxml", controller -> {
            if (controller instanceof WalletController && currentUser != null && clientSocket != null) {
                ((WalletController) controller).setUserData(currentUser, clientSocket);
            }
        });
    }

    @FXML
    public void loadAuctionHistoryView() {
        updateMenuSelection(menuRecent);
        loadView("/fxml/AuctionHistory.fxml", controller -> {
            if (controller instanceof AuctionHistoryController historyController) {
                User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
                ClientSocket socket = clientSocket != null
                        ? clientSocket
                        : ConnectionManager.getInstance().getClientSocket();
                historyController.setContext(user, socket, this);
            }
        });
    }

    @FXML
    private void openBecomeSeller() {
        updateMenuSelection(menuUpgrade);
        loadView("/fxml/BecomeSeller.fxml", controller -> {
            if (controller instanceof BecomeSellerController) {
                ((BecomeSellerController) controller).setCurrentUser(currentUser);
            }
        });
    }

    private void goToSellerHome() {
        NavigationManager.getInstance().setCurrentUser(currentUser);
        NavigationManager.getInstance().setClientSocket(clientSocket);
        NavigationManager.getInstance().goToSellerHome();
    }

    public void loadAuctionDetailView(server.model.Auction auction) {
        loadAuctionDetailView(auction, false);
    }

    public void loadAuctionDetailView(server.model.Auction auction, boolean endedMode) {
        updateMenuSelection(null);
        loadView("/fxml/AuctionDetail.fxml", controller -> {
            if (controller instanceof AuctionDetailController && auction != null) {
                AuctionDetailController detailController = (AuctionDetailController) controller;
                detailController.setEndedMode(endedMode);
                detailController.loadAuctionData(auction);
            }
        });
    }

    /**
     * Hàm Helper dùng chung để load các file FXML vào contentArea
     * Giúp giảm thiểu code lặp lại (Boilerplate code)
     */
    private void loadView(String fxmlPath, java.util.function.Consumer<Object> controllerSetup) {
        try {
            URL fxmlLocation = getClass().getResource(fxmlPath);
            if (fxmlLocation == null) {
                LoggerUtil.error("Không tìm thấy file: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent viewNode = loader.load();

            if (controllerSetup != null) {
                controllerSetup.accept(loader.getController());
            }

            if (contentArea != null) {
                prepareContentNode(viewNode);
                contentArea.getChildren().setAll(viewNode);
            }
        } catch (IOException e) {
            LoggerUtil.error("Lỗi load giao diện " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void prepareContentNode(Node viewNode) {
        StackPane.setAlignment(viewNode, Pos.TOP_LEFT);
        if (viewNode instanceof Region region) {
            region.setMinWidth(0);
            region.setMinHeight(0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
        if (viewNode instanceof ScrollPane scrollPane) {
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
            System.out.println("Searching for: " + keyword); // Sau này thay bằng logic tìm kiếm thực tế
        }
    }

    @FXML
    private void clearSearch() {
        if (productSearchInput != null) {
            productSearchInput.clear();
            productSearchInput.requestFocus();
        }
    }

    @FXML
    private void toggleNightMode() {
        isNightMode = !isNightMode;
        if (rootPane != null && rootPane.getScene() != null) {
            try {
                String cssPath = getClass().getResource("/CSS/dark-mode.css").toExternalForm();
                if (isNightMode) {
                    rootPane.getScene().getStylesheets().add(cssPath);
                    themeToggleBtn.setText("☀️");
                } else {
                    rootPane.getScene().getStylesheets().remove(cssPath);
                    themeToggleBtn.setText("🌙");
                }
            } catch (NullPointerException e) {
                LoggerUtil.error("Không tìm thấy file /CSS/dark-mode.css");
            }
        }
    }

    @FXML
    private void handleLogout() {
        if (onLogout != null) {
            onLogout.run();
            return;
        }

        try {
            NavigationManager.getInstance().setCurrentUser(null);
            NavigationManager.getInstance().setClientSocket(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(ResponsiveSceneUtil.createScaledScene(root));
            StageUtil.showMaximized(stage);
        } catch (Exception e) {
            LoggerUtil.error("Loi khi dang xuat tu Home: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
