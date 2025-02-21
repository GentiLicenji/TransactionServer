package com.sisal.transaction.server.model.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Account Entity - Database Model
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @NotNull
    @Column(name = "balance", nullable = false)
    private Double balance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    // Optional: Add audit fields
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "last_modified_at")
    private OffsetDateTime lastModifiedAt;

    /**
     * Upon account creation, it will automatically generate createdAt and lastModifiedAt.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        lastModifiedAt = createdAt;
    }

    /**
     * Upon account update, it will automatically update lastModifiedAt.
     */
    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = OffsetDateTime.now();
    }
}