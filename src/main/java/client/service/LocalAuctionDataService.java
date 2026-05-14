package client.service;

import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;
import util.JsonUtil;
import util.LoggerUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalAuctionDataService {
    private static final String NOT_UPDATED = "Chưa cập nhật";

    public List<Bid> loadBidsForAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return List.of();
        }

        List<Bid> result = new ArrayList<>();
        for (Bid bid : loadAllBids()) {
            if (bid != null && auctionId.equals(bid.getAuctionId())) {
                result.add(bid);
            }
        }
        result.sort(Comparator.comparingLong(Bid::getBidTime));
        return result;
    }

    public List<Bid> loadBidsForUser(User user, boolean filterByCurrentUser) {
        List<Bid> result = new ArrayList<>();
        for (Bid bid : loadAllBids()) {
            if (!filterByCurrentUser || isBidFromUser(bid, user)) {
                result.add(bid);
            }
        }
        result.sort(Comparator.comparingLong(Bid::getBidTime).reversed());
        return result;
    }

    public Map<String, Auction> loadAuctionsById() {
        File auctionsFile = findDataFile("auctions.json");
        if (!auctionsFile.exists()) {
            LoggerUtil.warn("Local auctions.json not found from: " + new File(".").getAbsolutePath());
            return Map.of();
        }

        try {
            List<Auction> auctions = JsonUtil.loadListFromJson(auctionsFile.getPath(), Auction.class);
            Map<String, Auction> auctionsById = new HashMap<>();
            for (Auction auction : auctions != null ? auctions : List.<Auction>of()) {
                if (auction != null && auction.getAuctionId() != null) {
                    auctionsById.put(auction.getAuctionId(), auction);
                }
            }
            return auctionsById;
        } catch (Exception e) {
            LoggerUtil.error("Local auctions load failed: " + e.getMessage());
            return Map.of();
        }
    }

    public Map<String, String> findSellerContact(Auction auction) {
        if (auction == null) {
            return null;
        }

        File usersFile = findDataFile("users.json");
        if (!usersFile.exists()) {
            return null;
        }

        try {
            List<User> users = JsonUtil.loadListFromJson(usersFile.getPath(), User.class);
            for (User user : users != null ? users : List.<User>of()) {
                if (user == null || !isSeller(auction, user)) {
                    continue;
                }

                Map<String, String> contact = new HashMap<>();
                contact.put("name", safeContact(user.getUsername(), auction.getSellerName()));
                contact.put("email", safeContact(user.getEmail(), NOT_UPDATED));
                contact.put("phone", NOT_UPDATED);
                contact.put("address", NOT_UPDATED);

                if (user instanceof RegularUser regularUser) {
                    contact.put("name", safeContact(regularUser.getShopName(), user.getUsername()));
                    contact.put("email", safeContact(regularUser.getShopEmail(), user.getEmail()));
                    contact.put("phone", safeContact(regularUser.getShopPhone(), NOT_UPDATED));
                    contact.put("address", safeContact(regularUser.getShopAddress(), NOT_UPDATED));
                }
                return contact;
            }
        } catch (Exception e) {
            LoggerUtil.error("Load local seller contact failed: " + e.getMessage());
        }
        return null;
    }

    public boolean isBidFromUser(Bid bid, User user) {
        if (bid == null || user == null) {
            return false;
        }
        String userId = user.getUserId();
        return userId != null && userId.equals(bid.getBidderId());
    }

    private List<Bid> loadAllBids() {
        File bidsFile = findDataFile("bids.json");
        if (!bidsFile.exists()) {
            LoggerUtil.warn("Local bids.json not found from: " + new File(".").getAbsolutePath());
            return List.of();
        }

        try {
            List<Bid> bids = JsonUtil.loadListFromJson(bidsFile.getPath(), Bid.class);
            return bids != null ? bids : List.of();
        } catch (Exception e) {
            LoggerUtil.error("Local bids load failed: " + e.getMessage());
            return List.of();
        }
    }

    private boolean isSeller(Auction auction, User user) {
        return (auction.getSellerId() != null && auction.getSellerId().equals(user.getUserId()))
                || (auction.getSellerName() != null && auction.getSellerName().equalsIgnoreCase(user.getUsername()));
    }

    private String safeContact(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private File findDataFile(String fileName) {
        File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 8 && current != null; i++) {
            File candidate = new File(current, "data/json/" + fileName);
            if (candidate.exists()) {
                return candidate;
            }
            current = current.getParentFile();
        }
        return new File("data/json/" + fileName);
    }
}
