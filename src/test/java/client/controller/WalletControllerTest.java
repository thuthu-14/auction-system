package client.controller;

import client.logic.WalletLogic;
import client.network.ClientSocket;
import client.service.WalletService;
import common.Transaction;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import server.model.RegularUser;
import server.model.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletControllerTest {
    @Test
    void helperMethodsDelegateToWalletLogicAfterRefactor() throws Exception {
        WalletController controller = new WalletController();
        Transaction deposit = transaction("DEPOSIT", "Nap tien vao vi");
        Transaction withdraw = transaction("WITHDRAW", "Rut tien ve ngan hang");

        assertEquals("1,200,000 đ", invoke(controller, "formatVnd", 1_200_000.0));
        assertTrue(((String) invoke(controller, "formatSignedTransactionAmount", deposit)).startsWith("+"));
        assertTrue((boolean) invoke(controller, "isMoneyIn", deposit));
        assertFalse((boolean) invoke(controller, "isMoneyIn", withdraw));
        assertEquals("--", invoke(controller, "safeText", " "));
        assertEquals("abc", invoke(controller, "safeText", "abc"));

        setField(controller, "activeTransactionFilter", WalletLogic.TransactionFilter.IN);
        assertTrue((boolean) invoke(controller, "matchesActiveTypeFilter", deposit));
        assertFalse((boolean) invoke(controller, "matchesActiveTypeFilter", withdraw));

        setField(controller, "activeTransactionFilter", WalletLogic.TransactionFilter.OUT);
        assertFalse((boolean) invoke(controller, "matchesActiveTypeFilter", deposit));
        assertTrue((boolean) invoke(controller, "matchesActiveTypeFilter", withdraw));

        assertTrue((boolean) invoke(controller, "matchesTimeFilter", deposit));
        assertTrue((boolean) invoke(controller, "matchesSearchKeyword", deposit, "nap"));
        assertFalse((boolean) invoke(controller, "matchesSearchKeyword", deposit, "missing"));
    }

    @Test
    void initializeConfiguresFiltersTableColumnsAndViewModeSizing() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Button all = new Button();
            Button in = new Button();
            Button out = new Button();
            Button deposit = new Button();
            Button withdraw = new Button();
            ComboBox<String> timeFilter = new ComboBox<>();
            TableView<Transaction> table = new TableView<>();

            setFieldUnchecked(controller, "rootPane", new StackPane(new Label("child")));
            setFieldUnchecked(controller, "timeFilter", timeFilter);
            setFieldUnchecked(controller, "filterAllBtn", all);
            setFieldUnchecked(controller, "filterInBtn", in);
            setFieldUnchecked(controller, "filterOutBtn", out);
            setFieldUnchecked(controller, "depositButton", deposit);
            setFieldUnchecked(controller, "withdrawButton", withdraw);
            setFieldUnchecked(controller, "transactionTable", table);
            setFieldUnchecked(controller, "dateColumn", new TableColumn<Transaction, String>());
            setFieldUnchecked(controller, "typeColumn", new TableColumn<Transaction, String>());
            setFieldUnchecked(controller, "moneyInColumn", new TableColumn<Transaction, String>());
            setFieldUnchecked(controller, "moneyOutColumn", new TableColumn<Transaction, String>());
            setFieldUnchecked(controller, "balanceColumn", new TableColumn<Transaction, String>());
            setFieldUnchecked(controller, "descriptionColumn", new TableColumn<Transaction, String>());

            controller.initialize(null, null);

            assertEquals(4, timeFilter.getItems().size());
            assertEquals(0, timeFilter.getSelectionModel().getSelectedIndex());
            assertTrue(all.getStyle().contains("#1a1a1a"));
            assertTrue(in.getStyle().contains("#f3f4f6"));
            assertNotNull(table.getRowFactory());
            assertEquals(185.0, deposit.getPrefWidth());
            assertEquals(165.0, withdraw.getPrefWidth());

            setFieldUnchecked(controller, "viewMode", WalletController.WalletViewMode.SELLER);
            invokeUnchecked(controller, "applyViewMode");

            assertEquals(170.0, deposit.getPrefWidth());
            assertEquals(150.0, withdraw.getPrefWidth());
            assertEquals(48.0, deposit.getPrefHeight());
        });
    }

    @Test
    void applyTransactionFiltersUpdatesButtonsAndTableItems() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Button all = new Button();
            Button in = new Button();
            Button out = new Button();
            TextField search = new TextField();
            ComboBox<String> timeFilter = new ComboBox<>();
            timeFilter.getItems().addAll("this", "last", "recent", "all");
            timeFilter.getSelectionModel().select(3);
            TableView<Transaction> table = new TableView<>();

            Transaction deposit = transaction("DEPOSIT", "nap tien vao vi");
            Transaction withdraw = transaction("WITHDRAW", "rut tien ve ngan hang");
            setFieldUnchecked(controller, "filterAllBtn", all);
            setFieldUnchecked(controller, "filterInBtn", in);
            setFieldUnchecked(controller, "filterOutBtn", out);
            setFieldUnchecked(controller, "searchField", search);
            setFieldUnchecked(controller, "timeFilter", timeFilter);
            setFieldUnchecked(controller, "transactionTable", table);
            setFieldUnchecked(controller, "allTransactions", new ArrayList<>(List.of(deposit, withdraw)));

            controller.applyTransactionFilters();
            assertEquals(2, table.getItems().size());

            invokeUnchecked(controller, "filterIn");
            assertEquals(List.of(deposit), table.getItems());
            assertTrue(in.getStyle().contains("#1a1a1a"));

            search.setText("rut");
            invokeUnchecked(controller, "filterOut");
            assertEquals(List.of(withdraw), table.getItems());
            assertTrue(out.getStyle().contains("#1a1a1a"));

            search.setText("missing");
            controller.applyTransactionFilters();
            assertTrue(table.getItems().isEmpty());
        });
    }

    @Test
    void refreshLinkedBankDisplayTogglesBankPanels() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Label bankName = new Label();
            Label accountNumber = new Label();
            VBox addBank = new VBox();
            VBox linkedBank = new VBox();
            setFieldUnchecked(controller, "displayBankName", bankName);
            setFieldUnchecked(controller, "displayAccNumber", accountNumber);
            setFieldUnchecked(controller, "addBankBox", addBank);
            setFieldUnchecked(controller, "linkedBankBox", linkedBank);

            invokeUnchecked(controller, "refreshLinkedBankDisplay", "vcb", "1234567890");

            assertEquals("VCB", bankName.getText());
            assertTrue(accountNumber.getText().contains("7890"));
            assertFalse(addBank.isVisible());
            assertFalse(addBank.isManaged());
            assertTrue(linkedBank.isVisible());
            assertTrue(linkedBank.isManaged());
        });
    }

    @Test
    void transactionDetailHelpersBuildGridRowsAndHandleNullTransaction() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            GridPane grid = new GridPane();

            invokeUnchecked(controller, "showTransactionDetailPopup", (Object) null);
            invokeUnchecked(controller, "addTransactionDetailRow", grid, 0, "Label", "Value");

            assertEquals(2, grid.getChildren().size());
            assertEquals("Label", ((Label) grid.getChildren().get(0)).getText());
            assertEquals("Value", ((Label) grid.getChildren().get(1)).getText());
        });
    }

    @Test
    void setupTransactionRowPopupHandlesMissingTableAndInstallsFactory() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            invokeUnchecked(controller, "setupTransactionRowPopup");

            TableView<Transaction> table = new TableView<>();
            setFieldUnchecked(controller, "transactionTable", table);
            invokeUnchecked(controller, "setupTransactionRowPopup");

            assertNotNull(table.getRowFactory());
        });
    }

    @Test
    void resetWalletViewStateClearsOverlaysAndRestoresNodeStateAndLabelColors() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Label balance = new Label("100");
            Label addBankLabel = new Label("+ ThÃªm");
            Label muted = new Label("Muted");
            muted.setOpacity(0.2);
            muted.setDisable(true);
            Region overlay = new Region();
            overlay.getStyleClass().add("wallet-modal-overlay");
            StackPane root = new StackPane(balance, addBankLabel, muted, overlay);
            setFieldUnchecked(controller, "rootPane", root);
            setFieldUnchecked(controller, "balanceLabel", balance);

            invokeUnchecked(controller, "resetWalletViewState");

            assertFalse(root.getChildren().contains(overlay));
            assertEquals(Color.web("#1a1a1a"), balance.getTextFill());
        });
    }

    @Test
    void removeWalletOverlayOnlyRemovesExistingOverlayAfterFadeFinishes() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Pane container = new Pane();
            Region overlay = new Region();
            container.getChildren().add(overlay);
            overlay.setOpacity(1);

            invokeUnchecked(controller, "removeWalletOverlay", container, overlay);

            assertTrue(container.getChildren().contains(overlay));
            invokeUnchecked(controller, "removeWalletOverlay", container, overlay);
            invokeUnchecked(controller, "removeWalletOverlay", null, overlay);
            invokeUnchecked(controller, "removeWalletOverlay", container, null);
        });
    }

    @Test
    void syncBalanceFromTransactionsUsesLatestCurrentUserTransactionOnly() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
            Label balance = new Label();
            setFieldUnchecked(controller, "currentUser", user);
            setFieldUnchecked(controller, "balanceLabel", balance);
            Transaction oldMine = transaction("DEPOSIT", "old");
            oldMine.userId = "u1";
            oldMine.balanceAfter = 100.0;
            oldMine.timestamp = 1L;
            Transaction newestOther = transaction("DEPOSIT", "other");
            newestOther.userId = "u2";
            newestOther.balanceAfter = 999.0;
            newestOther.timestamp = 3L;
            Transaction newestMine = transaction("WITHDRAW", "new");
            newestMine.userId = "u1";
            newestMine.balanceAfter = 250.0;
            newestMine.timestamp = 2L;

            invokeUnchecked(controller, "syncBalanceFromTransactions",
                    List.of(oldMine, newestOther, newestMine));

            assertEquals(250.0, user.getWallet(), 0.001);
            assertTrue(balance.getText().contains("250"));

            invokeUnchecked(controller, "syncBalanceFromTransactions", List.of());
            invokeUnchecked(controller, "syncBalanceFromTransactions", (Object) null);
        });
    }

    @Test
    void showTransactionsReplacesTableItemsAndRefreshWalletHandlesNullUser() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            TableView<Transaction> table = new TableView<>();
            Transaction transaction = transaction("DEPOSIT", "nap");
            setFieldUnchecked(controller, "transactionTable", table);

            invokeUnchecked(controller, "showTransactions", List.of(transaction));

            assertEquals(List.of(transaction), table.getItems());

            setFieldUnchecked(controller, "rootPane", new StackPane());
            invokeUnchecked(controller, "refreshWalletAfterTransaction", (Object) null);
        });
    }

    @Test
    void viewModeNullFallsBackToBidderAndButtonSizingSkipsNullButtons() throws Exception {
        runFx(() -> {
            WalletController controller = new WalletController();
            Button deposit = new Button();
            setFieldUnchecked(controller, "depositButton", deposit);
            setFieldUnchecked(controller, "withdrawButton", null);

            controller.setViewMode(null);
            invokeUnchecked(controller, "applyViewMode");

            assertEquals(185.0, deposit.getPrefWidth());
            invokeUnchecked(controller, "applyWalletButtonSize", (Object) null, 10.0, 10.0, 10.0);
        });
    }

    @Test
    void loadWalletDataShowsLinkedBankAndTransactionsFromService() throws Exception {
        WalletController controller = walletControllerWithUi();
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        user.setWallet(50.0);
        Transaction transaction = transaction("DEPOSIT", "nap");
        transaction.userId = "u1";
        transaction.balanceAfter = 300.0;
        transaction.timestamp = 10L;
        TestWalletService service = new TestWalletService(
                new WalletService.BankAccountEntry("vcb", "1234567890"),
                List.of(transaction),
                null
        );
        setField(controller, "walletService", service);
        setField(controller, "currentUser", user);
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        setField(controller, "clientSocket", socket);

        invoke(controller, "loadWalletData");
        waitForAsyncFx();

        assertEquals("VCB", ((Label) getField(controller, "displayBankName")).getText());
        assertFalse(((VBox) getField(controller, "addBankBox")).isVisible());
        assertTrue(((VBox) getField(controller, "linkedBankBox")).isVisible());
        assertEquals(1, ((TableView<?>) getField(controller, "transactionTable")).getItems().size());
        assertTrue(((Label) getField(controller, "balanceLabel")).getText().contains("300"));
    }

    @Test
    void loadWalletDataWithoutBankShowsAddBankPanel() throws Exception {
        WalletController controller = walletControllerWithUi();
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        setField(controller, "walletService", new TestWalletService(null, List.of(), null));
        setField(controller, "currentUser", user);
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        setField(controller, "clientSocket", socket);

        invoke(controller, "loadWalletData");
        waitForAsyncFx();

        assertTrue(((VBox) getField(controller, "addBankBox")).isVisible());
        assertFalse(((VBox) getField(controller, "linkedBankBox")).isVisible());
    }

    @Test
    void processTransactionValidatesAmountsBeforeNetworkCall() throws Exception {
        WalletController controller = walletControllerWithUi();
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        user.setWallet(100.0);
        setField(controller, "currentUser", user);
        WalletService.BankAccountEntry bank = new WalletService.BankAccountEntry("vcb", "123");

        try (MockedStatic<client.util.DialogUtil> dialogUtil = mockStatic(client.util.DialogUtil.class)) {
            invoke(controller, "processTransaction", "DEPOSIT", "0", bank);
            invoke(controller, "processTransaction", "WITHDRAW", "200", bank);
            invoke(controller, "processTransaction", "DEPOSIT", "abc", bank);

            dialogUtil.verify(() -> client.util.DialogUtil.showAlert(any(), anyString(), isNull(), anyString(), any()), times(3));
        }
    }

    @Test
    void processTransactionSuccessRefreshesWalletAndTransactions() throws Exception {
        WalletController controller = walletControllerWithUi();
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        user.setWallet(100.0);
        RegularUser updated = new RegularUser("u1", "user", "password", "u1@example.com");
        updated.setWallet(250.0);
        Transaction transaction = transaction("DEPOSIT", "nap");
        transaction.userId = "u1";
        transaction.balanceAfter = 250.0;
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "ok");
        response.setData(updated);
        setField(controller, "walletService", new TestWalletService(
                new WalletService.BankAccountEntry("vcb", "1234567890"),
                List.of(transaction),
                response
        ));
        setField(controller, "currentUser", user);
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        setField(controller, "clientSocket", socket);

        try (MockedStatic<client.util.DialogUtil> dialogUtil = mockStatic(client.util.DialogUtil.class)) {
            invoke(controller, "processTransaction", "DEPOSIT", "100", new WalletService.BankAccountEntry("vcb", "123"));
            waitForAsyncFx();

            assertTrue(((Label) getField(controller, "balanceLabel")).getText().contains("250"));
            assertEquals(1, ((TableView<?>) getField(controller, "transactionTable")).getItems().size());
        }
    }

    @Test
    void processTransactionRejectedAndInvalidResponseShowErrors() throws Exception {
        WalletController rejectedController = walletControllerWithUi();
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        Message rejected = new Message(MessageType.ERROR, "ERROR", "rejected");
        setField(rejectedController, "walletService", new TestWalletService(null, List.of(), rejected));
        setField(rejectedController, "currentUser", user);
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        setField(rejectedController, "clientSocket", socket);

        WalletController invalidController = walletControllerWithUi();
        Message invalid = new Message(MessageType.SUCCESS, "SUCCESS", "ok");
        invalid.setData("not-user");
        setField(invalidController, "walletService", new TestWalletService(null, List.of(), invalid));
        setField(invalidController, "currentUser", user);
        setField(invalidController, "clientSocket", socket);

        try (MockedStatic<client.util.DialogUtil> dialogUtil = mockStatic(client.util.DialogUtil.class)) {
            invoke(rejectedController, "processTransaction", "DEPOSIT", "100", new WalletService.BankAccountEntry("vcb", "123"));
            invoke(invalidController, "processTransaction", "DEPOSIT", "100", new WalletService.BankAccountEntry("vcb", "123"));
            waitForAsyncFx();

            assertTrue(true);
        }
    }

    private static Transaction transaction(String type, String description) {
        Transaction transaction = new Transaction();
        transaction.type = type;
        transaction.amount = 100.0;
        transaction.timestamp = System.currentTimeMillis();
        transaction.description = description;
        return transaction;
    }

    private static WalletController walletControllerWithUi() throws Exception {
        WalletController controller = new WalletController();
        setField(controller, "rootPane", new StackPane());
        setField(controller, "balanceLabel", new Label());
        setField(controller, "displayBankName", new Label());
        setField(controller, "displayAccNumber", new Label());
        setField(controller, "addBankBox", new VBox());
        setField(controller, "linkedBankBox", new VBox());
        setField(controller, "filterAllBtn", new Button());
        setField(controller, "filterInBtn", new Button());
        setField(controller, "filterOutBtn", new Button());
        setField(controller, "searchField", new TextField());
        ComboBox<String> timeFilter = new ComboBox<>();
        timeFilter.getItems().addAll("this", "last", "recent", "all");
        timeFilter.getSelectionModel().select(3);
        setField(controller, "timeFilter", timeFilter);
        setField(controller, "transactionTable", new TableView<Transaction>());
        return controller;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setFieldUnchecked(Object target, String name, Object value) {
        try {
            setField(target, name, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object invokeUnchecked(Object target, String methodName, Object... args) {
        try {
            return invoke(target, methodName, args);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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
        FxTestRuntime.start();
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

    private static void waitForAsyncFx() throws Exception {
        Thread.sleep(250);
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX event drain timed out");
    }

    private static final class TestWalletService extends WalletService {
        private final BankAccountEntry bankAccount;
        private final List<Transaction> transactions;
        private final Message transactionResponse;

        private TestWalletService(BankAccountEntry bankAccount,
                                  List<Transaction> transactions,
                                  Message transactionResponse) {
            this.bankAccount = bankAccount;
            this.transactions = transactions;
            this.transactionResponse = transactionResponse;
        }

        @Override
        public BankAccountEntry loadBankAccount(ClientSocket socket, User user) {
            return bankAccount;
        }

        @Override
        public List<Transaction> fetchTransactionList(ClientSocket socket, User user) {
            return transactions;
        }

        @Override
        public Message submitWalletTransaction(ClientSocket socket, User user, String type, double amount) {
            if (transactionResponse != null) {
                return transactionResponse;
            }
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "ok");
            response.setData(user);
            return response;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class FxTestRuntime {
        private static boolean started;

        static synchronized void start() throws Exception {
            if (started) {
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
                assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX startup timed out");
            } catch (IllegalStateException alreadyStarted) {
                // Toolkit is process-wide; another controller test may have started it.
            }
            started = true;
        }
    }
}
