package client.util;

import common.ItemCategory;
import server.model.Auction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RecommendationTracker {
    private static final int MAX_RECENT_AUCTIONS = 20;
    private static final int MAX_RECENT_CATEGORIES = 10;
    private static final Deque<String> recentAuctionIds = new ArrayDeque<>();
    private static final Deque<ItemCategory> recentCategories = new ArrayDeque<>();

    private RecommendationTracker() {
    }

    public static synchronized void recordAuctionClick(Auction auction) {
        if (auction == null) {
            return;
        }
        String auctionId = auction.getAuctionId();
        if (auctionId != null && !auctionId.isBlank() && !"--".equals(auctionId)) {
            recentAuctionIds.remove(auctionId);
            recentAuctionIds.addFirst(auctionId);
            while (recentAuctionIds.size() > MAX_RECENT_AUCTIONS) {
                recentAuctionIds.removeLast();
            }
        }
        if (auction.getItem() != null) {
            recordCategoryClick(auction.getItem().getCategory());
        }
    }

    public static synchronized void recordCategoryClick(ItemCategory category) {
        if (category == null) {
            return;
        }
        recentCategories.remove(category);
        recentCategories.addFirst(category);
        while (recentCategories.size() > MAX_RECENT_CATEGORIES) {
            recentCategories.removeLast();
        }
    }

    public static synchronized List<String> getRecentAuctionIds() {
        return new ArrayList<>(recentAuctionIds);
    }

    public static synchronized Map<ItemCategory, Integer> getCategoryWeights() {
        Map<ItemCategory, Integer> weights = new EnumMap<>(ItemCategory.class);
        int score = recentCategories.size() + 1;
        for (ItemCategory category : recentCategories) {
            weights.merge(category, score, Integer::sum);
            score = Math.max(1, score - 1);
        }
        return weights;
    }

    public static synchronized boolean hasSignals() {
        return !recentAuctionIds.isEmpty() || !recentCategories.isEmpty();
    }
}
