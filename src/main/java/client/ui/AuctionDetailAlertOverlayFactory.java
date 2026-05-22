package client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AuctionDetailAlertOverlayFactory {

    public void show(StackPane root, Alert.AlertType type, String title, String content) {
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(17, 24, 39, 0.28);");

        String accent = accentColor(type);
        VBox dialog = createDialog(title, content, accent);
        Button okButton = createOkButton(accent);
        okButton.setOnAction(event -> root.getChildren().remove(overlay));

        dialog.getChildren().add(okButton);
        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
        okButton.requestFocus();
    }

    private VBox createDialog(String title, String content, String accent) {
        VBox dialog = new VBox(8);
        dialog.setAlignment(Pos.CENTER_LEFT);
        dialog.setPadding(new Insets(14, 18, 14, 18));
        dialog.setPrefWidth(300);
        dialog.setMaxWidth(300);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 18, 0, 0, 6);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: " + accent + ";");

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

        dialog.getChildren().addAll(titleLabel, contentLabel);
        return dialog;
    }

    private Button createOkButton(String accent) {
        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setStyle(
                "-fx-background-color: " + accent + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 5 16;"
        );
        return okButton;
    }

    private String accentColor(Alert.AlertType type) {
        return switch (type) {
            case ERROR -> "#dc2626";
            case WARNING -> "#f59e0b";
            case INFORMATION -> "#16a34a";
            default -> "#2563eb";
        };
    }
}
