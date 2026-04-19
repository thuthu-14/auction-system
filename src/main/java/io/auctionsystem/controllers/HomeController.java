package io.auctionsystem.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.Arrays;
import java.util.List;

public class HomeController {

    @FXML
    private HBox menuHome, menuAI,
            menuRecent, menuFlash, menuMsg, menuPay,
            //menuUpgrade,
            menuSettings;

    private List<HBox> allMenus;

    @FXML
    private HBox menuUpgrade;

    @FXML
    private StackPane rootPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private TextField productSearchInput;

    @FXML
    private Button clearSearchBtn;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(
                menuHome, menuAI,
                menuRecent, menuFlash, menuMsg, menuPay,
                //menuUpgrade,
                menuSettings
        );

        updateMenuSelection(menuHome);

        if (productSearchInput != null && clearSearchBtn != null) {
            productSearchInput.textProperty().addListener((observable, oldValue, newValue) ->
                    clearSearchBtn.setVisible(!newValue.trim().isEmpty())
            );
        } else {
            System.err.println("Cảnh báo: productSearchInput hoặc clearSearchBtn bị null!");
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

    /* @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox clickedMenu) {
            updateMenuSelection(clickedMenu);
        }
    } */

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox clickedMenu) {
            if (clickedMenu == menuUpgrade) {
                System.out.println("Đang chuyển sang màn hình Become Seller");

                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/io/auctionsystem/BecomeSeller.fxml"));
                    javafx.scene.Node becomeSellerNode = loader.load();

                    // Clear vùng chứa hiện tại và add màn hình mới vào
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(becomeSellerNode);

                    // Màn hình mới tràn hết vùng chứa:
                    if (becomeSellerNode instanceof javafx.scene.layout.AnchorPane) {
                        javafx.scene.layout.AnchorPane.setTopAnchor(becomeSellerNode, 0.0);
                        javafx.scene.layout.AnchorPane.setBottomAnchor(becomeSellerNode, 0.0);
                        javafx.scene.layout.AnchorPane.setLeftAnchor(becomeSellerNode, 0.0);
                        javafx.scene.layout.AnchorPane.setRightAnchor(becomeSellerNode, 0.0);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Lỗi load màn hình BecomeSeller: " + e.getMessage());
                }

                // Cập nhật trạng thái chọn menu (đổi màu nền chẳng hạn)
                updateMenuSelection(clickedMenu);
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

    @FXML
    private void loadProfileView() {
        System.out.println("Merge sau nha pp");
    }
}