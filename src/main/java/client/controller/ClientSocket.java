package client.network;

import common.Message;
import util.LoggerUtil;
import java.io.*;
import java.net.Socket;

public class ClientSocket {

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean isConnected;

    public ClientSocket(Socket socket) throws IOException {
        this.socket = socket;
        // Tạo Output trước Input để tránh deadlock
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
        this.isConnected = true;
        LoggerUtil.info("✓ ClientSocket initialized");
    }

    /**
     * Gửi message và dọn dẹp cache stream bằng reset()
     */
    public synchronized void sendMessage(Message message) throws IOException {
        if (!isConnected || socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }

        try {
            oos.reset(); // Xóa bộ nhớ đệm để gửi dữ liệu mới nhất
            oos.writeObject(message);
            oos.flush();
            LoggerUtil.debug("📤 Message sent: " + message.getType());
        } catch (IOException e) {
            isConnected = false;
            throw e;
        }
    }

    /**
     * Nhận message đồng bộ để tránh lỗi 'invalid type code'
     */
    public synchronized Message receiveMessage() throws IOException, ClassNotFoundException {
        if (!isConnected || socket == null || socket.isClosed()) {
            throw new IOException("Socket is not connected");
        }

        try {
            Object obj = ois.readObject();
            if (obj instanceof Message) {
                Message message = (Message) obj;
                LoggerUtil.debug("📨 Message received: " + (message != null ? message.getType() : "null"));
                return message;
            }
            return null;
        } catch (EOFException e) {
            isConnected = false;
            throw new IOException("Connection closed by server", e);
        } catch (IOException | ClassNotFoundException e) {
            isConnected = false;
            throw e;
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && socket.isConnected();
    }

    public void close() {
        try {
            isConnected = false;
            if (oos != null) oos.close();
            if (ois != null) ois.close();
            if (socket != null && !socket.isClosed()) socket.close();
            LoggerUtil.info("✓ ClientSocket closed");
        } catch (IOException e) {
            LoggerUtil.error("Error closing socket: " + e.getMessage());
        }
    }
}