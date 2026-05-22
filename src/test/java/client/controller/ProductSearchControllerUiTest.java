package client.controller;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.Test;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductSearchControllerUiTest {
    @Test
    void showsEmptyStatesWithoutConnection() throws Exception {
        runFx(() -> {
            ProductSearchController controller = new ProductSearchController();
            Label title = new Label();
            Label summary = new Label();
            FlowPane results = new FlowPane();

            setField(controller, "titleLabel", title);
            setField(controller, "summaryLabel", summary);
            setField(controller, "resultsFlowPane", results);

            controller.initialize();
            assertEquals(1, results.getChildren().size());

            controller.search("   ");
            assertTrue(title.getText().contains("T"));
            assertEquals(1, results.getChildren().size());

            controller.search("watch");
            assertTrue(title.getText().contains("watch"));
            assertEquals(1, results.getChildren().size());
            assertFalse(summary.getText().isBlank());

            invoke(controller, "renderResults", List.of());
            assertEquals(1, results.getChildren().size());
        });
    }
}
