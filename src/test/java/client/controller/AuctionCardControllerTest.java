package client.controller;

import common.AuctionStatus;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import javafx.scene.image.ImageView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCardControllerTest {
    @Test
    void initializeConfiguresBidButtonAndImageClip() throws Exception {
        runFx(() -> {
            AuctionCardController controller = controllerWithControls();
            Button button = (Button) field(controller, "bidButton");
            StackPane container = (StackPane) field(controller, "imageContainer");

            controller.initialize();

            assertEquals("Đặt giá", button.getText());
            assertNotNull(button.getOnAction());
            assertNotNull(container.getClip());
        });
    }

    @Test
    void setAuctionDataRendersTextPriceAndStartsCountdown() throws Exception {
        runFx(() -> {
            AuctionCardController controller = controllerWithControls();
            controller.initialize();
            Auction auction = auction("A1", "Camera", 1_250_000.0, System.currentTimeMillis() + 60_000L);

            controller.setAuctionData(auction);

            assertEquals("Camera", label(controller, "productNameLabel").getText());
            assertTrue(label(controller, "priceLabel").getText().contains("1"));
            assertEquals("A1", controller.getCurrentAuctionId());
            assertNotNull(field(controller, "countdownTimer"));
            assertTrue(label(controller, "productNameLabel").getStyle().contains("#113254"));
            stopTimer(controller);
        });
    }

    @Test
    void updatePriceChangesAuctionAndLabelOnFxThread() throws Exception {
        AuctionCardController[] controllerRef = new AuctionCardController[1];
        Auction[] auctionRef = new Auction[1];
        runFx(() -> {
            AuctionCardController controller = controllerWithControls();
            controllerRef[0] = controller;
            controller.initialize();
            Auction auction = auction("A2", "Watch", 500_000.0, System.currentTimeMillis() + 60_000L);
            auctionRef[0] = auction;
            controller.setAuctionData(auction);

            controller.updatePrice(750_000.0);
        });
        drainFxEvents();
        runFx(() -> {
            AuctionCardController controller = controllerRef[0];
            Auction auction = auctionRef[0];

            assertEquals(750_000.0, auction.getCurrentPrice(), 0.001);
            assertTrue(label(controller, "priceLabel").getText().contains("750"));
            stopTimer(controller);
        });
    }

    @Test
    void expiredCountdownDisablesBidButtonAndStylesTimer() throws Exception {
        runFx(() -> {
            AuctionCardController controller = controllerWithControls();
            Button button = (Button) field(controller, "bidButton");
            Label timer = label(controller, "timerLabel");
            controller.initialize();
            controller.setAuctionData(auction("A3", "Expired", 100_000.0, System.currentTimeMillis() - 1_000L));

            Timeline timeline = (Timeline) field(controller, "countdownTimer");
            timeline.getKeyFrames().get(0).getOnFinished().handle(null);

            assertTrue(timer.getText().contains("Đã kết thúc"));
            assertTrue(button.isDisabled());
            assertTrue(timer.getStyle().contains("#6b7280"));
            stopTimer(controller);
        });
    }

    @Test
    void imageViewportCropsWideAndTallImagesToContainerRatio() throws Exception {
        runFx(() -> {
            AuctionCardController controller = controllerWithControls();
            StackPane container = (StackPane) field(controller, "imageContainer");
            ImageView imageView = (ImageView) field(controller, "productImageView");
            controller.initialize();
            container.resize(100, 50);
            imageView.setImage(new WritableImage(200, 200));

            invoke(controller, "updateImageViewport");

            assertNotNull(imageView.getViewport());
            assertTrue(imageView.getViewport().getWidth() > 0);
            assertTrue(imageView.getViewport().getHeight() > 0);
        });
    }

    @Test
    void nullAuctionKeepsCurrentAuctionIdEmpty() throws Exception {
        AuctionCardController controller = new AuctionCardController();

        controller.setAuctionData(null);

        assertEquals("", controller.getCurrentAuctionId());
    }

    private static AuctionCardController controllerWithControls() throws Exception {
        AuctionCardController controller = new AuctionCardController();
        ImageView imageView = new ImageView();
        StackPane container = new StackPane();
        container.resize(100, 80);
        setField(controller, "productImageView", imageView);
        setField(controller, "imageContainer", container);
        setField(controller, "productNameLabel", new Label());
        setField(controller, "timerLabel", new Label());
        setField(controller, "priceLabel", new Label());
        setField(controller, "bidButton", new Button());
        return controller;
    }

    private static Auction auction(String id, String name, double price, long endTime) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT-" + id, name, "desc", price, "seller", List.of());
        Auction auction = new Auction(id, item.getItemId(), "seller", "Seller", item, price, now - 1_000L, endTime);
        auction.setStatus(AuctionStatus.OPEN);
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

    private static void stopTimer(Object controller) throws Exception {
        Timeline timer = (Timeline) field(controller, "countdownTimer");
        if (timer != null) {
            timer.stop();
        }
    }

    private static void drainFxEvents() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX drain timed out");
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
