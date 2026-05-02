package server.storage;

import server.model.Auction;
import common.AuctionStatus;
import util.JsonUtil;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

public class AuctionDAO {

    /**
     * Lấy tất cả danh sách đấu giá từ file JSON
     */
    public static List<Auction> getAllAuctions() throws IOException, ClassNotFoundException {
        JsonUtil.createFileIfNotExists(DataManager.JSON_AUCTIONS);
        List<Auction> auctions = JsonUtil.loadListFromJson(DataManager.JSON_AUCTIONS, Auction.class);
        return auctions != null ? auctions : new ArrayList<>();
    }

    /**
     * Tìm một phiên đấu giá theo ID
     */
    public static Auction getAuctionById(String auctionId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        for (Auction auction : auctions) {
            if (auction.getAuctionId().equals(auctionId)) {
                return auction;
            }
        }
        return null;
    }

    /**
     * Lấy danh sách đấu giá của một Seller cụ thể
     */
    public static List<Auction> getAuctionsBySellerId(String sellerId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction.getSellerId() != null && auction.getSellerId().equals(sellerId)) {
                result.add(auction);
            }
        }
        return result;
    }

    /**
     * Lấy danh sách các phiên đang mở hoặc đang chạy
     */
    public static List<Auction> getActiveAuctions() throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
                result.add(auction);
            }
        }
        return result;
    }

    /**
     * Tạo mới một phiên đấu giá và lưu vào JSON
     */
    public static void createAuction(Auction auction) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        auctions.add(auction);

        // Chỉ sử dụng JSON để lưu trữ
        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);

        LoggerUtil.info("Auction created and saved to JSON: " + auction.getAuctionId());
    }

    /**
     * Cập nhật thông tin phiên đấu giá hiện có
     */
    public static void saveAuction(Auction auction) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();

        // Xóa bản cũ, thêm bản mới cập nhật
        auctions.removeIf(a -> a.getAuctionId().equals(auction.getAuctionId()));
        auctions.add(auction);

        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);

        LoggerUtil.info("Auction updated in JSON: " + auction.getAuctionId());
    }

    /**
     * Xóa một phiên đấu giá khỏi hệ thống
     */
    public static void deleteAuction(String auctionId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        auctions.removeIf(a -> a.getAuctionId().equals(auctionId));

        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);

        LoggerUtil.info("Auction deleted from JSON: " + auctionId);
    }
}