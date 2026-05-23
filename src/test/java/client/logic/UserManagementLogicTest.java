package client.logic;

import common.UserRole;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementLogicTest {
    @Test
    void matchesSearchesCoreUserFields() {
        RegularUser user = new RegularUser("u1", "alice", "password", "alice@example.com");

        assertTrue(UserManagementLogic.matches(user, "u1"));
        assertTrue(UserManagementLogic.matches(user, "alice"));
        assertTrue(UserManagementLogic.matches(user, "example"));
        assertFalse(UserManagementLogic.matches(user, "missing"));
        assertFalse(UserManagementLogic.matches(null, "alice"));
    }

    @Test
    void formatsDisplayFields() {
        RegularUser user = new RegularUser("u1", "alice", "password", "");

        assertEquals("alice", UserManagementLogic.displayUsername(user));
        user.setEmail("alice@example.com");
        assertEquals("alice (alice@example.com)", UserManagementLogic.displayUsername(user));
        assertEquals("-", UserManagementLogic.roleText(null));
        assertFalse(UserManagementLogic.roleText(UserRole.ADMIN).isBlank());
        assertFalse(UserManagementLogic.statusText(true).isBlank());
        assertFalse(UserManagementLogic.statusText(false).isBlank());
        assertEquals("", UserManagementLogic.value(null));
    }
}
