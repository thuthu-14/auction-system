package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HomeController {

    @FXML private HBox menuHome;
    @FXML private HBox menuAskAI;
    @FXML private HBox menuFlashBid;
    @FXML private HBox menuBecomeSeller;
    @FXML private HBox menuSellerHome;
    @FXML private HBox menuManageAuctions;
    @FXML private HBox menuBackToBidder;
    @FXML private HBox menuSettings;

    @FXML private Label titleLabel;
    @FXML private TextField searchInput;
    @FXML private StackPane contentArea;

    private List<HBox> allMenus;

    private boolean isSeller = false;
    private boolean sellerMode = false;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(
                menuHome,
                menuAskAI,
                menuFlashBid,
                menuBecomeSeller,
                menuSellerHome,
                menuManageAuctions,
                menuBackToBidder,
                menuSettings
        );

        switchToBidderHome();
    }

    @FXML
    private void handleMenuClick(javafx.scene.input.MouseEvent event) {
        HBox clickedMenu = (HBox) event.getSource();
        updateMenuSelection(clickedMenu);

        if (clickedMenu == menuHome) {
            if (sellerMode) {
                loadContent("/io/auctionsystem/home-seller.fxml");
            } else {
                loadContent("/io/auctionsystem/home-bidder.fxml");
            }
        } else if (clickedMenu == menuBecomeSeller) {
            loadContent("/io/auctionsystem/become-seller.fxml");
        } else if (clickedMenu == menuSellerHome) {
            loadContent("/io/auctionsystem/home-seller.fxml");
        } else if (clickedMenu == menuManageAuctions) {
            loadContent("/io/auctionsystem/manage-auctions.fxml");
        } else if (clickedMenu == menuBackToBidder) {
            goBackToBidderHome();
        } else if (clickedMenu == menuFlashBid) {
            showPlaceholder("Flash Bid", "Màn Flash Bid sẽ làm sau.");
        } else if (clickedMenu == menuAskAI) {
            showPlaceholder("Ask AI", "Màn Ask AI sẽ làm sau.");
        } else if (clickedMenu == menuSettings) {
            showPlaceholder("Cài đặt", "Màn cài đặt sẽ làm sau.");
        }
    }

    public void switchToSellerMode() {
        isSeller = true;
        sellerMode = true;

        menuBecomeSeller.setVisible(false);
        menuBecomeSeller.setManaged(false);

        menuSellerHome.setVisible(true);
        menuSellerHome.setManaged(true);

        menuManageAuctions.setVisible(true);
        menuManageAuctions.setManaged(true);

        menuBackToBidder.setVisible(true);
        menuBackToBidder.setManaged(true);

        titleLabel.setText("Xin chào, Seller");

        loadContent("/io/auctionsystem/home-seller.fxml");
        updateMenuSelection(menuSellerHome);
    }

    public void goBackToBidderHome() {
        switchToBidderHome();
    }

    private void switchToBidderHome() {
        sellerMode = false;

        if (isSeller) {
            menuBecomeSeller.setVisible(false);
            menuBecomeSeller.setManaged(false);

            menuSellerHome.setVisible(true);
            menuSellerHome.setManaged(true);

            menuManageAuctions.setVisible(true);
            menuManageAuctions.setManaged(true);

            menuBackToBidder.setVisible(true);
            menuBackToBidder.setManaged(true);
        } else {
            menuBecomeSeller.setVisible(true);
            menuBecomeSeller.setManaged(true);

            menuSellerHome.setVisible(false);
            menuSellerHome.setManaged(false);

            menuManageAuctions.setVisible(false);
            menuManageAuctions.setManaged(false);

            menuBackToBidder.setVisible(false);
            menuBackToBidder.setManaged(false);
        }

        titleLabel.setText("Xin chào, Bidder");

        loadContent("/io/auctionsystem/home-bidder.fxml");
        updateMenuSelection(menuHome);
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == null) continue;

            Node first = menu.getChildren().isEmpty() ? null : menu.getChildren().get(0);

            if (menu == selectedMenu) {
                menu.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 8; -fx-cursor: hand;");
                if (first != null) {
                    first.setStyle("-fx-background-color: transparent; -fx-text-fill: #2b6cb0; -fx-font-weight: bold; -fx-font-size: 16px;");
                }
            } else {
                menu.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
                if (first != null) {
                    if (menu == menuBecomeSeller) {
                        first.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 15px;");
                    } else if (menu == menuSellerHome) {
                        first.setStyle("-fx-background-color: transparent; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else if (menu == menuBackToBidder) {
                        first.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 16px;");
                    } else {
                        first.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a5568; -fx-font-size: 16px;");
                    }
                }
            }
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BecomeSellerController) {
                ((BecomeSellerController) controller).setHomeController(this);
            }

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Lỗi", "Không load được: " + fxmlPath);
        }
    }

    private void showPlaceholder(String title, String subtitle) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10);
        box.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.control.Label titleLb = new javafx.scene.control.Label(title);
        titleLb.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2e384d;");

        javafx.scene.control.Label subLb = new javafx.scene.control.Label(subtitle);
        subLb.setStyle("-fx-font-size: 15px; -fx-text-fill: #94a3b8;");

        box.getChildren().addAll(titleLb, subLb);
        contentArea.getChildren().setAll(box);
    }
}