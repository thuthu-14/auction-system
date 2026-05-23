package client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Notification;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SellerNotificationsControllerTest {
    @Test
    void rendersRowsAndWiresActions() throws Exception {
        runFx(() -> {
            SellerNotificationsController controller = new SellerNotificationsController();
            VBox container = new VBox();
            Button markAll = new Button();
            setField(controller, "notificationsContainer", container);
            setField(controller, "markAllReadBtn", markAll);

            controller.initialize(null, null);
            assertNotNull(markAll.getOnAction());

            // Thành (thêm proper title/description):
            Notification sale1 = new Notification("u1", "SALE", "Item Sold!", "Congrats", "now", "View", "A1");
            Notification sale2 = new Notification("u1", "SALE", "Another Item Sold!", "You got payment", "now", "View", "A2");
            invoke(controller, "renderNotifications", List.of(sale1, "ignored"));
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof HBox);

            HBox row = (HBox) container.getChildren().get(0);
            VBox contentBox = (VBox) row.getChildren().get(1);
            Label titleLabel = (Label) contentBox.getChildren().get(0);
            Label descLabel = (Label) contentBox.getChildren().get(1);
            VBox actionBox = (VBox) row.getChildren().get(2);
            assertTrue(titleLabel.isWrapText());
            assertTrue(descLabel.isWrapText());

            assertEquals(2, actionBox.getChildren().size());
            ((Button) actionBox.getChildren().get(1)).fire();

            invoke(controller, "renderNotifications", List.of());
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof Label);

            assertEquals("fallback", invoke(controller, "defaultText", null, "fallback"));
            assertEquals("value", invoke(controller, "defaultText", "value", "fallback"));

            Object timer = field(controller, "timeRefreshTimer");
            assertNotNull(timer);
            timer.getClass().getMethod("stop").invoke(timer);
        });
    }
}
