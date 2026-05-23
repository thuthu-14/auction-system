package server.repository;

import common.UserRole;
import server.model.Admin;
import server.model.RegularUser;
import server.model.User;
import server.exception.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper {

    public static User fromResultSet(ResultSet rs) throws SQLException, DataAccessException {
        String roleText = rs.getString("role");
        UserRole role = roleText != null ? UserRole.valueOf(roleText) : UserRole.BIDDER;

        User user;
        if (role == UserRole.ADMIN) {
            user = new Admin();
        } else {
            user = new RegularUser();
        }

        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRole(role);
        user.setWallet(rs.getDouble("wallet"));
        user.setActive(rs.getInt("active") == 1);

        if (user instanceof RegularUser regular) {
            regular.setSeller(rs.getInt("is_seller") == 1);
            regular.setShopName(rs.getString("shop_name"));
            regular.setShopPhone(rs.getString("shop_phone"));
            regular.setShopAddress(rs.getString("shop_address"));
            regular.setShopEmail(rs.getString("shop_email"));

            try {
                regular.setOwnedAuctionIds(SqlUserRepository.loadOwnedAuctions(user.getUserId()));
                regular.setBidIds(SqlUserRepository.loadUserBids(user.getUserId()));
            } catch (Exception e) {
                throw new DataAccessException("Failed to load user auctions/bids", e);
            }
        }

        return user;
    }

    public static void bindInsert(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getUserId());
        ps.setString(2, user.getUsername());
        ps.setString(3, user.getPassword());
        ps.setString(4, user.getEmail());
        ps.setString(5, user.getRole().name());
        ps.setDouble(6, user.getWallet());
        ps.setInt(7, user.isActive() ? 1 : 0);
        ps.setLong(8, user.getCreatedAt());

        if (user instanceof RegularUser regular) {
            ps.setInt(9, regular.isSeller() ? 1 : 0);
            ps.setString(10, regular.getShopName());
            ps.setString(11, regular.getShopPhone());
            ps.setString(12, regular.getShopAddress());
            ps.setString(13, regular.getShopEmail());
        } else {
            ps.setInt(9, 0);
            ps.setString(10, null);
            ps.setString(11, null);
            ps.setString(12, null);
            ps.setString(13, null);
        }
    }

    public static void bindUpdate(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getUsername());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getRole().name());
        ps.setDouble(5, user.getWallet());
        ps.setInt(6, user.isActive() ? 1 : 0);

        if (user instanceof RegularUser regular) {
            ps.setInt(7, regular.isSeller() ? 1 : 0);
            ps.setString(8, regular.getShopName());
            ps.setString(9, regular.getShopPhone());
            ps.setString(10, regular.getShopAddress());
            ps.setString(11, regular.getShopEmail());
        } else {
            ps.setInt(7, 0);
            ps.setString(8, null);
            ps.setString(9, null);
            ps.setString(10, null);
            ps.setString(11, null);
        }

        ps.setString(12, user.getUserId());
    }
}