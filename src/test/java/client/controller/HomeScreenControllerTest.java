package client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import java.util.Arrays;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class HomeScreenControllerTest {
    @Test
    void menuAndSearchHelpersHandleInjectedControls() throws Exception {
        runFx(() -> {
            HomeScreenController controller = new HomeScreenController();
            HBox home = menuWithButton("home");
            HBox upgrade = menuWithButton("upgrade");
            HBox settings = menuWithButton("settings");
            TextField search = new TextField("   ");
            Button clear = new Button();

            setField(controller, "menuHome", home);
            setField(controller, "menuUpgrade", upgrade);
            setField(controller, "menuSettings", settings);
            setField(controller, "allMenus", Arrays.asList(home, null, upgrade, settings));
            setField(controller, "productSearchInput", search);
            setField(controller, "clearSearchBtn", clear);
            setField(controller, "showingSearchResults", false);

            invoke(controller, "updateMenuSelection", upgrade);
            assertTrue(upgrade.getStyleClass().contains("menu-item-active"));
            assertFalse(home.getStyleClass().contains("menu-item-active"));

            setField(controller, "currentUser", new RegularUser("u1", "alice", "pw", "alice@example.com"));
            invoke(controller, "updateMenuBasedOnSellerStatus");
            assertTrue(((Button) upgrade.getChildren().get(0)).getText().contains("Seller"));

            invoke(controller, "handleProductSearch");
            invoke(controller, "clearSearch");
            assertEquals("", search.getText());
            assertNull(controller.getClientSocket());
        });
    }

    @Test
    void viewLoadersCoverNullContentAreaPaths() throws Exception {
        runFx(() -> {
            HomeScreenController controller = new HomeScreenController();
            HBox home = menuWithButton("home");
            HBox recent = menuWithButton("recent");
            HBox msg = menuWithButton("msg");
            HBox pay = menuWithButton("pay");
            HBox upgrade = menuWithButton("upgrade");
            HBox settings = menuWithButton("settings");

            setField(controller, "menuHome", home);
            setField(controller, "menuRecent", recent);
            setField(controller, "menuMsg", msg);
            setField(controller, "menuPay", pay);
            setField(controller, "menuUpgrade", upgrade);
            setField(controller, "menuSettings", settings);
            setField(controller, "allMenus", Arrays.asList(home, recent, msg, pay, upgrade, settings));
            setField(controller, "currentUser", new RegularUser("u1", "alice", "pw", "alice@example.com"));

            controller.loadDashboardView();
            controller.loadProfileView();
            controller.loadCategoryView("/missing.fxml");
            controller.loadNotificationsView();
            controller.loadWalletView();
            controller.loadAuctionHistoryView();
            invoke(controller, "openBecomeSeller");

            assertTrue(upgrade.getStyleClass().contains("menu-item-active"));
        });
    }

    @Test
    void initializeWiresSearchVisibility() throws Exception {
        runFx(() -> {
            HomeScreenController controller = new HomeScreenController();
            TextField search = new TextField();
            Button clear = new Button();

            setField(controller, "menuHome", menuWithButton("home"));
            setField(controller, "menuAI", menuWithButton("ai"));
            setField(controller, "menuRecent", menuWithButton("recent"));
            setField(controller, "menuFlash", menuWithButton("flash"));
            setField(controller, "menuMsg", menuWithButton("msg"));
            setField(controller, "menuPay", menuWithButton("pay"));
            setField(controller, "menuUpgrade", menuWithButton("upgrade"));
            setField(controller, "menuSettings", menuWithButton("settings"));
            setField(controller, "rootPane", new StackPane());
            setField(controller, "productSearchInput", search);
            setField(controller, "clearSearchBtn", clear);

            controller.initialize();
            search.setText("phone");
            assertTrue(clear.isVisible());
            search.clear();
            assertFalse(clear.isVisible());
        });
    }
}
