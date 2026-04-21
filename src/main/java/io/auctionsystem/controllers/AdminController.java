package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AdminController {

    @FXML
    private HBox menuManageUser, menuManageBid, menuLogOut;

    @FXML
    private StackPane rootPane;

    @FXML
    private StackPane contentArea;

    private List<HBox> allMenus;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(menuManageUser, menuManageBid, menuLogOut);
        loadUserManagementView();
    }

    @FXML
    private void loadUserManagementView() {
        updateMenuSelection(menuManageUser);
        loadViewIntoContentArea("/io/auctionsystem/AdminUserManagement.fxml");
    }

    @FXML
    private void loadLogOutView() {
        updateMenuSelection(menuLogOut);
        loadViewIntoContentArea("/io/auctionsystem/login.fxml");
    }

    @FXML
    private void loadAuctionManagementView() {
        updateMenuSelection(menuManageBid);
        loadViewIntoContentArea("/io/auctionsystem/AdminAuctionManagement.fxml");
    }

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox) {
            HBox clickedMenu = (HBox) event.getSource();

            if (clickedMenu == menuManageUser) loadUserManagementView();
            else if (clickedMenu == menuManageBid) loadAuctionManagementView();
            else if (clickedMenu == menuLogOut) loadLogOutView();
            else updateMenuSelection(clickedMenu);
        }
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null) continue;

            try {
                Button btn = (Button) menu.getChildren().get(0);
                if (menu == selectedMenu) {
                    menu.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 8; -fx-cursor: hand;");
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
                } else {
                    menu.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
                    if (menu == menuLogOut) { // Đổi menuUpgrade thành menuLogOut cho nút màu đỏ
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");
                    } else {
                        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a5568;");
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
                Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (IOException e) {
            System.err.println("Không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}