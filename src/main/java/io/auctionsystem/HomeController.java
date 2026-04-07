package io.auctionsystem;

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

    @FXML private HBox menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings;
    private List<HBox> allMenus;

    @FXML private StackPane rootPane;
    @FXML private StackPane searchOverlay;
    @FXML private TextField searchInput;
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings);
        updateMenuSelection(menuHome);

        Platform.runLater(() -> {
            KeyCombination ctrlK = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);

            if (rootPane.getScene() != null) {
                rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (ctrlK.match(event)) {
                        openSearchOverlay();
                        event.consume();
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        closeSearchOverlay();
                    }
                });
            }
        });
    }

    @FXML
    private void handleMenuClick(MouseEvent event) {
        HBox clickedMenu = (HBox) event.getSource();
        updateMenuSelection(clickedMenu);
    }

    private void updateMenuSelection(HBox selectedMenu) {
        for (HBox menu : allMenus) {
            if (menu == selectedMenu) {
                menu.setStyle("-fx-background-color: #edf2f7; -fx-background-radius: 8; -fx-cursor: hand;");
                Button btn = (Button) menu.getChildren().get(0);
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2b6cb0; -fx-font-weight: bold;");
            } else {
                menu.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
                Button btn = (Button) menu.getChildren().get(0);

                if (menu == menuUpgrade) {
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");
                } else {
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a5568;");
                }
            }
        }
    }

    @FXML
    private void openSearchOverlay() {
        searchOverlay.setVisible(true);
        Platform.runLater(() -> searchInput.requestFocus());
    }

    @FXML
    private void closeSearchOverlay() {
        searchOverlay.setVisible(false);
        searchInput.clear();
    }
}