package client.controller;

import common.AuctionStatus;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SellerAuctionDetailsControllerTest {
    @Test
    void initializeInstallsRoundedImageClip() throws Exception {
        runFx(() -> {
            SellerAuctionDetailsController controller = new SellerAuctionDetailsController();
            ImageView imageView = new ImageView();
            imageView.setFitWidth(160);
            imageView.setFitHeight(120);
            setField(controller, "productImage", imageView);

            controller.initialize();

            assertNotNull(imageView.getClip());
            assertTrue(imageView.getClip().getBoundsInLocal().getWidth() >= 0);
        });
    }

    @Test
    void updateUIPopulatesAuctionLabelsAndCancellableButton() throws Exception {
        runFx(() -> {
            SellerAuctionDetailsController controller = controllerWithLabels();
            Auction auction = auction("A1", "IT1", AuctionStatus.OPEN, System.currentTimeMillis() + 7_200_000L);
            auction.setBidIds(List.of("B1", "B2"));
            auction.setHighestBidderName("alice");

            invoke(controller, "updateUI", auction);

            assertEquals("Clock", label(controller, "productName").getText());
            assertTrue(label(controller, "productId").getText().contains("A1"));
            assertEquals("2 lượt", label(controller, "bidCount").getText());
            assertEquals("alice", label(controller, "topBidderName").getText());
            assertEquals("OPEN", label(controller, "status").getText());
            assertTrue(label(controller, "timeLeft").getText().contains("gi"));
            Button cancelButton = (Button) field(controller, "cancelAuctionButton");
            assertFalse(cancelButton.isDisabled());
            assertEquals("Hủy phiên", cancelButton.getText());
        });
    }

    @Test
    void updateUIHandlesEndedAuctionAndFallbackValues() throws Exception {
        runFx(() -> {
            SellerAuctionDetailsController controller = controllerWithLabels();
            Auction auction = new Auction();
            auction.setAuctionId(null);
            auction.setItemId("IT-FALLBACK");
            auction.setStatus(AuctionStatus.FINISHED);
            auction.setCurrentPrice(0);
            auction.setEndTime(System.currentTimeMillis() - 1_000L);

            invoke(controller, "updateUI", auction);

            assertEquals("N/A", label(controller, "productName").getText());
            assertTrue(label(controller, "productId").getText().contains("IT-FALLBACK"));
            assertEquals("Chưa có", label(controller, "topBidderName").getText());
            assertTrue(label(controller, "timeLeft").getStyle().contains("#dc2626"));
            Button cancelButton = (Button) field(controller, "cancelAuctionButton");
            assertTrue(cancelButton.isDisabled());
            assertEquals("Đã kết thúc", cancelButton.getText());
        });
    }

    @Test
    void idResolutionAndFormattingCoverFallbackBranches() throws Exception {
        runFx(() -> {
            SellerAuctionDetailsController controller = controllerWithLabels();
            label(controller, "productId").setText("Mã phiên: LABEL-1");

            assertEquals("1.200.000đ", invoke(controller, "formatVnd", 1_200_000.0));
            assertTrue(((String) invoke(controller, "formatDate", 0L)).contains("1970"));
            assertEquals("2 giá 5 phút", invoke(controller, "formatRemain", 7_500L));
            assertEquals("15 phút", invoke(controller, "formatRemain", 900L));
            assertTrue((boolean) invoke(controller, "isUsableKey", "A1"));
            assertFalse((boolean) invoke(controller, "isUsableKey", "--"));
            assertEquals("LABEL-1", invoke(controller, "extractIdFromProductLabel"));
            assertEquals("REQ-1", invoke(controller, "resolveAuctionId", "REQ-1"));

            Auction current = auction(null, "ITEM-2", AuctionStatus.RUNNING, System.currentTimeMillis() + 60_000L);
            setField(controller, "currentAuction", current);
            assertEquals("ITEM-2", invoke(controller, "resolveAuctionId", new Object[]{null}));
            assertEquals("ITEM-2", invoke(controller, "displayAuctionKey", current));
            assertEquals("--", invoke(controller, "displayAuctionKey", new Object[]{null}));
        });
    }

    @Test
    void userFriendlyCancelErrorNormalizesMessages() throws Exception {
        SellerAuctionDetailsController controller = new SellerAuctionDetailsController();

        assertEquals("Không thể hủy phiên.", invoke(controller, "userFriendlyCancelError", new Exception()));
        assertEquals("Không thể hủy phiên: boom",
                invoke(controller, "userFriendlyCancelError", new java.io.IOException("boom")));
        assertEquals("Kh\u00f4ng th\u1ec3 h\u1ee7y phi\u00ean dang chay",
                invoke(controller, "userFriendlyCancelError", new IllegalArgumentException("Khong the huy phien dang chay")));
    }

    private static SellerAuctionDetailsController controllerWithLabels() throws Exception {
        SellerAuctionDetailsController controller = new SellerAuctionDetailsController();
        for (String name : List.of(
                "productName", "productId", "productDesc", "startPrice", "currentPrice",
                "startTime", "endTime", "timeLeft", "status", "bidCount",
                "topBidderName", "topBidAmount")) {
            setField(controller, name, new Label());
        }
        setField(controller, "cancelAuctionButton", new Button());
        setField(controller, "productImage", new ImageView());
        return controller;
    }

    private static Auction auction(String auctionId, String itemId, AuctionStatus status, long endTime) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem(itemId, "Clock", "desc", 1_000_000.0, "seller-1", List.of());
        Auction auction = new Auction(auctionId, itemId, "seller-1", "Seller", item, 1_200_000.0, now - 1_000L, endTime);
        auction.setStatus(status);
        return auction;
    }

    private static Label label(Object target, String fieldName) throws Exception {
        return (Label) field(target, fieldName);
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
