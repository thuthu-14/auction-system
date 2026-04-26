package server.service;

import server.model.*;
import server.storage.*;
import server.exception.PermissionDeniedException;
import common.AuctionStatus;
import util.LoggerUtil;
import util.ValidationUtil;
import java.io.IOException;
import java.util.*;

public class AuctionService {
    public static Auction createAuction(RegularUser seller, Item item, int durationMinutes)
            throws PermissionDeniedException, IOException, ClassNotFoundException {

        if (seller == null) {
            throw new PermissionDeniedException("❌ Seller không hợp lệ!");
        }

        if (item == null) {
            throw new PermissionDeniedException("❌ Item không hợp lệ!");
        }

        if (!item.getCategory().equals(common.ItemCategory.ELECTRONICS) &&
                !item.getCategory().equals(common.ItemCategory.ART) &&
                !item.getCategory().equals(common.ItemCategory.VEHICLE) &&
                !item.getCategory().equals(common.ItemCategory.FASHION) &&
                !item.getCategory().equals(common.ItemCategory.JEWELRY)) {
            throw new PermissionDeniedException("❌ Loại sản phẩm không hợp lệ!");
        }

        // ← THÊM: Kiểm tra starting price
        String priceError = util.ItemValidationUtil.getStartingPriceErrorMessage(
                item.getCategory(), item.getStartingPrice());
        if (priceError != null) {
            throw new PermissionDeniedException("❌ " + priceError);
        }

        // ← THÊM: Kiểm tra duration
        String durationError = util.ItemValidationUtil.getDurationErrorMessage(
                item.getCategory(), durationMinutes);
        if (durationError != null) {
            throw new PermissionDeniedException("❌ " + durationError);
        }

        // Tạo auction
        String auctionId = "AU" + System.currentTimeMillis();
        String itemId = "IT" + System.currentTimeMillis();
        item.setItemId(itemId);

        Auction auction = new Auction(auctionId, itemId, seller.getUserId(),
                seller.getUsername(), item,
                item.getStartingPrice(), durationMinutes);

        AuctionDAO.createAuction(auction);
        ItemDAO.saveItem(item);
        seller.addOwnedAuction(auctionId);
        UserDAO.saveUser(seller);

        LoggerUtil.info("✓ Auction created: " + auctionId + " by " + seller.getUsername());
        return auction;
    }

    public static List<Auction> getActiveAuctions()
            throws IOException, ClassNotFoundException {
        return AuctionDAO.getActiveAuctions();
    }


    public static Auction getAuctionById(String auctionId)
            throws IOException, ClassNotFoundException {
        return AuctionDAO.getAuctionById(auctionId);
    }

    public static List<Auction> getAuctionsBySeller(String sellerId)
            throws IOException, ClassNotFoundException {
        return AuctionDAO.getAuctionsBySellerId(sellerId);
    }

    public static void finishAuction(String auctionId)
            throws IOException, ClassNotFoundException {
        Auction auction = AuctionDAO.getAuctionById(auctionId);
        if (auction != null && auction.getStatus() != AuctionStatus.FINISHED) {
            auction.setStatus(AuctionStatus.FINISHED);
            AuctionDAO.saveAuction(auction);
            LoggerUtil.info("Auction finished: " + auctionId);
        }
    }

    public static void deleteAuction(String auctionId)
            throws IOException, ClassNotFoundException {
        AuctionDAO.deleteAuction(auctionId);
        LoggerUtil.info("Auction deleted: " + auctionId);
    }
}