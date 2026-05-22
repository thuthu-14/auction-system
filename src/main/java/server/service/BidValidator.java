package server.service;

import server.exception.InsufficientFundsException;
import server.exception.InvalidBidException;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.RegularUser;
import util.ValidationUtil;

public class BidValidator {
    public void validateBidderAndAuction(RegularUser bidder, Auction auction, double amount)
            throws InvalidBidException, PermissionDeniedException, InsufficientFundsException {
        if (bidder == null) {
            throw new PermissionDeniedException("Người đấu giá không hợp lệ.");
        }
        if (auction == null) {
            throw new PermissionDeniedException("Phiên đấu giá không hợp lệ.");
        }
        if (!ValidationUtil.isValidBidAmount(amount)) {
            throw new InvalidBidException("Giá đặt không hợp lệ.");
        }
        if (bidder.getWallet() <= 0) {
            throw new PermissionDeniedException("Bạn cần liên kết ngân hàng và nạp tiền vào ví trước khi đấu giá.");
        }
        if (auction.getItem() == null || auction.getItem().getCategory() == null) {
            throw new InvalidBidException("Phiên đấu giá không có thông tin sản phẩm hợp lệ.");
        }
    }
}
