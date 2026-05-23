package client.service;

import common.LoginRequest;
import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;
import server.model.User;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthClientServiceTest {
    private final AuthClientService service = new AuthClientService();

    @Test
    void loginSendsLoginRequestAndReturnsUser() throws Exception {
        RegularUser expected = new RegularUser("U1", "bidder", "pw", "bidder@example.com");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.LOGIN, expected, "server"));

        User actual = service.login(transport, "bidder@example.com", "pw");

        assertSame(expected, actual);
        Message sent = transport.lastSent();
        assertEquals(MessageType.LOGIN, sent.getType());
        assertEquals("bidder@example.com", sent.getSenderId());
        LoginRequest request = assertInstanceOf(LoginRequest.class, sent.getData());
        assertEquals("bidder@example.com", request.getUsername());
        assertEquals("pw", request.getPassword());
    }

    @Test
    void loginThrowsServerMessageOnFailure() {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.LOGIN, "ERROR", "bad credentials"));

        IOException error = assertThrows(IOException.class,
                () -> service.login(transport, "bidder@example.com", "wrong"));

        assertEquals("bad credentials", error.getMessage());
    }

    @Test
    void loginAndRegisterUseDefaultErrorWhenResponseMissingMessage() {
        IOException loginError = assertThrows(IOException.class,
                () -> service.login(new FakeMessageTransport(), "bidder@example.com", "wrong"));
        assertTrue(loginError.getMessage().contains("mật khẩu")
                || loginError.getMessage().contains("mật khẩu"));

        IOException registerError = assertThrows(IOException.class,
                () -> service.register(new FakeMessageTransport(), "newuser", "pw", "new@example.com"));
        assertTrue(registerError.getMessage().contains("tài khoản")
                || registerError.getMessage().contains("tài khoản"));
    }

    @Test
    void registerSendsExpectedPayloadAndReturnsUser() throws Exception {
        RegularUser expected = new RegularUser("U2", "newuser", "pw", "new@example.com");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.REGISTER, expected, "server"));

        User actual = service.register(transport, "newuser", "pw", "new@example.com");

        assertSame(expected, actual);
        Message sent = transport.lastSent();
        assertEquals(MessageType.REGISTER, sent.getType());
        assertEquals("newuser", sent.getSenderId());
        Map<?, ?> payload = assertInstanceOf(Map.class, sent.getData());
        assertEquals("newuser", payload.get("username"));
        assertEquals("pw", payload.get("password"));
        assertEquals("new@example.com", payload.get("email"));
    }

    @Test
    void ensureConnectedReturnsExistingTransport() throws Exception {
        FakeMessageTransport transport = new FakeMessageTransport();

        assertSame(transport, service.ensureConnected(transport));
    }

    @Test
    void registerThrowsServerMessageOnFailure() {
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.REGISTER, "ERROR", "email exists"));

        IOException error = assertThrows(IOException.class,
                () -> service.register(transport, "newuser", "pw", "new@example.com"));

        assertEquals("email exists", error.getMessage());
    }

    @Test
    void parseUserConvertsMapPayloadToUser() throws Exception {
        Method parseUser = AuthClientService.class.getDeclaredMethod("parseUser", Object.class);
        parseUser.setAccessible(true);

        @SuppressWarnings("unchecked")
        User parsed = (User) parseUser.invoke(service, Map.of(
                "userId", "U9",
                "username", "mapped",
                "password", "pw",
                "email", "mapped@example.com",
                "role", "BIDDER",
                "active", true
        ));

        assertEquals("U9", parsed.getUserId());
        assertEquals("mapped", parsed.getUsername());
    }
}
