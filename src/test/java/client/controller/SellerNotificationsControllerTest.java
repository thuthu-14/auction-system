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

            Notification sale = new Notification("u1", "SALE", "", "", "now", "", "A1");
            Notification bidderOnly = new Notification("u1", "OUTBID", "Outbid", "Bidder", "now", "Open", "A2");
            invoke(controller, "renderNotifications", List.of(sale, bidderOnly, "ignored"));
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof HBox);

            HBox row = (HBox) container.getChildren().get(0);
            VBox contentBox = (VBox) row.getChildren().get(1);
            Label titleLabel = (Label) contentBox.getChildren().get(0);
            Label descLabel = (Label) contentBox.getChildren().get(1);
            VBox actionBox = (VBox) row.getChildren().get(2);
            assertTrue(titleLabel.isWrapText());
            assertTrue(descLabel.isWrapText());
            assertEquals(132, actionBox.getPrefWidth(), 0.001);
            assertEquals(3, actionBox.getChildren().size());
            assertTrue(actionBox.getChildren().get(2) instanceof Label);
            ((Button) actionBox.getChildren().get(0)).fire();

            invoke(controller, "renderNotifications", List.of(bidderOnly));
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
