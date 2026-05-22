package client.controller;

import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminHomeControllerTest {
    @Test
    void menuSelectionAndLogoutCallbackWorkWithoutFxmlLoad() throws Exception {
        runFx(() -> {
            AdminHomeController controller = new AdminHomeController();
            HBox users = menuWithButton("users");
            HBox auctions = menuWithButton("auctions");
            HBox logout = menuWithButton("logout");
            boolean[] loggedOut = {false};

            setField(controller, "menuRecent", users);
            setField(controller, "menuMsg", auctions);
            setField(controller, "menuUpgrade", logout);
            setField(controller, "allMenus", List.of(users, auctions, logout));
            controller.setOnLogout(() -> loggedOut[0] = true);

            invoke(controller, "updateMenuSelection", auctions);
            assertTrue(auctions.getStyleClass().contains("menu-item-active"));
            assertFalse(users.getStyleClass().contains("menu-item-active"));
            assertFalse(logout.getStyleClass().contains("menu-item-active"));

            invoke(controller, "loadLogOutView");
            assertTrue(loggedOut[0]);
            assertTrue(logout.getStyleClass().contains("menu-item-active"));
        });
    }

    @Test
    void initializeBuildsMenuList() throws Exception {
        AdminHomeController controller = new AdminHomeController();
        setField(controller, "menuRecent", new HBox());
        setField(controller, "menuMsg", new HBox());
        setField(controller, "menuUpgrade", new HBox());

        controller.initialize();

        assertNotNull(field(controller, "allMenus"));
    }
}
