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
    @FXML private StackPane contentArea;

    @FXML private TextField productSearchInput;
    @FXML private Button clearSearchBtn;

    @FXML
    public void initialize() {
        allMenus = Arrays.asList(menuHome, menuAI, menuRecent, menuFlash, menuMsg, menuPay, menuUpgrade, menuSettings);
        updateMenuSelection(menuHome);

        productSearchInput.textProperty().addListener((observable, oldValue, newValue) -> {
            clearSearchBtn.setVisible(!newValue.trim().isEmpty());
        });

        Platform.runLater(() -> {
            if (rootPane != null && rootPane.getScene() != null) {
                KeyCombination ctrlK = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
                rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (ctrlK.match(event)) {
                        productSearchInput.requestFocus();
                        event.consume();
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
            if (menu == null) continue;

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
        }
    }

    @FXML
    private void handleProductSearch() {
        String keyword = productSearchInput.getText().trim();
        if (!keyword.isEmpty()) {
            System.out.println("Searching for: " + keyword);
        }
    }

    @FXML
    private void clearSearch() {
        productSearchInput.clear();
        productSearchInput.requestFocus();
    }
}