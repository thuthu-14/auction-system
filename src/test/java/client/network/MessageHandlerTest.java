package client.network;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageHandlerTest {

    @Test
    void startReceivesMessagesAndNotifiesConnectionClosed() throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        Message message = new Message(MessageType.UPDATE, "payload", "server");
        when(socket.isConnected()).thenReturn(true, false);
        when(socket.receiveMessage()).thenReturn(message);

        MessageHandler handler = new MessageHandler(socket);
        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        List<Message> received = new ArrayList<>();
        handler.addListener(new RecordingListener(received, messageLatch, closedLatch));

        handler.start();

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
        assertTrue(closedLatch.await(2, TimeUnit.SECONDS));
        assertEquals(List.of(message), received);
        assertFalse(handler.isRunning());
    }

    @Test
    void receiveIOExceptionNotifiesErrorAndClosedWhileRunning() throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        when(socket.receiveMessage()).thenThrow(new IOException("lost"));

        MessageHandler handler = new MessageHandler(socket);
        CountDownLatch errorLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        List<String> errors = new ArrayList<>();
        handler.addListener(new MessageHandler.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
            }

            @Override
            public void onError(String errorMessage) {
                errors.add(errorMessage);
                errorLatch.countDown();
            }

            @Override
            public void onConnectionClosed() {
                closedLatch.countDown();
            }
        });

        handler.start();

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
        assertTrue(closedLatch.await(2, TimeUnit.SECONDS));
        assertTrue(errors.get(0).contains("lost"));
        assertFalse(handler.isRunning());
    }

    @Test
    void receiveClassNotFoundNotifiesDataErrorThenContinuesToNextMessage() throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        Message message = new Message(MessageType.SUCCESS, "payload", "server");
        when(socket.isConnected()).thenReturn(true, true, false);
        when(socket.receiveMessage())
                .thenThrow(new ClassNotFoundException("bad class"))
                .thenReturn(message);

        MessageHandler handler = new MessageHandler(socket);
        CountDownLatch errorLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        List<String> errors = new ArrayList<>();
        List<Message> messages = new ArrayList<>();
        handler.addListener(new MessageHandler.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                messages.add(message);
                messageLatch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errors.add(errorMessage);
                errorLatch.countDown();
            }

            @Override
            public void onConnectionClosed() {
                closedLatch.countDown();
            }
        });

        handler.start();

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
        assertTrue(closedLatch.await(2, TimeUnit.SECONDS));
        assertTrue(errors.get(0).contains("bad class"));
        assertEquals(List.of(message), messages);
    }

    @Test
    void listenerThrowingOnErrorDoesNotPreventOtherErrorListeners() throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        when(socket.receiveMessage()).thenThrow(new ClassNotFoundException("broken"));

        MessageHandler handler = new MessageHandler(socket);
        CountDownLatch errorLatch = new CountDownLatch(1);
        handler.addListener(new MessageHandler.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
            }

            @Override
            public void onError(String errorMessage) {
                throw new RuntimeException("error listener failed");
            }

            @Override
            public void onConnectionClosed() {
            }
        });
        handler.addListener(new MessageHandler.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
            }

            @Override
            public void onError(String errorMessage) {
                errorLatch.countDown();
                handler.stop();
            }

            @Override
            public void onConnectionClosed() {
            }
        });

        handler.start();

        assertTrue(errorLatch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void addRemoveListenerAndListenerExceptionsDoNotStopNotification() throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        Message message = new Message(MessageType.UPDATE, "payload", "server");
        when(socket.isConnected()).thenReturn(true, false);
        when(socket.receiveMessage()).thenReturn(message);

        MessageHandler handler = new MessageHandler(socket);
        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        MessageHandler.MessageListener removed = mock(MessageHandler.MessageListener.class);
        handler.addListener(removed);
        handler.addListener(removed);
        handler.removeListener(removed);
        handler.addListener(new MessageHandler.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                throw new RuntimeException("listener failed");
            }

            @Override
            public void onError(String errorMessage) {
            }

            @Override
            public void onConnectionClosed() {
                throw new RuntimeException("close failed");
            }
        });
        handler.addListener(new RecordingListener(new ArrayList<>(), messageLatch, closedLatch));

        handler.start();

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
        assertTrue(closedLatch.await(2, TimeUnit.SECONDS));
        verify(removed, never()).onMessageReceived(any());
    }

    @Test
    void stopInterruptsRunningHandler() {
        MessageHandler handler = new MessageHandler(mock(ClientSocket.class));

        handler.start();
        handler.stop();

        assertFalse(handler.isRunning());
    }

    private record RecordingListener(List<Message> messages,
                                     CountDownLatch messageLatch,
                                     CountDownLatch closedLatch) implements MessageHandler.MessageListener {
        @Override
        public void onMessageReceived(Message message) {
            messages.add(message);
            messageLatch.countDown();
        }

        @Override
        public void onError(String errorMessage) {
        }

        @Override
        public void onConnectionClosed() {
            closedLatch.countDown();
        }
    }
}
