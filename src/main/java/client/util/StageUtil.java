package client.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class StageUtil {
    private StageUtil() {
    }

    public static void showMaximized(Stage stage) {
        if (stage == null) {
            return;
        }

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.show();
        Platform.runLater(() -> stage.setMaximized(true));
    }
}
