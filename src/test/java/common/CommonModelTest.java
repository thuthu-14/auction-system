package common;

import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.Bid;
import server.model.OtherItem;
import server.model.RegularUser;
import server.service.BidPlacementResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommonModelTest {

    @Test
    void messageConstructorsSetStatusTimestampAndFields() {
        Message dataMessage = new Message(MessageType.LOGIN, 123, "sender-1");
        assertEquals(MessageType.LOGIN, dataMessage.getType());
        assertEquals(123, dataMessage.getData());
        assertEquals("sender-1", dataMessage.getSenderId());
        assertEquals("SUCCESS", dataMessage.getStatus());
        assertTrue(dataMessage.getTimestamp() > 0);
        assertTrue(dataMessage.toString().contains("LOGIN"));

        Message statusMessage = new Message(MessageType.ERROR, "ERROR", "failed");
        assertEquals(MessageType.ERROR, statusMessage.getType());
        assertEquals("ERROR", statusMessage.getStatus());
        assertEquals("failed", statusMessage.getMessage());
    }

    @Test
    void messageSettersUpdateFields() {
        Message message = new Message();

        message.setType(MessageType.SUCCESS);
        message.setData(123);
        message.setSenderId("sender");
        message.setTimestamp(10L);
        message.setStatus("OK");
        message.setMessage("done");

        assertEquals(MessageType.SUCCESS, message.getType());
        assertEquals(123, message.getData());
        assertEquals("sender", message.getSenderId());
        assertEquals(10L, message.getTimestamp());
        assertEquals("OK", message.getStatus());
        assertEquals("done", message.getMessage());
    }

    @Test
    void transactionLabelsAndMoneyColumnsDependOnType() {
        Transaction deposit = new Transaction("u1", "DEPOSIT", 1000.0, 2000.0, "deposit");
        Transaction payment = new Transaction("u1", "PAYMENT", 500.0, 1500.0, "payment");
        Transaction withdraw = new Transaction("u1", "WITHDRAW", 300.0, 1200.0, "withdraw");

        assertTrue(deposit.id.startsWith("TXN_"));
        assertEquals("+ Nạp tiền", deposit.getTypeLabel());
        assertFalse(deposit.getMoneyIn().isBlank());
        assertEquals("", deposit.getMoneyOut());
        assertEquals("- Thanh toán", payment.getTypeLabel());
        assertEquals("", payment.getMoneyIn());
        assertFalse(payment.getMoneyOut().isBlank());
        assertEquals("- Rút tiền", withdraw.getTypeLabel());
        assertFalse(withdraw.getMoneyOut().isBlank());
        assertEquals("withdraw", withdraw.getDescription());
        assertEquals("u1", withdraw.getUserId());
        assertEquals("WITHDRAW", withdraw.getType());
        assertEquals(1200.0, withdraw.getBalanceAfter(), 0.001);
        assertFalse(withdraw.getFormattedDate().isBlank());
        assertFalse(withdraw.getFormattedBalanceAfter().isBlank());
    }

    @Test
    void bidPlacementResultReturnsConstructorArguments() {
        Bid bid = new Bid("B1", "A1", "u1", "bidder", 100.0);
        OtherItem item = new OtherItem("IT1", "Item", "Desc", 10.0, "seller", List.of());
        Auction auction = new Auction("A1", "IT1", "seller", "seller", item, 10.0, 10);
        RegularUser bidder = new RegularUser("u1", "bidder", "password", "u1@example.com");
        BidPlacementResult result = new BidPlacementResult(bid, auction, bidder, "old");

        assertSame(bid, result.getBid());
        assertSame(auction, result.getAuction());
        assertSame(bidder, result.getBidder());
        assertEquals("old", result.getPreviousHighestBidderId());
    }

    @Test
    void appConfigSingletonStoresServerSettings() {
        AppConfig config = AppConfig.getInstance();
        String originalHost = config.getServerHost();
        int originalPort = config.getServerPort();

        config.setServerHost("127.0.0.1");
        config.setServerPort(1234);
        config.setDebugMode(true);

        assertSame(config, AppConfig.getInstance());
        assertEquals("127.0.0.1", config.getServerHost());
        assertEquals(1234, config.getServerPort());
        assertEquals("http://127.0.0.1:1234", config.getServerURL());
        assertTrue(config.isDebugMode());

        config.setServerHost(originalHost);
        config.setServerPort(originalPort);
        config.setDebugMode(false);
    }

    @Test
    void loginRequestAndUpdateUserRoleRequestAccessorsWork() {
        LoginRequest login = new LoginRequest("user", "password");
        assertEquals("user", login.getUsername());
        assertEquals("password", login.getPassword());
        assertTrue(login.toString().contains("user"));
        login.setUsername("other");
        login.setPassword("secret");
        assertEquals("other", login.getUsername());
        assertEquals("secret", login.getPassword());

        UpdateUserRoleRequest request = new UpdateUserRoleRequest(
                "u1", UserRole.SELLER, "Shop", "shop@example.com", "123", "Addr");
        assertEquals("u1", request.getUserId());
        assertEquals(UserRole.SELLER, request.getNewRole());
        assertEquals("Shop", request.getShopName());
        assertEquals("shop@example.com", request.getShopEmail());
        assertEquals("123", request.getShopPhone());
        assertEquals("Addr", request.getShopAddress());
        assertTrue(request.toString().contains("SELLER"));

        request.setUserId("u2");
        request.setNewRole(UserRole.ADMIN);
        request.setShopName("New Shop");
        request.setShopEmail("new@example.com");
        request.setShopPhone("456");
        request.setShopAddress("New Addr");
        assertEquals("u2", request.getUserId());
        assertEquals(UserRole.ADMIN, request.getNewRole());
        assertEquals("New Shop", request.getShopName());
        assertEquals("new@example.com", request.getShopEmail());
        assertEquals("456", request.getShopPhone());
        assertEquals("New Addr", request.getShopAddress());
    }

    @Test
    void constantsExposeExpectedDefaults() {
        assertEquals(5000, Constants.SERVER_PORT);
        assertEquals("localhost", Constants.SERVER_HOST);
        assertTrue(Constants.THREAD_POOL_SIZE > 0);
        assertTrue(Constants.SOCKET_TIMEOUT > 0);
        assertTrue(Constants.ANTI_SNIPE_MINUTES > 0);
        assertTrue(Constants.AUTO_EXTEND_MINUTES > 0);
    }
}
