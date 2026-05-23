package server.service;

import common.AuctionStatus;
import common.Transaction;
import server.exception.AuctionClosedException;
import server.exception.InsufficientFundsException;
import server.exception.InvalidBidException;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;
import server.concurrency.ConcurrentBidManager;
import server.repository.*;
import util.ItemValidationUtil;
import util.LoggerUtil;

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
    private final TransactionRepository transactionRepository;
    private final BidValidator bidValidator;
    private final BidChargeCalculator chargeCalculator;

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository) {
        this(bidRepository, auctionRepository, userRepository, new SqlTransactionRepository());
    }

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository,
                      TransactionRepository transactionRepository) {
        this(bidRepository, auctionRepository, userRepository, transactionRepository,
                new BidValidator(), new BidChargeCalculator());
    }

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository,
                      TransactionRepository transactionRepository,
                      BidValidator bidValidator,
                      BidChargeCalculator chargeCalculator) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.bidValidator = bidValidator;
        this.chargeCalculator = chargeCalculator;
    }

    public Bid place(RegularUser bidder, Auction auction, double amount)
            throws Exception {
        bidValidator.validateBidderAndAuction(bidder, auction, amount);

        return ConcurrentBidManager.getInstance().withAuctionWriteLock(auction.getAuctionId(), () -> {
            Auction latestAuction = auctionRepository.getAuctionById(auction.getAuctionId());
            Auction auctionToPersist = latestAuction != null ? latestAuction : auction;
            common.ItemCategory category = auctionToPersist.getItem() != null
                    ? auctionToPersist.getItem().getCategory()
                    : auction.getItem().getCategory();

            synchronized (auctionToPersist) {
            if (auctionToPersist.getStatus() != AuctionStatus.OPEN &&
                    auctionToPersist.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc.");
            }

            String bidError = auction.getMinimumBidIncrement() > 0
                    ? ItemValidationUtil.getBidAmountErrorMessage(auction.getCurrentPrice(), amount, auction.getMinimumBidIncrement())
                    : ItemValidationUtil.getBidAmountErrorMessage(category, auction.getCurrentPrice(), amount);
            if (bidError != null) {
                throw new InvalidBidException(bidError);
            }

            if (bidder.getUserId().equals(auctionToPersist.getSellerId())) {
                throw new PermissionDeniedException("Bạn không thể đặt giá trên phiên đấu giá của mình.");
            }

            double previousUserBidAmount = getHighestUserBidAmount(bidder.getUserId(), auctionToPersist);
            double chargeAmount = chargeCalculator.calculateChargeAmount(amount, previousUserBidAmount);
            if (chargeAmount <= 0) {
                throw new InvalidBidException("Gi\u00e1 \u0111\u1eb7t kh\u00f4ng h\u1ee3p l\u1ec7.");
            }
            if (chargeAmount > bidder.getWallet()) {
                throw new InsufficientFundsException("S\u1ed1 d\u01b0 v\u00ed kh\u00f4ng \u0111\u1ee7 \u0111\u1ec3 thanh to\u00e1n gi\u00e1 \u0111\u1ea5u n\u00e0y.");
            }

            String bidId = "BID" + System.currentTimeMillis() + "-" + System.nanoTime();
            Bid bid = new Bid(bidId, auction.getAuctionId(), bidder.getUserId(),
                    bidder.getUsername(), amount);

            try {
                if (amount > auctionToPersist.getCurrentPrice()) {
                    auctionToPersist.placeBid(bidder.getUserId(), bidder.getUsername(), amount);
                }
                auctionToPersist.addBidId(bidId);
                auctionToPersist.extendAuctionIfNeeded();
            } catch (Exception e) {
                throw new InvalidBidException(e.getMessage());
            }

            if (!bidder.deductFunds(chargeAmount)) {
                throw new InsufficientFundsException("S\u1ed1 d\u01b0 v\u00ed kh\u00f4ng \u0111\u1ee7 \u0111\u1ec3 thanh to\u00e1n gi\u00e1 \u0111\u1ea5u n\u00e0y.");
            }
            bidRepository.saveBid(bid);
            auctionRepository.saveAuction(auctionToPersist);
            bidder.addBidId(bidId);
            userRepository.saveUser(bidder);
            transactionRepository.saveTransaction(new Transaction(
                    bidder.getUserId(),
                    "PAYMENT",
                    chargeAmount,
                    bidder.getWallet(),
                    "Thanh to\u00e1n \u0111\u1ea5u gi\u00e1 s\u1ea3n ph\u1ea9m " + productName(auctionToPersist)
            ));

            LoggerUtil.info("Bid placed: " + amount + " by " + bidder.getUsername() +
                    " on auction " + auction.getAuctionId());

                return bid;
            }
        });
    }

    public List<Bid> getHistory(String auctionId) throws Exception {
        String resolvedAuctionId = resolveAuctionId(auctionId);
        List<Bid> bids = new ArrayList<>(bidRepository.getBidsByAuctionId(resolvedAuctionId));

        Auction auction = auctionRepository.getAuctionById(resolvedAuctionId);
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
        List<Bid> bids = new ArrayList<>(bidRepository.getBidsByBidderId(bidderId));

        for (Bid linkedBid : bidRepository.getBidsLinkedToUser(bidderId)) {
            if (linkedBid != null && bids.stream().noneMatch(existing -> isSameBid(existing, linkedBid))) {
                bids.add(linkedBid);
            }
        }

        bids.sort(Comparator.comparingLong(Bid::getBidTime).reversed());
        return bids;
    }

    public BidPlacementResult placeForCurrentUser(User currentUser, String auctionId, double amount)
            throws Exception {
        if (!(currentUser instanceof RegularUser bidder)) {
            throw new PermissionDeniedException("Bạn phải đăng nhập bằng tài khoản bidder!");
        }

        Auction auction = auctionRepository.getAuctionById(auctionId);
        if (auction == null) {
            throw new PermissionDeniedException("Không tìm thấy phiên đấu giá");
        }

        User latestUser = userRepository.getUserById(currentUser.getUserId());
        if (latestUser instanceof RegularUser latestRegularUser) {
            bidder = latestRegularUser;
        }
        if (!hasCompleteBidProfile(bidder)) {
            throw new PermissionDeniedException("Vui lòng cập nhật số điện thoại và địa chỉ trong hồ sơ trước khi đấu giá.");
        }

        String previousHighestBidderId = auction.getHighestBidderId();
        Bid bid = place(bidder, auction, amount);
        return new BidPlacementResult(bid, auction, bidder, previousHighestBidderId);
    }

    private boolean hasCompleteBidProfile(RegularUser user) {
        return user != null
                && user.getShopPhone() != null && !user.getShopPhone().isBlank()
                && user.getShopAddress() != null && !user.getShopAddress().isBlank();
    }

    public BidPlacementResult placeForCurrentUser(User currentUser, Object data)
            throws Exception {
        PlaceBidRequest request = PlaceBidRequest.from(data);
        return placeForCurrentUser(currentUser, request.auctionId(), request.amount());
    }

    public List<Bid> getHistory(Object data) throws Exception {
        return getHistory(AuctionIdRequest.from(data).auctionId());
    }

    public List<Bid> getUserBidList(User currentUser, Object data) throws Exception {
        String userId = RequestPayloadUtil.text(data);
        if (userId == null) {
            if (currentUser == null) {
                throw new IllegalStateException("Bạn phải đăng nhập");
            }
            userId = currentUser.getUserId();
        }
        return getUserBidList(userId);
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

    private double getHighestUserBidAmount(String bidderId, Auction auction) throws Exception {
        if (bidderId == null || auction == null || auction.getAuctionId() == null) {
            return 0.0;
        }

        double highestUserBidAmount = 0.0;
        for (Bid existingBid : bidRepository.getBidsByAuctionId(auction.getAuctionId())) {
            if (existingBid != null && bidderId.equals(existingBid.getBidderId())) {
                highestUserBidAmount = Math.max(highestUserBidAmount, existingBid.getAmount());
            }
        }
        return highestUserBidAmount;
    }

    private String resolveAuctionId(String requestedAuctionId) throws Exception {
        String value = RequestPayloadUtil.requiredAuctionId(requestedAuctionId);
        Auction auction = auctionRepository.getAuctionById(value);
        if (auction != null) {
            return auction.getAuctionId();
        }

        for (Auction candidate : auctionRepository.getAllAuctions()) {
            if (candidate != null && value.equals(candidate.getItemId())) {
                return candidate.getAuctionId();
            }
        }
        return value;
    }

    private String productName(Auction auction) {
        if (auction != null && auction.getItem() != null
                && auction.getItem().getName() != null
                && !auction.getItem().getName().isBlank()) {
            return auction.getItem().getName();
        }
        return auction != null && auction.getAuctionId() != null ? auction.getAuctionId() : "";
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

    public static BidPlacementResult placeBidForCurrentUser(User currentUser, String auctionId, double amount)
            throws InvalidBidException, PermissionDeniedException, AuctionClosedException,
            InsufficientFundsException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.placeForCurrentUser(currentUser, auctionId, amount);
        } catch (InvalidBidException | PermissionDeniedException | AuctionClosedException |
                 InsufficientFundsException | IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static BidPlacementResult placeBidForCurrentUser(User currentUser, Object data)
            throws InvalidBidException, PermissionDeniedException, AuctionClosedException,
            InsufficientFundsException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.placeForCurrentUser(currentUser, data);
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

    public static List<Bid> getBidHistoryByRequest(Object data)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getHistory(data);
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

    public static List<Bid> getUserBids(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getUserBidList(currentUser, data);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
