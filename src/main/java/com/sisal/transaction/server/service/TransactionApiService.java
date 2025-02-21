package com.sisal.transaction.server.service;


import com.sisal.transaction.server.exception.InsufficientBalanceException;
import com.sisal.transaction.server.exception.TransactionRateLimitException;
import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.db.TransactionEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import com.sisal.transaction.server.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class TransactionApiService {
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 5;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionApiService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }


    public TransactionAPIResponse createTransaction(TransactionAPIRequest transactionAPIRequest) {

        TransactionEntity.TransactionType transactionType = TransactionEntity.TransactionType.valueOf(transactionAPIRequest.getTransactionType().toString());

        TransactionEntity transactionEntity = createTransaction(transactionAPIRequest.getAccountId(), transactionAPIRequest.getAmount(), transactionType);

        TransactionAPIResponse.StatusEnum statusEnum = TransactionAPIResponse.StatusEnum.fromValue(transactionEntity.getStatus().toString());

        return new TransactionAPIResponse()
                .accountId(transactionEntity.getAccountId())
                .amount(transactionEntity.getAmount())
                .status(statusEnum)
                .timestamp(transactionEntity.getTimestamp())
                .transactionId(transactionEntity.getTransactionId());
    }

    /**
     * Creates a transaction record and updates the account record.
     * <p>
     * Business rules enforced as the following:
     * Enforce 5 transaction/minute limit
     * Enforce the $100 minimum rule (skip if it's a new account)
     * <p>
     * Transactional tag ensures db records are committed only at the end of the method call.
     *
     * @param accountId bank account number
     * @param amount    transaction amount applied on the account
     * @param type      type of transaction to be applied (deposit/withdrawl)
     * @return Transaction db record
     */
    @Transactional
    public TransactionEntity createTransaction(String accountId,
                                               Double amount,
                                               TransactionEntity.TransactionType type) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (isRateLimitExceeded(accountId)) {
            throw new TransactionRateLimitException(
                    "Rate limit exceeded: Maximum " + MAX_TRANSACTIONS_PER_MINUTE +
                            " transactions per minute allowed");
        }

        TransactionEntity transaction = new TransactionEntity();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionEntity.TransactionStatus.COMPLETED);

        // Adjust account balance
        if (type == TransactionEntity.TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance() + amount);
        } else {
            // Check if an account has sufficient funds
            if (account.getBalance() < amount) {
                throw new InsufficientBalanceException("Insufficient funds");
            }
            double newBalance = account.getBalance() - amount;

            //Enforce the $100 minimum rule (skip if it's a new account)
            if (!isNewAccount(account) && newBalance < 100.0) {
                throw new InsufficientBalanceException("Balance cannot drop below $100 for existing accounts");
            }
            account.setBalance(newBalance);
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    /**
     * Enforces the maximum number of transactions per minute that can be applied to db.
     *
     * @param accountId
     * @return
     */
    private boolean isRateLimitExceeded(String accountId) {
        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        long recentTransactions = transactionRepository
                .countRecentTransactions(accountId, oneMinuteAgo);

        return recentTransactions >= MAX_TRANSACTIONS_PER_MINUTE;
    }

    /**
     * Determines if the account is considered 'new'.
     * <p>
     * The Account is considered new if it is opened at least 10 days before.
     */
    private boolean isNewAccount(AccountEntity account) {
        return Duration.between(account.getCreatedAt(), OffsetDateTime.now()).toDays() >= 10;
    }
}