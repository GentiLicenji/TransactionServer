package com.sisal.transaction.server.service;

import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AccountApiService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountApiService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountEntity createAccount(Long accountId, String firstName, String lastName, String accountNumber, Double initialBalance) {
        AccountEntity account = new AccountEntity();
        account.setFirstName(firstName);
        account.setLastName(lastName);
        account.setAccountNumber(accountNumber);
        account.setAccountId(accountId);
        account.setBalance(initialBalance);
        account.setCreatedAt(OffsetDateTime.now());
        account.setLastModifiedAt(OffsetDateTime.now());

        return accountRepository.save(account);
    }
}
