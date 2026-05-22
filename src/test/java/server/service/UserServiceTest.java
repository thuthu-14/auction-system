package server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.Admin;
import server.model.RegularUser;
import server.repository.UserRepository;
import server.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Mock
    UserRepository userRepository;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void registerUser_duplicateUsername_throws() throws Exception {
        UserService svc = new UserService(userRepository);
        when(userRepository.getUserById("nx")).thenReturn(null);
        Exception ex = assertThrows(Exception.class, () -> svc.addFundsToUser("nx", 100.0));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void basicGettersDelegateToRepository() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        when(userRepository.getUserById("u1")).thenReturn(user);
        when(userRepository.getUserByUsername("user")).thenReturn(user);
        when(userRepository.getAllUsers()).thenReturn(List.of(user));

        assertSame(user, service.getById("u1"));
        assertSame(user, service.getByUsername("user"));
        assertEquals(List.of(user), service.getAll());
    }

    @Test
    public void getAllForAdmin_allowsOnlyAdmin() throws Exception {
        UserService service = new UserService(userRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser regular = new RegularUser("u1", "user", "password", "user@example.com");
        when(userRepository.getAllUsers()).thenReturn(List.of(admin, regular));

        assertEquals(List.of(admin, regular), service.getAllForAdmin(admin));
        assertThrows(IOException.class, () -> service.getAllForAdmin(regular));
    }

    @Test
    public void setUserStatusForAdmin_updatesRequestedUser() throws Exception {
        UserService service = new UserService(userRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser target = new RegularUser("u1", "user", "password", "user@example.com");
        when(userRepository.getUserById("u1")).thenReturn(target);

        service.setUserStatusForAdmin(admin, Map.of("userId", "u1", "active", false));

        assertFalse(target.isActive());
        verify(userRepository).saveUser(target);
    }

    @Test
    public void setUserStatusForAdmin_rejectsMissingPayloadUserId() {
        UserService service = new UserService(userRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");

        assertThrows(IOException.class, () -> service.setUserStatusForAdmin(admin, Map.of("active", true)));
    }

    @Test
    public void banUserAsAdmin_updatesTargetUserStatus() throws Exception {
        UserService service = new UserService(userRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser target = new RegularUser("u2", "target", "password", "target@example.com");
        when(userRepository.getUserById("u2")).thenReturn(target);

        service.banUserAsAdmin(admin, "u2");

        assertFalse(target.isActive());
        verify(userRepository).saveUser(target);
    }

    @Test
    public void banUserAsAdmin_rejectsNonAdmin() {
        UserService service = new UserService(userRepository);
        RegularUser regular = new RegularUser("u1", "user", "password", "user@example.com");

        assertThrows(IOException.class, () -> service.banUserAsAdmin(regular, "u2"));
    }

    @Test
    public void addFundsToUser_rejectsMissingUserAndNonPositiveAmount() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        when(userRepository.getUserById("missing")).thenReturn(null);
        when(userRepository.getUserById("u1")).thenReturn(user);

        assertThrows(IOException.class, () -> service.addFundsToUser("missing", 100.0));
        assertThrows(IOException.class, () -> service.addFundsToUser("u1", 0.0));
        verify(userRepository, never()).saveUser(user);
    }

    @Test
    public void addFundsToUser_successIncrementsWalletAndSaves() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        user.setWallet(25.0);
        when(userRepository.getUserById("u1")).thenReturn(user);

        service.addFundsToUser("u1", 75.0);

        assertEquals(100.0, user.getWallet(), 0.001);
        verify(userRepository).saveUser(user);
    }

    @Test
    public void setUserStatus_directlyUpdatesExistingUserAndRejectsMissingUser() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        when(userRepository.getUserById("u1")).thenReturn(user);
        when(userRepository.getUserById("missing")).thenReturn(null);

        service.setUserStatus("u1", false);

        assertFalse(user.isActive());
        verify(userRepository).saveUser(user);
        assertThrows(IOException.class, () -> service.setUserStatus("missing", true));
    }

    @Test
    public void upgradeUserToSeller_validatesUserTypeAndSellerState() throws Exception {
        UserService service = new UserService(userRepository);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser user = new RegularUser("u1", "user", "password", "user@example.com");
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        when(userRepository.getUserById("missing")).thenReturn(null);
        when(userRepository.getUserById("admin-1")).thenReturn(admin);
        when(userRepository.getUserById("u1")).thenReturn(user);
        when(userRepository.getUserById("seller-1")).thenReturn(seller);

        assertThrows(IOException.class,
                () -> service.upgradeUserToSeller("missing", "Shop", "123", "Addr", "shop@example.com"));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSeller("admin-1", "Shop", "123", "Addr", "shop@example.com"));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSeller("seller-1", "Shop", "123", "Addr", "shop@example.com"));

        service.upgradeUserToSeller("u1", "Shop", "123", "Addr", "shop@example.com");

        assertTrue(user.isSeller());
        assertEquals("Shop", user.getShopName());
        verify(userRepository).saveUser(user);
    }

    @Test
    public void upgradeUserToSellerWithPassword_validatesPasswordAndSellerState() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "secret", "user@example.com");
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser seller = new RegularUser("seller-1", "seller", "secret", "seller@example.com");
        seller.setSeller(true);
        when(userRepository.getUserById("u1")).thenReturn(user);
        when(userRepository.getUserById("missing")).thenReturn(null);
        when(userRepository.getUserById("admin-1")).thenReturn(admin);
        when(userRepository.getUserById("seller-1")).thenReturn(seller);

        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("missing", "secret", "Shop", "123", "Addr", null));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("admin-1", "password", "Shop", "123", "Addr", null));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("seller-1", "secret", "Shop", "123", "Addr", null));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("u1", " ", "Shop", "123", "Addr", null));
        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("u1", "wrong", "Shop", "123", "Addr", null));

        RegularUser upgraded = service.upgradeUserToSellerWithPassword("u1", "secret", "Shop", "123", "Addr", null);

        assertSame(user, upgraded);
        assertTrue(upgraded.isSeller());
        assertEquals("Shop", upgraded.getShopName());
        assertEquals("user@example.com", upgraded.getShopEmail());
        verify(userRepository).saveUser(user);
        assertThrows(IOException.class,
                () -> service.upgradeUserToSellerWithPassword("u1", "secret", "Shop", "123", "Addr", null));
    }

    @Test
    public void upgradeUserToSellerWithPassword_usesCurrentUserPayloadAndRejectsAnonymous() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser user = new RegularUser("u1", "user", "secret", "user@example.com");
        when(userRepository.getUserById("u1")).thenReturn(user);

        RegularUser upgraded = service.upgradeUserToSellerWithPassword(user, Map.of(
                "password", "secret",
                "shopName", "Shop",
                "phone", "123",
                "address", "Addr",
                "email", "shop@example.com"
        ));

        assertSame(user, upgraded);
        assertTrue(upgraded.isSeller());
        assertThrows(IOException.class, () -> service.upgradeUserToSellerWithPassword(null, Map.of()));
    }

    @Test
    public void getSellerContact_usesShopFieldsWithFallbacks() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setShopName("Shop Name");
        seller.setShopPhone("");
        seller.setShopAddress("Shop Address");
        seller.setShopEmail(null);
        when(userRepository.getUserById("seller-1")).thenReturn(seller);

        Map<String, String> contact = service.getSellerContact(" seller-1 ");

        assertEquals("Shop Name", contact.get("name"));
        assertEquals("seller@example.com", contact.get("email"));
        assertEquals("Chưa cập nhật", contact.get("phone"));
        assertEquals("Shop Address", contact.get("address"));
    }

    @Test
    public void getSellerContact_rejectsBlankOrMissingSellerAndUsesUserDefaults() throws Exception {
        UserService service = new UserService(userRepository);
        Admin adminSeller = new Admin("admin-1", "admin", "password", "admin@example.com");
        when(userRepository.getUserById("missing")).thenReturn(null);
        when(userRepository.getUserById("admin-1")).thenReturn(adminSeller);

        assertThrows(IOException.class, () -> service.getSellerContact(" "));
        assertThrows(IOException.class, () -> service.getSellerContact("missing"));

        Map<String, String> contact = service.getSellerContact("admin-1");

        assertEquals("admin", contact.get("name"));
        assertEquals("admin@example.com", contact.get("email"));
        assertEquals("Chưa cập nhật", contact.get("phone"));
        assertEquals("Chưa cập nhật", contact.get("address"));
    }

    @Test
    public void isSeller_returnsTrueOnlyForRegularSeller() throws Exception {
        UserService service = new UserService(userRepository);
        RegularUser seller = new RegularUser("seller-1", "seller", "password", "seller@example.com");
        seller.setSeller(true);
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        when(userRepository.getUserById("seller-1")).thenReturn(seller);
        when(userRepository.getUserById("admin-1")).thenReturn(admin);
        when(userRepository.getUserById("missing")).thenReturn(null);

        assertTrue(service.isSeller("seller-1"));
        assertFalse(service.isSeller("admin-1"));
        assertFalse(service.isSeller("missing"));
    }
}


