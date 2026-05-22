package client.controller;

import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static client.controller.ControllerTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class SellerHomeControllerTest {
    @Test
    void remembersAuctionIdsAndCleansWalletOverlays() throws Exception {
        runFx(() -> {
            SellerHomeController controller = new SellerHomeController();
            HBox home = new HBox();
            HBox manage = new HBox();
            AnchorPane root = new AnchorPane();
            StackPane content = new StackPane();
            TextField search = new TextField("phone");
            Region overlayByClass = new Region();
            overlayByClass.getStyleClass().add("wallet-modal-overlay");
            Region overlayByStyle = new Region();
            overlayByStyle.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");
            VBox normalNode = new VBox();

            root.getChildren().addAll(overlayByClass, normalNode);
            content.getChildren().add(overlayByStyle);
            content.setOpacity(0.4);
            content.setDisable(true);

            setField(controller, "menuHome", home);
            setField(controller, "menuManageAuctions", manage);
            setField(controller, "allMenus", List.of(home, manage));
            setField(controller, "rootPane", root);
            setField(controller, "contentArea", content);
            setField(controller, "productSearchInput", search);

            assertEquals("A1", controller.rememberSellerAuctionId("A1"));
            assertEquals("A1", controller.rememberSellerAuctionId("--"));
            assertEquals("A1", controller.getSelectedSellerAuctionId());

            invoke(controller, "updateMenuSelection", manage);
            assertTrue(manage.getStyleClass().contains("menu-item-active"));
            assertFalse(home.getStyleClass().contains("menu-item-active"));

            invoke(controller, "clearStaleWalletOverlays");
            assertFalse(root.getChildren().contains(overlayByClass));
            assertTrue(root.getChildren().contains(normalNode));
            assertTrue(content.getChildren().isEmpty());
            assertEquals(1.0, content.getOpacity());
            assertFalse(content.isDisabled());

            ScrollPane scrollPane = new ScrollPane();
            invoke(controller, "prepareContentNode", scrollPane);
            assertTrue(scrollPane.isFitToWidth());
            assertEquals(ScrollPane.ScrollBarPolicy.NEVER, scrollPane.getHbarPolicy());

            invoke(controller, "clearSearch");
            assertEquals("", search.getText());
        });
    }

    @Test
    void viewLoadersCoverNullContentAreaPaths() throws Exception {
        runFx(() -> {
            SellerHomeController controller = new SellerHomeController();
            HBox home = new HBox();
            HBox create = new HBox();
            HBox manage = new HBox();
            HBox stats = new HBox();
            HBox msg = new HBox();
            HBox pay = new HBox();

            setField(controller, "menuHome", home);
            setField(controller, "menuCreateAuctions", create);
            setField(controller, "menuManageAuctions", manage);
            setField(controller, "menuAuctionStatistic", stats);
            setField(controller, "menuMsg", msg);
            setField(controller, "menuPay", pay);
            setField(controller, "allMenus", List.of(home, create, manage, stats, msg, pay));
            setField(controller, "rootPane", new AnchorPane());

            controller.loadSellerDashboardView();
            controller.loadAddAuctionProductView();
            controller.loadSellerStatisticsView();
            controller.loadManageAuctionsView();
            controller.loadSellerNotificationsView();
            controller.loadProfileView();
            controller.loadWalletView();
            controller.loadSellerAuctionDetailsView("A1");
            controller.loadSellerAuctionBidHistoryView("--");
            controller.loadSellerWinnerInfoView(null);

            assertTrue(manage.getStyleClass().contains("menu-item-active"));
        });
    }

    @Test
    void initializeWiresSearchVisibility() throws Exception {
        runFx(() -> {
            SellerHomeController controller = new SellerHomeController();
            TextField search = new TextField();
            Button clear = new Button();

            setField(controller, "menuHome", new HBox());
            setField(controller, "menuAI", new HBox());
            setField(controller, "menuCreateAuctions", new HBox());
            setField(controller, "menuManageAuctions", new HBox());
            setField(controller, "menuAuctionStatistic", new HBox());
            setField(controller, "menuMsg", new HBox());
            setField(controller, "menuPay", new HBox());
            setField(controller, "menuDowngrade", new HBox());
            setField(controller, "menuSettings", new HBox());
            setField(controller, "rootPane", new AnchorPane());
            setField(controller, "productSearchInput", search);
            setField(controller, "clearSearchBtn", clear);

            controller.initialize();
            search.setText("watch");
            assertTrue(clear.isVisible());
            search.clear();
            assertFalse(clear.isVisible());
        });
    }
}
