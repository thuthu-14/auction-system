package client.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SignupControllerTest {
    @Test
    void buildUsernameUsesNameWhenValidAndSanitizesSpecialCharacters() throws Exception {
        SignupController controller = new SignupController();

        assertEquals("NguyenVanA", invoke(controller, "buildUsername", "Nguyen", "Van A", "a@example.com"));
        assertEquals("Alice_Bob", invoke(controller, "buildUsername", "Alice_", "Bob!", "alice@example.com"));
    }

    @Test
    void buildUsernameFallsBackToEmailPrefixAndLimitsLength() throws Exception {
        SignupController controller = new SignupController();

        assertEquals("validuser", invoke(controller, "buildUsername", "!", "@", "valid.user@example.com"));

        String longUsername = (String) invoke(controller,
                "buildUsername",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "fallback@example.com");
        assertEquals(50, longUsername.length());
        assertTrue(longUsername.startsWith("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, String.class, String.class, String.class);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
