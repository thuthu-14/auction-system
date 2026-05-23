package server.handler;

import common.Message;
import common.MessageType;
import common.Transaction;
import server.exception.ServerExceptionHandler;
import server.model.User;
import server.service.WalletAccountService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class WalletMessageProcessor {
    private final ClientSessionContext context;
    private final WalletAccountService walletAccountService;

    public WalletMessageProcessor(ClientSessionContext context) {
        this(context, new WalletAccountService());
    }

    WalletMessageProcessor(ClientSessionContext context, WalletAccountService walletAccountService) {
        this.context = context;
        this.walletAccountService = walletAccountService;
    }

    public void handleAddFunds(Message message) throws IOException {
        try {
            User user = walletAccountService.addFunds(context.getCurrentUser(), message.getData());
            syncCurrentUser(user);
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Nạp tiền thành công");
            response.setData(user);
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleWithdraw(Message message) throws IOException {
        try {
            User user = walletAccountService.withdraw(context.getCurrentUser(), message.getData());
            syncCurrentUser(user);
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Rút tiền thành công");
            response.setData(user);
            context.sendMessage(response);
        } catch (Exception e) {
            context.sendError(e.getMessage());
        }
    }

    public void handleLinkBank(Message message) throws IOException {
        try {
            walletAccountService.linkBank(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, "Bank linked successfully", "SERVER");
            response.setStatus("SUCCESS");
            response.setMessage("Liên kết ngân hàng thành công!");
            context.sendMessage(response);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Link bank", e);
            context.sendError(e.getMessage());
        }
    }

    public void handleGetTransactions(Message message) throws IOException {
        try {
            List<Transaction> transactions = walletAccountService.getTransactions(context.getCurrentUser(), message.getData());
            Message response = new Message(MessageType.SUCCESS, transactions, "SERVER");
            response.setStatus("SUCCESS");
            context.sendMessage(response);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Get transactions", e);
            context.sendError(e.getMessage());
        }
    }

    public void handleGetBankAccount(Message message) throws IOException {
        try {
            Map<String, String> bankInfo = walletAccountService.getBankAccount(context.getCurrentUser());
            Message response = new Message(MessageType.SUCCESS, bankInfo, "SERVER");
            response.setStatus("SUCCESS");
            context.sendMessage(response);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Get bank account", e);
            context.sendError(e.getMessage());
        }
    }

    private void syncCurrentUser(User user) {
        User currentUser = context.getCurrentUser();
        if (currentUser != null && user != null && currentUser.getUserId().equals(user.getUserId())) {
            context.setCurrentUser(user);
        }
    }
}
