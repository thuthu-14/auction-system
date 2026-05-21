package client.controller;



import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import navigation.NavigationManager;
import server.model.User;

import java.util.Arrays;
import java.util.List;

public class AdminHomeController {

    @FXML
    private HBox menuRecent, menuMsg, menuUpgrade;


    @FXML
    private StackPane rootPane;

    @FXML
    private StackPane contentArea;

    private List<HBox> allMenus;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(menuRecent, menuMsg, menuUpgrade);
    }
    // ← THÊM: Nhận dữ liệu từ ClientMain
    private User currentUser;
    private ClientSocket clientSocket;
    private Runnable onLogout;

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        NavigationManager.getInstance().setCurrentUser(user);
        NavigationManager.getInstance().setClientSocket(socket);
        loadUserManagementView();
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }


    @FXML
    private void loadUserManagementView() {
        updateMenuSelection(menuRecent);

        loadViewIntoContentArea("/fxml/AdminView/AdminUserManagement.fxml");

    }

    @FXML
    private void loadLogOutView() {
        updateMenuSelection(menuUpgrade);

        if (onLogout != null) {
            onLogout.run();
            return;
        }
        NavigationManager.getInstance().goToLogin();
    }


    @FXML
    private void loadAuctionManagementView() {
        updateMenuSelection(menuMsg);

        loadViewIntoContentArea("/fxml/AdminView/AdminAuctionManagement.fxml");
    }

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox) {
            HBox clickedMenu = (HBox) event.getSource();

            if (clickedMenu == menuRecent) loadUserManagementView();
            else if (clickedMenu == menuMsg) loadAuctionManagementView();
            else if (clickedMenu == menuUpgrade) loadLogOutView();

            else updateMenuSelection(clickedMenu);
        }
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null) continue;
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
                    if (menu == menuUpgrade) { // Đổi menuUpgrade thành menuLogOut cho nút màu đỏ
                        btn.setStyle("-fx-background-color: transparent;");
                    } else {
                        btn.setStyle("-fx-background-color: transparent;");
                    }
                }
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UI, "Admin menu selection", e);
            }
        }
    }
    private void loadViewIntoContentArea(String fxmlPath) {
        NavigationManager.getInstance().loadContent(contentArea, fxmlPath, controller -> {
            if (controller instanceof UserManagementController userManagementController) {
                userManagementController.setContext(currentUser, clientSocket);
            } else if (controller instanceof AdminAuctionManagementController auctionManagementController) {
                auctionManagementController.setContext(currentUser, clientSocket);
            }
        });
    }
}

