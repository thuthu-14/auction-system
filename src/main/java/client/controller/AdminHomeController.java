package client.controller;



import client.network.ClientSocket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import server.model.User;

import java.io.IOException;
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
        }
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
                System.out.println("Lỗi đổi màu menu: " + e.getMessage());
            }
        }
    }
    private void loadViewIntoContentArea(String fxmlPath) {
        try {
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent view = loader.load();
                Object controller = loader.getController();
                if (controller instanceof UserManagementController userManagementController) {
                    userManagementController.setContext(currentUser, clientSocket);
                } else if (controller instanceof AdminAuctionManagementController auctionManagementController) {
                    auctionManagementController.setContext(currentUser, clientSocket);
                }
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
