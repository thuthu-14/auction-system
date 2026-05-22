package client.controller;

import common.AuctionStatus;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.OtherItem;
import server.model.RegularUser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionHistoryControllerTest {
    @Test
    void contactAndFormattingHelpersHandleFallbacks() throws Exception {
        AuctionHistoryController controller = new AuctionHistoryController();

        assertEquals("fallback", invoke(controller, "safeContact", "", "fallback"));
        assertEquals("value", invoke(controller, "safeContact", "value", "fallback"));
        VBox content = (VBox) invoke(controller, "createContactContent",
                "BaoNgoc", "bn@gmail.com", "0975888665", "Ha Noi");
        assertEquals(4, content.getChildren().size());
        Label phone = (Label) content.getChildren().get(2);
        assertEquals("SĐT: 0975888665", phone.getText());
        assertTrue(phone.isWrapText());
        assertEquals(340, phone.getPrefWidth(), 0.001);
        assertTrue((boolean) invoke(controller, "hasUsefulContact", Map.of()));
        assertFalse((boolean) invoke(controller, "hasUsefulContact",
                Map.of("phone", "Chưa cập nhật", "email", "Chưa cập nhật")));
        assertTrue((boolean) invoke(controller, "hasUsefulContact",
                Map.of("phone", "090", "email", "Chưa cập nhật")));
        assertTrue(((String) invoke(controller, "formatVnd", 123456.0)).contains("123"));
    }

    @Test
    void timeFilterRecognizesCurrentPreviousAndRecentMonths() throws Exception {
        AuctionHistoryController controller = new AuctionHistoryController();
        long thisMonth = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long previousMonth = LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long old = LocalDate.now().minusMonths(4).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        assertTrue((boolean) invoke(controller, "matchesTimeFilter", thisMonth, "Tháng này"));
        assertTrue((boolean) invoke(controller, "matchesTimeFilter", previousMonth, "Tháng trước"));
        assertTrue((boolean) invoke(controller, "matchesTimeFilter", previousMonth, "3 tháng gần đây"));
        assertFalse((boolean) invoke(controller, "matchesTimeFilter", old, "3 tháng gần đây"));
        assertTrue((boolean) invoke(controller, "matchesTimeFilter", old, "Tất cả thời gian"));
    }

    @Test
    void historyRowDerivesProductStatusWinnerAndOutbidState() throws Exception {
        AuctionHistoryController controller = new AuctionHistoryController();
        setField(controller, "currentUser", new RegularUser("u1", "alice", "password", "a@example.com"));

        Auction activeWon = auction(AuctionStatus.RUNNING, System.currentTimeMillis() + 60_000L, "u1");
        Object activeWonRow = historyRow(controller, bid("u1", 100), activeWon);
        assertEquals("Clock", invoke(activeWonRow, "productName"));
        assertEquals("Đang đấu", invoke(activeWonRow, "statusText"));
        assertTrue((boolean) invoke(activeWonRow, "isActive"));
        assertFalse((boolean) invoke(activeWonRow, "isOutbid"));

        Auction activeOutbid = auction(AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, "u2");
        Object activeOutbidRow = historyRow(controller, bid("u1", 100), activeOutbid);
        assertTrue((boolean) invoke(activeOutbidRow, "isOutbid"));

        Auction finishedWon = auction(AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, "u1");
        Object finishedWonRow = historyRow(controller, bid("u1", 100), finishedWon);
        assertEquals("Trúng thầu", invoke(finishedWonRow, "statusText"));
        assertTrue((boolean) invoke(finishedWonRow, "isWinner"));

        Auction cancelled = auction(AuctionStatus.CANCELLED, System.currentTimeMillis() - 60_000L, "u1");
        Object cancelledRow = historyRow(controller, bid("u1", 100), cancelled);
        assertEquals("Trượt", invoke(cancelledRow, "statusText"));
        assertFalse((boolean) invoke(cancelledRow, "isWinner"));
    }

    @Test
    void initializeConfiguresFiltersTableAndDefaultSelections() throws Exception {
        runFx(() -> {
            AuctionHistoryController controller = new AuctionHistoryController();
            ComboBox<String> status = new ComboBox<>();
            ComboBox<String> time = new ComboBox<>();
            TextField search = new TextField();
            TableView<Object> table = new TableView<>();
            setField(controller, "statusFilter", status);
            setField(controller, "timeFilter", time);
            setField(controller, "searchField", search);
            setField(controller, "historyTable", table);
            setField(controller, "dateColumn", new TableColumn<>());
            setField(controller, "productNameColumn", new TableColumn<>());
            setField(controller, "bidAmountColumn", new TableColumn<>());
            setField(controller, "statusColumn", new TableColumn<>());
            setField(controller, "actionColumn", new TableColumn<>());

            invoke(controller, "initialize");

            assertEquals("Tất cả trạng thái", status.getValue());
            assertEquals("Tất cả thời gian", time.getValue());
            assertSame(field(controller, "visibleRows"), table.getItems());
            assertFalse(status.getItems().isEmpty());
            assertFalse(time.getItems().isEmpty());
        });
    }

    @Test
    void applyFiltersCombinesStatusTimeAndKeyword() throws Exception {
        runFx(() -> {
            AuctionHistoryController controller = new AuctionHistoryController();
            setField(controller, "currentUser", new RegularUser("u1", "alice", "password", "a@example.com"));
            ComboBox<String> status = new ComboBox<>();
            status.getItems().addAll("Tất cả trạng thái", "Đang đấu", "Trúng thầu", "Trượt", "Đã kết thúc");
            status.getSelectionModel().selectFirst();
            ComboBox<String> time = new ComboBox<>();
            time.getItems().addAll("Tất cả thời gian", "Tháng này", "Tháng trước", "3 tháng gần đây");
            time.getSelectionModel().selectFirst();
            TextField search = new TextField();
            setField(controller, "statusFilter", status);
            setField(controller, "timeFilter", time);
            setField(controller, "searchField", search);

            Object activeClock = historyRow(controller,
                    bid("u1", "A1", 100, System.currentTimeMillis()),
                    auction("A1", "Clock", AuctionStatus.RUNNING, System.currentTimeMillis() + 60_000L, "u2"));
            Object wonCamera = historyRow(controller,
                    bid("u1", "A2", 200, System.currentTimeMillis()),
                    auction("A2", "Camera", AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, "u1"));
            @SuppressWarnings("unchecked")
            List<Object> allRows = (List<Object>) field(controller, "allRows");
            allRows.add(activeClock);
            allRows.add(wonCamera);

            invoke(controller, "applyFilters");
            assertEquals(2, visibleSize(controller));

            status.setValue("Trúng thầu");
            invoke(controller, "applyFilters");
            assertEquals(1, visibleSize(controller));

            search.setText("clock");
            invoke(controller, "applyFilters");
            assertEquals(0, visibleSize(controller));

            status.setValue("Đang đấu");
            invoke(controller, "applyFilters");
            assertEquals(1, visibleSize(controller));
        });
    }

    @Test
    void winningRowHelpersAvoidDuplicatesAndCreateFallbackBid() throws Exception {
        AuctionHistoryController controller = new AuctionHistoryController();
        setField(controller, "currentUser", new RegularUser("u1", "alice", "password", "a@example.com"));
        Auction won = auction("A1", "Camera", AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, "u1");
        Bid userBid = bid("u1", "A1", 300, System.currentTimeMillis() - 1_000L);
        List<Object> rows = new ArrayList<>();

        invoke(controller, "addWonAuctionRows", rows, Map.of("A1", won), List.of(userBid));
        assertEquals(1, rows.size());
        assertTrue((boolean) invoke(controller, "hasRowForAuction", rows, "A1"));
        assertSame(userBid, field(rows.get(0), "bid"));

        invoke(controller, "addWonAuctionRows", rows, Map.of("A1", won), List.of());
        assertEquals(1, rows.size());

        Bid fallback = (Bid) invoke(controller, "createWinningBidFromAuction", won);
        assertEquals("A1", fallback.getAuctionId());
        assertEquals("u1", fallback.getBidderId());
        assertEquals(won.getCurrentPrice(), fallback.getAmount(), 0.001);
    }

    private static Auction auction(AuctionStatus status, long endTime, String highestBidderId) {
        return auction("A1", "Clock", status, endTime, highestBidderId);
    }

    private static Auction auction(String auctionId, String name, AuctionStatus status, long endTime, String highestBidderId) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT1", name, "desc", 100, "seller", List.of());
        Auction auction = new Auction(auctionId, "IT1", "seller", "Seller", item, 100, now - 10_000L, endTime);
        auction.setStatus(status);
        auction.setHighestBidderId(highestBidderId);
        auction.setHighestBidderName("Bidder");
        return auction;
    }

    private static Bid bid(String bidderId, double amount) {
        return bid(bidderId, "A1", amount, System.currentTimeMillis());
    }

    private static Bid bid(String bidderId, String auctionId, double amount, long bidTime) {
        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidderName("Bidder");
        bid.setAmount(amount);
        bid.setBidTime(bidTime);
        return bid;
    }

    private static int visibleSize(AuctionHistoryController controller) throws Exception {
        return ((List<?>) field(controller, "visibleRows")).size();
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object historyRow(AuctionHistoryController controller, Bid bid, Auction auction) throws Exception {
        Class<?> rowClass = Class.forName("client.controller.AuctionHistoryController$HistoryRow");
        Constructor<?> constructor = rowClass.getDeclaredConstructor(AuctionHistoryController.class, Bid.class, Auction.class);
        constructor.setAccessible(true);
        return constructor.newInstance(controller, bid, auction);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
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
