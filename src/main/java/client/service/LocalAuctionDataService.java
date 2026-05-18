package client.service;

import server.model.Auction;
import server.model.Bid;
import server.model.User;

import java.util.List;
import java.util.Map;

public class LocalAuctionDataService {
    public List<Bid> loadBidsForAuction(String auctionId) {
        return List.of();
    }

    public List<Bid> loadBidsForUser(User user, boolean filterByCurrentUser) {
        return List.of();
    }

    public Map<String, Auction> loadAuctionsById() {
        return Map.of();
    }

    public Map<String, String> findSellerContact(Auction auction) {
        return null;
    }

    public boolean isBidFromUser(Bid bid, User user) {
        if (bid == null || user == null) {
            return false;
        }
        String userId = user.getUserId();
        return userId != null && userId.equals(bid.getBidderId());
    }
}
