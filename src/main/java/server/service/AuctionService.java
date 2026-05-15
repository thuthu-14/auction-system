package server.service;

import common.AuctionStatus;
import server.exception.PermissionDeniedException;
import server.model.Auction;
import server.model.Item;
import server.model.RegularUser;
import server.repository.*;
import util.ItemValidationUtil;
import util.LoggerUtil;

import java.io.IOException;
import java.util.List;

public class AuctionService {

    private static final AuctionService DEFAULT = new AuctionService(
            new SqlAuctionRepository(),
            new SqlItemRepository(),
            new SqlUserRepository()
    );

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public AuctionService(AuctionRepository auctionRepository,
                          ItemRepository itemRepository,
                          UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    public Auction create(RegularUser seller, Item item, int durationMinutes) throws Exception {
        return create(seller, item, durationMinutes, 0.0, 0.0);
    }

    public Auction create(RegularUser seller, Item item, int durationMinutes,
                          double reservePrice, double minimumBidIncrement) throws Exception {
        validateCreateAuction(seller, item, durationMinutes);

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
                durationMinutes
        );
        auction.setReservePrice(Math.max(0.0, reservePrice));
        auction.setMinimumBidIncrement(Math.max(0.0, minimumBidIncrement));

        auctionRepository.createAuction(auction);
        itemRepository.saveItem(item);
        seller.addOwnedAuction(auctionId);
        userRepository.saveUser(seller);

        LoggerUtil.info("Auction created: " + auctionId + " by " + seller.getUsername());
        return auction;
    }

    public List<Auction> getActive() throws Exception {
        return auctionRepository.getActiveAuctions();
    }

    public List<Auction> getAll() throws Exception {
        return auctionRepository.getAllAuctions();
    }

    public Auction getById(String auctionId) throws Exception {
        return auctionRepository.getAuctionById(auctionId);
    }

    public List<Auction> getBySeller(String sellerId) throws Exception {
        return auctionRepository.getAuctionsBySellerId(sellerId);
    }

    public void finish(String auctionId) throws Exception {
        Auction auction = auctionRepository.getAuctionById(auctionId);
        if (auction != null && auction.getStatus() != AuctionStatus.FINISHED) {
            auction.setStatus(AuctionStatus.FINISHED);
            auctionRepository.saveAuction(auction);
            LoggerUtil.info("Auction finished: " + auctionId);
        }
    }

    public void delete(String auctionId) throws Exception {
        auctionRepository.deleteAuction(auctionId);
        LoggerUtil.info("Auction deleted: " + auctionId);
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
}
