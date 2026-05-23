package server;

import common.Message;
import common.MessageType;
import server.exception.ServerErrorType;
import server.exception.ServerExceptionHandler;
import server.handler.ClientMessageDispatcher;
import server.handler.ClientMessageProcessor;
import server.model.User;
import util.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuctionServer server;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;
    private final String clientId;
    private final ClientMessageProcessor messageProcessor;
    private final ClientMessageDispatcher messageDispatcher;
    private volatile boolean isConnected;

    public ClientHandler(Socket socket, AuctionServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
        this.isConnected = true;
        this.messageProcessor = new ClientMessageProcessor(this, server);
        this.messageDispatcher = new ClientMessageDispatcher(messageProcessor);
        LoggerUtil.info("ClientHandler created for " + clientId);
    }

    @Override
    public void run() {
        try {
            while (isConnected && socket.isConnected()) {
                try {
                    Message message = (Message) ois.readObject();
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (EOFException e) {
                    LoggerUtil.info("Client disconnected (EOF): " + clientId);
                    isConnected = false;
                } catch (ClassNotFoundException e) {
                    ServerExceptionHandler.handle(ServerErrorType.DATA, "Read client message", e);
                    sendError("Lỗi dữ liệu: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            ServerExceptionHandler.handle(ServerErrorType.NETWORK, "ClientHandler IO error (" + clientId + ")", e);
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(Message message) throws IOException {
        try {
            LoggerUtil.debug("Received message: " + message.getType() + " from " + clientId);
            messageDispatcher.dispatch(message);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Handle message " + message.getType(), e);
            sendError("Lỗi server: " + e.getMessage());
        }
    }

    public synchronized void sendMessage(Message message) throws IOException {
        if (isConnected && socket.isConnected()) {
            oos.reset();
            oos.writeObject(message);
            oos.flush();
            LoggerUtil.debug("Sent message: " + message.getType() + " to " + clientId);
        }
    }

    public void sendError(String errorMsg) throws IOException {
        sendMessage(new Message(MessageType.ERROR, "ERROR", errorMsg));
    }

    public void closeConnection() {
        try {
            isConnected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            server.removeClient(this);

            User currentUser = messageProcessor.getCurrentUser();
            if (currentUser != null) {
                LoggerUtil.info("Connection closed for user: " + currentUser.getUsername());
            } else {
                LoggerUtil.info("Connection closed for client: " + clientId);
            }
        } catch (IOException e) {
            ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Close client connection", e);
        }
    }
}
