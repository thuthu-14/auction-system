package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.exception.AuthenticationException;
import server.model.RegularUser;
import server.repository.UserRepository;
import server.model.User;

import java.util.List;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {
    UserRepository userRepository;

    @BeforeEach
    void init() {
        userRepository = new UserRepository() {
            @Override
            public User getUserById(String userId) {
                return null;
            }

            @Override
            public User getUserByUsername(String username) {
                return null;
            }

            @Override
            public User getUserByEmail(String email) {
                return null;
            }

            @Override
            public List<User> getAllUsers() {
                return List.of();
            }

            @Override
            public void saveUser(User user) {
            }

            @Override
            public void registerUser(User user) {
            }
        };
    }

    @Test
    public void login_invalidCredentials_throws() throws Exception {
        AuthService service = new AuthService(userRepository);
        assertThrows(Exception.class, () -> service.loginUser("nope@x.com","p"));
    }

    @Test
    public void login_rejectsBlankMissingWrongPasswordAndInactiveUser() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "secret", "user@example.com");
        AuthService service = new AuthService(new FakeUserRepository(user));

        assertThrows(AuthenticationException.class, () -> service.loginUser("", "secret"));
        assertThrows(AuthenticationException.class, () -> service.loginUser("missing@example.com", "secret"));
        assertThrows(AuthenticationException.class, () -> service.loginUser("user@example.com", "wrong"));

        user.setActive(false);
        assertThrows(AuthenticationException.class, () -> service.loginUser("user@example.com", "secret"));
    }

    @Test
    public void login_successReturnsActiveUser() throws Exception {
        RegularUser user = new RegularUser("u1", "user", "secret", "user@example.com");
        AuthService service = new AuthService(new FakeUserRepository(user));

        assertSame(user, service.loginUser("user@example.com", "secret"));
    }

    @Test
    public void register_rejectsInvalidInputsAndDuplicates() {
        RegularUser existing = new RegularUser("u1", "existing", "secret", "existing@example.com");
        AuthService service = new AuthService(new FakeUserRepository(existing));

        assertThrows(AuthenticationException.class,
                () -> service.registerUser("ab", "secret", "new@example.com"));
        assertThrows(AuthenticationException.class,
                () -> service.registerUser("newuser", "short", "new@example.com"));
        assertThrows(AuthenticationException.class,
                () -> service.registerUser("newuser", "secret", "not-email"));
        assertThrows(AuthenticationException.class,
                () -> service.registerUser("newuser", "secret", "existing@example.com"));
        assertThrows(AuthenticationException.class,
                () -> service.registerUser("admin", "secret", "admin@example.com"));
        assertThrows(AuthenticationException.class,
                () -> service.registerUser("existing", "secret", "new@example.com"));
    }

    @Test
    public void register_successCreatesBidderWithZeroWallet() throws Exception {
        FakeUserRepository repository = new FakeUserRepository(null);
        AuthService service = new AuthService(repository);

        User created = service.registerUser("newuser", "secret", "new@example.com");

        assertInstanceOf(RegularUser.class, created);
        assertTrue(created.getUserId().startsWith("U"));
        assertEquals("newuser", created.getUsername());
        assertEquals(0.0, created.getWallet(), 0.001);
        assertSame(created, repository.registeredUser);
    }

    @Test
    public void repositoryRuntimeErrors_areWrappedAsIOException() {
        AuthService service = new AuthService(new ThrowingUserRepository());

        assertThrows(IOException.class, () -> service.loginUser("user@example.com", "secret"));
        assertThrows(IOException.class, () -> service.registerUser("newuser", "secret", "new@example.com"));
    }

    private static final class FakeUserRepository implements UserRepository {
        private final User existingUser;
        private User registeredUser;

        private FakeUserRepository(User existingUser) {
            this.existingUser = existingUser;
        }

        @Override
        public User getUserById(String userId) {
            return existingUser != null && existingUser.getUserId().equals(userId) ? existingUser : null;
        }

        @Override
        public User getUserByUsername(String username) {
            return existingUser != null && existingUser.getUsername().equalsIgnoreCase(username) ? existingUser : null;
        }

        @Override
        public User getUserByEmail(String email) {
            return existingUser != null && existingUser.getEmail().equals(email) ? existingUser : null;
        }

        @Override
        public List<User> getAllUsers() {
            return existingUser == null ? List.of() : List.of(existingUser);
        }

        @Override
        public void saveUser(User user) {
        }

        @Override
        public void registerUser(User user) {
            registeredUser = user;
        }
    }

    private static final class ThrowingUserRepository implements UserRepository {
        @Override
        public User getUserById(String userId) {
            throw new RuntimeException("boom");
        }

        @Override
        public User getUserByUsername(String username) {
            throw new RuntimeException("boom");
        }

        @Override
        public User getUserByEmail(String email) {
            throw new RuntimeException("boom");
        }

        @Override
        public List<User> getAllUsers() {
            throw new RuntimeException("boom");
        }

        @Override
        public void saveUser(User user) {
            throw new RuntimeException("boom");
        }

        @Override
        public void registerUser(User user) {
            throw new RuntimeException("boom");
        }
    }
}


