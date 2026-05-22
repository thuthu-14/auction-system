package client.service;

import client.network.ClientSocket;
import common.Message;
import common.Transaction;
import server.model.User;

import java.util.List;

public interface WalletClient {
    double parseMoneyAmount(String amountStr);

    Message submitWalletTransaction(ClientSocket socket, User user, String type, double amount) throws Exception;

    Message linkBankAccount(ClientSocket socket, User user, String bank, String account, double amount) throws Exception;

    Message fetchBankAccount(ClientSocket socket, User user) throws Exception;

    Message fetchTransactions(ClientSocket socket, User user) throws Exception;

    List<Transaction> fetchTransactionList(ClientSocket socket, User user) throws Exception;

    WalletService.BankAccountEntry loadBankAccount(ClientSocket socket, User user) throws Exception;

    void saveBankAccount(ClientSocket socket, User user, String bankName, String accountNumber, double initialBalance) throws Exception;
}
