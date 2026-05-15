package client.service;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import common.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import server.model.User;
import util.JsonUtil;
import util.LoggerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletService {



    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class BankAccountEntry {
        public String userId;
        public String username;
        public String email;
        public String bankName;
        public String accountNumber;
        public double initialBalance;
        public long createdAt;
    }

    public double parseMoneyAmount(String amountStr) {
        if (amountStr == null) {
            throw new NumberFormatException("Amount is empty");
        }

        String normalized = amountStr.trim().replaceAll("[\\s,.]", "");
        if (!normalized.matches("\\d+")) {
            throw new NumberFormatException("Invalid amount: " + amountStr);
        }

        return Double.parseDouble(normalized);
    }

    public Message submitWalletTransaction(ClientSocket socket, User user, String type, double amount) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("amount", amount);

        MessageType msgType = "DEPOSIT".equals(type) ? MessageType.ADD_FUNDS : MessageType.WITHDRAW;
        return socket.sendAndReceive(new Message(msgType, data, user.getUsername()));
    }


    public Message linkBankAccount(ClientSocket socket, User user, String bank, String account, double amount) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("bankName", bank);
        data.put("accountNumber", account);
        data.put("amount", amount);

        return socket.sendAndReceive(new Message(MessageType.LINK_BANK, data, user.getUsername()));
    }

    public Message fetchBankAccount(ClientSocket socket, User user) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        return socket.sendAndReceive(
                new Message(MessageType.GET_BANK_ACCOUNT, user.getUserId(), user.getUsername())
        );
    }
    public Message fetchTransactions(ClientSocket socket, User user) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        return socket.sendAndReceive(
                new Message(MessageType.GET_TRANSACTIONS, user.getUserId(), user.getUsername())
        );
    }

}
