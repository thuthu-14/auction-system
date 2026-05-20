package server.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.Auction;

import static org.mockito.Mockito.*;

public class BidRepositoryInterfaceTest {

    @Mock
    BidRepository bidRepository;

    @Mock
    AuctionRepository auctionRepository;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) mocksClose.close();
    }

    @Test
    public void cancel_usesBidRepositoryToFetchBids() throws Exception {
        // arrange
        Auction auction = mock(Auction.class);
        when(auction.getAuctionId()).thenReturn("A3");
        when(auction.getSellerId()).thenReturn("seller1");

        when(auctionRepository.getAllAuctions()).thenReturn(java.util.List.of(auction));

        server.service.AuctionService service = new server.service.AuctionService(auctionRepository, null, null,
                bidRepository, null);

        // act
        service.cancel("A3", "seller1");

        // assert: bidRepository.getBidsByAuctionId should be called
        verify(bidRepository, atLeastOnce()).getBidsByAuctionId("A3");
    }
}

