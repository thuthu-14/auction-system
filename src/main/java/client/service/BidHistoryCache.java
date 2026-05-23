package client.service;

import server.model.Bid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BidHistoryCache {
    private final Map<String, List<Bid>> bidsByAuctionId = new ConcurrentHashMap<>();

    public List<Bid> get(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return List.of();
        }
        return new ArrayList<>(bidsByAuctionId.getOrDefault(auctionId, List.of()));
    }

    public void put(String auctionId, List<Bid> bids) {
        if (auctionId == null || auctionId.isBlank() || bids == null || bids.isEmpty()) {
            return;
        }
        List<Bid> sortedBids = new ArrayList<>(bids);
        sortedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        bidsByAuctionId.put(auctionId, sortedBids);
    }

    public List<Bid> merge(String auctionId, List<Bid> incomingBids) {
        List<Bid> mergedBids = get(auctionId);
        for (Bid bid : incomingBids != null ? incomingBids : List.<Bid>of()) {
            mergedBids.removeIf(existingBid -> isSameBid(existingBid, bid));
            mergedBids.add(bid);
        }
        mergedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        put(auctionId, mergedBids);
        return mergedBids;
    }

    public List<Bid> merge(String auctionId, Bid bid) {
        if (bid == null) {
            return get(auctionId);
        }
        return merge(auctionId, List.of(bid));
    }

    public double getHighestPrice(String auctionId) {
        double highest = 0;
        for (Bid bid : get(auctionId)) {
            if (bid != null) {
                highest = Math.max(highest, bid.getAmount());
            }
        }
        return highest;
    }

    public String buildSignature(List<Bid> sortedBids, double firstValue) {
        StringBuilder signature = new StringBuilder();
        signature.append(Math.round(firstValue)).append('|');
        for (Bid bid : sortedBids != null ? sortedBids : List.<Bid>of()) {
            if (bid == null) {
                continue;
            }
            signature.append(safeText(bid.getBidId()))
                    .append(':')
                    .append(safeText(bid.getAuctionId()))
                    .append(':')
                    .append(safeText(bid.getBidderId()))
                    .append(':')
                    .append(bid.getBidTime())
                    .append(':')
                    .append(Math.round(bid.getAmount()))
                    .append('|');
        }
        return signature.toString();
    }

    private boolean isSameBid(Bid existingBid, Bid newBid) {
        if (existingBid == null || newBid == null) {
            return false;
        }
        if (existingBid.getBidId() != null && newBid.getBidId() != null) {
            return existingBid.getBidId().equals(newBid.getBidId());
        }
        return existingBid.getAuctionId() != null
                && existingBid.getAuctionId().equals(newBid.getAuctionId())
                && existingBid.getBidderId() != null
                && existingBid.getBidderId().equals(newBid.getBidderId())
                && existingBid.getBidTime() == newBid.getBidTime()
                && Double.compare(existingBid.getAmount(), newBid.getAmount()) == 0;
    }

    private String safeText(String value) {
        return value != null && !value.isBlank() ? value : "";
    }
}
