package client.service;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import common.Transaction;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {
    private final WalletService service = new WalletService();
    private final RegularUser user = new RegularUser("U1", "alice", "pw", "alice@example.com");

    @Test
    void parseMoneyAmountAcceptsFormattedDigitsAndRejectsInvalidValues() {
        assertEquals(1234567, service.parseMoneyAmount("1,234,567"));
        assertEquals(1234567, service.parseMoneyAmount("1.234.567"));
        assertEquals(1234567, service.parseMoneyAmount(" 1 234 567 "));

        assertThrows(NumberFormatException.class, () -> service.parseMoneyAmount(null));
        assertThrows(NumberFormatException.class, () -> service.parseMoneyAmount("12a"));
        assertThrows(NumberFormatException.class, () -> service.parseMoneyAmount(""));
    }

    @Test
    void submitWalletTransactionSendsDepositOrWithdrawMessage() throws Exception {
        ClientSocket socket = connectedSocket(new Message(MessageType.ADD_FUNDS, "SUCCESS", "ok"));

        Message response = service.submitWalletTransaction(socket, user, "DEPOSIT", 100_000);

        assertEquals("ok", response.getMessage());
        verify(socket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.ADD_FUNDS
                        && "alice".equals(message.getSenderId())
                        && ((Map<?, ?>) message.getData()).get("amount").equals(100_000.0)
        ));

        ClientSocket withdrawSocket = connectedSocket(new Message(MessageType.WITHDRAW, "SUCCESS", "ok"));
        service.submitWalletTransaction(withdrawSocket, user, "WITHDRAW", 50_000);

        verify(withdrawSocket).sendAndReceive(argThat(message -> message.getType() == MessageType.WITHDRAW));
    }

    @Test
    void walletRequestsRejectDisconnectedSocketOrMissingUser() {
        ClientSocket disconnected = mock(ClientSocket.class);
        when(disconnected.isConnected()).thenReturn(false);

        assertThrows(IOException.class,
                () -> service.submitWalletTransaction(disconnected, user, "DEPOSIT", 1));
        assertThrows(IllegalStateException.class,
                () -> service.submitWalletTransaction(connectedSocket(null), null, "DEPOSIT", 1));
        assertThrows(IOException.class,
                () -> service.fetchBankAccount(disconnected, user));
        assertThrows(IllegalStateException.class,
                () -> service.fetchTransactions(connectedSocket(null), null));
    }

    @Test
    void loadBankAccountReturnsEntryOnlyForSuccessfulMapResponse() throws Exception {
        ClientSocket socket = connectedSocket(success(MessageType.GET_BANK_ACCOUNT,
                Map.of("bankName", "VCB", "accountNumber", "123456")));

        WalletService.BankAccountEntry entry = service.loadBankAccount(socket, user);

        assertNotNull(entry);
        assertEquals("VCB", entry.bankName);
        assertEquals("123456", entry.accountNumber);
        verify(socket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.GET_BANK_ACCOUNT
                        && "U1".equals(message.getData())
                        && "alice".equals(message.getSenderId())
        ));

        assertNull(service.loadBankAccount(connectedSocket(new Message(MessageType.GET_BANK_ACCOUNT, "ERROR", "missing")), user));
        assertNull(service.loadBankAccount(connectedSocket(success(MessageType.GET_BANK_ACCOUNT, "not a map")), user));
        assertNull(service.loadBankAccount(connectedSocket(success(MessageType.GET_BANK_ACCOUNT, Map.of("bankName", "VCB"))), user));
    }

    @Test
    void fetchTransactionListKeepsTransactionItemsAndReportsFailures() throws Exception {
        Transaction transaction = new Transaction("U1", "DEPOSIT", 100_000, 200_000, "top up");
        ClientSocket socket = connectedSocket(success(MessageType.GET_TRANSACTIONS, List.of(transaction, "ignored")));

        List<Transaction> transactions = service.fetchTransactionList(socket, user);

        assertEquals(List.of(transaction), transactions);

        IOException namedError = assertThrows(IOException.class,
                () -> service.fetchTransactionList(connectedSocket(new Message(MessageType.GET_TRANSACTIONS, "ERROR", "cannot load")), user));
        assertEquals("cannot load", namedError.getMessage());

        IOException defaultError = assertThrows(IOException.class,
                () -> service.fetchTransactionList(connectedSocket(null), user));
        assertEquals("Cannot load transactions", defaultError.getMessage());
    }

    @Test
    void saveBankAccountThrowsWhenLinkFails() throws Exception {
        ClientSocket successSocket = connectedSocket(new Message(MessageType.LINK_BANK, "SUCCESS", "linked"));
        assertDoesNotThrow(() -> service.saveBankAccount(successSocket, user, "VCB", "123456", 100_000));

        verify(successSocket).sendAndReceive(argThat(message ->
                message.getType() == MessageType.LINK_BANK
                        && ((Map<?, ?>) message.getData()).get("bankName").equals("VCB")
                        && ((Map<?, ?>) message.getData()).get("accountNumber").equals("123456")
        ));

        IOException failure = assertThrows(IOException.class,
                () -> service.saveBankAccount(
                        connectedSocket(new Message(MessageType.LINK_BANK, "ERROR", "duplicate")),
                        user, "VCB", "123456", 100_000));
        assertEquals("duplicate", failure.getMessage());
    }

    private static ClientSocket connectedSocket(Message response) throws Exception {
        ClientSocket socket = mock(ClientSocket.class);
        when(socket.isConnected()).thenReturn(true);
        when(socket.sendAndReceive(any(Message.class))).thenReturn(response);
        return socket;
    }

    private static Message success(MessageType type, Object data) {
        return new Message(type, data, "server");
    }
}
