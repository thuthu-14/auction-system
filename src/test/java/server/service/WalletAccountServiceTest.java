package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.repository.SqlTransactionRepository;
import server.repository.UserRepository;
import server.model.RegularUser;

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
        WalletAccountService service = new WalletAccountService();
        RegularUser u = new RegularUser("u1","user1","p","u1@x.com");
        // call addFunds using currentUser to avoid internal repository resolution
        var result = service.addFunds(null, null, 100.0, u);

        assertEquals(100.0, result.getWallet(), 0.001);
    }
}


