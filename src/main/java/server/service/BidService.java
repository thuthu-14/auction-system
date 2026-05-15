package server.service;

import common.AuctionStatus;
import server.exception.AuctionClosedException;
import server.exception.InsufficientFundsException;
import server.exception.InvalidBidException;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.repository.*;
import util.ItemValidationUtil;
import util.LoggerUtil;
import util.ValidationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class BidService {

    private static final BidService DEFAULT = new BidService(
            new SqlBidRepository(),
            new SqlAuctionRepository(),
            new SqlUserRepository()
    );

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    public Bid place(RegularUser bidder, Auction auction, double amount)
            throws Exception {
        validateBidderAndAuction(bidder, auction, amount);

        common.ItemCategory category = auction.getItem().getCategory();

        synchronized (auction) {
            if (auction.getStatus() != AuctionStatus.OPEN &&
                    auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc.");
            }

            String bidError = auction.getMinimumBidIncrement() > 0
                    ? ItemValidationUtil.getBidAmountErrorMessage(auction.getCurrentPrice(), amount, auction.getMinimumBidIncrement())
                    : ItemValidationUtil.getBidAmountErrorMessage(category, auction.getCurrentPrice(), amount);
            if (bidError != null) {
                throw new InvalidBidException(bidError);
            }

            if (bidder.getUserId().equals(auction.getSellerId())) {
                throw new PermissionDeniedException("Bạn không thể đặt giá trên phiên đấu giá của mình.");
            }

            String bidId = "BID" + System.currentTimeMillis();
            Bid bid = new Bid(bidId, auction.getAuctionId(), bidder.getUserId(),
                    bidder.getUsername(), amount);

            try {
                auction.placeBid(bidder.getUserId(), bidder.getUsername(), amount);
                auction.addBidId(bidId);
                auction.extendAuctionIfNeeded();
            } catch (Exception e) {
                throw new InvalidBidException(e.getMessage());
            }

            bidRepository.saveBid(bid);
            auctionRepository.saveAuction(auction);
            bidder.addBidId(bidId);
            userRepository.saveUser(bidder);

            LoggerUtil.info("Bid placed: " + amount + " by " + bidder.getUsername() +
                    " on auction " + auction.getAuctionId());

            return bid;
        }
    }

    public List<Bid> getHistory(String auctionId) throws Exception {
        List<Bid> bids = new ArrayList<>(bidRepository.getBidsByAuctionId(auctionId));

        Auction auction = auctionRepository.getAuctionById(auctionId);
        if (auction != null) {
            for (String bidId : auction.getBidIds()) {
                Bid bid = bidRepository.getBidById(bidId);
                if (bid != null && bids.stream().noneMatch(existing -> isSameBid(existing, bid))) {
                    bids.add(bid);
                }
            }
        }

        bids.sort(Comparator.comparingLong(Bid::getBidTime));
        return bids;
    }

    public List<Bid> getUserBidList(String bidderId) throws Exception {
        return bidRepository.getBidsByBidderId(bidderId);
    }

    private void validateBidderAndAuction(RegularUser bidder, Auction auction, double amount)
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
        if (amount > bidder.getWallet()) {
            throw new InsufficientFundsException("Số dư ví không đủ để đặt giá này.");
        }
        if (auction.getItem() == null || auction.getItem().getCategory() == null) {
            throw new InvalidBidException("Phiên đấu giá không có thông tin sản phẩm hợp lệ.");
        }
    }

    private static boolean isSameBid(Bid existingBid, Bid newBid) {
        if (existingBid == null || newBid == null) {
            return false;
        }
        if (existingBid.getBidId() != null && newBid.getBidId() != null) {
            return existingBid.getBidId().equals(newBid.getBidId());
        }
        return Objects.equals(existingBid.getAuctionId(), newBid.getAuctionId())
                && Objects.equals(existingBid.getBidderId(), newBid.getBidderId())
                && existingBid.getBidTime() == newBid.getBidTime()
                && Double.compare(existingBid.getAmount(), newBid.getAmount()) == 0;
    }

    public static Bid placeBid(RegularUser bidder, Auction auction, double amount)
            throws InvalidBidException, PermissionDeniedException, AuctionClosedException,
            InsufficientFundsException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.place(bidder, auction, amount);
        } catch (InvalidBidException | PermissionDeniedException | AuctionClosedException |
                 InsufficientFundsException | IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Bid> getBidHistory(String auctionId)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getHistory(auctionId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Bid> getUserBids(String bidderId)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getUserBidList(bidderId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
