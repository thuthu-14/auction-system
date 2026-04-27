package client.controller;

import navigation.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

public class SellerHomeController {

    // THAY ĐỔI 1: bỏ private → package-private
    // để SellerDashboardController truy cập trực tiếp
    @FXML
    HBox menuHome,
            menuCreateAuctions, menuManageAuctions, menuAuctionStatistic, menuMsg, menuPay,
            menuDowngrade, menuSettings;

    private List<HBox> allMenus;

    @FXML private AnchorPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private TextField productSearchInput;
    @FXML private Button clearSearchBtn;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(
                menuHome,
                menuCreateAuctions, menuManageAuctions, menuAuctionStatistic, menuMsg, menuPay,
                menuSettings
        );

        //load dashboard vào contentArea khi khởi động
        loadDashboard();

        if (productSearchInput != null && clearSearchBtn != null) {
            productSearchInput.textProperty().addListener((obs, oldVal, newVal) ->
                    clearSearchBtn.setVisible(!newVal.trim().isEmpty())
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

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/SellerDashboard.fxml"));
            VBox dashboard = loader.load();

            SellerDashboardController dashCtrl = loader.getController();
            dashCtrl.setHomeController(this);

            contentArea.getChildren().setAll(dashboard);
            updateMenuSelection(menuHome);

        } catch (Exception e) {
            System.err.println("Không load được SellerDashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * Được gọi từ SellerDashboardController khi người dùng click card.
     * Highlight menu sidebar tương ứng + load view tương ứng.
     */
    public void selectMenu(HBox menu) {
        if (menu == null) return;
        updateMenuSelection(menu);
        // TODO: thêm logic load view tương ứng ở đây, ví dụ:
        // if (menu == menuCreateAuctions) loadCreateAuctionView();
        if (menu == menuManageAuctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ManageAuctions.fxml"));
                javafx.scene.Node view = loader.load();
                contentArea.getChildren().setAll(view);

            } catch (java.io.IOException e) {
                System.err.println("Lỗi: Không thể load màn hình ManageAuctions!");
                // In ra chi tiết dòng code nào gây lỗi để dễ fix
                e.printStackTrace();
            }
        }
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

    void updateMenuSelection(HBox selectedMenu) {
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