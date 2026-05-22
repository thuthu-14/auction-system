package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.AuctionServer;
import server.ClientHandler;
import server.model.RegularUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSessionContextTest {

    @Test
    void delegatesMessagesErrorsAndTracksCurrentUser() throws Exception {
        ClientHandler client = mock(ClientHandler.class);
        AuctionServer server = mock(AuctionServer.class);
        ClientSessionContext context = new ClientSessionContext(client, server);
        RegularUser user = new RegularUser("u1", "user", "password", "u1@example.com");
        Message message = new Message(MessageType.SUCCESS, "ok", "server");

        context.setCurrentUser(user);
        context.sendMessage(message);
        context.sendError("bad");

        assertSame(user, context.getCurrentUser());
        assertSame(server, context.getServer());
        verify(client).sendMessage(message);
        verify(client).sendError("bad");
    }
}
