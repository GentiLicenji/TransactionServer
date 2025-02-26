package com.sisal.transaction.server.service;

import com.sisal.transaction.server.model.db.AccountEntity;
import com.sisal.transaction.server.model.db.TransactionEntity;
import com.sisal.transaction.server.repository.AccountRepository;
import com.sisal.transaction.server.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.PersistenceException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TransactionApiService
 *
 * <p>Tests transaction creation scenarios including:</p>
 * <ul>
 *   <li>Successful transaction creation and account update</li>
 *   <li>Account save failure with transaction status update</li>
 *   <li>Initial transaction creation failure</li>
 * </ul>
 *
 * <p>Key test verifications:</p>
 * <ul>
 *   <li>Transaction status transitions (COMPLETED → FAILED)</li>
 *   <li>Account balance updates</li>
 *   <li>Transaction rollback scenarios</li>
 *   <li>Repository call sequences</li>
 * </ul>
 *
 * @see TransactionApiService
 * @see TransactionEntity
 * @see AccountEntity
 */
@ExtendWith(MockitoExtension.class)
class TransactionApiServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionApiService transactionApiService;

    private AccountEntity testAccount;

    private static final String ACCOUNT_NUMBER = "TEST1K60161331926819";
    private static final UUID TRANSACTION_ID = UUID.fromString("DDF17BFD-5472-8946-8647-665A80F0F9BE");

    /**
     * Creating a test account.
     */
    @BeforeEach
    void setUp() {
        testAccount = new AccountEntity();
        testAccount.setAccountId(1L);
        testAccount.setAccountNumber(ACCOUNT_NUMBER);
        testAccount.setBalance(200.0);
        testAccount.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void whenTransactionCreateAndAccountUpdateSucceeds_thenBothEntitiesAreSaved() {
        // Given
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(testAccount));

        when(accountRepository.save(any(AccountEntity.class)))
                .thenReturn(testAccount);

        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TransactionEntity result = transactionApiService.createTransaction(
                ACCOUNT_NUMBER,
                50.0,
                TransactionEntity.TransactionType.DEPOSIT
        );

        // Then
        assertAll(
                () -> assertEquals(250.0, testAccount.getBalance()), // 200 + 50
                () -> assertEquals(TransactionEntity.TransactionStatus.COMPLETED, result.getStatus()),
                () -> verify(accountRepository).save(testAccount),
                () -> verify(transactionRepository).save(any(TransactionEntity.class))
        );
    }

    @Test
    void whenAccountSaveFails_thenTransactionIsSavedWithFailedStatus() {
        // Given
        ArgumentCaptor<TransactionEntity> transactionCaptor =
                ArgumentCaptor.forClass(TransactionEntity.class);
        AtomicReference<TransactionEntity> savedTransaction = new AtomicReference<>();

        // Mock account existence
        when(accountRepository.findByAccountNumber(anyString()))
                .thenReturn(Optional.of(testAccount));

        // Mock the transaction save call in the main transaction create
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity transaction = invocation.getArgument(0);
                    if (transaction.getTransactionId() == null) {
                        transaction.setTransactionId(TRANSACTION_ID);
                        savedTransaction.set(transaction); // Store for later find
                    }
                    return transaction;
                });

        // Mock account save failure
        when(accountRepository.save(any(AccountEntity.class)))
                .thenThrow(new RuntimeException("DB Error"));

        // Mock findByTransactionId in updateStatusFailed()
        when(transactionRepository.findByTransactionId(TRANSACTION_ID))
                .thenAnswer(invocation -> {
                    // Return the saved transaction from the main transaction create
                    TransactionEntity found = savedTransaction.get();
                    // Create a new instance to simulate DB fetch
                    TransactionEntity detached = new TransactionEntity();
                    detached.setTransactionId(found.getTransactionId());
                    detached.setStatus(found.getStatus());
                    detached.setAmount(found.getAmount());
                    detached.setTransactionType(found.getTransactionType());
                    detached.setAccountId(found.getAccountId());
                    return Optional.of(detached);
                });

        // When
        assertThrows(PersistenceException.class, () ->
                transactionApiService.createTransaction(
                        testAccount.getAccountNumber(),
                        50.0,
                        TransactionEntity.TransactionType.DEPOSIT
                )
        );

        // Then
        InOrder inOrder = inOrder(transactionRepository);

        // Verify save in the main transaction create
        inOrder.verify(transactionRepository).save(transactionCaptor.capture());
        TransactionEntity initialSave = transactionCaptor.getValue();
        assertAll(
                "Verify initial transaction create",
                () -> assertEquals(TRANSACTION_ID, initialSave.getTransactionId()),
                () -> assertEquals(TransactionEntity.TransactionStatus.COMPLETED,
                        initialSave.getStatus()),
                () -> assertEquals(50.0, initialSave.getAmount())
        );

        // Verify operations in new transaction (will commit)
        inOrder.verify(transactionRepository).findByTransactionId(TRANSACTION_ID);
        inOrder.verify(transactionRepository).save(transactionCaptor.capture());

        TransactionEntity updatedTransaction = transactionCaptor.getValue();
        assertAll(
                "Verify updated transaction (in new transaction)",
                () -> assertEquals(TRANSACTION_ID, updatedTransaction.getTransactionId()),
                () -> assertEquals(TransactionEntity.TransactionStatus.FAILED,
                        updatedTransaction.getStatus()),
                () -> assertEquals(50.0, updatedTransaction.getAmount()),
                () -> assertNotSame(initialSave, updatedTransaction,
                        "Should be different instances due to new transaction")
        );

        // Verify the exact number of calls
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class)); //2 saves
        verify(transactionRepository, times(1)).findByTransactionId(any(UUID.class));//1 query
    }

    @Test
    void whenMainTransactionCreateFails_thenAccountNeverUpdated() {
        // Mock account found
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(testAccount));

        // Mock the transaction create to fail in REQUIRES_NEW transaction
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenThrow(new RuntimeException("Transaction save failed"));

        // When/Then
        assertThrows(PersistenceException.class, () ->
                transactionApiService.createTransaction(
                        ACCOUNT_NUMBER,
                        50.0,
                        TransactionEntity.TransactionType.DEPOSIT
                )
        );

        assertAll(
                "Verify account was not modified",
                () -> verify(accountRepository, never()).save(any()), // Account was never saved
                () -> verify(transactionRepository, never())
                        .findByTransactionId(any()), // Transaction update should never be called
                () -> verify(transactionRepository, times(1))
                        .save(any()) // Only the initial Transaction-create failure
        );
    }

}