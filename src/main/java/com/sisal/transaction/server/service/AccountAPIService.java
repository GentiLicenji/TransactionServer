package com.sisal.transaction.server.service;

import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AccountAPIService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountAPIService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountEntity createAccount(String accountId, Double initialBalance) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setBalance(initialBalance);
        account.setCreatedAt(OffsetDateTime.now());
        account.setLastModifiedAt(OffsetDateTime.now());

        return accountRepository.save(account);
    }
}
