package server.handler;

import common.Message;
import common.MessageType;
import server.exception.*;
import server.model.*;
import server.observer.AuctionManager;
import server.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuctionBidMessageProcessor {
    private final ClientSessionContext context;
    private final SellerAuctionService sellerAuctionService = new SellerAuctionService();

    public AuctionBidMessageProcessor(ClientSessionContext context) {
        this.context = context;
    }

    public void handleCreateAuction(Message message) throws IOException, ClassNotFoundException {
        try {
            Auction auction = sellerAuctionService.createAuction(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction created");
            response.setData(auction);
            context.sendMessage(response);
            context.getServer().broadcastMessage(new Message(MessageType.UPDATE, auction, context.getCurrentUser().getUserId()));
        } catch (PermissionDeniedException e) {
            context.sendError(e.getMessage());
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleGetAuctions(Message message) throws IOException, ClassNotFoundException {
        try {
            List<Auction> auctions = AuctionService.getAuctions(message.getType() == MessageType.GET_ALL_AUCTIONS);
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auctions retrieved");
            response.setData(new ArrayList<>(auctions));
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleGetAuctionDetail(Message message) throws IOException {
        try {
            Auction auction = AuctionService.getAuctionByRequest(message.getData());
            Message response = new Message(MessageType.SUCCESS, auction, "SERVER");
            response.setStatus("SUCCESS");
            context.sendMessage(response);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Get auction detail", e);
            context.sendError(e.getMessage());
        }
    }

    public void handlePlaceBid(Message message) throws IOException, ClassNotFoundException {
        try {
            BidPlacementResult result = BidService.placeBidForCurrentUser(context.getCurrentUser(), message.getData());
            Bid bid = result.getBid();
            Auction auction = result.getAuction();
            RegularUser bidder = result.getBidder();
            String previousHighestBidderId = result.getPreviousHighestBidderId();
            context.setCurrentUser(bidder);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Bid placed successfully");
            response.setData(bid);
            response.setStatus("SUCCESS");
            context.sendMessage(response);

            context.getServer().broadcastMessage(new Message(MessageType.UPDATE_PRICE_REALTIME, auction, context.getCurrentUser().getUserId()));
            context.getServer().broadcastMessage(new Message(MessageType.UPDATE_PRICE_REALTIME, bid, context.getCurrentUser().getUserId()));
            AuctionManager.getInstance().notifyBidPlaced(auction, bid, previousHighestBidderId);

            if (previousHighestBidderId != null && !previousHighestBidderId.equals(bidder.getUserId())) {
                context.getServer().broadcastMessage(new Message(MessageType.OUTBID_NOTIFICATION, auction, context.getCurrentUser().getUserId()));
            }
        } catch (InvalidBidException | AuctionClosedException | PermissionDeniedException e) {
            context.sendError(e.getMessage());
        } catch (InsufficientFundsException e) {
            context.sendError("So du vi khong du!");
        } catch (Exception e) {
            context.sendError("Khong the dat gia: " + e.getMessage());
        }
    }

    public void handleGetBidHistory(Message message) throws IOException, ClassNotFoundException {
        try {
            List<Bid> bids = BidService.getBidHistoryByRequest(message.getData());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Bid history retrieved");
            response.setData(new ArrayList<>(bids));
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleGetUserBids(Message message) throws IOException, ClassNotFoundException {
        try {
            List<Bid> bids = BidService.getUserBids(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "User bids retrieved");
            response.setData(new ArrayList<>(bids));
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleDeleteAuctionAdmin(Message message) throws IOException, ClassNotFoundException {
        try {
            String auctionId = AuctionService.deleteAuctionForAdmin(context.getCurrentUser(), message.getData());
            context.sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "Auction deleted successfully"));
            context.getServer().broadcastMessage(new Message(MessageType.UPDATE, "Auction deleted", auctionId));
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleCancelAuction(Message message) throws IOException, ClassNotFoundException {
        try {
            Auction auction = AuctionService.cancelAuction(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction cancelled successfully");
            response.setData(auction);
            context.sendMessage(response);
            context.getServer().broadcastMessage(new Message(MessageType.AUCTION_FINISHED_NOTIFICATION, auction, context.getCurrentUser().getUserId()));
            context.getServer().broadcastMessage(new Message(MessageType.SELLER_AUCTIONS_UPDATED, auction, context.getCurrentUser().getUserId()));
        } catch (Exception e) {
            context.sendError(e.getMessage() != null ? e.getMessage() : "Khong the huy phien");
        }
    }

    public void handleGetSellerAuctions(Message message) throws IOException, ClassNotFoundException {
        try {
            List<Auction> sellerAuctions = AuctionService.getAuctionsBySeller(context.getCurrentUser());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Seller auctions retrieved");
            response.setData(new ArrayList<>(sellerAuctions));
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleCreateSellerItem(Message message) throws IOException, ClassNotFoundException {
        try {
            Auction auction = sellerAuctionService.createSellerAuction(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction created successfully");
            response.setData(auction);
            context.sendMessage(response);
            context.getServer().broadcastMessage(new Message(MessageType.UPDATE, auction, context.getCurrentUser().getUserId()));
        } catch (Exception e) {
            ServerExceptionHandler.handle("Create seller auction", e);
            context.sendError(e.getMessage());
        }
    }

    public void handleDeleteSellerItem(Message message) throws IOException, ClassNotFoundException {
        try {
            String auctionId = sellerAuctionService.deleteSellerAuction(context.getCurrentUser(), message.getData());
            context.sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "Auction deleted successfully"));
            context.getServer().broadcastMessage(new Message(MessageType.UPDATE, "Auction deleted", auctionId));
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }
}
