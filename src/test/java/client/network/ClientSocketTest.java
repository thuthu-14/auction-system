package client.network;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClientSocketTest {

    @Test
    void sendAndReceiveWritesRequestSkipsAsyncBroadcastAndStoresIt() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            ArrayBlockingQueue<Message> receivedByServer = new ArrayBlockingQueue<>(1);
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(serverSide.getInputStream())) {
                    out.flush();
                    receivedByServer.offer((Message) in.readObject());
                    out.writeObject(new Message(MessageType.NOTIFICATION, "async", "server"));
                    out.flush();
                    out.writeObject(new Message(MessageType.LOGIN, "SUCCESS", "ok"));
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "client-socket-test-server");
            serverThread.start();

            try (Socket rawClient = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                ClientSocket clientSocket = new ClientSocket(rawClient);

                Message response = clientSocket.sendAndReceive(new Message(MessageType.LOGIN, (Object) "payload", "client"));

                assertEquals(MessageType.LOGIN, response.getType());
                assertEquals("ok", response.getMessage());
                Message request = receivedByServer.poll(2, TimeUnit.SECONDS);
                assertNotNull(request);
                assertEquals(MessageType.LOGIN, request.getType());
                assertEquals("payload", request.getData());
                assertEquals(MessageType.NOTIFICATION, clientSocket.pollAsyncMessage().getType());
                assertNull(clientSocket.pollAsyncMessage());
                clientSocket.close();
                assertFalse(clientSocket.isConnected());
            }
            serverThread.join(2_000);
        }
    }

    @Test
    void receiveMessageThrowsIOExceptionWhenServerClosesConnection() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream())) {
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "client-socket-close-test-server");
            serverThread.start();

            try (Socket rawClient = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                ClientSocket clientSocket = new ClientSocket(rawClient);

                assertThrows(java.io.IOException.class, clientSocket::receiveMessage);
                assertFalse(clientSocket.isConnected());
            }
            serverThread.join(2_000);
        }
    }

    @Test
    void receiveMessageReturnsNullForNonMessageObject() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream())) {
                    out.flush();
                    out.writeObject("not-a-message");
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "client-socket-non-message-test-server");
            serverThread.start();

            try (Socket rawClient = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                ClientSocket clientSocket = new ClientSocket(rawClient);

                assertNull(clientSocket.receiveMessage());
                clientSocket.close();
            }
            serverThread.join(2_000);
        }
    }

    @Test
    void sendMessageThrowsWhenSocketAlreadyClosed() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream());
                     ObjectInputStream ignored = new ObjectInputStream(serverSide.getInputStream())) {
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "client-socket-closed-send-test-server");
            serverThread.start();

            Socket rawClient = new Socket("127.0.0.1", serverSocket.getLocalPort());
            ClientSocket clientSocket = new ClientSocket(rawClient);
            rawClient.close();

            assertThrows(java.io.IOException.class,
                    () -> clientSocket.sendMessage(new Message(MessageType.LOGIN, "payload", "client")));
            assertFalse(clientSocket.isConnected());
            clientSocket.close();
            serverThread.join(2_000);
        }
    }

    @ParameterizedTest
    @EnumSource(value = MessageType.class, names = {
            "UPDATE",
            "NOTIFICATION",
            "OUTBID_NOTIFICATION",
            "AUCTION_FINISHED_NOTIFICATION",
            "AUCTION_EXTENDED_NOTIFICATION",
            "UPDATE_PRICE_REALTIME",
            "SELLER_AUCTIONS_UPDATED"
    })
    void sendAndReceiveQueuesEachAsyncBroadcastType(MessageType asyncType) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket serverSide = serverSocket.accept();
                     ObjectOutputStream out = new ObjectOutputStream(serverSide.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(serverSide.getInputStream())) {
                    out.flush();
                    in.readObject();
                    out.writeObject(new Message(asyncType, "async", "server"));
                    out.flush();
                    out.writeObject(new Message(MessageType.SUCCESS, "SUCCESS", "ok"));
                    out.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "client-socket-async-type-test-server");
            serverThread.start();

            try (Socket rawClient = new Socket("127.0.0.1", serverSocket.getLocalPort())) {
                ClientSocket clientSocket = new ClientSocket(rawClient);

                Message response = clientSocket.sendAndReceive(new Message(MessageType.GET_AUCTIONS, "payload", "client"));

                assertEquals(MessageType.SUCCESS, response.getType());
                assertEquals(asyncType, clientSocket.pollAsyncMessage().getType());
                assertNull(clientSocket.pollAsyncMessage());
                clientSocket.close();
            }
            serverThread.join(2_000);
        }
    }
}
