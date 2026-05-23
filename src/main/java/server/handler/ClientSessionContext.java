package server.handler;

import common.Message;
import server.AuctionServer;
import server.ClientHandler;
import server.model.User;

import java.io.IOException;

public class ClientSessionContext {
    private final ClientHandler client;
    private final AuctionServer server;
    private User currentUser;

    public ClientSessionContext(ClientHandler client, AuctionServer server) {
        this.client = client;
        this.server = server;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public AuctionServer getServer() {
        return server;
    }

    public void sendMessage(Message message) throws IOException {
        client.sendMessage(message);
    }

    public void sendError(String errorMsg) throws IOException {
        client.sendError(errorMsg);
    }
}
