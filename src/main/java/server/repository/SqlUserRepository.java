package server.repository;

import server.model.RegularUser;
import server.model.User;
import server.storage.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqlUserRepository implements UserRepository {

    @Override
    public User getUserById(String userId) throws Exception {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UserMapper.fromResultSet(rs);
        }
        return null;
    }

    @Override
    public User getUserByUsername(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UserMapper.fromResultSet(rs);
        }
        return null;
    }

    @Override
    public User getUserByEmail(String email) throws Exception {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UserMapper.fromResultSet(rs);
        }
        return null;
    }

    @Override
    public List<User> getAllUsers() throws Exception {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(UserMapper.fromResultSet(rs));
            }
        }
        return users;
    }

    @Override
    public void saveUser(User user) throws Exception {
        if (user instanceof RegularUser r) {
            syncOwnedAuctions(user.getUserId(), r.getOwnedAuctionIds());
            syncUserBids(user.getUserId(), r.getBidIds());
        }


        String sql = """
            UPDATE users
            SET username=?, password=?, email=?, role=?, wallet=?, active=?,
                is_seller=?, shop_name=?, shop_phone=?, shop_address=?, shop_email=?
            WHERE user_id=?
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            UserMapper.bindUpdate(ps, user);
            ps.executeUpdate();
        }
    }

    @Override
    public void registerUser(User user) throws Exception {
        String sql = """
            INSERT INTO users
            (user_id, username, password, email, role, wallet, active, created_at,
             is_seller, shop_name, shop_phone, shop_address, shop_email)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            UserMapper.bindInsert(ps, user);
            ps.executeUpdate();
        }
    }

    private void syncOwnedAuctions(String userId, List<String> auctionIds) throws Exception {
        if (userId == null) return;
        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM user_owned_auctions WHERE user_id = ?")) {
                del.setString(1, userId);
                del.executeUpdate();
            }
            if (auctionIds != null) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO user_owned_auctions (user_id, auction_id) VALUES (?, ?)")) {
                    for (String a : auctionIds) {
                        ins.setString(1, userId);
                        ins.setString(2, a);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        }
    }

    private void syncUserBids(String userId, List<String> bidIds) throws Exception {
        if (userId == null) return;
        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM user_bids WHERE user_id = ?")) {
                del.setString(1, userId);
                del.executeUpdate();
            }
            if (bidIds != null) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO user_bids (user_id, bid_id) VALUES (?, ?)")) {
                    for (String b : bidIds) {
                        ins.setString(1, userId);
                        ins.setString(2, b);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        }
    }

    public static List<String> loadOwnedAuctions(String userId) throws Exception {
        List<String> list = new ArrayList<>();
        String sql = "SELECT auction_id FROM user_owned_auctions WHERE user_id = ?";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("auction_id"));
            }
        }
        return list;
    }

    public static List<String> loadUserBids(String userId) throws Exception {
        List<String> list = new ArrayList<>();
        String sql = "SELECT bid_id FROM user_bids WHERE user_id = ?";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("bid_id"));
            }
        }
        return list;
    }
}