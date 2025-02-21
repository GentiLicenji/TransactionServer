package com.sisal.transaction.server.repository;

import com.sisal.transaction.server.model.db.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    List<AccountEntity> findByCreatedAtAfter(OffsetDateTime date);

    List<AccountEntity> findByLastModifiedAtAfter(OffsetDateTime date);

    List<AccountEntity> findByBalanceGreaterThan(BigDecimal amount);

    List<AccountEntity> findByBalanceLessThan(BigDecimal amount);
}