package client.network;

import common.Message;

public interface MessageTransport {
    Message sendAndReceive(Message message) throws Exception;

    boolean isConnected();
}
