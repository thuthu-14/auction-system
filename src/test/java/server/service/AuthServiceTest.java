package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.repository.UserRepository;
import server.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @Test
    public void login_invalidCredentials_throws() throws Exception {
        AuthService service = new AuthService(userRepository);
        when(userRepository.getUserByEmail("nope@x.com")).thenReturn(null);
        assertThrows(Exception.class, () -> service.loginUser("nope@x.com","p"));
    }
}


