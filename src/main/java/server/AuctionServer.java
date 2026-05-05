package server;

import server.service.*;
import server.observer.AuctionManager;
import server.storage.DataManager;
import util.LoggerUtil;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private List<ClientHandler> clients;
    private static final int PORT = 5000;
    private static final int THREAD_POOL_SIZE = 20;
    private volatile boolean isRunning;

    public AuctionServer() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.isRunning = true;
        LoggerUtil.info("AuctionServer initialized");
    }

    public void start() {
        try {
            LoggerUtil.info("Starting Auction Server...");

            DataManager.initializeDataFiles();

            try {
                InitializeDataService.initializeDefaultData();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Lỗi nghiêm trọng: Không thể khởi tạo dữ liệu mặc định!");
                e.printStackTrace();

            }

            SchedulerService.startScheduler();

            AuctionManager.getInstance();

            System.out.println("═══════════════════════════════════════════");
            System.out.println("Server listening on port " + PORT);
            System.out.println("═══════════════════════════════════════════");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    threadPool.execute(handler);

                    LoggerUtil.info("Client connected: " + clientSocket.getInetAddress().getHostAddress() +
                            " - Active clients: " + clients.size());
                } catch (IOException e) {
                    if (isRunning) {
                        LoggerUtil.error("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            LoggerUtil.error("Server startup error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }


    public synchronized void shutdown() {
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            threadPool.shutdownNow();

            SchedulerService.stopScheduler();

            for (ClientHandler client : new ArrayList<>(clients)) {
                try {
                    client.closeConnection();
                } catch (Exception e) {
                    LoggerUtil.error("Error closing client: " + e.getMessage());
                }
            }

            LoggerUtil.info("Server shutdown complete");
        } catch (IOException e) {
            LoggerUtil.error("Error during shutdown: " + e.getMessage());
        }
    }

    public synchronized void broadcastMessage(common.Message message) {
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);
        for (ClientHandler client : clientsCopy) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                LoggerUtil.error("Error broadcasting message: " + e.getMessage());
                removeClient(client);
            }
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        LoggerUtil.info("Client removed - Active clients: " + clients.size());
    }

    public static void main(String[] args) {
        try {
            AuctionServer server = new AuctionServer();
            server.start();
        } catch (IOException e) {
            LoggerUtil.error("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
