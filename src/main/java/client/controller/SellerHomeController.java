package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import navigation.NavigationManager;
import server.model.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
    private String selectedSellerAuctionId;

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
            LoggerUtil.warn("productSearchInput or clearSearchBtn is null");
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

        loadSellerDashboardView();
    }

    // ================= DATA INJECTION =================

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        NavigationManager.getInstance().setCurrentUser(user);
        NavigationManager.getInstance().setClientSocket(socket);
        LoggerUtil.info("SellerHomeController received user: " + (user != null ? user.getUsername() : "null"));
    }

    // ================= MENU ACTIONS =================

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox clickedMenu) {

            if (clickedMenu == menuDowngrade) {
                LoggerUtil.info("Switching to Bidder home");
                switchToBidderMode();
                return;
            }

            if (clickedMenu == menuSettings) {
                handleLogout();
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
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            NavigationManager navigation = NavigationManager.getInstance();
            navigation.setMainStage(stage);
            navigation.setCurrentUser(currentUser);
            navigation.setClientSocket(clientSocket);
            navigation.goToHome();
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Error switching to Bidder: " + e.getMessage())));
            ClientExceptionHandler.showError(ClientErrorType.NAVIGATION, "Switch seller to bidder", e);
        }
    }

    @FXML
    private void handleLogout() {
        NavigationManager navigation = NavigationManager.getInstance();
        navigation.setMainStage((javafx.stage.Stage) rootPane.getScene().getWindow());
        navigation.goToLogin();
    }

    // ================= VIEW LOADERS =================

    @FXML
    public void loadSellerDashboardView() {
        loadView("/fxml/SellerView/SellerDashboard.fxml", menuHome, null);
    }

    @FXML
    public void loadAddAuctionProductView() {
        loadView("/fxml/SellerView/AddAuctionProduct.fxml", menuCreateAuctions, null);
    }

    @FXML
    public void loadSellerStatisticsView() {
        loadView("/fxml/SellerView/SellerStatistics.fxml", menuAuctionStatistic, controller -> {
            if (controller instanceof SellerStatisticsController statisticsController) {
                statisticsController.setUserData(currentUser, clientSocket);
            }
        });
    }

    @FXML
    public void loadManageAuctionsView() {
        loadView("/fxml/SellerView/SellerManageAuctions.fxml", menuManageAuctions, controller -> {
            if (controller instanceof SellerManagementController sellerManagementController) {
                sellerManagementController.setSellerHomeController(this);
                sellerManagementController.setUserData(currentUser, clientSocket);
            }
        });
    }

    public void loadSellerAuctionDetailsView(String auctionId) {
        String targetAuctionId = rememberSellerAuctionId(auctionId);
        if (targetAuctionId == null) {
            LoggerUtil.warn("Cannot open seller auction details: missing auction id");
            return;
        }
        loadView("/fxml/SellerView/SellerAuctionDetails.fxml", menuManageAuctions, controller -> {
            if (controller instanceof SellerAuctionDetailsController detailsController) {
                detailsController.setSellerHomeController(this);
                detailsController.setUserData(currentUser, clientSocket);
                detailsController.loadSellerAuctionDetails(targetAuctionId);
            }
        });
    }

    public void loadSellerAuctionBidHistoryView(String auctionId) {
        String targetAuctionId = rememberSellerAuctionId(auctionId);
        if (targetAuctionId == null) {
            LoggerUtil.warn("Cannot open seller auction bid history: missing auction id");
            return;
        }
        loadView("/fxml/SellerView/SellerAuctionBidHistory.fxml", menuManageAuctions, controller -> {
            if (controller instanceof SellerAuctionBidHistoryController bidHistoryController) {
                bidHistoryController.setSellerHomeController(this);
                bidHistoryController.setUserData(currentUser, clientSocket);
                bidHistoryController.loadAuctionBidHistory(targetAuctionId);
            }
        });
    }

    public void loadSellerWinnerInfoView(String auctionId) {
        String targetAuctionId = rememberSellerAuctionId(auctionId);
        if (targetAuctionId == null) {
            LoggerUtil.warn("Cannot open seller winner info: missing auction id");
            return;
        }
        loadView("/fxml/SellerView/SellerWinnerInfo.fxml", menuManageAuctions, controller -> {
            if (controller instanceof SellerWinnerInfoController winnerInfoController) {
                winnerInfoController.setSellerHomeController(this);
                winnerInfoController.setUserData(currentUser, clientSocket);
                winnerInfoController.loadWinnerInfo(targetAuctionId);
            }
        });
    }

    public String getSelectedSellerAuctionId() {
        return selectedSellerAuctionId;
    }

    public String rememberSellerAuctionId(String auctionId) {
        if (auctionId != null && !auctionId.isBlank() && !"--".equals(auctionId)) {
            selectedSellerAuctionId = auctionId;
        }
        return selectedSellerAuctionId;
    }

    @FXML
    public void loadSellerNotificationsView() {
        loadView("/fxml/SellerView/SellerNotifications.fxml", menuMsg, controller -> {
            if (controller instanceof SellerNotificationsController notificationsController) {
                notificationsController.setSellerHomeController(this);
                notificationsController.setUserData(currentUser, clientSocket);
            }
        });
    }

    @FXML
    public void loadProfileView() {
        loadView("/fxml/BidderView/ProfileView.fxml", menuHome, null);
    }

    @FXML
    public void loadWalletView() {
        loadView("/fxml/WalletView.fxml", menuPay, controller -> {
            if (controller instanceof WalletController walletController) {
                User user = NavigationManager.getInstance().getCurrentUser();
                if (user == null) {
                    user = this.currentUser;  // Fallback
                }

                if (user != null) {
                    this.currentUser = user;
                }

                ClientSocket socket = clientSocket != null
                        ? clientSocket
                        : NavigationManager.getInstance().getClientSocket();
                walletController.setUserData(user, socket);
                walletController.reloadWalletData();
            } else {
                LoggerUtil.warn("Cannot pass data to WalletController because user or socket is null");
            }
        });
    }

    private void loadView(String fxmlPath, HBox menuToSelect, Consumer<Object> controllerSetup) {
        updateMenuSelection(menuToSelect);
        clearStaleWalletOverlays();
        Object controller = NavigationManager.getInstance().loadContent(contentArea, fxmlPath, loadedController -> {
            if (loadedController instanceof SellerDashboardController dashboardController) {
                dashboardController.setSellerHomeController(this);
            }
            if (controllerSetup != null) {
                controllerSetup.accept(loadedController);
            }
        });
        clearStaleWalletOverlays();
        if (controller instanceof WalletController walletController) {
            Platform.runLater(walletController::reloadWalletData);
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

    private void clearStaleWalletOverlays() {
        if (rootPane == null) {
            return;
        }

        rootPane.getChildren().removeIf(this::isStaleWalletOverlay);
        if (contentArea != null) {
            contentArea.getChildren().removeIf(this::isStaleWalletOverlay);
            contentArea.setOpacity(1);
            contentArea.setDisable(false);
        }
    }

    private boolean isStaleWalletOverlay(Node node) {
        if (node == null) {
            return false;
        }

        if (node.getStyleClass().contains("wallet-modal-overlay")) {
            return true;
        }

        String style = node.getStyle();
        return node instanceof Region
                && style != null
                && (style.contains("rgba(229, 231, 235") || style.contains("rgba(255, 255, 255"));
    }

    // ================= UTILITIES =================

    @FXML
    private void handleProductSearch() {
        if (productSearchInput == null) return;
        String keyword = productSearchInput.getText().trim();
        if (!keyword.isEmpty()) {
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



