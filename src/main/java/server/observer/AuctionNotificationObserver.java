package server.observer;

import server.model.Auction;
import server.model.Bid;
import server.model.Notification;
import server.repository.SqlBidRepository;
import util.LoggerUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuctionNotificationObserver implements AuctionObserver {
    @Override
    public void update(Auction auction, AuctionEvent event) {
        try {
            switch (event.getType()) {
                case AuctionEvent.BID_PLACED -> createBidNotifications(auction, event);
                case AuctionEvent.AUCTION_FINISHED -> createFinishedNotifications(auction);
                case AuctionEvent.AUCTION_CANCELLED -> createCancelledNotifications(auction);
                default -> {
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("Auction notification observer failed: " + e.getMessage());
        }
    }

    private void createBidNotifications(Auction auction, AuctionEvent event) throws Exception {
        Bid bid = event.getBid();
        if (bid == null) {
            return;
        }

        if (auction.getSellerId() != null && !auction.getSellerId().equals(bid.getBidderId())) {
            NotificationManager.getInstance().addNotification(new Notification(
                    auction.getSellerId(),
                    "SELLER_BID",
                    "Phiên đấu giá có lượt đặt giá mới",
                    "Sản phẩm " + productName(auction) + " vừa được đặt giá " + formatVnd(bid.getAmount()) + ".",
                    "Vừa xong",
                    "Xem phiên",
                    auction.getAuctionId()
            ));
        }

        String previousBidderId = event.getPreviousHighestBidderId();
        if (previousBidderId != null && !previousBidderId.isBlank()
                && !previousBidderId.equals(bid.getBidderId())) {
            NotificationManager.getInstance().addNotification(new Notification(
                    previousBidderId,
                    "OUTBID",
                    "Bạn đã bị vượt giá!",
                    "Sản phẩm " + productName(auction)
                            + " đã có người đặt mức giá mới: " + formatVnd(bid.getAmount()),
                    "Vừa xong",
                    "Đấu giá lại",
                    auction.getAuctionId()
            ));
        }
    }

    private void createCancelledNotifications(Auction auction) throws Exception {
        List<Bid> bids = new SqlBidRepository().getBidsByAuctionId(auction.getAuctionId());
        if (bids == null || bids.isEmpty()) {
            return;
        }

        Map<String, Double> refundsByUser = calculateRefundsByUser(auction, bids, true);
        Set<String> notifiedUsers = new HashSet<>();

        for (Bid bid : bids) {
            if (bid == null || bid.getBidderId() == null || bid.getBidderId().isBlank()
                    || !notifiedUsers.add(bid.getBidderId())) {
                continue;
            }

            double refundAmount = refundsByUser.getOrDefault(bid.getBidderId(), 0.0);
            String description = "Phiên đấu giá " + productName(auction)
                    + " đã kết thúc do người bán hủy.";
            if (refundAmount > 0) {
                description += " Hệ thống đã hoàn " + formatVnd(refundAmount) + " vào ví của bạn.";
            }

            NotificationManager.getInstance().addNotification(new Notification(
                    bid.getBidderId(),
                    "AUCTION_CANCELLED",
                    "Phiên đấu giá đã kết thúc",
                    description,
                    "Vừa xong",
                    "Xem ví",
                    auction.getAuctionId()
            ));
        }
    }

    private void createFinishedNotifications(Auction auction) throws Exception {
        List<Bid> bids = new SqlBidRepository().getBidsByAuctionId(auction.getAuctionId());
        if (bids == null || bids.isEmpty()) {
            return;
        }

        Set<String> notifiedUsers = new HashSet<>();
        for (Bid bid : bids) {
            if (bid == null || bid.getBidderId() == null || bid.getBidderId().isBlank()
                    || !notifiedUsers.add(bid.getBidderId())) {
                continue;
            }

            boolean winner = auction.getHighestBidderId() != null
                    && auction.getHighestBidderId().equals(bid.getBidderId());
            NotificationManager.getInstance().addNotification(new Notification(
                    bid.getBidderId(),
                    winner ? "WIN" : "AUCTION_FINISHED",
                    winner ? "Bạn đã trúng đấu giá" : "Phiên đấu giá đã kết thúc",
                    winner
                            ? "Bạn đã thắng phiên " + productName(auction)
                            + " với giá " + formatVnd(auction.getCurrentPrice()) + "."
                            : "Phiên " + productName(auction)
                            + " đã kết thúc. Nếu bạn không trúng, phần tiền đặt giá đã được hoàn vào ví.",
                    "Vừa xong",
                    winner ? "Xem ví" : "Xem lịch sử",
                    auction.getAuctionId()
            ));
        }
    }

    private Map<String, Double> calculateRefundsByUser(Auction auction, List<Bid> bids, boolean includeWinner) {
        bids.sort((first, second) -> Long.compare(first.getBidTime(), second.getBidTime()));
        Map<String, Double> refundsByUser = new HashMap<>();
        double previousPrice = startingPrice(auction);

        for (Bid bid : bids) {
            if (bid == null || bid.getBidderId() == null || bid.getBidderId().isBlank()) {
                continue;
            }
            double chargedAmount = bid.getAmount() - previousPrice;
            previousPrice = Math.max(previousPrice, bid.getAmount());
            if (chargedAmount <= 0) {
                continue;
            }
            if (includeWinner || auction.getHighestBidderId() == null
                    || !auction.getHighestBidderId().equals(bid.getBidderId())) {
                refundsByUser.merge(bid.getBidderId(), chargedAmount, Double::sum);
            }
        }
        return refundsByUser;
    }

    private double startingPrice(Auction auction) {
        if (auction != null && auction.getItem() != null) {
            return Math.max(0.0, auction.getItem().getStartingPrice());
        }
        return 0.0;
    }

    private String productName(Auction auction) {
        if (auction != null && auction.getItem() != null
                && auction.getItem().getName() != null
                && !auction.getItem().getName().isBlank()) {
            return auction.getItem().getName();
        }
        return auction != null && auction.getAuctionId() != null ? auction.getAuctionId() : "";
    }

    private String formatVnd(double amount) {
        return String.format(java.util.Locale.US, "%,.0f", amount).replace(',', '.') + "đ";
    }
}
