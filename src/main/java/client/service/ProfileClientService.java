package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileClientService implements ProfileClient {
    public User updateProfile(MessageTransport transport,
                              User user,
                              String name,
                              String phone,
                              String address,
                              String password) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("phone", phone);
        payload.put("address", address);
        payload.put("password", password);

        Message response = transport.sendAndReceive(new Message(MessageType.UPDATE_PROFILE, payload, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new IOException(response != null ? response.getMessage() : "Không thể cập nhật hồ sơ");
        }
        if (!(response.getData() instanceof User updatedUser)) {
            throw new IOException("Dữ liệu hồ sơ không hợp lệ");
        }
        return updatedUser;
    }

    private void ensureConnected(MessageTransport transport) throws IOException {
        if (transport == null || !transport.isConnected()) {
            throw new IOException("Socket is not connected");
        }
    }
}
