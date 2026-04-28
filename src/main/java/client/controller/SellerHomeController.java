package client.controller;

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
                // TODO://
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
    public void loadAddAuctionProductView() {
        updateMenuSelection(menuHome);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/AddAuctionProduct.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file AddAuctionProduct.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dashboardNode = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardNode);
            } else {
                System.err.println("Cảnh báo: contentArea bị null (chưa gắn fx:id trong SellerHome.fxml)!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi load giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void loadSellerStatisticsView() {
        updateMenuSelection(menuHome);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/SellerStatistics.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file SellerStatistics.fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dashboardNode = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardNode);
            } else {
                System.err.println("Cảnh báo: contentArea bị null (chưa gắn fx:id trong SellerStatistics.fxml)!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi load giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    public void loadSellerNotificationsView() {
        updateMenuSelection(menuHome);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/SellerNotifications.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dashboardNode = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardNode);
            } else {
                System.err.println("Cảnh báo: contentArea bị null (chưa gắn fx:id trong SellerNotifications.fxml)!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi load giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    public void loadWalletView() {
        updateMenuSelection(menuHome);
        try {
            java.net.URL fxmlLocation = getClass().getResource("/fxml/WalletView.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file fxml!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dashboardNode = loader.load();

            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(dashboardNode);
            } else {
                System.err.println("Cảnh báo: contentArea bị null (chưa gắn fx:id trong WalletViews.fxml)!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi load giao diện Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
