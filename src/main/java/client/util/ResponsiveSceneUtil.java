package client.util;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class ResponsiveSceneUtil {
    public static final double DESIGN_WIDTH = 1300.0;
    public static final double DESIGN_HEIGHT = 800.0;

    private ResponsiveSceneUtil() {
    }

    public static Scene createScaledScene(Parent content) {
        return createScaledScene(content, DESIGN_WIDTH, DESIGN_HEIGHT);
    }

    public static Scene createScaledScene(Parent content, double sceneWidth, double sceneHeight) {
        StackPane viewport = new StackPane(content);
        viewport.setAlignment(Pos.CENTER);
        viewport.setStyle("-fx-background-color: #fcfcfc;");

        allowContentResize(content);
        return new Scene(viewport, sceneWidth, sceneHeight);
    }

    private static void allowContentResize(Parent content) {
        if (content instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }
}
