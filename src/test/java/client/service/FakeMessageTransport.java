package client.service;

import client.network.MessageTransport;
import common.Message;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

final class FakeMessageTransport implements MessageTransport {
    private final Queue<Message> responses = new ArrayDeque<>();
    private final List<Message> sentMessages = new ArrayList<>();
    private boolean connected = true;

    FakeMessageTransport respondWith(Message response) {
        responses.add(response);
        return this;
    }

    FakeMessageTransport disconnected() {
        connected = false;
        return this;
    }

    Message lastSent() {
        if (sentMessages.isEmpty()) {
            return null;
        }
        return sentMessages.get(sentMessages.size() - 1);
    }

    int sentCount() {
        return sentMessages.size();
    }

    @Override
    public Message sendAndReceive(Message message) {
        sentMessages.add(message);
        return responses.poll();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
