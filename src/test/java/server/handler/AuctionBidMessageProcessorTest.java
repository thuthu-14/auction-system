package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.service.BidPlacementResult;
import server.service.BidService;

import static org.mockito.Mockito.*;

public class AuctionBidMessageProcessorTest {

    @Mock
    ClientSessionContext context;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    private AutoCloseable mocksClose;

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) mocksClose.close();
    }

    @Test
    public void handlePlaceBid_success_sendsResponsesAndBroadcasts() throws Exception {
        try (MockedStatic<BidService> bidServiceStatic = mockStatic(BidService.class)) {
            AuctionBidMessageProcessor processor = new AuctionBidMessageProcessor(context);

            // setup context and return values
            RegularUser bidder = mock(RegularUser.class);
            when(context.getCurrentUser()).thenReturn(bidder);

            Bid placedBid = mock(Bid.class);
            Auction auction = mock(Auction.class);
            BidPlacementResult result = mock(BidPlacementResult.class);
            when(result.getBid()).thenReturn(placedBid);
            when(result.getAuction()).thenReturn(auction);
            when(result.getBidder()).thenReturn(bidder);
            when(result.getPreviousHighestBidderId()).thenReturn(null);

            bidServiceStatic.when(() -> BidService.placeBidForCurrentUser(any(), any())).thenReturn(result);

            Message msg = new Message(MessageType.PLACE_BID, "PLACE_BID", "data");

            processor.handlePlaceBid(msg);

            // verify context interactions
            verify(context, atLeastOnce()).sendMessage(any(Message.class));
            verify(context, atLeastOnce()).getServer();
        }
    }

    @Test
    public void handlePlaceBid_invalidBid_sendsError() throws Exception {
        try (MockedStatic<BidService> bidServiceStatic = mockStatic(BidService.class)) {
            AuctionBidMessageProcessor processor = new AuctionBidMessageProcessor(context);

            when(context.getCurrentUser()).thenReturn(mock(RegularUser.class));

            bidServiceStatic.when(() -> BidService.placeBidForCurrentUser(any(), any()))
                    .thenThrow(new server.exception.InvalidBidException("Invalid"));

            Message msg = new Message(MessageType.PLACE_BID, "PLACE_BID", "data");
            processor.handlePlaceBid(msg);

            verify(context, atLeastOnce()).sendError(anyString());
        }
    }
}

