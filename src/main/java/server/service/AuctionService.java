package server.service;

import common.AuctionStatus;
import common.Transaction;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.model.RegularUser;
import server.model.User;
import server.concurrency.ConcurrentBidManager;
import server.observer.AuctionManager;
import server.repository.*;
import server.storage.ImageUploadManager;
import util.ItemValidationUtil;
import util.LoggerUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionService {

    private static final AuctionService DEFAULT = new AuctionService(
            new SqlAuctionRepository(),
            new SqlItemRepository(),
            new SqlUserRepository()
    );

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BidRepository bidRepository;
    private final SqlTransactionRepository transactionRepository;

    public AuctionService(AuctionRepository auctionRepository,
                          ItemRepository itemRepository,
                          UserRepository userRepository) {
        this(auctionRepository, itemRepository, userRepository,
                new SqlBidRepository(), new SqlTransactionRepository());
    }

    public AuctionService(AuctionRepository auctionRepository,
                          ItemRepository itemRepository,
                          UserRepository userRepository,
                          BidRepository bidRepository,
                          SqlTransactionRepository transactionRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.bidRepository = bidRepository;
        this.transactionRepository = transactionRepository;
    }

    public Auction create(RegularUser seller, Item item, int durationMinutes) throws Exception {
        return create(seller, item, durationMinutes, 0.0, 0.0);
    }

    public Auction create(RegularUser seller, Item item, int durationMinutes,
                          double reservePrice, double minimumBidIncrement) throws Exception {
        long startTimeMillis = System.currentTimeMillis();
        long endTimeMillis = startTimeMillis + durationMinutes * 60_000L;
        return create(seller, item, startTimeMillis, endTimeMillis, reservePrice, minimumBidIncrement);
    }

    public Auction create(RegularUser seller, Item item, long startTimeMillis, long endTimeMillis,
                          double reservePrice, double minimumBidIncrement) throws Exception {
        int durationMinutes = (int) ((endTimeMillis - startTimeMillis) / 60_000L);
        validateCreateAuction(seller, item, durationMinutes);
        validateTimeWindow(startTimeMillis, endTimeMillis);

        String auctionId = "AU" + System.currentTimeMillis();
        String itemId = "IT" + System.currentTimeMillis();
        item.setItemId(itemId);

        Auction auction = new Auction(
                auctionId,
                itemId,
                seller.getUserId(),
                seller.getUsername(),
                item,
                item.getStartingPrice(),
                startTimeMillis,
                endTimeMillis
        );
        auction.setReservePrice(Math.max(0.0, reservePrice));
        auction.setMinimumBidIncrement(Math.max(0.0, minimumBidIncrement));

        try {
            itemRepository.saveItem(item);
            auctionRepository.createAuction(auction);
            seller.addOwnedAuction(auctionId);
            userRepository.saveUser(seller);
        } catch (Exception e) {
            try {
                auctionRepository.deleteAuction(auctionId);
            } catch (Exception cleanupError) {
                LoggerUtil.warn("Failed to cleanup auction after create error: " + cleanupError.getMessage());
            }
            throw e;
        }

        LoggerUtil.info("Auction created: " + auctionId + " by " + seller.getUsername());
        AuctionManager.getInstance().notifyAuctionCreated(auction);
        return auction;
    }

    public List<Auction> getActive() throws Exception {
        return auctionRepository.getActiveAuctions();
    }

    public List<Auction> getAll() throws Exception {
        return auctionRepository.getAllAuctions();
    }

    public List<Auction> getList(boolean includeAll) throws Exception {
        return includeAll ? getAll() : getActive();
    }

    public Auction getById(String auctionId) throws Exception {
        Auction auction = findAuctionByAuctionOrItemId(RequestPayloadUtil.requiredAuctionId(auctionId));
        if (auction == null) {
            throw new IOException("Khong tim thay phien dau gia");
        }
        return auction;
    }

    public List<Auction> getBySeller(String sellerId) throws Exception {
        return auctionRepository.getAuctionsBySellerId(sellerId);
    }

    public List<Auction> getBySeller(User currentUser) throws Exception {
        RegularUser seller = requireSeller(currentUser);
        return auctionRepository.getAuctionsBySellerId(seller.getUserId());
    }

    public void finish(String auctionId) throws Exception {
        ConcurrentBidManager.getInstance().withAuctionWriteLock(auctionId, () -> {
            Auction auction = auctionRepository.getAuctionById(auctionId);
            if (auction != null && auction.getStatus() != AuctionStatus.FINISHED
                    && auction.getStatus() != AuctionStatus.CANCELLED) {
                refundLosingBids(auction);
                auction.setStatus(AuctionStatus.FINISHED);
                auctionRepository.saveAuction(auction);
                AuctionManager.getInstance().notifyAuctionFinished(auction);
                LoggerUtil.info("Auction finished: " + auctionId);
            }
            return null;
        });
    }

    public Auction cancel(String auctionId, String sellerId) throws Exception {
        return ConcurrentBidManager.getInstance().withAuctionWriteLock(auctionId, () -> {
            Auction auction = findAuctionByAuctionOrItemId(auctionId);
            if (auction == null) {
                throw new PermissionDeniedException("Phi\u00ean \u0111\u1ea5u gi\u00e1 kh\u00f4ng t\u1ed3n t\u1ea1i.");
            }
            if (sellerId == null || !sellerId.equals(auction.getSellerId())) {
                throw new PermissionDeniedException("B\u1ea1n kh\u00f4ng ph\u1ea3i ch\u1ee7 c\u1ee7a phi\u00ean \u0111\u1ea5u gi\u00e1 n\u00e0y.");
            }
            if (auction.getStatus() == AuctionStatus.CANCELLED) {
                return auction;
            }
            if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.CLOSED) {
                throw new PermissionDeniedException("Phi\u00ean \u0111\u1ea5u gi\u00e1 \u0111\u00e3 k\u1ebft th\u00fac, kh\u00f4ng th\u1ec3 h\u1ee7y.");
            }

            List<Bid> bids = bidRepository.getBidsByAuctionId(auction.getAuctionId());
            refundAllBidsForCancelledAuction(auction, bids);

            auction.setStatus(AuctionStatus.CANCELLED);
            auction.setHighestBidderId(null);
            auction.setHighestBidderName(null);
            auctionRepository.saveAuction(auction);
            AuctionManager.getInstance().notifyAuctionCancelled(auction);
            LoggerUtil.info("Auction cancelled: " + auctionId + " by seller " + sellerId);
            return auction;
        });
    }

    public Auction cancel(User currentUser, Object data) throws Exception {
        RegularUser seller = requireSeller(currentUser);
        return cancel(RequestPayloadUtil.requiredAuctionId(data), seller.getUserId());
    }

    private void refundLosingBids(Auction auction) throws Exception {
        List<Bid> bids = bidRepository.getBidsByAuctionId(auction.getAuctionId());
        if (bids.isEmpty()) {
            return;
        }

        Map<String, Double> refundsByUser = calculateRefundsByUser(auction, bids, false);
        for (Map.Entry<String, Double> refund : refundsByUser.entrySet()) {
            User bidder = userRepository.getUserById(refund.getKey());
            if (bidder == null) {
                continue;
            }

            double refundAmount = refund.getValue();
            if (refundAmount <= 0) {
                continue;
            }
            bidder.addFunds(refundAmount);
            userRepository.saveUser(bidder);
            transactionRepository.saveTransaction(new Transaction(
                    bidder.getUserId(),
                    "DEPOSIT",
                    refundAmount,
                    bidder.getWallet(),
                    "Ho\u00e0n ti\u1ec1n \u0111\u1ea5u gi\u00e1 s\u1ea3n ph\u1ea9m " + productName(auction)
            ));
        }
    }

    private void refundAllBidsForCancelledAuction(Auction auction, List<Bid> bids) throws Exception {
        if (bids == null || bids.isEmpty()) {
            return;
        }

        Map<String, Double> refundsByUser = calculateRefundsByUser(auction, bids, true);
        for (Map.Entry<String, Double> refund : refundsByUser.entrySet()) {
            User bidder = userRepository.getUserById(refund.getKey());
            if (bidder == null || refund.getValue() <= 0) {
                continue;
            }

            double refundAmount = refund.getValue();
            bidder.addFunds(refundAmount);
            userRepository.saveUser(bidder);
            transactionRepository.saveTransaction(new Transaction(
                    bidder.getUserId(),
                    "DEPOSIT",
                    refundAmount,
                    bidder.getWallet(),
                    "Ho\u00e0n ti\u1ec1n do phi\u00ean \u0111\u1ea5u gi\u00e1 b\u1ecb h\u1ee7y: " + productName(auction)
            ));
        }
    }

    private Map<String, Double> calculateRefundsByUser(Auction auction, List<Bid> bids, boolean includeWinner) {
        bids.sort((first, second) -> Long.compare(first.getBidTime(), second.getBidTime()));
        Map<String, Double> refundsByUser = new HashMap<>();
        Map<String, Double> previousBidAmountByUser = new HashMap<>();

        for (Bid bid : bids) {
            if (bid == null || bid.getBidderId() == null || bid.getBidderId().isBlank()) {
                continue;
            }
            double previousUserBidAmount = previousBidAmountByUser.getOrDefault(bid.getBidderId(), 0.0);
            double chargedAmount = bid.getAmount() - previousUserBidAmount;
            previousBidAmountByUser.put(bid.getBidderId(), Math.max(previousUserBidAmount, bid.getAmount()));
            if (chargedAmount <= 0) {
                continue;
            }
            if (includeWinner || auction.getHighestBidderId() == null || !auction.getHighestBidderId().equals(bid.getBidderId())) {
                refundsByUser.merge(bid.getBidderId(), chargedAmount, Double::sum);
            }
        }
        return refundsByUser;
    }

    private String productName(Auction auction) {
        if (auction != null && auction.getItem() != null
                && auction.getItem().getName() != null
                && !auction.getItem().getName().isBlank()) {
            return auction.getItem().getName();
        }
        return auction != null && auction.getAuctionId() != null ? auction.getAuctionId() : "";
    }

    public void delete(String auctionId) throws Exception {
        String resolvedAuctionId = RequestPayloadUtil.requiredAuctionId(auctionId);
        auctionRepository.deleteAuction(resolvedAuctionId);
        LoggerUtil.info("Auction deleted: " + resolvedAuctionId);
    }

    public String deleteForAdmin(User currentUser, Object data) throws Exception {
        requireAdmin(currentUser);
        String auctionId = RequestPayloadUtil.requiredAuctionId(data);
        delete(auctionId);
        return auctionId;
    }

    private void validateCreateAuction(RegularUser seller, Item item, int durationMinutes)
            throws PermissionDeniedException {
        if (seller == null) {
            throw new PermissionDeniedException("Người bán không hợp lệ.");
        }
        if (!seller.isActive()) {
            throw new PermissionDeniedException("Tài khoản người bán đang bị khóa.");
        }
        if (!seller.isSeller()) {
            throw new PermissionDeniedException("Chỉ người bán mới có thể tạo phiên đấu giá.");
        }
        if (item == null) {
            throw new PermissionDeniedException("Sản phẩm không hợp lệ.");
        }
        if (item.getCategory() == null) {
            throw new PermissionDeniedException("Loại sản phẩm không hợp lệ.");
        }

        String priceError = ItemValidationUtil.getStartingPriceErrorMessage(
                item.getCategory(), item.getStartingPrice());
        if (priceError != null) {
            throw new PermissionDeniedException(priceError);
        }

        String durationError = ItemValidationUtil.getDurationErrorMessage(
                item.getCategory(), durationMinutes);
        if (durationError != null) {
            throw new PermissionDeniedException(durationError);
        }
    }

    private void validateTimeWindow(long startTimeMillis, long endTimeMillis) throws PermissionDeniedException {
        long now = System.currentTimeMillis();
        if (startTimeMillis < now - 60_000L) {
            throw new PermissionDeniedException("Thời gian bắt đầu phải từ thời điểm hiện tại trở đi.");
        }
        if (endTimeMillis <= startTimeMillis) {
            throw new PermissionDeniedException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    public static Auction createAuction(RegularUser seller, Item item, int durationMinutes)
            throws PermissionDeniedException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.create(seller, item, durationMinutes);
        } catch (PermissionDeniedException | IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Auction createAuction(RegularUser seller, Item item, int durationMinutes,
                                        double reservePrice, double minimumBidIncrement)
            throws PermissionDeniedException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.create(seller, item, durationMinutes, reservePrice, minimumBidIncrement);
        } catch (PermissionDeniedException | IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Auction createAuction(RegularUser seller, Item item, long startTimeMillis, long endTimeMillis,
                                        double reservePrice, double minimumBidIncrement)
            throws PermissionDeniedException, IOException, ClassNotFoundException {
        try {
            return DEFAULT.create(seller, item, startTimeMillis, endTimeMillis, reservePrice, minimumBidIncrement);
        } catch (PermissionDeniedException | IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Auction> getActiveAuctions()
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getActive();
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Auction> getAllAuctions()
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getAll();
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Auction> getAuctions(boolean includeAll)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getList(includeAll);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Auction getAuctionById(String auctionId)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getById(auctionId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Auction getAuctionByRequest(Object data)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getById(RequestPayloadUtil.requiredAuctionId(data));
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Auction> getAuctionsBySeller(String sellerId)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getBySeller(sellerId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static List<Auction> getAuctionsBySeller(User currentUser)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.getBySeller(currentUser);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void finishAuction(String auctionId)
            throws IOException, ClassNotFoundException {
        try {
            DEFAULT.finish(auctionId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void deleteAuction(String auctionId)
            throws IOException, ClassNotFoundException {
        try {
            DEFAULT.delete(auctionId);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static String deleteAuctionForAdmin(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.deleteForAdmin(currentUser, data);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private RegularUser requireSeller(User user) throws PermissionDeniedException {
        if (!(user instanceof RegularUser seller)) {
            throw new PermissionDeniedException("Ban phai dang nhap bang tai khoan nguoi ban");
        }
        if (!seller.isSeller()) {
            throw new PermissionDeniedException("Ban khong phai Seller");
        }
        return seller;
    }

    private Auction findAuctionByAuctionOrItemId(String id) throws Exception {
        String resolvedId = RequestPayloadUtil.requiredAuctionId(id);
        Auction auction = auctionRepository.getAuctionById(resolvedId);
        if (auction != null) {
            return auction;
        }

        for (Auction candidate : auctionRepository.getAllAuctions()) {
            if (candidate != null && resolvedId.equals(candidate.getItemId())) {
                return candidate;
            }
        }
        return null;
    }

    private void requireAdmin(User user) throws PermissionDeniedException {
        if (!(user instanceof server.model.Admin)) {
            throw new PermissionDeniedException("Chi Admin co quyen thuc hien thao tac nay");
        }
    }

    public static Auction cancelAuction(String auctionId, String sellerId)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.cancel(auctionId, sellerId);
        } catch (PermissionDeniedException | IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage() != null ? e.getMessage() : "Khong the huy phien");
        }
    }
    public static Auction cancelAuction(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        try {
            return DEFAULT.cancel(currentUser, data);
        } catch (PermissionDeniedException | IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage() != null ? e.getMessage() : "Khong the huy phien");
        }
    }
    /**
     * Xóa auction và tất cả ảnh của nó
     */
    /**
     * Xóa auction và tất cả ảnh của nó
     */
    public void deleteAuctionWithImages(String auctionId) throws Exception {
        try {
            // [1] Lấy auction
            Auction auction = getById(auctionId);

            // [2] Xóa tất cả ảnh của item (từ DB)
            if (auction != null && auction.getItem() != null) {
                try {
                    ImageUploadManager.deleteItemImages(auction.getItem().getItemId());
                } catch (Exception e) {
                    LoggerUtil.warn("Failed to delete item images: " + auctionId + " | " + e.getMessage());
                }
            }

            // [3] Xóa auction từ DB
            delete(auctionId);
            LoggerUtil.info("Auction deleted with images: " + auctionId);

        } catch (Exception e) {
            LoggerUtil.error("Error deleting auction with images: " + auctionId + " | " + e.getMessage());
            throw e;
        }
    }


}
