package client.network;

import common.AppConfig;
import common.Constants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionManagerTest {

    @Test
    void getInstanceReturnsSingletonAndDisconnectIsSafeWhenNotConnected() {
        ConnectionManager first = ConnectionManager.getInstance();
        ConnectionManager second = ConnectionManager.getInstance();

        assertSame(first, second);
        first.disconnect();
        assertFalse(first.isConnected());
        if (first.getClientSocket() != null) {
            assertFalse(first.getClientSocket().isConnected());
        }
    }

    @Test
    void reconnectFailsWhenServerIsUnavailable() throws Exception {
        AppConfig config = AppConfig.getInstance();
        String originalHost = config.getServerHost();
        int originalPort = config.getServerPort();
        ConnectionManager manager = ConnectionManager.getInstance();
        manager.disconnect();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            config.setServerHost("127.0.0.1");
            config.setServerPort(serverSocket.getLocalPort());
        }

        try {
            IOException error = assertThrows(IOException.class, manager::reconnect);

            assertTrue(error.getMessage().contains("Failed to reconnect")
                    || error.getMessage().contains("Reconnect interrupted"));
        } finally {
            manager.disconnect();
            config.setServerHost(originalHost);
            config.setServerPort(originalPort == 0 ? Constants.SERVER_PORT : originalPort);
        }
    }

    @Test
    void connectCreatesClientSocketAndDisconnectClosesIt() throws Exception {
        AppConfig config = AppConfig.getInstance();
        String originalHost = config.getServerHost();
        int originalPort = config.getServerPort();
        CountDownLatch accepted = new CountDownLatch(1);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream());
                     ObjectInputStream ignored = new ObjectInputStream(serverSide.getInputStream())) {
                    out.flush();
                    accepted.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "connection-manager-connect-test-server");
            serverThread.start();

            config.setServerHost("127.0.0.1");
            config.setServerPort(serverSocket.getLocalPort());
            ConnectionManager manager = ConnectionManager.getInstance();
            manager.disconnect();

            assertTrue(manager.connect());
            assertTrue(accepted.await(2, TimeUnit.SECONDS));
            assertNotNull(manager.getClientSocket());
            assertTrue(manager.isConnected());

            manager.disconnect();

            assertFalse(manager.isConnected());
            assertFalse(manager.getClientSocket().isConnected());
            serverThread.join(2_000);
        } finally {
            config.setServerHost(originalHost);
            config.setServerPort(originalPort == 0 ? Constants.SERVER_PORT : originalPort);
        }
    }
}
