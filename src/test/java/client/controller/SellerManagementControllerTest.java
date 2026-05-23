package client.controller;

import common.AuctionStatus;
import common.ItemCategory;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.OtherItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SellerManagementControllerTest {
    @Test
    void auctionItemDerivesStatusCategoryAndTimeText() {
        SellerManagementController.AuctionItem active = new SellerManagementController.AuctionItem(
                auction("A1", "Camera", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, 2));
        assertTrue(active.isActive());
        assertEquals("active", active.getStatusKey());
        assertFalse(active.getStatusText().isBlank());
        assertFalse(active.timeLeftStrProperty().get().isBlank());
        assertEquals("Điện tử", active.getCategoryLabel());
        assertEquals("2", active.bidsProperty().get());

        SellerManagementController.AuctionItem pending = new SellerManagementController.AuctionItem(
                auction("A2", "Draft", ItemCategory.FASHION, AuctionStatus.DRAFT, System.currentTimeMillis() + 60_000L, 0));
        assertTrue(pending.isPending());
        assertEquals("pending", pending.getStatusKey());

        SellerManagementController.AuctionItem cancelled = new SellerManagementController.AuctionItem(
                auction("A3", "Cancel", ItemCategory.OTHER, AuctionStatus.CANCELLED, System.currentTimeMillis() - 60_000L, 0));
        assertTrue(cancelled.isCancelled());
        assertTrue(cancelled.timeLeftStrProperty().get().contains("h"));
    }

    @Test
    void initializeSetsFiltersColumnsStatsAndTimer() throws Exception {
        runFx(() -> {
            SellerManagementController controller = controllerWithControls();

            controller.initialize();

            assertEquals(4, combo(controller, "sortCombo").getItems().size());
            assertEquals(7, combo(controller, "categoryCombo").getItems().size());
            assertEquals("0", label(controller, "statTotal").getText());
            assertEquals("0.0%", label(controller, "statSuccessRate").getText());
            assertNotNull(field(controller, "countdownTimer"));
            assertTrue(((TableView<?>) field(controller, "auctionTable")).isEditable());
            stopTimer(controller);
        });
    }

    @Test
    void updateStatsCountsAllAuctionStatesAndSuccessRate() throws Exception {
        runFx(() -> {
            SellerManagementController controller = controllerWithControls();
            addItems(controller,
                    auction("A1", "Active", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, 1),
                    auction("A2", "Pending", ItemCategory.FASHION, AuctionStatus.DRAFT, System.currentTimeMillis() + 60_000L, 0),
                    auction("A3", "Ended", ItemCategory.JEWELRY, AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, 2),
                    auction("A4", "Cancelled", ItemCategory.OTHER, AuctionStatus.CANCELLED, System.currentTimeMillis() - 60_000L, 0));

            invoke(controller, "updateStats");

            assertEquals("4", label(controller, "statTotal").getText());
            assertEquals("1", label(controller, "statActive").getText());
            assertEquals("100.0%", label(controller, "statSuccessRate").getText());
            assertEquals("1", label(controller, "countPending").getText());
            assertEquals("1", label(controller, "countCancelled").getText());
        });
    }

    @Test
    void refreshTableAppliesTabSearchCategoryAndSort() throws Exception {
        runFx(() -> {
            SellerManagementController controller = controllerWithControls();
            addItems(controller,
                    auction("A1", "Camera", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, 1),
                    auction("A2", "Ring", ItemCategory.JEWELRY, AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, 2),
                    auction("A3", "Coat", ItemCategory.FASHION, AuctionStatus.DRAFT, System.currentTimeMillis() + 60_000L, 0));
            combo(controller, "sortCombo").getItems().addAll("Mới nhất", "Cũ nhất", "Giá cao", "Giá thấp");
            combo(controller, "sortCombo").getSelectionModel().select("Mới nhất");
            combo(controller, "categoryCombo").getItems().addAll("Tất cả", "Điện tử", "Thời trang", "Trang sức", "Sưu tầm", "Xe cộ", "Khác");
            combo(controller, "categoryCombo").getSelectionModel().select("Tất cả");

            invoke(controller, "refreshTable");
            assertEquals(3, table(controller).getItems().size());

            setField(controller, "currentTab", "active");
            invoke(controller, "refreshTable");
            assertEquals(1, table(controller).getItems().size());

            setField(controller, "currentTab", "all");
            textField(controller, "searchField").setText("ring");
            invoke(controller, "refreshTable");
            assertEquals(1, table(controller).getItems().size());

            textField(controller, "searchField").clear();
            combo(controller, "categoryCombo").setValue("Thời trang");
            invoke(controller, "refreshTable");
            assertEquals(1, table(controller).getItems().size());
            assertEquals("Coat", table(controller).getItems().get(0).getName());
        });
    }

    @Test
    void selectTabAndResetFilterUpdateStylesAndTable() throws Exception {
        runFx(() -> {
            SellerManagementController controller = controllerWithControls();
            HBox activeTab = (HBox) field(controller, "tabActive");
            setField(controller, "allTabs", List.of(field(controller, "tabAll"), activeTab));
            addItems(controller, auction("A1", "Camera", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, 1));
            combo(controller, "sortCombo").getItems().addAll("Mới nhất", "Cũ nhất");
            combo(controller, "sortCombo").getSelectionModel().select(1);
            combo(controller, "categoryCombo").getItems().addAll("Tất cả", "Điện tử");
            combo(controller, "categoryCombo").getSelectionModel().select(1);
            textField(controller, "searchField").setText("camera");

            invoke(controller, "selectTab", "active", activeTab);
            assertEquals("active", field(controller, "currentTab"));
            assertTrue(activeTab.getStyleClass().contains("auction-tab-active"));

            invoke(controller, "handleResetFilter");
            assertEquals("", textField(controller, "searchField").getText());
            assertEquals(0, combo(controller, "sortCombo").getSelectionModel().getSelectedIndex());
            assertEquals(0, combo(controller, "categoryCombo").getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    void helperMethodsFormatStyleAndMatchTabs() throws Exception {
        SellerManagementController controller = new SellerManagementController();
        SellerManagementController.AuctionItem active = new SellerManagementController.AuctionItem(
                auction("A1", "Camera", ItemCategory.ELECTRONICS, AuctionStatus.OPEN, System.currentTimeMillis() + 60_000L, 1));
        SellerManagementController.AuctionItem ended = new SellerManagementController.AuctionItem(
                auction("A2", "Ring", ItemCategory.JEWELRY, AuctionStatus.FINISHED, System.currentTimeMillis() - 60_000L, 1));

        assertTrue(((String) invoke(controller, "formatVnd", 1200000L)).contains("1"));
        assertTrue(((String) invoke(controller, "statusStyle", "active")).contains("#eaf3de"));
        setField(controller, "currentTab", "active");
        assertTrue((boolean) invoke(controller, "matchesTab", active));
        assertFalse((boolean) invoke(controller, "matchesTab", ended));
        setField(controller, "currentTab", "ended");
        assertTrue((boolean) invoke(controller, "matchesTab", ended));
    }

    private static SellerManagementController controllerWithControls() throws Exception {
        SellerManagementController controller = new SellerManagementController();
        for (String name : List.of("statTotal", "statActive", "statSuccessRate",
                "countAll", "countActive", "countPending", "countEnded", "countCancelled", "pageInfoLabel")) {
            setField(controller, name, new Label());
        }
        setField(controller, "tabAll", new HBox());
        setField(controller, "tabActive", new HBox());
        setField(controller, "tabPending", new HBox());
        setField(controller, "tabEnded", new HBox());
        setField(controller, "tabCancelled", new HBox());
        setField(controller, "searchField", new TextField());
        setField(controller, "sortCombo", new ComboBox<String>());
        setField(controller, "categoryCombo", new ComboBox<String>());
        setField(controller, "auctionTable", new TableView<SellerManagementController.AuctionItem>());
        setField(controller, "colSelect", new TableColumn<SellerManagementController.AuctionItem, Boolean>());
        setField(controller, "colProduct", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colId", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colPrice", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colBids", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colTimeLeft", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colStatus", new TableColumn<SellerManagementController.AuctionItem, String>());
        setField(controller, "colActions", new TableColumn<SellerManagementController.AuctionItem, Void>());
        setField(controller, "allTabs", List.of(field(controller, "tabAll"), field(controller, "tabActive"),
                field(controller, "tabPending"), field(controller, "tabEnded"), field(controller, "tabCancelled")));
        return controller;
    }

    private static void addItems(SellerManagementController controller, Auction... auctions) throws Exception {
        @SuppressWarnings("unchecked")
        List<SellerManagementController.AuctionItem> items =
                (List<SellerManagementController.AuctionItem>) field(controller, "allItems");
        for (Auction auction : auctions) {
            items.add(new SellerManagementController.AuctionItem(auction));
        }
    }

    private static Auction auction(String id, String name, ItemCategory category, AuctionStatus status, long endTime, int bids) {
        long now = System.currentTimeMillis();
        OtherItem item = new OtherItem("IT-" + id, name, "desc", 100, "seller", List.of());
        item.setCategory(category);
        Auction auction = new Auction(id, item.getItemId(), "seller", "Seller", item, 100 + bids, now - bids * 1_000L, endTime);
        auction.setStatus(status);
        for (int i = 0; i < bids; i++) {
            auction.addBidId("B" + i);
        }
        return auction;
    }

    @SuppressWarnings("unchecked")
    private static TableView<SellerManagementController.AuctionItem> table(Object target) throws Exception {
        return (TableView<SellerManagementController.AuctionItem>) field(target, "auctionTable");
    }

    private static Label label(Object target, String name) throws Exception {
        return (Label) field(target, name);
    }

    private static TextField textField(Object target, String name) throws Exception {
        return (TextField) field(target, name);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> combo(Object target, String name) throws Exception {
        return (ComboBox<String>) field(target, name);
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
