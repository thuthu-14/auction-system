package client.service;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import common.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import server.model.User;
import util.LoggerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletService {

    private static final String BANK_FILE = "data/json/bank_accounts.json";
    private static final String TRANSACTIONS_FILE = "data/json/transactions.json";

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

    public void saveBankAccount(User user, String bank, String account, double amount) throws Exception {
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Path path = ensureJsonFile(BANK_FILE);
        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) content = "[]";

        List<BankAccountEntry> list = gson.fromJson(content, new TypeToken<List<BankAccountEntry>>(){}.getType());
        if (list == null) list = new ArrayList<>();

        list.removeIf(entry -> isBankAccountOfUser(user, entry));

        BankAccountEntry entry = new BankAccountEntry();
        entry.userId = user.getUserId();
        entry.username = user.getUsername();
        entry.email = user.getEmail();
        entry.bankName = bank;
        entry.accountNumber = account;
        entry.initialBalance = amount;
        entry.createdAt = System.currentTimeMillis();

        list.add(entry);
        Files.writeString(path, gson.toJson(list));
        LoggerUtil.info("Saved linked bank account for user: " + getUserKey(user));
    }

    public BankAccountEntry loadBankAccount(User user) {
        try {
            if (user == null) return null;
            Path path = Paths.get(BANK_FILE);
            if (!Files.exists(path)) return null;
            String content = Files.readString(path).trim();
            if (content.isEmpty() || content.equals("null")) return null;

            List<BankAccountEntry> list = gson.fromJson(content, new TypeToken<List<BankAccountEntry>>(){}.getType());
            if (list == null) return null;
            return list.stream()
                    .filter(entry -> isBankAccountOfUser(user, entry))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LoggerUtil.error("Cannot load linked bank account: " + e.getMessage());
            return null;
        }
    }

    public void saveTransaction(User user, String type, double amount, double balanceAfter) throws Exception {
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Path path = ensureJsonFile(TRANSACTIONS_FILE);
        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) content = "[]";

        List<Transaction> txns = gson.fromJson(content, new TypeToken<List<Transaction>>(){}.getType());
        if (txns == null) txns = new ArrayList<>();
        txns.add(new Transaction(user.getUserId(), type, amount, balanceAfter,
                "DEPOSIT".equals(type) ? "Nạp tiền vào ví" : "Rút tiền về ngân hàng"));
        Files.writeString(path, gson.toJson(txns));
    }

    public List<Transaction> loadTransactions(User user) throws Exception {
        if (user == null) return new ArrayList<>();
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) return new ArrayList<>();
        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) return new ArrayList<>();

        List<Transaction> all = gson.fromJson(content, new TypeToken<List<Transaction>>(){}.getType());
        if (all == null) return new ArrayList<>();
        return all.stream()
                .filter(t -> user.getUserId().equals(t.userId))
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .collect(java.util.stream.Collectors.toList());
    }

    private Path ensureJsonFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.writeString(path, "[]");
        }
        return path;
    }

    private boolean isBankAccountOfUser(User user, BankAccountEntry entry) {
        if (entry == null || user == null) {
            return false;
        }

        return sameText(user.getUserId(), entry.userId)
                || sameText(user.getUsername(), entry.username)
                || sameText(user.getEmail(), entry.email);
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String getUserKey(User user) {
        if (user == null) {
            return "unknown";
        }
        if (user.getUserId() != null && !user.getUserId().isBlank()) {
            return user.getUserId();
        }
        return user.getUsername();
    }
}
