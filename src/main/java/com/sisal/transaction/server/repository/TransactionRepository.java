package com.sisal.transaction.server.repository;

import com.sisal.transaction.server.model.db.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByAccountAccountId(String accountId);

    List<TransactionEntity> findByTransactionType(TransactionEntity.TransactionType type);

    List<TransactionEntity> findByStatus(TransactionEntity.TransactionStatus status);

    List<TransactionEntity> findByTimestampBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    List<TransactionEntity> findByAccountAccountIdAndAmountGreaterThan(String accountId, BigDecimal amount);

    List<TransactionEntity> findByAccountAccountIdAndTransactionType(String accountId, TransactionEntity.TransactionType type);

    long countByAccountAccountId(String accountId);

    @Query("SELECT SUM(t.amount) FROM TransactionEntity t WHERE t.account.accountId = :accountId AND t.transactionType = :type")
    BigDecimal sumAmountByAccountAndType(@Param("accountId") String accountId, @Param("type") TransactionEntity.TransactionType type);
}
