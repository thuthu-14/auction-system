package client.controller;

import common.AuctionStatus;
import javafx.application.Platform;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Auction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SellerStatisticsControllerTest {
    @Test
    void helperMethodsDelegateToSellerStatisticsLogicAfterRefactor() throws Exception {
        SellerStatisticsController controller = new SellerStatisticsController();
        Auction ended = auction(AuctionStatus.FINISHED, System.currentTimeMillis() - 1_000L);
        Auction running = auction(AuctionStatus.RUNNING, System.currentTimeMillis() + 60_000L);
        Auction cancelled = auction(AuctionStatus.CANCELLED, System.currentTimeMillis() + 60_000L);

        assertTrue((boolean) invoke(controller, "isEnded", ended));
        assertFalse((boolean) invoke(controller, "isEnded", running));
        assertTrue((boolean) invoke(controller, "isCancelled", cancelled));
        assertFalse((boolean) invoke(controller, "isCancelled", running));
        assertEquals(20.0, (double) invoke(controller, "calculateTickUnit", 75.0), 0.001);
        assertTrue(((String) invoke(controller, "formatVnd", 1_200_000.0)).contains("1"));
    }

    @Test
    void initializeConfiguresChartAndEmptyStatistics() throws Exception {
        runFx(() -> {
            SellerStatisticsController controller = new SellerStatisticsController();
            Label revenue = new Label();
            Label avgBid = new Label();
            Label successRate = new Label();
            LineChart<String, Number> chart = chart();
            setField(controller, "totalRevenueLabel", revenue);
            setField(controller, "avgBidLabel", avgBid);
            setField(controller, "successRateLabel", successRate);
            setField(controller, "revenueChart", chart);

            controller.initialize(null, null);

            assertFalse(chart.getAnimated());
            assertFalse(chart.isLegendVisible());
            assertTrue(chart.getCreateSymbols());
            assertEquals(350.0, chart.getPrefHeight());
            assertEquals("0.0", avgBid.getText());
            assertEquals("0.0%", successRate.getText());
            assertEquals(1, chart.getData().size());
            assertEquals(2, chart.getData().get(0).getData().size());
            assertFalse(chart.getXAxis().isAutoRanging());
            assertFalse(chart.getYAxis().isAutoRanging());
        });
    }

    @Test
    void processAndDisplayStatisticsUpdatesLabelsAndRevenueSeries() throws Exception {
        runFx(() -> {
            SellerStatisticsController controller = new SellerStatisticsController();
            Label revenue = new Label();
            Label avgBid = new Label();
            Label successRate = new Label();
            LineChart<String, Number> chart = chart();
            setField(controller, "totalRevenueLabel", revenue);
            setField(controller, "avgBidLabel", avgBid);
            setField(controller, "successRateLabel", successRate);
            setField(controller, "revenueChart", chart);

            Auction finished = auction(AuctionStatus.FINISHED, System.currentTimeMillis() - 1_000L);
            finished.setCurrentPrice(2_000_000.0);
            finished.setBidIds(List.of("B1", "B2"));
            invoke(controller, "processAndDisplayStatistics", List.of(finished));

            assertTrue(revenue.getText().startsWith("2.0M"), revenue.getText());
            assertEquals("2.0", avgBid.getText());
            assertEquals("100.0%", successRate.getText());
            assertEquals(1, chart.getData().size());
            assertEquals(1, chart.getData().get(0).getData().size());
            assertTrue(((NumberAxis) chart.getYAxis()).getUpperBound() > 2_000_000.0);
        });
    }

    @Test
    void updateRevenueChartUsesEmptyAndDataAxisConfiguration() throws Exception {
        runFx(() -> {
            SellerStatisticsController controller = new SellerStatisticsController();
            LineChart<String, Number> chart = chart();
            setField(controller, "revenueChart", chart);

            invoke(controller, "updateRevenueChart", Map.of());
            assertEquals(2, ((CategoryAxis) chart.getXAxis()).getCategories().size());
            assertEquals(110.0, ((NumberAxis) chart.getYAxis()).getUpperBound());

            long now = System.currentTimeMillis();
            invoke(controller, "updateRevenueChart", Map.of(now, 500_000.0, now + 120_000L, 1_250_000.0));
            assertEquals(2, chart.getData().get(0).getData().size());
            assertEquals(2, ((CategoryAxis) chart.getXAxis()).getCategories().size());
            assertTrue(((NumberAxis) chart.getYAxis()).getUpperBound() >= 1_250_000.0);
        });
    }

    @Test
    void applyReadableStylesColorsNestedLabels() throws Exception {
        runFx(() -> {
            SellerStatisticsController controller = new SellerStatisticsController();
            Label revenue = new Label("100");
            Label nested = new Label("Nested");
            VBox content = new VBox(revenue, nested);
            ScrollPane root = new ScrollPane(content);
            LineChart<String, Number> chart = chart();
            content.getChildren().add(chart);
            setField(controller, "totalRevenueLabel", revenue);
            setField(controller, "avgBidLabel", new Label());
            setField(controller, "successRateLabel", new Label());
            setField(controller, "revenueChart", chart);

            invoke(controller, "applyReadableStyles", content);
            invoke(controller, "setLabelColor", revenue, "#111827");

            assertTrue(revenue.getStyle().contains("#111827"), revenue.getStyle());
            assertTrue(nested.getStyle().contains("#4b5563") || nested.getStyle().contains("#111827"), nested.getStyle());
            assertNull(invoke(controller, "findStatisticsRoot"));
            assertSame(root.getContent(), content);
        });
    }

    private static Auction auction(AuctionStatus status, long endTime) {
        Auction auction = new Auction();
        auction.setStatus(status);
        auction.setEndTime(endTime);
        return auction;
    }

    private static LineChart<String, Number> chart() {
        return new LineChart<>(new CategoryAxis(), new NumberAxis());
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
