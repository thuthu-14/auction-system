package client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Notification;
import server.model.RegularUser;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificationsControllerTest {
    @Test
    void rendersVisibleRowsAndEmptyState() throws Exception {
        runFx(() -> {
            NotificationsController controller = new NotificationsController();
            VBox container = new VBox();
            Button markAll = new Button();
            setField(controller, "notificationsContainer", container);
            setField(controller, "markAllReadBtn", markAll);
            RegularUser user = new RegularUser("u1", "user", "pw", "u@example.com");
            setField(controller, "currentUser", user);

            controller.initialize(null, null);
            assertNotNull(markAll.getOnAction());

            Notification win1 = new Notification("u1", "WIN", "You Won!", "Congrats", "now", "View", "A1");
            Notification win2 = new Notification("u1", "WIN", "You Won Again!", "Another auction", "now", "View", "A2");
            invoke(controller, "renderNotifications", List.of(win1, win2, "ignored"));
            assertEquals(2, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof HBox);
            HBox row = (HBox) container.getChildren().get(1);
            VBox contentBox = (VBox) row.getChildren().get(1);
            Label titleLabel = (Label) contentBox.getChildren().get(0);
            Label descLabel = (Label) contentBox.getChildren().get(1);
            VBox actionBox = (VBox) row.getChildren().get(2);
            assertTrue(titleLabel.isWrapText());
            assertTrue(descLabel.isWrapText());
            assertEquals(2, actionBox.getChildren().size());
            invoke(controller, "renderNotifications", List.of(win1));
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof HBox);

            user.setShopPhone("0912345678");
            user.setShopAddress("Ha Noi");
            invoke(controller, "renderNotifications", List.of());
            assertEquals(1, container.getChildren().size());
            assertTrue(container.getChildren().get(0) instanceof Label);

            assertEquals("fallback", invoke(controller, "defaultText", "   ", "fallback"));
            assertEquals("value", invoke(controller, "defaultText", "value", "fallback"));

            Object timer = field(controller, "timeRefreshTimer");
            assertNotNull(timer);
            timer.getClass().getMethod("stop").invoke(timer);
        });
    }
}
