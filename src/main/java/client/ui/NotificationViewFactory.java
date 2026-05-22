package client.ui;

import client.service.NotificationClient;
import client.service.NotificationClientService.NotificationPresentation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import server.model.Notification;

import java.util.function.Consumer;

public class NotificationViewFactory {

    private final NotificationClient notificationService;

    public NotificationViewFactory(NotificationClient notificationService) {
        this.notificationService = notificationService;
    }

    public HBox createNotificationRow(
            Notification data,
            Consumer<Notification> actionHandler,
            Consumer<Runnable> timeLabelUpdaterRegistrar
    ) {
        HBox container = new HBox(20);
        container.setAlignment(Pos.TOP_LEFT);
        container.setStyle(notificationService.containerStyle(data));

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setMinSize(60, 60);
        iconBox.setStyle(presentation(data).iconBoxStyle());

        Label iconLabel = new Label(presentation(data).icon());
        iconLabel.setFont(Font.font(28));
        iconLabel.setStyle("-fx-text-fill: " + presentation(data).iconColor() + ";");
        iconBox.getChildren().add(iconLabel);

        VBox contentBox = new VBox(5);
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(defaultText(data.getTitle(), "Thong bao"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-text-fill: " + presentation(data).titleColor() + ";");

        Label descLabel = new Label(defaultText(data.getDescription(), ""));
        descLabel.setFont(Font.font("System", 14));
        descLabel.setWrapText(true);
        descLabel.setMinWidth(0);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setStyle("-fx-text-fill: #4a5568;");

        contentBox.getChildren().addAll(titleLabel, descLabel);

        VBox actionBox = new VBox();
        actionBox.setAlignment(Pos.TOP_RIGHT);
        actionBox.setMinWidth(132);
        actionBox.setPrefWidth(132);
        actionBox.setMaxWidth(132);
        actionBox.setMinHeight(68);

        Label timeLabel = new Label(data.formatTimeAgo());
        timeLabel.setFont(Font.font("System", 12));
        timeLabel.setWrapText(true);
        timeLabel.setMaxWidth(132);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setStyle("-fx-text-fill: #9ca3af;");
        timeLabelUpdaterRegistrar.accept(() -> timeLabel.setText(data.formatTimeAgo()));

        Button actionBtn = new Button(defaultText(data.getButtonText(), "Xem chi tiet"));
        actionBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        actionBtn.setWrapText(true);
        actionBtn.setMaxWidth(132);
        actionBtn.setStyle(presentation(data).buttonStyle());
        actionBtn.setOnAction(event -> actionHandler.accept(data));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        actionBox.getChildren().addAll(actionBtn, spacer, timeLabel);
        container.getChildren().addAll(iconBox, contentBox, actionBox);

        return container;
    }

    public Label createStateLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web("#a0aec0"));
        label.setFont(Font.font("System", 15));
        label.setStyle("-fx-text-fill: #a0aec0;");
        return label;
    }

    private NotificationPresentation presentation(Notification data) {
        return notificationService.presentation(data);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
