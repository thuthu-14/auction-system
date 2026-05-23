package server.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.Auction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionRepositoryInterfaceTest {

    @Mock
    AuctionRepository auctionRepository;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) mocksClose.close();
    }

    @Test
    public void findAuctionByItemId_fallbacksToAllAuctions() throws Exception {
        // Arrange
        String searchId = "ITEM-123";
        Auction candidate = mock(Auction.class);
        when(candidate.getItemId()).thenReturn(searchId);

        when(auctionRepository.getAuctionById(searchId)).thenReturn(null);
        when(auctionRepository.getAllAuctions()).thenReturn(List.of(candidate));

        // create service with mocked repository
        server.service.AuctionService service = new server.service.AuctionService(auctionRepository, null, null);

        // Act
        Auction found = service.getById(searchId);

        // Assert
        assertNotNull(found);
        assertEquals(searchId, found.getItemId());
        verify(auctionRepository, times(1)).getAuctionById(searchId);
        verify(auctionRepository, times(1)).getAllAuctions();
    }
}

