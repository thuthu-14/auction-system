package client.service;

import client.network.MessageTransport;
import server.model.User;

public interface ProfileClient {
    User updateProfile(MessageTransport transport, User user, String name, String phone, String address, String password) throws Exception;
}
