package client.service;

import client.network.ConnectionManager;
import client.network.MessageTransport;
import common.LoginRequest;
import common.Message;
import common.MessageType;
import server.model.RegularUser;
import server.model.User;
import util.JsonUtil;
import util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

public class AuthClientService {
    public MessageTransport ensureConnected(MessageTransport currentTransport) throws Exception {
        if (currentTransport != null && currentTransport.isConnected()) {
            return currentTransport;
        }

        ConnectionManager connectionManager = ConnectionManager.getInstance();
        if (!connectionManager.isConnected() && !connectionManager.connect()) {
            throw new java.io.IOException("Cannot connect to server");
        }
        return connectionManager.getClientSocket();
    }

    public User login(MessageTransport transport, String email, String password) throws Exception {
        MessageTransport activeTransport = ensureConnected(transport);
        Message response = activeTransport.sendAndReceive(
                new Message(MessageType.LOGIN, new LoginRequest(email, password), email));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Sai tài khoản hoặc mật khẩu");
        }
        return parseUser(response.getData());
    }

    public User register(MessageTransport transport, String username, String password, String email) throws Exception {
        MessageTransport activeTransport = ensureConnected(transport);
        Map<String, String> registerData = new HashMap<>();
        registerData.put("username", username);
        registerData.put("password", password);
        registerData.put("email", email);

        Message response = activeTransport.sendAndReceive(new Message(MessageType.REGISTER, registerData, username));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Không thể tạo tài khoản");
        }
        return parseUser(response.getData());
    }



    private User parseUser(Object rawData) {
        if (rawData instanceof User user) {
            return user;
        }
        String json = JsonUtil.getGson().toJson(rawData);
        return JsonUtil.getGson().fromJson(json, User.class);
    }
}
