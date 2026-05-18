package server;

import server.service.*;
import server.observer.AuctionManager;
import server.concurrency.ThreadPoolManager;
import server.exception.ServerErrorType;
import server.exception.ServerExceptionHandler;
// import server.storage.DataManager;  ← XÓA HOẶC COMMENT (không dùng nữa)
import util.LoggerUtil;
import java.io.*;
import java.net.*;
import java.util.*;

public class AuctionServer {

    private ServerSocket serverSocket;
    private ThreadPoolManager threadPoolManager;
    private List<ClientHandler> clients;
    private static final int PORT = 5000;
    private volatile boolean isRunning;

    public AuctionServer() throws IOException {
        this.serverSocket = new ServerSocket(PORT);
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.isRunning = true;
        LoggerUtil.info("AuctionServer initialized");
    }

    public void start() {
        ServerExceptionHandler.installGlobalHandlers();
        try {
            LoggerUtil.info("Starting Auction Server...");

            try {
                InitializeDataService.initializeDefaultData();
            } catch (Exception e) {
                ServerExceptionHandler.handle(ServerErrorType.DATA, "Initialize default data", e);
            }

            SchedulerService.startScheduler();
            AuctionManager.getInstance();

            LoggerUtil.info("Server listening on port " + PORT);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    clients.add(handler);
                    threadPoolManager.execute(handler);

                    LoggerUtil.info("Client connected: " + clientSocket.getInetAddress().getHostAddress() +
                            " - Active clients: " + clients.size());
                } catch (IOException e) {
                    if (isRunning) {
                        ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Accept client", e);
                    }
                }
            }
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

            SchedulerService.stopScheduler();

            for (ClientHandler client : new ArrayList<>(clients)) {
                try {
                    client.closeConnection();
                } catch (Exception e) {
                    ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Close client", e);
                }
            }

            threadPoolManager.shutdown();

            LoggerUtil.info("Server shutdown complete");
        } catch (IOException e) {
            ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Server shutdown", e);
        }
    }

    public synchronized void broadcastMessage(common.Message message) {
        List<ClientHandler> clientsCopy = new ArrayList<>(clients);
        for (ClientHandler client : clientsCopy) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Broadcast message", e);
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
            ServerExceptionHandler.handle(ServerErrorType.NETWORK, "Start server", e);
            System.exit(1);
        }
    }
}
