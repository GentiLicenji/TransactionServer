package com.sisal.transaction.server.service;


import com.sisal.transaction.server.model.api.TransactionAPIRequest;
import com.sisal.transaction.server.model.api.TransactionAPIResponse;
import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.db.TransactionEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import com.sisal.transaction.server.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TransactionApiService {

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


    private TransactionEntity createTransaction(String accountId,
                                                Double amount,
                                                TransactionEntity.TransactionType type) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setTimestamp(OffsetDateTime.now());
        transaction.setStatus(TransactionEntity.TransactionStatus.COMPLETED);

        // Update account balance
        if (type == TransactionEntity.TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance()+amount);
        } else {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient funds");
            }
            account.setBalance(account.getBalance()-amount);
        }
        account.setLastModifiedAt(OffsetDateTime.now());

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }


}