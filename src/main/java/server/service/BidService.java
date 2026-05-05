// src/main/java/server/service/BidService.java
package server.service;

import server.model.*;
import server.storage.*;
import server.exception.*;
import common.AuctionStatus;
import util.LoggerUtil;
import util.ValidationUtil;
import util.ItemValidationUtil;
import java.io.IOException;
import java.util.*;

/**
 * BidService - Xử lý bidding operations
 * Thread-safe: Sử dụng synchronized
 *
 * ← SỬA: Kiểm tra bid phải cao hơn >= (current + increment)
 */
public class BidService {

    /**
     * Đặt giá - Thread-safe
     * ← SỬA: Validate bid amount dựa trên category increment
     */
    public static Bid placeBid(RegularUser bidder, Auction auction, double amount)
            throws InvalidBidException, PermissionDeniedException, AuctionClosedException,
            InsufficientFundsException, IOException, ClassNotFoundException {

        if (bidder == null) {
            throw new PermissionDeniedException("❌ Bidder không hợp lệ!");
        }

        if (auction == null) {
            throw new PermissionDeniedException("❌ Auction không hợp lệ!");
        }

        if (!ValidationUtil.isValidBidAmount(amount)) {
            throw new InvalidBidException("❌ Giá bid không hợp lệ!");
        }

        // Lấy category của item để kiểm tra increment
        common.ItemCategory category = auction.getItem().getCategory();

        // Synchronized - Đảm bảo thread-safe
        synchronized (auction) {
            // Kiểm tra auction còn mở không
            if (auction.getStatus() != AuctionStatus.OPEN &&
                    auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("❌ Phiên đấu giá đã kết thúc!");
            }

            // ← SỬA: Kiểm tra bid amount phải >= (current + increment)
            String bidError = ItemValidationUtil.getBidAmountErrorMessage(
                    category, auction.getCurrentPrice(), amount);
            if (bidError != null) {
                throw new InvalidBidException(bidError);
            }

            // Kiểm tra không phải seller của auction
            if (bidder.getUserId().equals(auction.getSellerId())) {
                throw new PermissionDeniedException("❌ Bạn không thể bid trên auction của mình!");
            }

            // Tạo bid
            String bidId = "BID" + System.currentTimeMillis();
            Bid bid = new Bid(bidId, auction.getAuctionId(), bidder.getUserId(),
                    bidder.getUsername(), amount);

            try {
                // Cập nhật auction
                auction.placeBid(bidder.getUserId(), bidder.getUsername(), amount);
                auction.addBidId(bidId);

                // Anti-sniping: Gia hạn phiên nếu cần
                auction.extendAuctionIfNeeded();

            } catch (Exception e) {
                throw new InvalidBidException("❌ " + e.getMessage());
            }

            // Lưu vào database
            BidDAO.saveBid(bid);
            AuctionDAO.saveAuction(auction);
            bidder.addBidId(bidId);
            UserDAO.saveUser(bidder);

            LoggerUtil.info("✓ Bid placed: $" + amount + " by " + bidder.getUsername() +
                    " on auction " + auction.getAuctionId() +
                    " (Increment: $" + category.getMinimumBidIncrement() + ")");

            return bid;
        }
    }

    /**
     * Lấy bid history của auction
     */
    public static List<Bid> getBidHistory(String auctionId)
            throws IOException, ClassNotFoundException {
        return BidDAO.getBidsByAuctionId(auctionId);
    }

    /**
     * Lấy tất cả bids của user
     */
    public static List<Bid> getUserBids(String bidderId)
            throws IOException, ClassNotFoundException {
        return BidDAO.getBidsByBidderId(bidderId);
    }
}

