package com.sisal.transaction.server.repository;

import com.sisal.transaction.server.model.db.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("SELECT t FROM TransactionEntity t WHERE t.transactionId = :transactionId")
    Optional<TransactionEntity> findByTransactionId(@Param("transactionId") UUID transactionId);

    @Query("SELECT COUNT(t) FROM TransactionEntity t " +
            "WHERE t.accountId = :accountId " +
            "AND t.timestamp >= :cutoffTime")
    long countRecentTransactions(@Param("accountId") Long accountId,
                                 @Param("cutoffTime") OffsetDateTime cutoffTime);

    List<TransactionEntity> findByTimestampBetween(
            OffsetDateTime startTime,
            OffsetDateTime endTime
    );
}
