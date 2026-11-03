package com.bunledger.repository;

import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("TransactionRepository Integration Tests")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Should group expenses by itemName, calculate sum and order by sum descending")
    void testFindCategoryBreakdown() {
        transactionRepository.deleteAll();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Expense 1 today: Thịt bò - 200.000
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Thịt bò")
                .amount(new BigDecimal("200000"))
                .transactionDate(today)
                .build());

        // Expense 2 today: Thịt bò - 100.000 -> Total Thịt bò = 300.000
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Thịt bò")
                .amount(new BigDecimal("100000"))
                .transactionDate(today)
                .build());

        // Expense 3 today: Bún tươi - 150.000
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Bún tươi")
                .amount(new BigDecimal("150000"))
                .transactionDate(today)
                .build());

        // Expense yesterday: Rau - 50.000 (should be excluded)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Rau sống")
                .amount(new BigDecimal("50000"))
                .transactionDate(yesterday)
                .build());

        // Income today: Bán bún - 500.000 (should be excluded)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bán bún")
                .amount(new BigDecimal("500000"))
                .transactionDate(today)
                .build());

        List<Object[]> breakdown = transactionRepository.findCategoryBreakdown(TransactionType.EXPENSE, today);

        assertThat(breakdown).isNotEmpty();
        assertThat(breakdown).hasSize(2);

        // 1st category: Thịt bò (300.000)
        Object[] row1 = breakdown.get(0);
        assertThat(row1[0]).isEqualTo("Thịt bò");
        assertThat(new BigDecimal(row1[1].toString()).compareTo(new BigDecimal("300000"))).isZero();

        // 2nd category: Bún tươi (150.000)
        Object[] row2 = breakdown.get(1);
        assertThat(row2[0]).isEqualTo("Bún tươi");
        assertThat(new BigDecimal(row2[1].toString()).compareTo(new BigDecimal("150000"))).isZero();
    }

    @Test
    @DisplayName("Should sum amounts by type and date range correctly")
    void testSumAmountByTypeAndDateRange() {
        transactionRepository.deleteAll();
        LocalDate day1 = LocalDate.of(2026, 11, 1);
        LocalDate day2 = LocalDate.of(2026, 11, 2);
        LocalDate outside = LocalDate.of(2026, 11, 10);

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bán sáng")
                .amount(new BigDecimal("500000"))
                .transactionDate(day1)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bán chiều")
                .amount(new BigDecimal("700000"))
                .transactionDate(day2)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bán ngoài khoảng")
                .amount(new BigDecimal("1000000"))
                .transactionDate(outside)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Mua thịt")
                .amount(new BigDecimal("400000"))
                .transactionDate(day1)
                .build());

        BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDateRange(
                TransactionType.INCOME, day1, LocalDate.of(2026, 11, 5));
        BigDecimal totalExpense = transactionRepository.sumAmountByTypeAndDateRange(
                TransactionType.EXPENSE, day1, LocalDate.of(2026, 11, 5));

        assertThat(totalIncome.compareTo(new BigDecimal("1200000"))).isZero();
        assertThat(totalExpense.compareTo(new BigDecimal("400000"))).isZero();

        BigDecimal emptySum = transactionRepository.sumAmountByTypeAndDateRange(
                TransactionType.INCOME, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertThat(emptySum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should count distinct active transaction days between date range")
    void testCountActiveDaysBetween() {
        transactionRepository.deleteAll();
        LocalDate d1 = LocalDate.of(2026, 11, 1);
        LocalDate d2 = LocalDate.of(2026, 11, 2);
        LocalDate dOutside = LocalDate.of(2026, 12, 1);

        // 2 transactions on d1
        transactionRepository.save(Transaction.builder().type(TransactionType.INCOME).itemName("A").amount(new BigDecimal("10000")).transactionDate(d1).build());
        transactionRepository.save(Transaction.builder().type(TransactionType.EXPENSE).itemName("B").amount(new BigDecimal("20000")).transactionDate(d1).build());

        // 1 transaction on d2
        transactionRepository.save(Transaction.builder().type(TransactionType.INCOME).itemName("C").amount(new BigDecimal("30000")).transactionDate(d2).build());

        // 1 transaction outside range
        transactionRepository.save(Transaction.builder().type(TransactionType.INCOME).itemName("D").amount(new BigDecimal("40000")).transactionDate(dOutside).build());

        long activeDays = transactionRepository.countActiveDaysBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(activeDays).isEqualTo(2);

        long zeroDays = transactionRepository.countActiveDaysBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertThat(zeroDays).isEqualTo(0);
    }

    @Test
    @DisplayName("Should group category breakdown between date range ordered descending")
    void testFindCategoryBreakdownBetween() {
        transactionRepository.deleteAll();
        LocalDate d1 = LocalDate.of(2026, 11, 5);
        LocalDate d2 = LocalDate.of(2026, 11, 15);

        transactionRepository.save(Transaction.builder().type(TransactionType.EXPENSE).itemName("Thịt bò").amount(new BigDecimal("300000")).transactionDate(d1).build());
        transactionRepository.save(Transaction.builder().type(TransactionType.EXPENSE).itemName("Thịt bò").amount(new BigDecimal("200000")).transactionDate(d2).build());
        transactionRepository.save(Transaction.builder().type(TransactionType.EXPENSE).itemName("Bún").amount(new BigDecimal("150000")).transactionDate(d1).build());

        List<Object[]> results = transactionRepository.findCategoryBreakdownBetween(
                TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(results).hasSize(2);
        assertThat(results.get(0)[0]).isEqualTo("Thịt bò");
        assertThat(new BigDecimal(results.get(0)[1].toString()).compareTo(new BigDecimal("500000"))).isZero();
        assertThat(results.get(1)[0]).isEqualTo("Bún");
        assertThat(new BigDecimal(results.get(1)[1].toString()).compareTo(new BigDecimal("150000"))).isZero();
    }

    @Test
    @DisplayName("Should find transactions between date range ordered by date asc and id asc")
    void testFindByTransactionDateBetweenOrderByTransactionDateAscIdAsc() {
        transactionRepository.deleteAll();
        LocalDate d1 = LocalDate.of(2026, 11, 1);
        LocalDate d2 = LocalDate.of(2026, 11, 2);

        Transaction t1 = transactionRepository.save(Transaction.builder().type(TransactionType.INCOME).itemName("T1").amount(new BigDecimal("10000")).transactionDate(d2).build());
        Transaction t2 = transactionRepository.save(Transaction.builder().type(TransactionType.EXPENSE).itemName("T2").amount(new BigDecimal("20000")).transactionDate(d1).build());
        Transaction t3 = transactionRepository.save(Transaction.builder().type(TransactionType.INCOME).itemName("T3").amount(new BigDecimal("30000")).transactionDate(d1).build());

        List<Transaction> list = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(list).hasSize(3);
        // d1 before d2; among d1, t2 id < t3 id
        assertThat(list.get(0).getId()).isEqualTo(t2.getId());
        assertThat(list.get(1).getId()).isEqualTo(t3.getId());
        assertThat(list.get(2).getId()).isEqualTo(t1.getId());
    }
}

