package client.controller;

import navigation.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.Arrays;
import java.util.List;

public class SellerHomeController {

    @FXML
    private HBox menuHome, menuAI,
            menuCreateAuctions, menuManageAuctions, menuAuctionStatistic, menuMsg, menuPay,
            menuDowngrade, menuSettings;

    private List<HBox> allMenus;

    @FXML
    private AnchorPane rootPane;

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
                menuCreateAuctions, menuManageAuctions, menuAuctionStatistic, menuMsg, menuPay,
                menuSettings
        );

        //updateMenuSelection(menuHome);

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

    @FXML
    private void handleMenuClick(MouseEvent event) {
        if (event.getSource() instanceof HBox clickedMenu) {
            if (clickedMenu == menuDowngrade) {
                System.out.println("Đang chuyển về màn hình Bidder");
                NavigationManager.getInstance().goToHome();
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