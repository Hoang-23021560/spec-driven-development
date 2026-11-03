package com.bunledger.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    @DisplayName("TransactionType display names should be correct")
    void testTransactionTypeDisplayName() {
        assertEquals("Khoản Thu", TransactionType.INCOME.getDisplayName());
        assertEquals("Khoản Chi", TransactionType.EXPENSE.getDisplayName());
    }

    @Test
    @DisplayName("Transaction builder should instantiate entity properly")
    void testTransactionBuilder() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal amount = new BigDecimal("45000");

        Transaction tx = Transaction.builder()
                .id(1L)
                .type(TransactionType.INCOME)
                .itemName("Bún bò tái")
                .amount(amount)
                .description("Khách ăn tại quán")
                .transactionDate(date)
                .createdAt(now)
                .build();

        assertNotNull(tx);
        assertEquals(1L, tx.getId());
        assertEquals(TransactionType.INCOME, tx.getType());
        assertEquals("Bún bò tái", tx.getItemName());
        assertEquals(amount, tx.getAmount());
        assertEquals("Khách ăn tại quán", tx.getDescription());
        assertEquals(date, tx.getTransactionDate());
        assertEquals(now, tx.getCreatedAt());
    }

    @Test
    @DisplayName("Transaction prePersist should set defaults when null")
    void testPrePersist() {
        Transaction tx = new Transaction();
        assertNull(tx.getCreatedAt());
        assertNull(tx.getTransactionDate());

        tx.onCreate();

        assertNotNull(tx.getCreatedAt());
        assertNotNull(tx.getTransactionDate());
        assertEquals(LocalDate.now(), tx.getTransactionDate());
    }
}

