package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import common.AuctionStatus;
import server.model.Auction;
import server.model.Electronics;
import server.repository.SqlBankAccountRepository;
import server.repository.SqlAuctionRepository;
import server.repository.SqlTransactionRepository;
import server.repository.SqlUserRepository;
import server.repository.UserRepository;
import server.model.RegularUser;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WalletAccountServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SqlTransactionRepository transactionRepository;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void credit_incrementsWallet_and_savesTransaction() throws Exception {
        server.storage.SchemaInitializer.init();
        WalletAccountService service = new WalletAccountService();
        RegularUser u = new RegularUser("u1","user1","p","u1@x.com");
        // call addFunds using currentUser to avoid internal repository resolution
        var result = service.addFunds(null, u.getUsername(), 100.0, u);

        assertEquals(100.0, result.getWallet(), 0.001);
    }

    @Test
    public void addFunds_resolvesByIdAndPersistsTransaction() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        try (MockedConstruction<SqlUserRepository> userRepos = mockConstruction(SqlUserRepository.class,
                     (mock, context) -> when(mock.getUserById("u1")).thenReturn(user));
             MockedConstruction<SqlTransactionRepository> txRepos = mockConstruction(SqlTransactionRepository.class);
             MockedConstruction<SqlBankAccountRepository> ignored = mockConstruction(SqlBankAccountRepository.class)) {

            WalletAccountService service = new WalletAccountService();
            UserRepository constructedUserRepo = userRepos.constructed().get(0);
            SqlTransactionRepository constructedTxRepo = txRepos.constructed().get(0);

            assertSame(user, service.addFunds("u1", null, 50.0, null));

            assertEquals(50.0, user.getWallet(), 0.001);
            verify(constructedUserRepo).saveUser(user);
            verify(constructedTxRepo).saveTransaction(argThat(tx ->
                    "DEPOSIT".equals(tx.type) && tx.amount == 50.0 && tx.balanceAfter == 50.0));
        }
    }

    @Test
    public void addFunds_rejectsNonPositiveAmountAndMissingUser() throws Exception {
        try (MockedConstruction<SqlUserRepository> ignoredUsers = mockConstruction(SqlUserRepository.class);
             MockedConstruction<SqlTransactionRepository> ignoredTx = mockConstruction(SqlTransactionRepository.class);
             MockedConstruction<SqlBankAccountRepository> ignoredBanks = mockConstruction(SqlBankAccountRepository.class)) {
            WalletAccountService service = new WalletAccountService();

            assertThrows(IllegalArgumentException.class, () -> service.addFunds("u1", null, 0.0, null));
            assertThrows(IllegalArgumentException.class, () -> service.addFunds("missing", null, 10.0, null));
        }
    }

    @Test
    public void withdraw_deductsFundsAndRejectsInsufficientBalance() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        user.setWallet(100.0);
        try (MockedConstruction<SqlUserRepository> userRepos = mockConstruction(SqlUserRepository.class,
                     (mock, context) -> when(mock.getUserByUsername("user")).thenReturn(user));
             MockedConstruction<SqlTransactionRepository> txRepos = mockConstruction(SqlTransactionRepository.class);
             MockedConstruction<SqlBankAccountRepository> ignored = mockConstruction(SqlBankAccountRepository.class)) {

            WalletAccountService service = new WalletAccountService();

            assertSame(user, service.withdraw(null, "user", 40.0, null));
            assertEquals(60.0, user.getWallet(), 0.001);
            verify(userRepos.constructed().get(0)).saveUser(user);
            verify(txRepos.constructed().get(0)).saveTransaction(argThat(tx ->
                    "WITHDRAW".equals(tx.type) && tx.amount == 40.0 && tx.balanceAfter == 60.0));
            assertThrows(IllegalArgumentException.class, () -> service.withdraw(null, "user", 100.0, null));
        }
    }

    @Test
    public void payloadBasedAddFundsAndWithdrawReadAmountFromMap() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        user.setWallet(100.0);
        try (MockedConstruction<SqlUserRepository> userRepos = mockConstruction(SqlUserRepository.class,
                     (mock, context) -> when(mock.getUserById("u1")).thenReturn(user));
             MockedConstruction<SqlTransactionRepository> ignoredTx = mockConstruction(SqlTransactionRepository.class);
             MockedConstruction<SqlBankAccountRepository> ignoredBanks = mockConstruction(SqlBankAccountRepository.class)) {
            WalletAccountService service = new WalletAccountService();

            service.addFunds(null, Map.of("userId", "u1", "amount", "25.5"));
            service.withdraw(null, Map.of("userId", "u1", "amount", 20.5));

            assertEquals(105.0, user.getWallet(), 0.001);
            verify(userRepos.constructed().get(0), times(2)).saveUser(user);
        }
    }

    @Test
    public void linkBank_validatesUserFieldsAndExistingAccount() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        try (MockedConstruction<SqlUserRepository> ignoredUsers = mockConstruction(SqlUserRepository.class);
             MockedConstruction<SqlTransactionRepository> ignoredTx = mockConstruction(SqlTransactionRepository.class);
             MockedConstruction<SqlBankAccountRepository> bankRepos = mockConstruction(SqlBankAccountRepository.class)) {
            WalletAccountService service = new WalletAccountService();
            SqlBankAccountRepository bankRepo = bankRepos.constructed().get(0);

            assertThrows(IllegalStateException.class, () -> service.linkBank(null, "Bank", "123", 0.0));
            assertThrows(IllegalArgumentException.class, () -> service.linkBank(user, "", "123", 0.0));

            service.linkBank(user, "Bank", "123", 500.0);
            verify(bankRepo).saveBankAccount("u1", "user", "user@example.com", "Bank", "123", 500.0);

            when(bankRepo.hasBankAccount("u1")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> service.linkBank(user, "Bank", "456", 0.0));
        }
    }

    @Test
    public void getTransactionsAndBankAccountUseCurrentUserFallbacks() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        common.Transaction tx = new common.Transaction("u1", "DEPOSIT", 10.0, 10.0, "deposit");
        try (MockedConstruction<SqlUserRepository> ignoredUsers = mockConstruction(SqlUserRepository.class);
             MockedConstruction<SqlTransactionRepository> txRepos = mockConstruction(SqlTransactionRepository.class,
                     (mock, context) -> when(mock.getByUser("u1")).thenReturn(List.of(tx)));
             MockedConstruction<SqlBankAccountRepository> bankRepos = mockConstruction(SqlBankAccountRepository.class,
                     (mock, context) -> {
                         when(mock.hasBankAccount("u1")).thenReturn(true);
                         when(mock.getBankAccount("u1")).thenReturn(Map.of("bankName", "Bank"));
                     });
             MockedConstruction<SqlAuctionRepository> ignoredAuctions = mockConstruction(SqlAuctionRepository.class)) {
            WalletAccountService service = new WalletAccountService();

            assertEquals(List.of(tx), service.getTransactions(user, ""));
            assertEquals(Map.of("bankName", "Bank"), service.getBankAccount(user));
            assertThrows(IllegalStateException.class, () -> service.getTransactions(null, ""));
            assertThrows(IllegalStateException.class, () -> service.getBankAccount(null));
        }
    }

    @Test
    public void getTransactions_reconcilesMissingSellerPayouts() throws Exception {
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setWallet(0.0);

        Electronics item = new Electronics();
        item.setName("Laptop");

        Auction auction = new Auction();
        auction.setAuctionId("A1");
        auction.setSellerId("seller-1");
        auction.setHighestBidderId("winner-1");
        auction.setCurrentPrice(420_000.0);
        auction.setStatus(AuctionStatus.FINISHED);
        auction.setItem(item);

        try (MockedConstruction<SqlUserRepository> userRepos = mockConstruction(SqlUserRepository.class,
                     (mock, context) -> when(mock.getUserById("seller-1")).thenReturn(seller));
             MockedConstruction<SqlTransactionRepository> txRepos = mockConstruction(SqlTransactionRepository.class,
                     (mock, context) -> when(mock.getByUser("seller-1")).thenReturn(List.of()));
             MockedConstruction<SqlBankAccountRepository> ignoredBanks = mockConstruction(SqlBankAccountRepository.class);
             MockedConstruction<SqlAuctionRepository> auctionRepos = mockConstruction(SqlAuctionRepository.class,
                     (mock, context) -> when(mock.getAuctionsBySellerId("seller-1")).thenReturn(List.of(auction)))) {
            WalletAccountService service = new WalletAccountService();

            service.getTransactions(seller, "seller-1");

            assertEquals(420_000.0, seller.getWallet(), 0.001);
            verify(userRepos.constructed().get(0)).saveUser(seller);
            verify(txRepos.constructed().get(0)).saveTransaction(argThat(tx ->
                    "seller-1".equals(tx.userId)
                            && "DEPOSIT".equals(tx.type)
                            && tx.amount == 420_000.0
                            && tx.balanceAfter == 420_000.0
                            && tx.description.contains("auction A1")));
            verify(auctionRepos.constructed().get(0)).getAuctionsBySellerId("seller-1");
        }
    }
}


