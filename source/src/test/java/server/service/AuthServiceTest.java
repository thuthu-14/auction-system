package server.service;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.*;

import server.exception.AuthenticationException;
import server.model.User;
import server.model.RegularUser;

public class AuthServiceTest {

    @Test
    public void testRegisterNewUser() {
        try {
            User user = AuthService.register("testuser", "password123", "test@email.com");

            assertNotNull(user);
            SSsassertEquals("testuser", user.getUsername());
            assertEquals("test@email.com", user.getEmail());
            assertTrue(user.isActive());

            System.out.println("Test registerNewUser PASSED");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    @Test
    public void testRegisterUsernameTooShort() {
        try {
            AuthService.register("ab", "password123", "test@email.com");
            fail("Should throw AuthenticationException");
        } catch (AuthenticationException e) {
            assertTrue(e.getMessage().contains("3-50"));
            System.out.println("Test registerUsernameTooShort PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testRegisterPasswordTooShort() {
        try {
            AuthService.register("testuser", "pass", "test@email.com");
            fail("Should throw AuthenticationException");
        } catch (AuthenticationException e) {
            assertTrue(e.getMessage().contains("6 ký tự"));
            System.out.println("Test registerPasswordTooShort PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testRegisterInvalidEmail() {
        try {
            AuthService.register("testuser", "password123", "notanemail");
            fail("Should throw AuthenticationException");
        } catch (AuthenticationException e) {
            assertTrue(e.getMessage().contains("Email"));
            System.out.println("Test registerInvalidEmail PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testLoginEmptyUsername() {
        try {
            AuthService.login("", "password123");
            fail("Should throw AuthenticationException");
        } catch (AuthenticationException e) {
            assertTrue(e.getMessage().contains("rỗng"));
            System.out.println("Test loginEmptyUsername PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testLoginEmptyPassword() {
        try {
            AuthService.login("testuser", "");
            fail("Should throw AuthenticationException");
        } catch (AuthenticationException e) {
            assertTrue(e.getMessage().contains("rỗng"));
            System.out.println("" +
                    "Test loginEmptyPassword PASSED");
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
