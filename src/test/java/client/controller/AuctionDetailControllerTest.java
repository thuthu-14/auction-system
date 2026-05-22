package client.controller;

import com.google.gson.JsonObject;
import common.AuctionStatus;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDetailControllerTest {
    @Test
    void helperMethodsDelegateToAuctionDetailLogicAfterRefactor() throws Exception {
        AuctionDetailController controller = new AuctionDetailController();
        setField(controller, "currentAuctionId", "A1");

        Auction current = new Auction();
        current.setAuctionId("A1");
        current.setStatus(AuctionStatus.OPEN);
        Auction other = new Auction();
        other.setAuctionId("A2");

        assertTrue((boolean) invoke(controller, "isCurrentAuction", current));
        assertFalse((boolean) invoke(controller, "isCurrentAuction", other));
        assertFalse((boolean) invoke(controller, "isCurrentAuction", new Object[]{null}));
        assertEquals("", invoke(controller, "safeRealtimeText", new Object[]{null}));
        assertEquals("", invoke(controller, "safeRealtimeText", "   "));
        assertEquals("ok", invoke(controller, "safeRealtimeText", "ok"));
        assertEquals("1.235 VND", invoke(controller, "formatVnd", 1234.56));
        assertEquals("1.235", invoke(controller, "formatPlainNumber", 1234.56));
    }

    @Test
    void loadAuctionDataPopulatesLabelsAndBidAmount() throws Exception {
        runFx(() -> {
            AuctionDetailController controller = new AuctionDetailController();
            Label name = new Label();
            Label description = new Label();
            Label price = new Label();
            TextField bidAmount = new TextField();
            setField(controller, "productNameLabel", name);
            setField(controller, "descriptionLabel", description);
            setField(controller, "currentPriceLabel", price);
            setField(controller, "bidAmountField", bidAmount);

            controller.loadAuctionData(detailJson("A1", "IT1", "Vintage Clock", "Old clock", 2_000_000, 250_000));

            assertEquals("Vintage Clock", name.getText());
            assertEquals("Old clock", description.getText());
            assertEquals("2.000.000 VND", price.getText());
            assertEquals("2.250.000", bidAmount.getText());
            assertEquals("A1", field(controller, "currentAuctionId"));
            assertEquals("IT1", field(controller, "currentItemId"));
        });
    }

    @Test
    void bidStepperAndLatestPriceUseCurrentPriceAndIncrement() throws Exception {
        runFx(() -> {
            AuctionDetailController controller = new AuctionDetailController();
            Label price = new Label();
            TextField bidAmount = new TextField("2.250.000");
            setField(controller, "currentPriceLabel", price);
            setField(controller, "bidAmountField", bidAmount);
            setField(controller, "currentPrice", 2_000_000.0);
            setField(controller, "minimumBidIncrement", 250_000.0);

            invoke(controller, "handleIncreaseBid");
            assertEquals("2.500.000", bidAmount.getText());

            invoke(controller, "handleDecreaseBid");
            assertEquals("2.250.000", bidAmount.getText());

            invoke(controller, "applyLatestBidPrice", 1_900_000.0);
            assertEquals("", price.getText());

            invoke(controller, "applyLatestBidPrice", 2_750_000.0);
            assertEquals("2.750.000 VND", price.getText());
            assertEquals("3.000.000", bidAmount.getText());
        });
    }

    @Test
    void endedModeTogglesBidControlsAndClearViewResetsVisibleData() throws Exception {
        runFx(() -> {
            AuctionDetailController controller = new AuctionDetailController();
            VBox bidAction = new VBox();
            Label ended = new Label();
            Button confirm = new Button();
            Label name = new Label("name");
            setField(controller, "bidActionBox", bidAction);
            setField(controller, "endedAuctionLabel", ended);
            setField(controller, "confirmBidBtn", confirm);
            setField(controller, "productNameLabel", name);

            controller.setEndedMode(true);
            assertFalse(bidAction.isVisible());
            assertFalse(bidAction.isManaged());
            assertTrue(ended.isVisible());
            assertTrue(confirm.isDisabled());

            controller.loadAuctionData((JsonObject) null);
            assertEquals("Không có dữ liệu", name.getText());
        });
    }

    @Test
    void syncPriceFromBidHistoryUsesLatestBidAndIgnoresEmptyLists() throws Exception {
        runFx(() -> {
            AuctionDetailController controller = new AuctionDetailController();
            Label price = new Label();
            TextField bidAmount = new TextField();
            setField(controller, "currentPriceLabel", price);
            setField(controller, "bidAmountField", bidAmount);
            setField(controller, "currentPrice", 1_000_000.0);
            setField(controller, "minimumBidIncrement", 100_000.0);

            invoke(controller, "syncPriceFromBidHistory", List.of());
            assertEquals("", price.getText());

            Bid first = new Bid("B1", "A1", "u1", "alice", 1_200_000.0);
            first.setBidTime(1L);
            Bid second = new Bid("B2", "A1", "u2", "bob", 1_500_000.0);
            second.setBidTime(2L);
            invoke(controller, "syncPriceFromBidHistory", List.of(first, second));

            assertEquals("1.500.000 VND", price.getText());
            assertEquals("1.600.000", bidAmount.getText());
        });
    }

    private static JsonObject detailJson(String auctionId, String itemId, String name, String description,
                                         double currentPrice, double increment) {
        JsonObject item = new JsonObject();
        item.addProperty("itemId", itemId);
        item.addProperty("name", name);
        item.addProperty("description", description);
        item.addProperty("startingPrice", 1_000_000.0);
        item.addProperty("type", "OTHER");

        JsonObject root = new JsonObject();
        root.addProperty("auctionId", auctionId);
        root.addProperty("currentPrice", currentPrice);
        root.addProperty("minimumBidIncrement", increment);
        root.add("item", item);
        return root;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
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
