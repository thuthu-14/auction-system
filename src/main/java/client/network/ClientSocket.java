// src/main/java/client/network/ClientSocket.java
package client.network;

import common.Message;
import util.LoggerUtil;
import java.io.*;
import java.net.Socket;

/**
 * ClientSocket - Quản lý socket connection của client
 * Mục đích: Gửi và nhận message từ server
 */
public class ClientSocket {

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean isConnected;

    /**
     * Constructor
     */
    public ClientSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
        this.isConnected = true;
        LoggerUtil.info("✓ ClientSocket initialized");
    }

    /**
     * Gửi message tới server
     */
    public synchronized void sendMessage(Message message) throws IOException {
        if (!isConnected || socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }

        try {
            oos.writeObject(message);
            oos.flush();
            LoggerUtil.debug("📤 Message sent: " + message.getType());
        } catch (IOException e) {
            isConnected = false;
            throw e;
        }
    }

    /**
     * Nhận message từ server
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        if (!isConnected || socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }

        try {
            Message message = (Message) ois.readObject();
            LoggerUtil.debug("📨 Message received: " + (message != null ? message.getType() : "null"));
            return message;
        } catch (EOFException e) {
            isConnected = false;
            throw new IOException("Connection closed by server", e);
        } catch (IOException | ClassNotFoundException e) {
            isConnected = false;
            throw e;
        }
    }

    /**
     * Kiểm tra kết nối
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected();
    }

    /**
     * Đóng kết nối
     */
    public void close() {
        try {
            isConnected = false;

            if (oos != null) {
                oos.close();
            }

            if (ois != null) {
                ois.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            LoggerUtil.info("✓ ClientSocket closed");
        } catch (IOException e) {
            LoggerUtil.error("Error closing socket: " + e.getMessage());
        }
    }
}
