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
                    : "Sai tai khoan hoac mat khau");
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
                    : "Khong the tao tai khoan");
        }
        return parseUser(response.getData());
    }

    public void saveSellerContext(RegularUser seller) {
        if (seller == null) {
            return;
        }
        try {
            Map<String, String> sellerIdMap = new HashMap<>();
            sellerIdMap.put("sellerId", seller.getUserId());
            sellerIdMap.put("sellerName", seller.getShopName());
            sellerIdMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
            JsonUtil.saveToJson("data/json/current_seller_id.json", sellerIdMap);
        } catch (Exception e) {
            LoggerUtil.warn("Could not save seller context: " + e.getMessage());
        }
    }

    private User parseUser(Object rawData) {
        if (rawData instanceof User user) {
            return user;
        }
        String json = JsonUtil.getGson().toJson(rawData);
        return JsonUtil.getGson().fromJson(json, User.class);
    }
}
