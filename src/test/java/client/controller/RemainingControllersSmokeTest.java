package client.controller;

import org.junit.jupiter.api.Test;
import server.model.RegularUser;
import server.model.User;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RemainingControllersSmokeTest {
    @Test
    void controllersWithoutDedicatedTestsCanBeConstructed() {
        List<Class<?>> controllerTypes = List.of(
                AddAuctionProductController.class,
                AddBankDialogController.class,
                AdminHomeController.class,
                AuctionCardController.class,
                BecomeSellerController.class,
                CategoryController.class,
                HomeScreenController.class,
                LoginController.class,
                NotificationsController.class,
                ProfileController.class,
                SellerAuctionBidHistoryController.class,
                SellerAuctionDetailsController.class,
                SellerDashboardController.class,
                SellerHomeController.class,
                SellerManagementController.class,
                SellerNotificationsController.class,
                WalletTransactionDialogController.class
        );

        for (Class<?> type : controllerTypes) {
            assertDoesNotThrow(() -> type.getDeclaredConstructor().newInstance(), type.getName());
        }
    }

    @Test
    void simpleControllerSettersStoreStateWithoutFxmlInjection() throws Exception {
        User user = new RegularUser("u1", "alice", "password", "alice@example.com");

        BecomeSellerController becomeSellerController = new BecomeSellerController();
        becomeSellerController.setCurrentUser(user);
        assertSame(user, becomeSellerController.getCurrentUser());

        LoginController loginController = new LoginController();
        Runnable loginCallback = () -> { };
        Runnable adminCallback = () -> { };
        loginController.setOnLoginSuccess(loginCallback);
        loginController.setOnAdminLoginSuccess(adminCallback);
        loginController.setClientSocket(null);
        assertNull(loginController.getCurrentUser());
        assertNull(loginController.getClientSocket());
        assertSame(loginCallback, field(loginController, "onLoginSuccess"));
        assertSame(adminCallback, field(loginController, "onAdminLoginSuccess"));

        AuctionCardController auctionCardController = new AuctionCardController();
        auctionCardController.setHomeScreenController(null);
        assertEquals("", auctionCardController.getCurrentAuctionId());

        AddBankDialogController addBankDialogController = new AddBankDialogController();
        Runnable bankClose = () -> { };
        AddBankDialogController.OnBankLinkedListener bankListener = (bank, account, balance) -> { };
        addBankDialogController.setUserData(user, null);
        addBankDialogController.setOnCloseCallback(bankClose);
        addBankDialogController.setOnBankLinkedListener(bankListener);
        assertSame(user, field(addBankDialogController, "currentUser"));
        assertSame(bankClose, field(addBankDialogController, "onCloseCallback"));
        assertSame(bankListener, field(addBankDialogController, "onBankLinkedListener"));

        WalletTransactionDialogController transactionDialogController = new WalletTransactionDialogController();
        Runnable transactionClose = () -> { };
        WalletTransactionDialogController.OnTransactionConfirmListener confirmListener = amount -> { };
        transactionDialogController.setOnCloseCallback(transactionClose);
        transactionDialogController.setOnConfirmListener(confirmListener);
        assertSame(transactionClose, field(transactionDialogController, "onCloseCallback"));
        assertSame(confirmListener, field(transactionDialogController, "onConfirmListener"));
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
