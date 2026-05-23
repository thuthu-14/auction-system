package server.handler;

import common.LoginRequest;
import common.Message;
import common.MessageType;
import server.exception.AuthenticationException;
import server.model.RegularUser;
import server.model.User;
import server.service.AuthService;
import server.service.UserService;
import util.LoggerUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthUserMessageProcessor {
    private final ClientSessionContext context;

    public AuthUserMessageProcessor(ClientSessionContext context) {
        this.context = context;
    }

    public void handleLogin(Message message) throws IOException, ClassNotFoundException {
        LoginRequest loginRequest = (LoginRequest) message.getData();
        try {
            User user = AuthService.login(loginRequest.getUsername(), loginRequest.getPassword());
            context.setCurrentUser(user);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Login successful");
            response.setData(user);
            context.sendMessage(response);

            LoggerUtil.info("User logged in: " + user.getUsername() + " (Role: " + user.getRole() + ")");
        } catch (AuthenticationException e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleRegister(Message message) throws IOException, ClassNotFoundException {
        try {
            User user = AuthService.register(message.getData());
            context.setCurrentUser(user);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Registration successful");
            response.setData(user);
            context.sendMessage(response);
        } catch (AuthenticationException e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleGetSellerContact(Message message) throws IOException, ClassNotFoundException {
        try {
            Map<String, String> contact = UserService.getSellerContactById(String.valueOf(message.getData()));
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Seller contact retrieved");
            response.setData(contact);
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleGetAllUsers(Message message) throws IOException, ClassNotFoundException {
        try {
            List<User> users = UserService.getAllUsersForAdmin(context.getCurrentUser());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Users retrieved");
            response.setData(new ArrayList<>(users));
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleBanUser(Message message) throws IOException, ClassNotFoundException {
        try {
            UserService.banUserForAdmin(context.getCurrentUser(), message.getData());
            context.sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "User banned successfully"));
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleUpdateUserStatus(Message message) throws IOException {
        try {
            UserService.updateUserStatusForAdmin(context.getCurrentUser(), message.getData());
            context.sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "User status updated successfully"));
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleUpgradeSeller(Message message) throws IOException, ClassNotFoundException {
        try {
            RegularUser seller = UserService.upgradeSellerWithPassword(context.getCurrentUser(), message.getData());
            context.setCurrentUser(seller);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Upgraded to Seller successfully");
            response.setData(seller);
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleUpdateProfile(Message message) throws IOException, ClassNotFoundException {
        try {
            User user = UserService.updateUserProfile(context.getCurrentUser(), message.getData());
            context.setCurrentUser(user);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Profile updated successfully");
            response.setData(user);
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }
}
