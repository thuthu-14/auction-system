// src/main/java/client/network/MessageHandler.java
package client.network;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import common.Message;
import common.MessageType;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MessageHandler - Xử lý message từ server
 * Mục đích: Nhận message từ server và notify listeners
 * Thread-safe: CopyOnWriteArrayList
 */
public class MessageHandler {

    private ClientSocket clientSocket;
    private List<MessageListener> listeners;
    private volatile boolean isRunning;
    private Thread messageThread;

    /**
     * Interface cho message listeners
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
        void onError(String errorMessage);
        void onConnectionClosed();
    }

    /**
     * Constructor
     */
    public MessageHandler(ClientSocket clientSocket) {
        this.clientSocket = clientSocket;
        this.listeners = new CopyOnWriteArrayList<>();
        this.isRunning = false;
        LoggerUtil.info("✓ MessageHandler initialized");
    }

    /**
     * Đăng ký listener
     */
    public void addListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            LoggerUtil.debug("✓ MessageListener added. Total: " + listeners.size());
        }
    }

    /**
     * Hủy đăng ký listener
     */
    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
        LoggerUtil.debug("✓ MessageListener removed. Total: " + listeners.size());
    }

    /**
     * Khởi động message receiving thread
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            messageThread = new Thread(() -> receiveMessages(), "MessageHandlerThread");
            messageThread.setDaemon(true);
            messageThread.start();
            LoggerUtil.info("✓ MessageHandler started");
        }
    }

    /**
     * Dừng message receiving
     */
    public void stop() {
        isRunning = false;
        if (messageThread != null) {
            messageThread.interrupt();
        }
        LoggerUtil.info("✓ MessageHandler stopped");
    }

    /**
     * Nhận messages từ server (blocking)
     */
    private void receiveMessages() {
        try {
            while (isRunning && clientSocket.isConnected()) {
                try {
                    Message message = clientSocket.receiveMessage();
                    if (message != null) {
                        notifyListeners(message);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        ClientExceptionHandler.handle(ClientErrorType.NETWORK, "Receive server message", e);
                        notifyError("Connection error: " + e.getMessage());
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    ClientExceptionHandler.handle(ClientErrorType.DATA, "Deserialize server message", e);
                    notifyError("Data error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            ClientExceptionHandler.handle("Message handler", e);
            notifyError("Error: " + e.getMessage());
        } finally {
            isRunning = false;
            notifyConnectionClosed();
            LoggerUtil.info("✓ MessageHandler thread ended");
        }
    }

    /**
     * Notify tất cả listeners
     */
    private void notifyListeners(Message message) {
        for (MessageListener listener : listeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UI, "Notify message listener", e);
            }
        }
    }

    /**
     * Notify error tới listeners
     */
    private void notifyError(String errorMessage) {
        for (MessageListener listener : listeners) {
            try {
                listener.onError(errorMessage);
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UI, "Notify error listener", e);
            }
        }
    }

    /**
     * Notify connection closed
     */
    private void notifyConnectionClosed() {
        for (MessageListener listener : listeners) {
            try {
                listener.onConnectionClosed();
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UI, "Notify close listener", e);
            }
        }
    }

    /**
     * Kiểm tra đang chạy không
     */
    public boolean isRunning() {
        return isRunning;
    }
}
