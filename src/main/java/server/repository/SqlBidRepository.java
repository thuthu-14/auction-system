package server.repository;

import server.model.Bid;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlBidRepository implements BidRepository {

    @Override
    public List<Bid> getAllBids() throws Exception {
        String sql = "SELECT * FROM bids";
        List<Bid> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapBid(rs));
        }
        return list;
    }

    @Override
    public Bid getBidById(String bidId) throws Exception {
        String sql = "SELECT * FROM bids WHERE bid_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bidId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapBid(rs);
        }
        return null;
    }

    @Override
    public List<Bid> getBidsByAuctionId(String auctionId) throws Exception {
        String sql = "SELECT * FROM bids WHERE auction_id = ?";
        List<Bid> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBid(rs));
        }
        return list;
    }

    @Override
    public List<Bid> getBidsByBidderId(String bidderId) throws Exception {
        String sql = "SELECT * FROM bids WHERE bidder_id = ?";
        List<Bid> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBid(rs));
        }
        return list;
    }

    @Override
    public List<Bid> getBidsLinkedToUser(String userId) throws Exception {
        String sql = """
            SELECT b.* FROM bids b
            INNER JOIN user_bids ub ON ub.bid_id = b.bid_id
            WHERE ub.user_id = ?
        """;
        List<Bid> list = new ArrayList<>();

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBid(rs));
        }
        return list;
    }

    @Override
    public void saveBid(Bid bid) throws Exception {
        String sql = """
            INSERT OR REPLACE INTO bids
            (bid_id, auction_id, bidder_id, bidder_name, amount, bid_time, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bid.getBidId());
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidderId());
            ps.setString(4, bid.getBidderName());
            ps.setDouble(5, bid.getAmount());
            ps.setLong(6, bid.getBidTime());
            ps.setString(7, bid.getStatus());
            ps.executeUpdate();
        }

        // update mapping tables
        linkBidToAuction(bid.getAuctionId(), bid.getBidId());
        linkBidToUser(bid.getBidderId(), bid.getBidId());
    }

    private Bid mapBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setBidId(rs.getString("bid_id"));
        bid.setAuctionId(rs.getString("auction_id"));
        bid.setBidderId(rs.getString("bidder_id"));
        bid.setBidderName(rs.getString("bidder_name"));
        bid.setAmount(rs.getDouble("amount"));
        bid.setBidTime(rs.getLong("bid_time"));
        bid.setStatus(rs.getString("status"));
        return bid;
    }

    private void linkBidToAuction(String auctionId, String bidId) throws Exception {
        if (auctionId == null || bidId == null) return;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO auction_bids (auction_id, bid_id) VALUES (?, ?)")) {
            ps.setString(1, auctionId);
            ps.setString(2, bidId);
            ps.executeUpdate();
        }
    }

    private void linkBidToUser(String userId, String bidId) throws Exception {
        if (userId == null || bidId == null) return;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO user_bids (user_id, bid_id) VALUES (?, ?)")) {
            ps.setString(1, userId);
            ps.setString(2, bidId);
            ps.executeUpdate();
        }
    }
}
