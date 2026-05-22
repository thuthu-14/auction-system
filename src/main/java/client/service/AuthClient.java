package client.service;

import client.network.MessageTransport;
import server.model.User;

public interface AuthClient {
    MessageTransport ensureConnected(MessageTransport currentTransport) throws Exception;

    User login(MessageTransport transport, String email, String password) throws Exception;

    User register(MessageTransport transport, String username, String password, String email) throws Exception;
}
