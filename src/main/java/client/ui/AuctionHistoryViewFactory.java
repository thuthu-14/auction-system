package client.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AuctionHistoryViewFactory {

    public VBox createContactContent(String name, String email, String phone, String address) {
        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 0, 0));
        content.setPrefWidth(340);
        content.setMinWidth(340);
        content.setMaxWidth(340);
        content.getChildren().addAll(
                contactLine("T\u00ean: " + name),
                contactLine("Email: " + email),
                contactLine("S\u0110T: " + phone),
                contactLine("\u0110\u1ecba ch\u1ec9: " + address)
        );
        return content;
    }

    public Button createActionButton() {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    public void configureActiveButton(Button button, boolean outbid) {
        button.setText(outbid ? "Tra gia tiep" : "Dau gia ngay");
        button.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
    }

    public void configureWinnerButton(Button button) {
        button.setText("Lien he");
        button.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
    }

    public void configureEndedButton(Button button) {
        button.setText("Xem chi tiet");
        button.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563; -fx-background-radius: 6; -fx-font-weight: 700; -fx-cursor: hand;");
    }

    private Label contactLine(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(0);
        label.setPrefWidth(340);
        label.setMaxWidth(340);
        label.setMinHeight(Region.USE_PREF_SIZE);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #020617;");
        return label;
    }
}
