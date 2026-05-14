package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Auction;
import server.model.Bid;
import server.model.User;
import util.LoggerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionHistoryClientService {
    private final LocalAuctionDataService localDataService;
    private final AuctionBidClientService bidClientService;

    public AuctionHistoryClientService() {
        this(new LocalAuctionDataService());
    }

    public AuctionHistoryClientService(LocalAuctionDataService localDataService) {
        this.localDataService = localDataService;
        this.bidClientService = new AuctionBidClientService(localDataService);
    }

    public List<Bid> fetchUserBids(MessageTransport transport, User user) throws Exception {
        if (user == null) {
            return List.of();
        }
        ensureConnected(transport);
        Message response = transport.sendAndReceive(
                new Message(MessageType.GET_USER_BIDS, user.getUserId(), user.getUsername()));
        return bidClientService.extractBidList(response != null ? response.getData() : null);
    }

    public Auction fetchAuction(MessageTransport transport, User user, String auctionId) {
        try {
            if (user == null || auctionId == null || auctionId.isBlank()) {
                return null;
            }
            ensureConnected(transport);
            Message response = transport.sendAndReceive(
                    new Message(MessageType.GET_AUCTION_DETAIL, auctionId, user.getUsername()));
            if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() instanceof Auction auction) {
                return auction;
            }
        } catch (Exception e) {
            LoggerUtil.error("Fetch auction failed: " + e.getMessage());
        }
        return null;
    }

    public Map<String, String> fetchSellerContact(MessageTransport transport, User user, Auction auction) {
        Map<String, String> localContact = localDataService.findSellerContact(auction);
        if (auction == null || user == null || transport == null || !transport.isConnected()) {
            return localContact;
        }

        try {
            Message response = transport.sendAndReceive(
                    new Message(MessageType.GET_SELLER_CONTACT, auction.getSellerId(), user.getUsername()));
            if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() instanceof Map<?, ?> data) {
                Map<String, String> contact = new HashMap<>();
                contact.put("name", asText(data.get("name"), auction.getSellerName()));
                contact.put("email", asText(data.get("email"), "Chưa cập nhật"));
                contact.put("phone", asText(data.get("phone"), "Chưa cập nhật"));
                contact.put("address", asText(data.get("address"), "Chưa cập nhật"));
                return contact;
            }
        } catch (Exception e) {
            LoggerUtil.error("Load seller contact failed: " + e.getMessage());
        }
        return localContact;
    }

    public List<Bid> loadLocalBids(User user, boolean filterByCurrentUser) {
        return localDataService.loadBidsForUser(user, filterByCurrentUser);
    }

    public Map<String, Auction> loadLocalAuctionsById() {
        return localDataService.loadAuctionsById();
    }

    public Map<String, String> findLocalSellerContact(Auction auction) {
        return localDataService.findSellerContact(auction);
    }

    public boolean isBidFromUser(Bid bid, User user) {
        return localDataService.isBidFromUser(bid, user);
    }

    private void ensureConnected(MessageTransport transport) throws Exception {
        if (transport == null || !transport.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
    }

    private String asText(Object value, String fallback) {
        String text = value != null ? value.toString() : "";
        return text.isBlank() ? fallback : text;
    }
}
