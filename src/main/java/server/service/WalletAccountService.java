package server.service;

import common.AuctionStatus;
import common.Transaction;
import server.model.Auction;
import server.model.User;
import server.repository.SqlAuctionRepository;
import server.repository.SqlBankAccountRepository;
import server.repository.SqlTransactionRepository;
import server.repository.SqlUserRepository;
import util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WalletAccountService {
    private static final String SELLER_PAYOUT_PREFIX = "Nh\u1eadn ti\u1ec1n b\u00e1n \u0111\u1ea5u gi\u00e1 s\u1ea3n ph\u1ea9m ";

    private final SqlUserRepository userRepository = new SqlUserRepository();
    private final SqlTransactionRepository transactionRepository = new SqlTransactionRepository();
    private final SqlBankAccountRepository bankAccountRepository = new SqlBankAccountRepository();
    private final SqlAuctionRepository auctionRepository = new SqlAuctionRepository();

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
            throw new IllegalArgumentException("Số dư ví không đủ để thực hiện rút tiền");
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
            throw new IllegalArgumentException("Thông tin ngân hàng không hợp lệ");
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
        reconcileMissingSellerPayouts(resolvedUserId, currentUser);
        return transactionRepository.getByUser(resolvedUserId);
    }

    public List<Transaction> getTransactions(User currentUser, Object data) throws Exception {
        return getTransactions(currentUser, RequestPayloadUtil.text(data));
    }

    public Map<String, String> getBankAccount(User user) throws Exception {
        requireUser(user);
        if (!bankAccountRepository.hasBankAccount(user.getUserId())) {
            return null;
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
            throw new IllegalArgumentException("Không tìm thấy người dùng trong hệ thống");
        }
        return user;
    }

    private void reconcileMissingSellerPayouts(String sellerId, User currentUser) throws Exception {
        if (sellerId == null || sellerId.isBlank()) {
            return;
        }

        List<Auction> sellerAuctions = auctionRepository.getAuctionsBySellerId(sellerId);
        if (sellerAuctions == null || sellerAuctions.isEmpty()) {
            return;
        }

        List<Transaction> existingTransactions = new ArrayList<>(transactionRepository.getByUser(sellerId));
        User seller = null;
        for (Auction auction : sellerAuctions) {
            if (!needsSellerPayout(auction) || hasSellerPayout(existingTransactions, auction)) {
                continue;
            }

            if (seller == null) {
                seller = userRepository.getUserById(sellerId);
                if (seller == null && currentUser != null && sellerId.equals(currentUser.getUserId())) {
                    seller = currentUser;
                }
                if (seller == null) {
                    LoggerUtil.warn("Cannot reconcile seller payout: user not found " + sellerId);
                    return;
                }
            }

            double payoutAmount = auction.getCurrentPrice();
            seller.addFunds(payoutAmount);
            userRepository.saveUser(seller);

            Transaction payout = new Transaction(
                    seller.getUserId(),
                    "DEPOSIT",
                    payoutAmount,
                    seller.getWallet(),
                    sellerPayoutDescription(auction)
            );
            transactionRepository.saveTransaction(payout);
            existingTransactions.add(payout);

            if (currentUser != null && seller.getUserId().equals(currentUser.getUserId())) {
                currentUser.setWallet(seller.getWallet());
            }
            LoggerUtil.info("Reconciled seller payout for auction " + auction.getAuctionId()
                    + ": " + payoutAmount + " to seller " + sellerId);
        }
    }

    private boolean needsSellerPayout(Auction auction) {
        if (auction == null
                || auction.getSellerId() == null
                || auction.getSellerId().isBlank()
                || auction.getHighestBidderId() == null
                || auction.getHighestBidderId().isBlank()
                || auction.getCurrentPrice() <= 0) {
            return false;
        }
        AuctionStatus status = auction.getStatus();
        return status == AuctionStatus.FINISHED
                || status == AuctionStatus.CLOSED
                || (auction.getEndTime() > 0 && auction.getEndTime() <= System.currentTimeMillis());
    }

    private boolean hasSellerPayout(List<Transaction> transactions, Auction auction) {
        if (transactions == null || auction == null) {
            return false;
        }
        String auctionMarker = "(auction " + auction.getAuctionId() + ")";
        String productName = productName(auction);
        for (Transaction transaction : transactions) {
            if (transaction == null
                    || !"DEPOSIT".equals(transaction.type)
                    || Math.abs(transaction.amount - auction.getCurrentPrice()) > 0.001) {
                continue;
            }

            String description = transaction.description == null ? "" : transaction.description;
            if (description.contains(auctionMarker)) {
                return true;
            }
            if (description.startsWith(SELLER_PAYOUT_PREFIX) && description.contains(productName)) {
                return true;
            }
        }
        return false;
    }

    private String sellerPayoutDescription(Auction auction) {
        return SELLER_PAYOUT_PREFIX + productName(auction) + " (auction " + auction.getAuctionId() + ")";
    }

    private String productName(Auction auction) {
        if (auction != null && auction.getItem() != null
                && auction.getItem().getName() != null
                && !auction.getItem().getName().isBlank()) {
            return auction.getItem().getName();
        }
        return auction != null && auction.getAuctionId() != null ? auction.getAuctionId() : "";
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new IllegalStateException("Bạn phải đăng nhập");
        }
    }
}
