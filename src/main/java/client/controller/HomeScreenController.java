package client.controller;
import client.network.ClientSocket;
import server.model.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HomeScreenController {

    @FXML
    private HBox menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings;
    private List<HBox> allMenus;

    @FXML
    private StackPane rootPane;
    @FXML
    private StackPane contentArea;

    @FXML
    private TextField productSearchInput;
    @FXML
    private Button clearSearchBtn;

    @FXML
    private Button themeToggleBtn;
    private boolean isNightMode = false;
    private User currentUser;
    private ClientSocket clientSocket;
    private Runnable onLogout;

    @FXML

    public void initialize() {
        allMenus = Arrays.asList(menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings);
        try {
            loadDashboardView();
        } catch (Exception e) {
            System.err.println("Cảnh báo: Lỗi khi load ngầm Dashboard: " + e.getMessage());
        }
        if (productSearchInput != null && clearSearchBtn != null) {
            productSearchInput.textProperty().addListener((observable, oldValue, newValue) -> {
                clearSearchBtn.setVisible(!newValue.trim().isEmpty());
            });
        } else {
            System.err.println("Cảnh báo: productSearchInput hoặc clearSearchBtn bị null (chưa gắn fx:id)!");
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
    }

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox) {
            HBox clickedMenu = (HBox) event.getSource();
            updateMenuSelection(clickedMenu);
        }
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null || menu.getChildren().isEmpty()) continue;

            try {
                Button btn = (Button) menu.getChildren().get(0);
                if (menu == selectedMenu) {
                    menu.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 8; -fx-cursor: hand;");
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
                } else {
                    menu.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
                    if (menu == menuUpgrade) {
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");
                    } else {
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a5568;");
                    }
                }
            } catch (ClassCastException e) {
                System.err.println("Cảnh báo: Element bên trong menu không phải là Button!");
            }
        }
    }

    @FXML
    public void loadDashboardView() {
        updateMenuSelection(menuHome);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/Dashboard.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file Dashboard.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dashboardNode = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardNode);
            } else {
                System.err.println("Cảnh báo: contentArea bị null (chưa gắn fx:id trong Home.fxml)!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi load giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    public void loadProfileView() {
        updateMenuSelection(null);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/ProfileView.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file ProfileView.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent profileNode = loader.load();
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(profileNode);
            }
        } catch (IOException e) {
            System.err.println("Lỗi load giao diện Profile");
            e.printStackTrace();
        }
    }

    public void loadNotificationsView() {
        updateMenuSelection(menuMsg);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/Notifications.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file Notifications.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent notiNode = loader.load();
            NotificationsController notiController = loader.getController();
            notiController.setHomeController(this);
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(notiNode);
            }
        } catch (IOException e) {
            System.err.println("Lỗi load giao diện Thông báo");
            e.printStackTrace();
        }
    }

    public void loadWalletView() {
        updateMenuSelection(menuPay);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/WalletView.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file WalletView.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent walletNode = loader.load();

            WalletController walletController = loader.getController();
            if (currentUser != null && clientSocket != null) {
                walletController.setUserData(currentUser, clientSocket);  // ← SỬA ĐÂY
            }

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(walletNode);
            }

        } catch (IOException e) {
            System.err.println("Lỗi load giao diện Wallet");
            e.printStackTrace();
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
                    System.out.println("🌙 Đã bật Night Mode");
                } else {
                    rootPane.getScene().getStylesheets().remove(cssPath);
                    themeToggleBtn.setText("🌙");
                    System.out.println("☀️ Đã tắt Night Mode");
                }
            } catch (NullPointerException e) {
                System.err.println("Cảnh báo: Không tìm thấy file /CSS/dark-mode.css");
            }
        }
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    @FXML
    private void handleLogout() {
        if (onLogout != null) {
            onLogout.run();
        }
    }


}
