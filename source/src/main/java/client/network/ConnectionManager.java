// src/main/java/client/network/ConnectionManager.java
package client.network;

import common.AppConfig;
import common.Constants;
import util.LoggerUtil;
import java.io.IOException;
import java.net.Socket;

/**
 * ConnectionManager - Quản lý kết nối tới server
 * Design Pattern: Singleton
 * Mục đích: Cung cấp kết nối duy nhất tới server
 */
public class ConnectionManager {

    private static ConnectionManager instance;
    private Socket socket;
    private ClientSocket clientSocket;
    private boolean isConnected;

    /**
     * Private constructor
     */
    private ConnectionManager() {
        this.isConnected = false;
    }

    /**
     * Singleton pattern
     */
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /**
     * Kết nối tới server
     */
    public synchronized boolean connect() {
        try {
            AppConfig config = AppConfig.getInstance();
            String host = config.getServerHost();
            int port = config.getServerPort();

            LoggerUtil.info("🔗 Connecting to server: " + host + ":" + port);

            socket = new Socket(host, port);
            socket.setSoTimeout(Constants.SOCKET_TIMEOUT);

            clientSocket = new ClientSocket(socket);
            isConnected = true;

            LoggerUtil.info("✓ Connected to server successfully");
            return true;

        } catch (IOException e) {
            LoggerUtil.error("Failed to connect: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    /**
     * Ngắt kết nối
     */
    public synchronized void disconnect() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            isConnected = false;
            LoggerUtil.info("✓ Disconnected from server");

        } catch (IOException e) {
            LoggerUtil.error("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Lấy ClientSocket
     */
    public ClientSocket getClientSocket() {
        return clientSocket;
    }

    /**
     * Kiểm tra kết nối
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected();
    }

    /**
     * Reconnect tới server
     */
    public void reconnect() throws IOException {
        disconnect();

        try {
            Thread.sleep(1000); // Wait 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Reconnect interrupted", e);
        }

        if (!connect()) {
            throw new IOException("Failed to reconnect to server");
        }
    }

}
