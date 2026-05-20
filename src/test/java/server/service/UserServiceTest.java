package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.repository.UserRepository;
import server.model.User;

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

    @Test
    public void registerUser_duplicateUsername_throws() throws Exception {
        UserService svc = new UserService(userRepository);
        when(userRepository.getUserById("nx")).thenReturn(null);
        Exception ex = assertThrows(Exception.class, () -> svc.addFundsToUser("nx", 100.0));
        assertNotNull(ex.getMessage());
    }
}


