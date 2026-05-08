package client.util;

import javafx.geometry.Pos;
import javafx.scene.Group;
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
        Group scaledContent = new Group(content);
        StackPane viewport = new StackPane(scaledContent);
        viewport.setAlignment(Pos.CENTER);
        viewport.setStyle("-fx-background-color: #fcfcfc;");

        lockDesignSize(content);

        viewport.widthProperty().addListener((obs, oldValue, newValue) -> updateScale(viewport, scaledContent));
        viewport.heightProperty().addListener((obs, oldValue, newValue) -> updateScale(viewport, scaledContent));

        Scene scene = new Scene(viewport, sceneWidth, sceneHeight);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> updateScale(viewport, scaledContent));
        scene.heightProperty().addListener((obs, oldValue, newValue) -> updateScale(viewport, scaledContent));
        return scene;
    }

    private static void lockDesignSize(Parent content) {
        if (content instanceof Region region) {
            region.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
            region.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
            region.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        }
    }

    private static void updateScale(StackPane viewport, Group scaledContent) {
        double width = viewport.getWidth();
        double height = viewport.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double scale = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        scaledContent.setScaleX(scale);
        scaledContent.setScaleY(scale);
    }
}
