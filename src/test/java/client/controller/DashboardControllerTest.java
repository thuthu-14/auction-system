package client.controller;

import common.AuctionStatus;
import common.ItemCategory;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DashboardControllerTest {
    @Test
    void helperMethodsDelegateToDashboardLogicAfterRefactor() throws Exception {
        DashboardController controller = new DashboardController();
        Auction visible = auction(AuctionStatus.OPEN);
        Auction hidden = auction(AuctionStatus.FINISHED);

        assertTrue((boolean) invoke(controller, "isVisibleAuction", visible));
        assertFalse((boolean) invoke(controller, "isVisibleAuction", hidden));
        assertEquals(0, invoke(controller, "getBidCount", visible));

        int score = (int) invoke(controller,
                "recommendationScore",
                visible,
                Map.of(ItemCategory.OTHER, 3),
                Map.of(visible.getAuctionId(), 5));
        assertTrue(score > 0);
    }

    @Test
    void renderProductCardsShowsEmptyStatesWhenNoAuctions() throws Exception {
        runFx(() -> {
            DashboardController controller = new DashboardController();
            HBox ending = new HBox();
            HBox suggested = new HBox();
            Button prevEnding = new Button();
            Button nextEnding = new Button();
            Button prevSuggested = new Button();
            Button nextSuggested = new Button();

            setField(controller, "endingSoonHBox", ending);
            setField(controller, "suggestedHBox", suggested);
            setField(controller, "prevEndingBtn", prevEnding);
            setField(controller, "nextEndingBtn", nextEnding);
            setField(controller, "prevSuggestedBtn", prevSuggested);
            setField(controller, "nextSuggestedBtn", nextSuggested);

            invoke(controller, "renderProductCards", List.of());

            assertEquals(1, ending.getChildren().size());
            assertTrue(ending.getChildren().get(0) instanceof VBox);
            assertTrue(suggested.getChildren().isEmpty());
            assertFalse(prevEnding.isVisible());
            assertFalse(nextEnding.isVisible());
            assertFalse(prevSuggested.isVisible());
            assertFalse(nextSuggested.isVisible());
        });
    }

    @Test
    void carouselButtonsOnlyShowWhenMoreThanFourCardsExist() throws Exception {
        runFx(() -> {
            DashboardController controller = new DashboardController();
            HBox ending = new HBox();
            HBox suggested = new HBox();
            Button prevEnding = new Button();
            Button nextEnding = new Button();
            Button prevSuggested = new Button();
            Button nextSuggested = new Button();

            setField(controller, "endingSoonHBox", ending);
            setField(controller, "suggestedHBox", suggested);
            setField(controller, "prevEndingBtn", prevEnding);
            setField(controller, "nextEndingBtn", nextEnding);
            setField(controller, "prevSuggestedBtn", prevSuggested);
            setField(controller, "nextSuggestedBtn", nextSuggested);

            ending.getChildren().addAll(new VBox(), new VBox(), new VBox(), new VBox());
            suggested.getChildren().addAll(new VBox(), new VBox(), new VBox(), new VBox(), new VBox());

            invoke(controller, "updateEndingSoonButtons");
            invoke(controller, "updateSuggestedButtons");

            assertFalse(prevEnding.isVisible());
            assertFalse(nextEnding.isManaged());
            assertTrue(prevSuggested.isVisible());
            assertTrue(nextSuggested.isManaged());
        });
    }

    @Test
    void setupScrollControlsWiresActionsAndScrollsWithinBounds() throws Exception {
        runFx(() -> {
            DashboardController controller = new DashboardController();
            ScrollPane endingScroll = new ScrollPane();
            ScrollPane suggestedScroll = new ScrollPane();
            HBox ending = new HBox(new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
            HBox suggested = new HBox(new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
            Button prevEnding = new Button();
            Button nextEnding = new Button();
            Button prevSuggested = new Button();
            Button nextSuggested = new Button();

            setField(controller, "endingSoonScroll", endingScroll);
            setField(controller, "suggestedScroll", suggestedScroll);
            setField(controller, "endingSoonHBox", ending);
            setField(controller, "suggestedHBox", suggested);
            setField(controller, "prevEndingBtn", prevEnding);
            setField(controller, "nextEndingBtn", nextEnding);
            setField(controller, "prevSuggestedBtn", prevSuggested);
            setField(controller, "nextSuggestedBtn", nextSuggested);

            invoke(controller, "setupEndingSoonControls");
            invoke(controller, "setupSuggestedControls");
            invoke(controller, "scrollPaneSmoothly", endingScroll, 1);
            invoke(controller, "scrollPaneSmoothly", suggestedScroll, -1);

            assertNotNull(prevEnding.getOnAction());
            assertNotNull(nextEnding.getOnAction());
            assertNotNull(prevSuggested.getOnAction());
            assertNotNull(nextSuggested.getOnAction());
            assertTrue(prevEnding.isVisible());
            assertTrue(nextSuggested.isVisible());
            assertTrue(endingScroll.getHvalue() >= 0.0 && endingScroll.getHvalue() <= 1.0);
            assertTrue(suggestedScroll.getHvalue() >= 0.0 && suggestedScroll.getHvalue() <= 1.0);
        });
    }

    private static Auction auction(AuctionStatus status) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", "Clock", "desc", 100.0, "seller-1", List.of());
        Auction auction = new Auction("A1", "IT1", "seller-1", "Seller", item, 100.0, now - 1_000L, now + 60_000L);
        auction.setStatus(status);
        return auction;
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args.length);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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
