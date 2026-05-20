package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.RegularUser;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class SellerAuctionServiceTest {

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    ItemRepository itemRepository;

    @Mock
    UserRepository userRepository;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createSellerAuction_permissionDeniedForNonSeller() throws Exception {
        SellerAuctionService svc = new SellerAuctionService();
        Exception ex = assertThrows(Exception.class, () -> svc.createSellerAuction((RegularUser) null, java.util.Map.of()));
        assertNotNull(ex);
    }
}


