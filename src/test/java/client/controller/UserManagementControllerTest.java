package client.controller;

import common.UserRole;
import org.junit.jupiter.api.Test;
import server.model.Admin;
import server.model.RegularUser;
import server.model.User;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementControllerTest {
    @Test
    void userRowFormatsUserFieldsRoleAndStatus() {
        RegularUser user = new RegularUser("u1", "alice", "password", "alice@example.com");
        UserManagementController.UserRow row = new UserManagementController.UserRow(user);

        assertSame(user, row.getUser());
        assertEquals("u1", row.idProperty().get());
        assertEquals("alice (alice@example.com)", row.usernameProperty().get());
        assertTrue(row.roleProperty().get().toLowerCase().contains("gi"));
        assertFalse(row.statusProperty().get().isBlank());

        user.setActive(false);
        assertFalse(row.statusProperty().get().isBlank());
    }

    @Test
    void userRowUsesUsernameWhenEmailMissingAndSupportsAdminRole() {
        Admin admin = new Admin("admin-1", "admin", "password", "");
        UserManagementController.UserRow row = new UserManagementController.UserRow(admin);

        assertEquals("admin", row.usernameProperty().get());
        assertFalse(row.roleProperty().get().isBlank());
    }

    @Test
    void privateMatchesSearchesAllUserFields() throws Exception {
        UserManagementController controller = new UserManagementController();
        Method matches = UserManagementController.class.getDeclaredMethod("matches", User.class, String.class);
        matches.setAccessible(true);

        RegularUser user = new RegularUser("u42", "searchable", "password", "person@example.com");

        assertTrue((boolean) matches.invoke(controller, user, "u42"));
        assertTrue((boolean) matches.invoke(controller, user, "search"));
        assertTrue((boolean) matches.invoke(controller, user, "person@"));
        assertTrue((boolean) matches.invoke(controller, user, "gi"));
        assertFalse((boolean) matches.invoke(controller, user, "missing"));
    }

    @Test
    void roleAndStatusHelpersHandleNullAndInactive() throws Exception {
        Method roleText = UserManagementController.class.getDeclaredMethod("roleText", UserRole.class);
        Method statusText = UserManagementController.class.getDeclaredMethod("statusText", boolean.class);
        roleText.setAccessible(true);
        statusText.setAccessible(true);

        assertEquals("-", roleText.invoke(null, (Object) null));
        assertFalse(((String) roleText.invoke(null, UserRole.ADMIN)).isBlank());
        assertFalse(((String) statusText.invoke(null, true)).isBlank());
        assertFalse(((String) statusText.invoke(null, false)).isBlank());
    }
}
