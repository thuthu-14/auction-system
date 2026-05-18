package server.service;

import common.Transaction;
import server.model.User;
import server.repository.SqlBankAccountRepository;
import server.repository.SqlTransactionRepository;
import server.repository.SqlUserRepository;

import java.util.List;
import java.util.Map;

public class WalletAccountService {
    private final SqlUserRepository userRepository = new SqlUserRepository();
    private final SqlTransactionRepository transactionRepository = new SqlTransactionRepository();
    private final SqlBankAccountRepository bankAccountRepository = new SqlBankAccountRepository();

    public User addFunds(String userId, String username, double amount, User currentUser) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nap phai > 0");
        }

        User user = resolveUser(userId, username, currentUser);
        user.addFunds(amount);
        userRepository.saveUser(user);
        transactionRepository.saveTransaction(new Transaction(
                user.getUserId(),
                "DEPOSIT",
                amount,
                user.getWallet(),
                "Nap tien vao vi"
        ));
        return user;
    }

    public User addFunds(User currentUser, Object data) throws Exception {
        Map<String, Object> payload = RequestPayloadUtil.objectMap(data);
        return addFunds(
                RequestPayloadUtil.text(payload.get("userId")),
                RequestPayloadUtil.text(payload.get("username")),
                RequestPayloadUtil.requiredDouble(payload, "amount"),
                currentUser
        );
    }

    public User withdraw(String userId, String username, double amount, User currentUser) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien rut phai > 0");
        }

        User user = resolveUser(userId, username, currentUser);
        if (!user.deductFunds(amount)) {
            throw new IllegalArgumentException("So du vi khong du de thuc hien rut tien");
        }
        userRepository.saveUser(user);
        transactionRepository.saveTransaction(new Transaction(
                user.getUserId(),
                "WITHDRAW",
                amount,
                user.getWallet(),
                "Rut tien ve ngan hang"
        ));
        return user;
    }

    public User withdraw(User currentUser, Object data) throws Exception {
        Map<String, Object> payload = RequestPayloadUtil.objectMap(data);
        return withdraw(
                RequestPayloadUtil.text(payload.get("userId")),
                RequestPayloadUtil.text(payload.get("username")),
                RequestPayloadUtil.requiredDouble(payload, "amount"),
                currentUser
        );
    }

    public void linkBank(User user, String bankName, String accountNumber, double initialBalance) throws Exception {
        requireUser(user);
        if (bankName == null || bankName.trim().isEmpty()
                || accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Thong tin ngan hang khong hop le");
        }
        if (bankAccountRepository.hasBankAccount(user.getUserId())) {
            throw new IllegalStateException("Ban da lien ket tai khoan ngan hang roi");
        }
        bankAccountRepository.saveBankAccount(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                bankName,
                accountNumber,
                initialBalance
        );
    }

    public void linkBank(User user, Object data) throws Exception {
        Map<String, Object> payload = RequestPayloadUtil.objectMap(data);
        linkBank(
                user,
                RequestPayloadUtil.text(payload.get("bankName")),
                RequestPayloadUtil.text(payload.get("accountNumber")),
                RequestPayloadUtil.optionalDouble(payload, "initialBalance", 0.0)
        );
    }

    public List<Transaction> getTransactions(User currentUser, String userId) throws Exception {
        String resolvedUserId = userId != null ? userId.trim() : "";
        if (resolvedUserId.isBlank()) {
            requireUser(currentUser);
            resolvedUserId = currentUser.getUserId();
        }
        return transactionRepository.getByUser(resolvedUserId);
    }

    public List<Transaction> getTransactions(User currentUser, Object data) throws Exception {
        return getTransactions(currentUser, RequestPayloadUtil.text(data));
    }

    public Map<String, String> getBankAccount(User user) throws Exception {
        requireUser(user);
        if (!bankAccountRepository.hasBankAccount(user.getUserId())) {
            throw new IllegalStateException("Ban chua lien ket tai khoan ngan hang");
        }
        return bankAccountRepository.getBankAccount(user.getUserId());
    }

    private User resolveUser(String userId, String username, User currentUser) throws Exception {
        User user = userId != null && !userId.isBlank() ? userRepository.getUserById(userId) : null;
        if (user == null && username != null && !username.isBlank()) {
            user = userRepository.getUserByUsername(username);
        }
        if (user == null && currentUser != null && username != null
                && username.equals(currentUser.getUsername())) {
            user = currentUser;
        }
        if (user == null) {
            throw new IllegalArgumentException("Khong tim thay nguoi dung trong he thong");
        }
        return user;
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Ban phai dang nhap");
        }
    }
}
