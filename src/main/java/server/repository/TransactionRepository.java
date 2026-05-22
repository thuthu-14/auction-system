package server.repository;

import common.Transaction;

public interface TransactionRepository {
    void saveTransaction(Transaction transaction) throws Exception;
}
