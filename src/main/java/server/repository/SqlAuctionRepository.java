package server.repository;

import common.AuctionStatus;
import server.model.Auction;
import server.model.Item;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlAuctionRepository implements AuctionRepository {

    private final SqlItemRepository itemRepository = new SqlItemRepository();

    @Override
    public List<Auction> getAllAuctions() throws Exception {
        String sql = "SELECT * FROM auctions";
        List<Auction> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapAuction(rs));
            }
        }
        return list;
    }

    @Override
    public List<Auction> getActiveAuctions() throws Exception {
        String sql = "SELECT * FROM auctions WHERE status IN (?, ?, ?)";
        List<Auction> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, AuctionStatus.WAITING.name());
            ps.setString(2, AuctionStatus.OPEN.name());
            ps.setString(3, AuctionStatus.RUNNING.name());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Auction auction = mapAuction(rs);
                auction.syncStatusWithTime();
                list.add(auction);
            }
        }
        return list;
    }

    @Override
    public Auction getAuctionById(String auctionId) throws Exception {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAuction(rs);
        }
        return null;
    }

    @Override
    public List<Auction> getAuctionsBySellerId(String sellerId) throws Exception {
        String sql = "SELECT * FROM auctions WHERE seller_id = ?";
        List<Auction> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapAuction(rs));
            }
        }
        return list;
    }

    @Override
    public void createAuction(Auction auction) throws Exception {
        insertOrUpdate(auction, true);
    }

    @Override
    public void saveAuction(Auction auction) throws Exception {
        insertOrUpdate(auction, false);
    }

    @Override
    public void deleteAuction(String auctionId) throws Exception {
        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM auctions WHERE auction_id = ?")) {
                ps.setString(1, auctionId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM auction_bids WHERE auction_id = ?")) {
                ps.setString(1, auctionId);
                ps.executeUpdate();
            }
        }
    }

    private void insertOrUpdate(Auction auction, boolean insert) throws Exception {
        String sql = insert
                ? """
                  INSERT OR REPLACE INTO auctions
                  (auction_id, item_id, seller_id, seller_name, current_price,
                   highest_bidder_id, highest_bidder_name, status, start_time, end_time,
                   created_at, reserve_price, minimum_bid_increment, view_count)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """
                : """
                  UPDATE auctions SET
                    item_id=?, seller_id=?, seller_name=?, current_price=?,
                    highest_bidder_id=?, highest_bidder_name=?, status=?, start_time=?, end_time=?,
                    reserve_price=?, minimum_bid_increment=?, view_count=?
                  WHERE auction_id=?
                  """;

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (insert) {
                ps.setString(1, auction.getAuctionId());
                ps.setString(2, auction.getItemId());
                ps.setString(3, auction.getSellerId());
                ps.setString(4, auction.getSellerName());
                ps.setDouble(5, auction.getCurrentPrice());
                ps.setString(6, auction.getHighestBidderId());
                ps.setString(7, auction.getHighestBidderName());
                ps.setString(8, auction.getStatus().name());
                ps.setLong(9, auction.getStartTime());
                ps.setLong(10, auction.getEndTime());
                ps.setLong(11, auction.getCreatedAt());
                ps.setDouble(12, auction.getReservePrice());
                ps.setDouble(13, auction.getMinimumBidIncrement());
                ps.setInt(14, auction.getViewCount());
            } else {
                ps.setString(1, auction.getItemId());
                ps.setString(2, auction.getSellerId());
                ps.setString(3, auction.getSellerName());
                ps.setDouble(4, auction.getCurrentPrice());
                ps.setString(5, auction.getHighestBidderId());
                ps.setString(6, auction.getHighestBidderName());
                ps.setString(7, auction.getStatus().name());
                ps.setLong(8, auction.getStartTime());
                ps.setLong(9, auction.getEndTime());
                ps.setDouble(10, auction.getReservePrice());
                ps.setDouble(11, auction.getMinimumBidIncrement());
                ps.setInt(12, auction.getViewCount());
                ps.setString(13, auction.getAuctionId());
            }

            ps.executeUpdate();
        }

        syncAuctionBidIds(auction.getAuctionId(), auction.getBidIds());
    }

    private Auction mapAuction(ResultSet rs) throws Exception {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getString("auction_id"));
        auction.setItemId(rs.getString("item_id"));
        auction.setSellerId(rs.getString("seller_id"));
        auction.setSellerName(rs.getString("seller_name"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setHighestBidderId(rs.getString("highest_bidder_id"));
        auction.setHighestBidderName(rs.getString("highest_bidder_name"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            auction.setHighestBidderId(null);
            auction.setHighestBidderName(null);
        }
        auction.setStartTime(rs.getLong("start_time"));
        auction.setEndTime(rs.getLong("end_time"));
        auction.setReservePrice(rs.getDouble("reserve_price"));
        auction.setMinimumBidIncrement(rs.getDouble("minimum_bid_increment"));
        auction.setViewCount(rs.getInt("view_count"));

        // NOTE: createdAt khong co setter trong Auction.
        // auction.setCreatedAt(rs.getLong("created_at"));

        Item item = itemRepository.getItemById(auction.getItemId());
        auction.setItem(item);

        auction.setBidIds(loadBidIds(auction.getAuctionId()));
        return auction;
    }

    private List<String> loadBidIds(String auctionId) throws Exception {
        List<String> list = new ArrayList<>();
        String sql = "SELECT bid_id FROM auction_bids WHERE auction_id = ?";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("bid_id"));
        }
        return list;
    }

    private void syncAuctionBidIds(String auctionId, List<String> bidIds) throws Exception {
        if (auctionId == null) return;

        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM auction_bids WHERE auction_id = ?")) {
                del.setString(1, auctionId);
                del.executeUpdate();
            }

            if (bidIds != null) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO auction_bids (auction_id, bid_id) VALUES (?, ?)")) {
                    for (String bidId : bidIds) {
                        ins.setString(1, auctionId);
                        ins.setString(2, bidId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        }
    }
}
