package server;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ClientHandlerTest {

    @Test
    public void sendMessage_writesToSocketOutputStream() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            // connect client socket
            Socket client = new Socket("127.0.0.1", port);

            // accept server side socket
            Socket serverSide = serverSocket.accept();

            // create handler with server-side socket
            AuctionServer serverMock = mock(AuctionServer.class);
            ClientHandler handler = new ClientHandler(serverSide, serverMock);

            // read from client side
            ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

            // send message via handler
            Message m = new Message(MessageType.SUCCESS, "OK", "payload");
            handler.sendMessage(m);

            // client should receive the object
            Object received = ois.readObject();
            assertNotNull(received);
            assertInstanceOf(Message.class, received);
            Message rm = (Message) received;
            assertEquals(MessageType.SUCCESS, rm.getType());

            // cleanup
            handler.closeConnection();
            client.close();
        }
    }

    @Test
    public void sendError_sendsErrorMessage() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            Socket client = new Socket("127.0.0.1", port);
            Socket serverSide = serverSocket.accept();
            AuctionServer serverMock = mock(AuctionServer.class);
            ClientHandler handler = new ClientHandler(serverSide, serverMock);
            ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

            handler.sendError("some error");
            Object received = ois.readObject();
            assertNotNull(received);
            assertInstanceOf(Message.class, received);
            Message rm = (Message) received;
            assertEquals(MessageType.ERROR, rm.getType());

            handler.closeConnection();
            client.close();
        }
    }
}

