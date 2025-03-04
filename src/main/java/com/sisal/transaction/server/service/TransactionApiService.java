package com.sisal.transaction.server.service;


import com.sisal.transaction.server.config.RateLimitProperties;
import com.sisal.transaction.server.exception.AccountNotFoundException;
import com.sisal.transaction.server.exception.InsufficientBalanceException;
import com.sisal.transaction.server.exception.TransactionRateLimitException;
import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.db.TransactionEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import com.sisal.transaction.server.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for managing financial transactions and account operations.
 *
 * <p>Provides transaction creation, account balance management, and transaction status handling
 * with proper transactional boundaries and error recovery.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Transaction creation with atomic account updates</li>
 *   <li>Rate limiting (5 transactions/minute)</li>
 *   <li>Balance validation rules</li>
 *   <li>New account handling</li>
 *   <li>Transaction failure recovery</li>
 * </ul>
 *
 * <p>Business Rules:</p>
 * <ul>
 *   <li>Minimum balance of $100 for existing accounts</li>
 *   <li>Rate limit of 5 transactions per minute</li>
 *   <li>Special handling for accounts less than 10 days old</li>
 *   <li>Insufficient funds validation</li>
 * </ul>
 *
 * <p>Transaction Management:</p>
 * <ul>
 *   <li>Main transaction for account updates</li>
 *   <li>Separate transaction for initial transaction record (REQUIRES_NEW)</li>
 *   <li>Independent transaction for failure status updates</li>
 * </ul>
 *
 * @throws AccountNotFoundException if an account doesn't exist
 * @throws TransactionRateLimitException if rate limit exceeded
 * @throws InsufficientBalanceException if balance rules violated
 * @throws PersistenceException if database operations fail
 * @see TransactionEntity
 * @see AccountEntity
 * @see Transactional
 */
@Service
public class TransactionApiService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionApiService.class);

    private final RateLimitProperties rateLimitProperties;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionApiService(RateLimitProperties rateLimitProperties, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.rateLimitProperties = rateLimitProperties;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }


    public TransactionAPIResponse createTransaction(TransactionAPIRequest transactionAPIRequest) {

        TransactionEntity.TransactionType transactionType = TransactionEntity.TransactionType.valueOf(transactionAPIRequest.getTransactionType().toString());

        TransactionEntity transactionEntity = createTransaction(transactionAPIRequest.getaccountNumber(), transactionAPIRequest.getAmount(), transactionType);

        TransactionAPIResponse.StatusEnum statusEnum = TransactionAPIResponse.StatusEnum.fromValue(transactionEntity.getStatus().toString());
        TransactionAPIResponse.TransactionTypeEnum tranType = TransactionAPIResponse.TransactionTypeEnum.fromValue(transactionEntity.getTransactionType().toString());

        return new TransactionAPIResponse()
                .transactionType(tranType)
                .accountNumber(transactionEntity.getAccount().getAccountNumber())
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
     * @param accountNumber bank account number
     * @param amount        transaction amount applied on the account
     * @param type          type of transaction to be applied (deposit/withdrawal)
     * @return Transaction db record
     */
    @Transactional
    public TransactionEntity createTransaction(String accountNumber,
                                               Double amount,
                                               TransactionEntity.TransactionType type) {

        AccountEntity account = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for accountNumber=" + accountNumber));

        if (isRateLimitExceeded(account.getAccountId())) {
            throw new TransactionRateLimitException(
                    "Rate limit exceeded: Maximum " + rateLimitProperties.getMaxPerMinute() +
                            " transactions per minute allowed");
        }

        TransactionEntity transaction = new TransactionEntity();
        transaction.setAccount(account);
        transaction.setAccountId(account.getAccountId());
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionEntity.TransactionStatus.COMPLETED);//We handle failure before rollback below

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

        // Commit the transaction create immediately to ensure it exists
        TransactionEntity savedTransaction;
        try {

            savedTransaction = commitAndStartNewTransaction(transaction);
            logger.debug("Initial transaction created with ID: {}", savedTransaction.getTransactionId());
        } catch (Exception exception) {

            logger.error("Failed to create initial transaction", exception);
            throw new PersistenceException("Could not create transaction record", exception);
        }

        try {

            accountRepository.save(account);
            logger.debug("Account update successful for transaction: {}", savedTransaction.getTransactionId());

        } catch (Exception exception) {

            updateFailedStatus(transaction.getTransactionId());
            logger.debug("Transaction {} marked as failed", savedTransaction.getTransactionId());
            throw new PersistenceException("Failed to persist account update record to DB. " +
                    "Transaction persisted with a failed status.", exception);
        }
        return transaction;
    }

    /**
     * Enforces the maximum number of transactions per minute that can be applied to db.
     *
     * @param accountId unique account identifier
     * @return
     */
    private boolean isRateLimitExceeded(Long accountId) {

        if (!rateLimitProperties.isEnabled()) {
            return false;  // Skip rate limiting if disabled
        }

        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        long recentTransactions = transactionRepository
                .countRecentTransactions(accountId, oneMinuteAgo);

        return recentTransactions >= rateLimitProperties.getMaxPerMinute();
    }

    /**
     * Determines if the account is considered 'new'.
     * <p>
     * The Account is considered new if it is opened at least 10 days before.
     */
    private boolean isNewAccount(AccountEntity account) {
        return Duration.between(account.getCreatedAt(), OffsetDateTime.now()).toDays() <= 10;
    }

    /**
     * Commit first transaction create record to DB.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public TransactionEntity commitAndStartNewTransaction(TransactionEntity transaction) {
        return transactionRepository.save(transaction);
    }

    /**
     * Mark the original transaction as failed in case of account update failure.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateFailedStatus(UUID transactionId) {
        try {
            TransactionEntity transaction = transactionRepository
                    .findByTransactionId(transactionId)
                    .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

            transaction.setStatus(TransactionEntity.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
        } catch (Exception rollbackFailure) {
            logger.error("Failed to update transaction status to FAILED", rollbackFailure);
        }
    }
}