package server.handler;

import common.LoginRequest;
import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import server.exception.AuthenticationException;
import server.model.RegularUser;
import server.model.User;
import server.service.AuthService;
import server.service.UserService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthUserMessageProcessorTest {
    @Test
    void handleLoginSetsCurrentUserAndSendsSuccessResponse() throws Exception {
        ClientSessionContext context = mock(ClientSessionContext.class);
        AuthUserMessageProcessor processor = new AuthUserMessageProcessor(context);
        RegularUser user = new RegularUser("U1", "user", "pw", "user@example.com");

        try (MockedStatic<AuthService> auth = mockStatic(AuthService.class)) {
            auth.when(() -> AuthService.login("user@example.com", "pw")).thenReturn(user);

            processor.handleLogin(new Message(MessageType.LOGIN, new LoginRequest("user@example.com", "pw"), "client"));
        }

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).setCurrentUser(user);
        verify(context).sendMessage(responseCaptor.capture());
        Message response = responseCaptor.getValue();
        assertEquals(MessageType.SUCCESS, response.getType());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Login successful", response.getMessage());
        assertSame(user, response.getData());
    }

    @Test
    void handleLoginSendsAuthenticationError() throws Exception {
        ClientSessionContext context = mock(ClientSessionContext.class);
        AuthUserMessageProcessor processor = new AuthUserMessageProcessor(context);

        try (MockedStatic<AuthService> auth = mockStatic(AuthService.class)) {
            auth.when(() -> AuthService.login("user@example.com", "bad"))
                    .thenThrow(new AuthenticationException("Invalid login"));

            processor.handleLogin(new Message(MessageType.LOGIN, new LoginRequest("user@example.com", "bad"), "client"));
        }

        verify(context).sendError("Invalid login");
        verify(context, never()).sendMessage(any());
    }

    @Test
    void handleRegisterSetsCurrentUserAndSendsSuccessResponse() throws Exception {
        ClientSessionContext context = mock(ClientSessionContext.class);
        AuthUserMessageProcessor processor = new AuthUserMessageProcessor(context);
        RegularUser user = new RegularUser("U2", "new", "pw", "new@example.com");
        Map<String, String> payload = Map.of("username", "new");

        try (MockedStatic<AuthService> auth = mockStatic(AuthService.class)) {
            auth.when(() -> AuthService.register(payload)).thenReturn(user);

            processor.handleRegister(new Message(MessageType.REGISTER, payload, "client"));
        }

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).setCurrentUser(user);
        verify(context).sendMessage(responseCaptor.capture());
        assertSame(user, responseCaptor.getValue().getData());
        assertEquals("Registration successful", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleGetSellerContactAndAllUsersSendDataResponses() throws Exception {
        ClientSessionContext context = mock(ClientSessionContext.class);
        AuthUserMessageProcessor processor = new AuthUserMessageProcessor(context);
        User admin = new RegularUser("A1", "admin", "pw", "admin@example.com");
        User user = new RegularUser("U1", "user", "pw", "user@example.com");
        when(context.getCurrentUser()).thenReturn(admin);

        try (MockedStatic<UserService> users = mockStatic(UserService.class)) {
            users.when(() -> UserService.getSellerContactById("S1"))
                    .thenReturn(Map.of("email", "seller@example.com"));
            users.when(() -> UserService.getAllUsersForAdmin(admin))
                    .thenReturn(List.of(user));

            processor.handleGetSellerContact(new Message(MessageType.GET_SELLER_CONTACT, (Object) "S1", "client"));
            processor.handleGetAllUsers(new Message(MessageType.GET_ALL_USERS, (Object) null, "client"));
        }

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context, times(2)).sendMessage(responseCaptor.capture());
        assertEquals(Map.of("email", "seller@example.com"), responseCaptor.getAllValues().get(0).getData());
        assertEquals(List.of(user), responseCaptor.getAllValues().get(1).getData());
    }

    @Test
    void handleAdminMutationsAndUpgradeSellerDelegateToUserService() throws Exception {
        ClientSessionContext context = mock(ClientSessionContext.class);
        AuthUserMessageProcessor processor = new AuthUserMessageProcessor(context);
        RegularUser current = new RegularUser("U1", "user", "pw", "user@example.com");
        RegularUser seller = new RegularUser("U1", "user", "pw", "user@example.com");
        when(context.getCurrentUser()).thenReturn(current);
        Map<String, Object> statusPayload = Map.of("userId", "U2", "active", false);
        Map<String, String> sellerPayload = Map.of("shopName", "Shop");

        try (MockedStatic<UserService> users = mockStatic(UserService.class)) {
            users.when(() -> UserService.upgradeSellerWithPassword(current, sellerPayload)).thenReturn(seller);

            processor.handleBanUser(new Message(MessageType.BAN_USER, (Object) "U2", "admin"));
            processor.handleUpdateUserStatus(new Message(MessageType.UPDATE_USER_STATUS, statusPayload, "admin"));
            processor.handleUpgradeSeller(new Message(MessageType.UPGRADE_SELLER, sellerPayload, "client"));

            users.verify(() -> UserService.banUserForAdmin(current, "U2"));
            users.verify(() -> UserService.updateUserStatusForAdmin(current, statusPayload));
            users.verify(() -> UserService.upgradeSellerWithPassword(current, sellerPayload));
        }

        verify(context).setCurrentUser(seller);
        verify(context, times(3)).sendMessage(any(Message.class));
    }
}
