package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.AuctionServer;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.service.BidPlacementResult;
import server.service.AuctionService;
import server.service.BidService;
import server.service.SellerAuctionService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionBidMessageProcessorTest {

    @Mock
    ClientSessionContext context;

    @Mock
    AuctionService auctionService;

    @Mock
    BidService bidService;

    @Mock
    SellerAuctionService sellerAuctionService;

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
        AuctionBidMessageProcessor processor = processor();

        RegularUser bidder = mock(RegularUser.class);
        AuctionServer server = mock(AuctionServer.class);
        when(bidder.getUserId()).thenReturn("U1");
        when(context.getCurrentUser()).thenReturn(bidder);
        when(context.getServer()).thenReturn(server);

        Bid placedBid = mock(Bid.class);
        Auction auction = mock(Auction.class);
        BidPlacementResult result = mock(BidPlacementResult.class);
        when(result.getBid()).thenReturn(placedBid);
        when(result.getAuction()).thenReturn(auction);
        when(result.getBidder()).thenReturn(bidder);
        when(result.getPreviousHighestBidderId()).thenReturn(null);

        when(bidService.placeForCurrentUser(any(), any())).thenReturn(result);

        Message msg = new Message(MessageType.PLACE_BID, "PLACE_BID", "data");

        processor.handlePlaceBid(msg);

        verify(context, atLeastOnce()).sendMessage(any(Message.class));
        verify(server, times(2)).broadcastMessage(any(Message.class));
        verify(context).setCurrentUser(bidder);
    }

    @Test
    public void handlePlaceBid_broadcastsOutbidNotificationForPreviousWinner() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        AuctionServer server = mock(AuctionServer.class);
        RegularUser bidder = mock(RegularUser.class);
        when(bidder.getUserId()).thenReturn("new-winner");
        when(context.getCurrentUser()).thenReturn(bidder);
        when(context.getServer()).thenReturn(server);

        BidPlacementResult result = mock(BidPlacementResult.class);
        when(result.getBid()).thenReturn(mock(Bid.class));
        when(result.getAuction()).thenReturn(mock(Auction.class));
        when(result.getBidder()).thenReturn(bidder);
        when(result.getPreviousHighestBidderId()).thenReturn("old-winner");
        when(bidService.placeForCurrentUser(any(), any())).thenReturn(result);

        processor.handlePlaceBid(new Message(MessageType.PLACE_BID, (Object) "payload", "client"));

        ArgumentCaptor<Message> broadcastCaptor = ArgumentCaptor.forClass(Message.class);
        verify(server, times(3)).broadcastMessage(broadcastCaptor.capture());
        assertTrue(broadcastCaptor.getAllValues().stream()
                .anyMatch(message -> message.getType() == MessageType.OUTBID_NOTIFICATION));
    }

    @Test
    public void handlePlaceBid_invalidBid_sendsError() throws Exception {
        AuctionBidMessageProcessor processor = processor();

        when(context.getCurrentUser()).thenReturn(mock(RegularUser.class));

        when(bidService.placeForCurrentUser(any(), any()))
                .thenThrow(new server.exception.InvalidBidException("Invalid"));

        Message msg = new Message(MessageType.PLACE_BID, "PLACE_BID", "data");
        processor.handlePlaceBid(msg);

        verify(context, atLeastOnce()).sendError(anyString());
    }

    @Test
    public void handleGetAuctions_sendsListResponseForAllAuctionsRequest() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        Auction auction = mock(Auction.class);
        when(auctionService.getList(true)).thenReturn(List.of(auction));

        processor.handleGetAuctions(new Message(MessageType.GET_ALL_AUCTIONS, (Object) null, "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        Message response = responseCaptor.getValue();
        assertEquals(MessageType.SUCCESS, response.getType());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Auctions retrieved", response.getMessage());
        assertEquals(List.of(auction), response.getData());
    }

    @Test
    public void handleGetAuctionDetail_sendsAuctionData() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        Auction auction = mock(Auction.class);
        when(auctionService.getById("A1")).thenReturn(auction);

        processor.handleGetAuctionDetail(new Message(MessageType.GET_AUCTION_DETAIL, (Object) "A1", "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        Message response = responseCaptor.getValue();
        assertEquals(MessageType.SUCCESS, response.getType());
        assertEquals("SUCCESS", response.getStatus());
        assertSame(auction, response.getData());
    }

    @Test
    public void handleGetBidHistoryAndUserBidsSendArrayListResponses() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        RegularUser user = mock(RegularUser.class);
        Bid bid = mock(Bid.class);
        when(context.getCurrentUser()).thenReturn(user);
        doReturn(List.of(bid)).when(bidService).getHistory(any(Object.class));
        doReturn(List.of(bid)).when(bidService).getUserBidList(any(), any(Object.class));

        processor.handleGetBidHistory(new Message(MessageType.GET_BID_HISTORY, (Object) "A1", "client"));
        processor.handleGetUserBids(new Message(MessageType.GET_USER_BIDS, (Object) "U1", "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context, times(2)).sendMessage(responseCaptor.capture());
        assertEquals(List.of(bid), responseCaptor.getAllValues().get(0).getData());
        assertEquals("Bid history retrieved", responseCaptor.getAllValues().get(0).getMessage());
        assertEquals(List.of(bid), responseCaptor.getAllValues().get(1).getData());
        assertEquals("User bids retrieved", responseCaptor.getAllValues().get(1).getMessage());
    }

    @Test
    public void handleCancelAuction_sendsResponseAndBroadcastsSellerUpdates() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        AuctionServer server = mock(AuctionServer.class);
        RegularUser user = mock(RegularUser.class);
        Auction auction = mock(Auction.class);
        when(user.getUserId()).thenReturn("U1");
        when(context.getCurrentUser()).thenReturn(user);
        when(context.getServer()).thenReturn(server);
        when(auctionService.cancel(user, "A1")).thenReturn(auction);

        processor.handleCancelAuction(new Message(MessageType.CANCEL_AUCTION, (Object) "A1", "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<Message> broadcastCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        verify(server, times(2)).broadcastMessage(broadcastCaptor.capture());
        assertSame(auction, responseCaptor.getValue().getData());
        assertEquals(MessageType.AUCTION_FINISHED_NOTIFICATION, broadcastCaptor.getAllValues().get(0).getType());
        assertEquals(MessageType.SELLER_AUCTIONS_UPDATED, broadcastCaptor.getAllValues().get(1).getType());
    }

    @Test
    public void handlePlaceBid_insufficientFundsSendsFriendlyError() throws Exception {
        AuctionBidMessageProcessor processor = processor();

        when(bidService.placeForCurrentUser(any(), any()))
                .thenThrow(new server.exception.InsufficientFundsException("not enough"));

        processor.handlePlaceBid(new Message(MessageType.PLACE_BID, (Object) "payload", "client"));

        verify(context).sendError("Số dư ví không đủ!");
    }

    @Test
    public void createAndSellerAuctionHandlersSendResponsesAndBroadcasts() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        AuctionServer server = mock(AuctionServer.class);
        RegularUser seller = mock(RegularUser.class);
        Auction auction = mock(Auction.class);
        when(seller.getUserId()).thenReturn("seller-1");
        when(context.getCurrentUser()).thenReturn(seller);
        when(context.getServer()).thenReturn(server);
        when(sellerAuctionService.createAuction(seller, "create")).thenReturn(auction);
        when(sellerAuctionService.createSellerAuction(seller, "seller-create")).thenReturn(auction);

        processor.handleCreateAuction(new Message(MessageType.CREATE_AUCTION, (Object) "create", "client"));
        processor.handleCreateSellerItem(new Message(MessageType.CREATE_SELLER_ITEM, (Object) "seller-create", "client"));

        ArgumentCaptor<Message> sentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context, times(2)).sendMessage(sentCaptor.capture());
        assertEquals("Auction created", sentCaptor.getAllValues().get(0).getMessage());
        assertEquals("Auction created successfully", sentCaptor.getAllValues().get(1).getMessage());
        verify(server, times(2)).broadcastMessage(any(Message.class));
    }

    @Test
    public void adminDeleteSellerDeleteAndSellerAuctionsHandlersSendResponses() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        AuctionServer server = mock(AuctionServer.class);
        RegularUser user = mock(RegularUser.class);
        Auction auction = mock(Auction.class);
        when(context.getCurrentUser()).thenReturn(user);
        when(context.getServer()).thenReturn(server);
        when(auctionService.deleteForAdmin(user, "A1")).thenReturn("A1");
        when(sellerAuctionService.deleteSellerAuction((server.model.User) user, (Object) "A2")).thenReturn("A2");
        when(auctionService.getBySeller((server.model.User) user)).thenReturn(List.of(auction));

        processor.handleDeleteAuctionAdmin(new Message(MessageType.DELETE_AUCTION_ADMIN, (Object) "A1", "client"));
        processor.handleDeleteSellerItem(new Message(MessageType.DELETE_SELLER_ITEM, (Object) "A2", "client"));
        processor.handleGetSellerAuctions(new Message(MessageType.GET_SELLER_AUCTIONS, (Object) null, "client"));

        verify(context, times(3)).sendMessage(any(Message.class));
        verify(server, times(2)).broadcastMessage(any(Message.class));
    }

    @Test
    public void auctionHandlersSendErrorWhenServicesFail() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        when(auctionService.getList(false)).thenThrow(new IOException("list failed"));
        when(auctionService.getById("A1")).thenThrow(new IOException("detail failed"));
        when(bidService.getHistory(any(Object.class))).thenThrow(new IOException("history failed"));
        when(bidService.getUserBidList(any(), any(Object.class))).thenThrow(new IOException("user bids failed"));
        when(sellerAuctionService.createAuction(any(), eq("create"))).thenThrow(new IOException("create failed"));
        when(sellerAuctionService.createSellerAuction(any(), eq("seller-create"))).thenThrow(new IOException("seller create failed"));
        when(auctionService.deleteForAdmin(any(), eq("A1"))).thenThrow(new IOException("delete failed"));
        when(sellerAuctionService.deleteSellerAuction(any(server.model.User.class), eq((Object) "A2")))
                .thenThrow(new IOException("seller delete failed"));
        when(auctionService.getBySeller(nullable(server.model.User.class))).thenThrow(new IOException("seller list failed"));

        processor.handleGetAuctions(new Message(MessageType.GET_AUCTIONS, (Object) null, "client"));
        processor.handleGetAuctionDetail(new Message(MessageType.GET_AUCTION_DETAIL, (Object) "A1", "client"));
        processor.handleGetBidHistory(new Message(MessageType.GET_BID_HISTORY, (Object) "A1", "client"));
        processor.handleGetUserBids(new Message(MessageType.GET_USER_BIDS, (Object) "U1", "client"));
        processor.handleCreateAuction(new Message(MessageType.CREATE_AUCTION, (Object) "create", "client"));
        processor.handleCreateSellerItem(new Message(MessageType.CREATE_SELLER_ITEM, (Object) "seller-create", "client"));
        processor.handleDeleteAuctionAdmin(new Message(MessageType.DELETE_AUCTION_ADMIN, (Object) "A1", "client"));
        processor.handleDeleteSellerItem(new Message(MessageType.DELETE_SELLER_ITEM, (Object) "A2", "client"));
        processor.handleGetSellerAuctions(new Message(MessageType.GET_SELLER_AUCTIONS, (Object) null, "client"));

        verify(context, times(9)).sendError(anyString());
    }

    @Test
    public void handleCancelAuction_nullErrorMessageUsesFallback() throws Exception {
        AuctionBidMessageProcessor processor = processor();
        RuntimeException error = new RuntimeException((String) null);
        when(auctionService.cancel(nullable(server.model.User.class), eq((Object) "A1"))).thenThrow(error);

        processor.handleCancelAuction(new Message(MessageType.CANCEL_AUCTION, (Object) "A1", "client"));

        verify(context).sendError("Không thể hủy phiên");
    }

    private AuctionBidMessageProcessor processor() {
        return new AuctionBidMessageProcessor(context, auctionService, bidService, sellerAuctionService);
    }
}

