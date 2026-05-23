package server.handler;

import common.Message;
import common.MessageType;
import common.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.User;
import server.service.WalletAccountService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletMessageProcessorTest {

    @Mock
    ClientSessionContext context;

    @Mock
    WalletAccountService walletAccountService;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) {
            mocksClose.close();
        }
    }

    @Test
    void handleAddFundsSendsUpdatedUserAndSyncsCurrentUser() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        User current = mock(User.class);
        User updated = mock(User.class);
        when(current.getUserId()).thenReturn("U1");
        when(updated.getUserId()).thenReturn("U1");
        when(context.getCurrentUser()).thenReturn(current);
        when(walletAccountService.addFunds(current, "payload")).thenReturn(updated);

        processor.handleAddFunds(new Message(MessageType.ADD_FUNDS, (Object) "payload", "client"));

        verify(context).setCurrentUser(updated);
        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals(MessageType.SUCCESS, responseCaptor.getValue().getType());
        assertSame(updated, responseCaptor.getValue().getData());
    }

    @Test
    void handleWithdrawDoesNotSyncDifferentUserAndSendsResponse() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        User current = mock(User.class);
        User updated = mock(User.class);
        when(current.getUserId()).thenReturn("U1");
        when(updated.getUserId()).thenReturn("U2");
        when(context.getCurrentUser()).thenReturn(current);
        when(walletAccountService.withdraw(current, "payload")).thenReturn(updated);

        processor.handleWithdraw(new Message(MessageType.WITHDRAW, (Object) "payload", "client"));

        verify(context, never()).setCurrentUser(any());
        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals("Rút tiền thành công", responseCaptor.getValue().getMessage());
        assertSame(updated, responseCaptor.getValue().getData());
    }

    @Test
    void handleLinkBankSendsSuccessMessage() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        User current = mock(User.class);
        when(context.getCurrentUser()).thenReturn(current);

        processor.handleLinkBank(new Message(MessageType.LINK_BANK, (Object) Map.of("bankName", "VCB"), "client"));

        verify(walletAccountService).linkBank(eq(current), any());
        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals(MessageType.SUCCESS, responseCaptor.getValue().getType());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
        assertEquals("Liên kết ngân hàng thành công!", responseCaptor.getValue().getMessage());
    }

    @Test
    void handleGetTransactionsSendsTransactions() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        User current = mock(User.class);
        Transaction transaction = new Transaction();
        transaction.id = "T1";
        when(context.getCurrentUser()).thenReturn(current);
        when(walletAccountService.getTransactions(eq(current), any(Object.class))).thenReturn(List.of(transaction));

        processor.handleGetTransactions(new Message(MessageType.GET_TRANSACTIONS, (Object) "U1", "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals(MessageType.SUCCESS, responseCaptor.getValue().getType());
        assertEquals(List.of(transaction), responseCaptor.getValue().getData());
    }

    @Test
    void handleGetBankAccountSendsBankInfo() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        User current = mock(User.class);
        Map<String, String> bankInfo = Map.of("bankName", "VCB", "accountNumber", "001");
        when(context.getCurrentUser()).thenReturn(current);
        when(walletAccountService.getBankAccount(current)).thenReturn(bankInfo);

        processor.handleGetBankAccount(new Message(MessageType.GET_BANK_ACCOUNT, (Object) null, "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        assertEquals(MessageType.SUCCESS, responseCaptor.getValue().getType());
        assertSame(bankInfo, responseCaptor.getValue().getData());
    }

    @Test
    void serviceErrorsSendError() throws Exception {
        WalletMessageProcessor processor = new WalletMessageProcessor(context, walletAccountService);
        when(walletAccountService.addFunds(any(), any())).thenThrow(new IllegalArgumentException("bad amount"));

        processor.handleAddFunds(new Message(MessageType.ADD_FUNDS, (Object) "payload", "client"));

        verify(context).sendError("bad amount");
        verify(context, never()).sendMessage(any());
    }
}
