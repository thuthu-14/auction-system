package server.repository;

import common.UserRole;
import org.junit.jupiter.api.Test;
import server.model.Admin;
import server.model.RegularUser;

import java.sql.PreparedStatement;

import static org.mockito.Mockito.*;

class UserMapperTest {
    @Test
    void bindInsertWritesRegularUserIncludingSellerFields() throws Exception {
        RegularUser user = new RegularUser("U1", "seller", "pw", "seller@example.com");
        user.setWallet(250.0);
        user.setActive(false);
        user.upgradeSeller("Shop", "090", "HN", "shop@example.com");
        PreparedStatement ps = mock(PreparedStatement.class);

        UserMapper.bindInsert(ps, user);

        verify(ps).setString(1, "U1");
        verify(ps).setString(2, "seller");
        verify(ps).setString(3, "pw");
        verify(ps).setString(4, "seller@example.com");
        verify(ps).setString(5, UserRole.SELLER.name());
        verify(ps).setDouble(6, 250.0);
        verify(ps).setInt(7, 0);
        verify(ps).setLong(eq(8), anyLong());
        verify(ps).setInt(9, 1);
        verify(ps).setString(10, "Shop");
        verify(ps).setString(11, "090");
        verify(ps).setString(12, "HN");
        verify(ps).setString(13, "shop@example.com");
    }

    @Test
    void bindUpdateWritesAdminAndClearsSellerFields() throws Exception {
        Admin admin = new Admin("A1", "admin", "pw", "admin@example.com");
        PreparedStatement ps = mock(PreparedStatement.class);

        UserMapper.bindUpdate(ps, admin);

        verify(ps).setString(1, "admin");
        verify(ps).setString(2, "pw");
        verify(ps).setString(3, "admin@example.com");
        verify(ps).setString(4, UserRole.ADMIN.name());
        verify(ps).setDouble(5, 0.0);
        verify(ps).setInt(6, 1);
        verify(ps).setInt(7, 0);
        verify(ps).setString(8, null);
        verify(ps).setString(9, null);
        verify(ps).setString(10, null);
        verify(ps).setString(11, null);
        verify(ps).setString(12, "A1");
    }
}
