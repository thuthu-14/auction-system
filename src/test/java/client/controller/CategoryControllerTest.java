package client.controller;

import common.AuctionStatus;
import common.ItemCategory;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CategoryControllerTest {
    @Test
    void initializeWiresButtonsAndHeroImageClip() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("electronicsFlowPane");
            Button back = (Button) field(controller, "backBtn");
            Button heroBid = (Button) field(controller, "heroBidButton");
            ImageView hero = (ImageView) field(controller, "heroImageView");
            ComboBox<?> sort = (ComboBox<?>) field(controller, "sortCombo");

            controller.initialize();

            assertNotNull(back.getOnAction());
            assertNotNull(heroBid.getOnAction());
            assertNotNull(hero.getClip());
            assertEquals(190.0, hero.getFitWidth());
            assertFalse(sort.getItems().isEmpty());
            assertEquals("Giá cao nhất", sort.getValue());
        });
    }

    @Test
    void activeFlowPaneAndCategoryFollowInjectedPane() throws Exception {
        runFx(() -> {
            CategoryController electronics = baseController("electronicsFlowPane");
            assertEquals(ItemCategory.ELECTRONICS, invoke(electronics, "getCurrentCategory"));
            assertSame(field(electronics, "electronicsFlowPane"), invoke(electronics, "getActiveFlowPane"));

            CategoryController art = baseController("productsFlowPane");
            assertEquals(ItemCategory.ART, invoke(art, "getCurrentCategory"));

            CategoryController other = baseController("otherFlowPane");
            assertEquals(ItemCategory.OTHER, invoke(other, "getCurrentCategory"));
        });
    }

    @Test
    void renderAuctionsShowsEmptyStateWhenNoVisibleMatchingAuctions() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("fashionFlowPane");
            FlowPane pane = (FlowPane) field(controller, "fashionFlowPane");
            Auction electronics = auction("A1", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, 1, 100);
            Auction finishedFashion = auction("A2", ItemCategory.FASHION, AuctionStatus.FINISHED, 2, 200);

            invoke(controller, "renderAuctions", List.of(electronics, finishedFashion));

            assertEquals(1, pane.getChildren().size());
            assertTrue(pane.getChildren().get(0) instanceof VBox);
        });
    }

    @Test
    void renderAuctionsSelectsHeroAndUpdatesHeroText() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("electronicsFlowPane");
            FlowPane pane = (FlowPane) field(controller, "electronicsFlowPane");
            Auction low = auction("A1", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, 1, 100);
            Auction hero = auction("A2", ItemCategory.ELECTRONICS, AuctionStatus.RUNNING, 3, 500);

            invoke(controller, "renderAuctions", List.of(low, hero));

            assertSame(hero, field(controller, "heroAuction"));
            assertEquals("Item A2", label(controller, "heroTitleLabel").getText());
            assertTrue(label(controller, "heroDescriptionLabel").getText().contains("3"));
            assertFalse(pane.getChildren().isEmpty());
        });
    }

    @Test
    void heroImageFallbackTogglesIconAndImage() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("vehiclesFlowPane");
            ImageView hero = (ImageView) field(controller, "heroImageView");
            Label icon = label(controller, "heroIconLabel");

            invoke(controller, "showHeroIcon");

            assertFalse(hero.isVisible());
            assertFalse(hero.isManaged());
            assertTrue(icon.isVisible());
            assertTrue(icon.isManaged());
        });
    }

    @Test
    void helpersClassifyVisibilityBidCountAndColors() throws Exception {
        runFx(() -> {
            CategoryController electronics = baseController("electronicsFlowPane");
            Auction auction = auction("A1", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, 2, 100);

            assertTrue((boolean) invoke(electronics, "isVisibleAuction", auction));
            assertFalse((boolean) invoke(electronics, "isVisibleAuction", auction("A2", ItemCategory.ELECTRONICS, AuctionStatus.CANCELLED, 0, 0)));
            assertEquals(2, invoke(electronics, "getBidCount", auction));
            assertEquals("#1e3a8a", invoke(electronics, "getHeroDescriptionColor"));

            CategoryController art = baseController("productsFlowPane");
            assertEquals("#78350f", invoke(art, "getHeroDescriptionColor"));
            assertNotNull(invoke(art, "createEmptyState"));
        });
    }

    @Test
    void sortComparatorFollowsSelectedOption() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("electronicsFlowPane");
            ComboBox<String> sort = (ComboBox<String>) field(controller, "sortCombo");
            Auction cheap = auction("A1", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, 1, 100);
            Auction pricey = auction("A2", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, 3, 500);

            invoke(controller, "configureSortCombo");
            Comparator<Auction> byDefault = (Comparator<Auction>) invoke(controller, "getAuctionComparator");
            assertTrue(byDefault.compare(pricey, cheap) < 0);

            sort.setValue("Nhiều lượt đặt giá");
            Comparator<Auction> byBids = (Comparator<Auction>) invoke(controller, "getAuctionComparator");
            assertTrue(byBids.compare(pricey, cheap) < 0);

            sort.setValue("Giá thấp nhất");
            Comparator<Auction> byLowPrice = (Comparator<Auction>) invoke(controller, "getAuctionComparator");
            assertTrue(byLowPrice.compare(cheap, pricey) < 0);
        });
    }

    @Test
    void forceCategoryTextColorsRestoresReadableLabels() throws Exception {
        runFx(() -> {
            CategoryController controller = baseController("productsFlowPane");
            Label pageTitle = new Label("Đồ sưu tầm");
            Label count = new Label("Tất cả sản phẩm (124)");
            Label badge = new Label("TIÊU ĐIỂM");
            badge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white;");
            Label heroTitle = label(controller, "heroTitleLabel");
            Label heroDesc = label(controller, "heroDescriptionLabel");
            Label heroIcon = label(controller, "heroIconLabel");

            pageTitle.setTextFill(Color.WHITE);
            count.setTextFill(Color.WHITE);
            heroTitle.setTextFill(Color.WHITE);
            heroDesc.setTextFill(Color.WHITE);
            heroIcon.setTextFill(Color.WHITE);
            VBox content = new VBox(pageTitle, badge, heroTitle, heroDesc, heroIcon, count);
            setField(controller, "categoryRoot", new ScrollPane(content));

            invoke(controller, "forceCategoryTextColors");

            assertEquals(Color.web("#1a1a1a"), pageTitle.getTextFill());
            assertEquals(Color.web("#ffffff"), badge.getTextFill());
            assertEquals(Color.web("#1a1a1a"), heroTitle.getTextFill());
            assertEquals(Color.web("#78350f"), heroDesc.getTextFill());
            assertEquals(Color.web("#78350f"), heroIcon.getTextFill());
            assertEquals(Color.web("#718096"), count.getTextFill());
        });
    }

    private static CategoryController baseController(String flowPaneField) throws Exception {
        CategoryController controller = new CategoryController();
        setField(controller, flowPaneField, new FlowPane());
        setField(controller, "backBtn", new Button());
        setField(controller, "heroBidButton", new Button());
        setField(controller, "sortCombo", new ComboBox<String>());
        setField(controller, "heroTitleLabel", new Label());
        setField(controller, "heroDescriptionLabel", new Label());
        setField(controller, "heroImageView", new ImageView());
        setField(controller, "heroIconLabel", new Label());
        return controller;
    }

    private static Auction auction(String id, ItemCategory category, AuctionStatus status, int bidCount, double price) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT-" + id, "Item " + id, "desc", 100, "seller", List.of());
        item.setCategory(category);
        Auction auction = new Auction(id, item.getItemId(), "seller", "Seller", item, price, now - 1_000L, now + 60_000L);
        auction.setStatus(status);
        for (int i = 0; i < bidCount; i++) {
            auction.addBidId("B" + i);
        }
        return auction;
    }

    private static Label label(Object target, String name) throws Exception {
        return (Label) field(target, name);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args.length);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, int argCount) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }

    private static void runFx(ThrowingRunnable action) throws Exception {
        startFx();
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX action timed out");
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    private static synchronized void startFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX startup timed out");
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit is process-wide; another controller test may have started it.
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
