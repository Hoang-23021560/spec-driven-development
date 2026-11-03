package com.bunledger.service;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.DailySummaryDto;
import com.bunledger.dto.MonthlySummaryDto;
import com.bunledger.dto.TransactionForm;
import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import com.bunledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @Nested
    @DisplayName("Tests for saveTransaction")
    class SaveTransactionTests {

        @Test
        @DisplayName("Should save transaction and auto-assign today when transactionDate is null")
        void testSaveTransaction_WithNullDate_ShouldAssignToday() {
            TransactionForm form = TransactionForm.builder()
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò tái")
                    .amount(new BigDecimal("350000"))
                    .description("Mua 3kg tại chợ")
                    .transactionDate(null)
                    .build();

            Transaction savedTx = Transaction.builder()
                    .id(1L)
                    .type(form.getType())
                    .itemName(form.getItemName())
                    .amount(form.getAmount())
                    .description(form.getDescription())
                    .transactionDate(today)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

            Transaction result = transactionService.saveTransaction(form);

            verify(transactionRepository, times(1)).save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertNotNull(captured);
            assertEquals(TransactionType.EXPENSE, captured.getType());
            assertEquals("Thịt bò tái", captured.getItemName());
            assertEquals(new BigDecimal("350000"), captured.getAmount());
            assertEquals("Mua 3kg tại chợ", captured.getDescription());
            assertEquals(today, captured.getTransactionDate());

            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should preserve custom transactionDate when provided in form")
        void testSaveTransaction_WithExplicitDate_ShouldPreserveDate() {
            LocalDate customDate = LocalDate.of(2026, 11, 1);
            TransactionForm form = TransactionForm.builder()
                    .type(TransactionType.INCOME)
                    .itemName("Bán 10 tô bún bò")
                    .amount(new BigDecimal("450000"))
                    .transactionDate(customDate)
                    .build();

            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Transaction result = transactionService.saveTransaction(form);

            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction captured = transactionCaptor.getValue();

            assertEquals(customDate, captured.getTransactionDate());
            assertEquals(customDate, result.getTransactionDate());
            assertEquals(TransactionType.INCOME, result.getType());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when form is null")
        void testSaveTransaction_WithNullForm_ShouldThrowException() {
            assertThrows(IllegalArgumentException.class, () -> transactionService.saveTransaction(null));
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Tests for getDailySummary")
    class GetDailySummaryTests {

        @Test
        @DisplayName("Should calculate netProfit correctly when income exceeds expense (profitable)")
        void testGetDailySummary_Profitable() {
            BigDecimal totalIncome = new BigDecimal("1250000");
            BigDecimal totalExpense = new BigDecimal("850000");

            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.INCOME), eq(today)))
                    .thenReturn(totalIncome);
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(totalExpense);

            DailySummaryDto summary = transactionService.getDailySummary(today);

            assertNotNull(summary);
            assertEquals(today, summary.getDate());
            assertEquals(new BigDecimal("1250000"), summary.getTotalIncome());
            assertEquals(new BigDecimal("850000"), summary.getTotalExpense());
            assertEquals(new BigDecimal("400000"), summary.getNetProfit());
            assertTrue(summary.isProfitable());
            assertEquals("1.250.000 đ", summary.getFormattedTotalIncome());
            assertEquals("850.000 đ", summary.getFormattedTotalExpense());
            assertEquals("400.000 đ", summary.getFormattedNetProfit());
        }

        @Test
        @DisplayName("Should calculate negative netProfit when expense exceeds income (loss)")
        void testGetDailySummary_Loss() {
            BigDecimal totalIncome = new BigDecimal("300000");
            BigDecimal totalExpense = new BigDecimal("500000");

            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.INCOME), eq(today)))
                    .thenReturn(totalIncome);
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(totalExpense);

            DailySummaryDto summary = transactionService.getDailySummary(today);

            assertNotNull(summary);
            assertEquals(new BigDecimal("-200000"), summary.getNetProfit());
            assertFalse(summary.isProfitable());
            assertEquals("-200.000 đ", summary.getFormattedNetProfit());
        }

        @Test
        @DisplayName("Should handle null repository returns gracefully as zero")
        void testGetDailySummary_WithNullSums_ShouldDefaultToZero() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.INCOME), eq(today)))
                    .thenReturn(null);
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(null);

            DailySummaryDto summary = transactionService.getDailySummary(today);

            assertNotNull(summary);
            assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
            assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
            assertEquals(BigDecimal.ZERO, summary.getNetProfit());
            assertTrue(summary.isProfitable());
        }

        @Test
        @DisplayName("Should default to today if date argument is null")
        void testGetDailySummary_WithNullDate_ShouldDefaultToToday() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.INCOME), eq(today)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(BigDecimal.ZERO);

            DailySummaryDto summary = transactionService.getDailySummary(null);

            assertNotNull(summary);
            assertEquals(today, summary.getDate());
            verify(transactionRepository).sumAmountByTypeAndDate(TransactionType.INCOME, today);
            verify(transactionRepository).sumAmountByTypeAndDate(TransactionType.EXPENSE, today);
        }
    }

    @Nested
    @DisplayName("Tests for getRecentTransactions")
    class GetRecentTransactionsTests {

        @Test
        @DisplayName("Should return transactions ordered by createdAt descending")
        void testGetRecentTransactions() {
            Transaction tx1 = Transaction.builder()
                    .id(2L)
                    .type(TransactionType.INCOME)
                    .itemName("Tô đặc biệt")
                    .amount(new BigDecimal("60000"))
                    .transactionDate(today)
                    .createdAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            Transaction tx2 = Transaction.builder()
                    .id(1L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Rau sống")
                    .amount(new BigDecimal("50000"))
                    .transactionDate(today)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findByTransactionDateOrderByCreatedAtDesc(today))
                    .thenReturn(List.of(tx1, tx2));

            List<Transaction> result = transactionService.getRecentTransactions(today);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Tô đặc biệt", result.get(0).getItemName());
            assertEquals("Rau sống", result.get(1).getItemName());
            verify(transactionRepository).findByTransactionDateOrderByCreatedAtDesc(today);
        }

        @Test
        @DisplayName("Should default to today if query date is null")
        void testGetRecentTransactions_NullDate_ShouldDefaultToToday() {
            when(transactionRepository.findByTransactionDateOrderByCreatedAtDesc(today))
                    .thenReturn(List.of());

            List<Transaction> result = transactionService.getRecentTransactions(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(transactionRepository).findByTransactionDateOrderByCreatedAtDesc(today);
        }
    }

    @Nested
    @DisplayName("Tests for deleteTransaction")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should call repository deleteById when id is provided")
        void testDeleteTransaction_ValidId() {
            transactionService.deleteTransaction(10L);
            verify(transactionRepository, times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("Should not call repository deleteById when id is null")
        void testDeleteTransaction_NullId() {
            transactionService.deleteTransaction(null);
            verify(transactionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Tests for getExpenseBreakdown")
    class GetExpenseBreakdownTests {

        @BeforeEach
        void setUpBreakdown() {
            lenient().when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.INCOME), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should calculate percentages and format amounts for multiple expense categories")
        void testGetExpenseBreakdown_MultipleCategories() {
            BigDecimal totalExpense = new BigDecimal("1000000");
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(totalExpense);

            List<Object[]> queryResults = List.of(
                    new Object[]{"Thịt bò tái", new BigDecimal("500000")},
                    new Object[]{"Bún tươi", new BigDecimal("300000")},
                    new Object[]{"Rau thơm", new BigDecimal("200000")}
            );
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertNotNull(breakdown);
            assertEquals(3, breakdown.size());

            CategoryBreakdownDto item1 = breakdown.get(0);
            assertEquals("Thịt bò tái", item1.getItemName());
            assertEquals(new BigDecimal("500000"), item1.getAmount());
            assertEquals(50.0, item1.getPercentage());
            assertEquals("500.000 đ", item1.getFormattedAmount());

            CategoryBreakdownDto item2 = breakdown.get(1);
            assertEquals("Bún tươi", item2.getItemName());
            assertEquals(new BigDecimal("300000"), item2.getAmount());
            assertEquals(30.0, item2.getPercentage());
            assertEquals("300.000 đ", item2.getFormattedAmount());

            CategoryBreakdownDto item3 = breakdown.get(2);
            assertEquals("Rau thơm", item3.getItemName());
            assertEquals(new BigDecimal("200000"), item3.getAmount());
            assertEquals(20.0, item3.getPercentage());
            assertEquals("200.000 đ", item3.getFormattedAmount());
        }

        @Test
        @DisplayName("Should round percentage to 1 decimal place using HALF_UP")
        void testGetExpenseBreakdown_RoundingToOneDecimalPlace() {
            BigDecimal totalExpense = new BigDecimal("300000");
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(totalExpense);

            List<Object[]> queryResults = List.of(
                    new Object[]{"Thịt bò", new BigDecimal("100000")}, // 100000/300000 * 100 = 33.333% -> 33.3%
                    new Object[]{"Bún", new BigDecimal("200000")}       // 200000/300000 * 100 = 66.666% -> 66.7%
            );
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertEquals(2, breakdown.size());
            assertEquals(33.3, breakdown.get(0).getPercentage());
            assertEquals(66.7, breakdown.get(1).getPercentage());
        }

        @Test
        @DisplayName("Should return 0.0 percentage when totalExpense is zero")
        void testGetExpenseBreakdown_ZeroTotalExpense() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(BigDecimal.ZERO);

            List<Object[]> queryResults = List.<Object[]>of(
                    new Object[]{"Khoản chi 0đ", BigDecimal.ZERO}
            );
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertEquals(1, breakdown.size());
            assertEquals(0.0, breakdown.get(0).getPercentage());
            assertEquals("0 đ", breakdown.get(0).getFormattedAmount());
        }

        @Test
        @DisplayName("Should return empty list when no expense categories found")
        void testGetExpenseBreakdown_EmptyResults() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(List.of());

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertNotNull(breakdown);
            assertTrue(breakdown.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when repository returns null")
        void testGetExpenseBreakdown_NullResults() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(null);

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertNotNull(breakdown);
            assertTrue(breakdown.isEmpty());
        }

        @Test
        @DisplayName("Should default to today when date argument is null")
        void testGetExpenseBreakdown_NullDate_ShouldDefaultToToday() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(List.of());

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(null);

            assertNotNull(breakdown);
            verify(transactionRepository).findCategoryBreakdown(TransactionType.EXPENSE, today);
        }

        @Test
        @DisplayName("Should safely handle non-BigDecimal number types in query results")
        void testGetExpenseBreakdown_NonBigDecimalAmounts() {
            when(transactionRepository.sumAmountByTypeAndDate(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(new BigDecimal("150000"));

            List<Object[]> queryResults = List.<Object[]>of(
                    new Object[]{"Tiền điện", 150000L}
            );
            when(transactionRepository.findCategoryBreakdown(eq(TransactionType.EXPENSE), eq(today)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(today);

            assertEquals(1, breakdown.size());
            assertEquals("Tiền điện", breakdown.get(0).getItemName());
            assertEquals(new BigDecimal("150000"), breakdown.get(0).getAmount());
            assertEquals(100.0, breakdown.get(0).getPercentage());
            assertEquals("150.000 đ", breakdown.get(0).getFormattedAmount());
        }
    }

    @Nested
    @DisplayName("Tests for getTransactionById")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return transaction when found by id")
        void testGetTransactionById_Found() {
            Transaction mockTx = Transaction.builder()
                    .id(5L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Bún tươi")
                    .amount(new BigDecimal("100000"))
                    .transactionDate(today)
                    .build();

            when(transactionRepository.findById(5L)).thenReturn(Optional.of(mockTx));

            Transaction result = transactionService.getTransactionById(5L);

            assertNotNull(result);
            assertEquals(5L, result.getId());
            assertEquals("Bún tươi", result.getItemName());
            verify(transactionRepository).findById(5L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when transaction is not found")
        void testGetTransactionById_NotFound() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.getTransactionById(999L));

            assertEquals("Không tìm thấy giao dịch với id: 999", ex.getMessage());
            verify(transactionRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when id is null")
        void testGetTransactionById_NullId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.getTransactionById(null));

            assertEquals("Không tìm thấy giao dịch với id: null", ex.getMessage());
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Tests for updateTransaction")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Should update transaction fields and preserve date when form date is null")
        void testUpdateTransaction_PreserveOriginalDate() {
            LocalDate originalDate = LocalDate.of(2026, 11, 1);
            LocalDateTime originalCreatedAt = LocalDateTime.now().minusHours(2);
            Transaction existingTx = Transaction.builder()
                    .id(10L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò gầu")
                    .amount(new BigDecimal("200000"))
                    .description("Mua buổi sáng")
                    .transactionDate(originalDate)
                    .createdAt(originalCreatedAt)
                    .build();

            when(transactionRepository.findById(10L)).thenReturn(Optional.of(existingTx));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TransactionForm updateForm = TransactionForm.builder()
                    .type(TransactionType.INCOME)
                    .itemName("Bán 20 tô bún bò")
                    .amount(new BigDecimal("800000"))
                    .description("Cập nhật lại loại giao dịch")
                    .transactionDate(null)
                    .build();

            Transaction updatedTx = transactionService.updateTransaction(10L, updateForm);

            verify(transactionRepository).findById(10L);
            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction saved = transactionCaptor.getValue();

            assertEquals(TransactionType.INCOME, saved.getType());
            assertEquals("Bán 20 tô bún bò", saved.getItemName());
            assertEquals(new BigDecimal("800000"), saved.getAmount());
            assertEquals("Cập nhật lại loại giao dịch", saved.getDescription());
            assertEquals(originalDate, saved.getTransactionDate());
            assertEquals(originalCreatedAt, saved.getCreatedAt());

            assertSame(existingTx, updatedTx);
        }

        @Test
        @DisplayName("Should update transactionDate when form provides a new date")
        void testUpdateTransaction_WithExplicitNewDate() {
            LocalDate originalDate = LocalDate.of(2026, 11, 1);
            LocalDate newDate = LocalDate.of(2026, 11, 3);
            Transaction existingTx = Transaction.builder()
                    .id(10L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Rau thơm")
                    .amount(new BigDecimal("50000"))
                    .transactionDate(originalDate)
                    .build();

            when(transactionRepository.findById(10L)).thenReturn(Optional.of(existingTx));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TransactionForm updateForm = TransactionForm.builder()
                    .type(TransactionType.EXPENSE)
                    .itemName("Rau sống & giá")
                    .amount(new BigDecimal("60000"))
                    .description("Mua bổ sung")
                    .transactionDate(newDate)
                    .build();

            Transaction updatedTx = transactionService.updateTransaction(10L, updateForm);

            assertEquals(newDate, updatedTx.getTransactionDate());
            assertEquals("Rau sống & giá", updatedTx.getItemName());
            assertEquals(new BigDecimal("60000"), updatedTx.getAmount());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when transaction id does not exist")
        void testUpdateTransaction_NotFound_ShouldThrowException() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            TransactionForm form = TransactionForm.builder()
                    .type(TransactionType.EXPENSE)
                    .itemName("Hành ngò")
                    .amount(new BigDecimal("20000"))
                    .build();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(999L, form));

            assertEquals("Không tìm thấy giao dịch với id: 999", ex.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when form is null")
        void testUpdateTransaction_NullForm_ShouldThrowException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> transactionService.updateTransaction(1L, null));

            assertEquals("Dữ liệu form giao dịch không được để trống", ex.getMessage());
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Tests for getMonthlySummary")
    class GetMonthlySummaryTests {

        private final YearMonth testMonth = YearMonth.of(2026, 11);
        private final LocalDate startDate = testMonth.atDay(1);
        private final LocalDate endDate = testMonth.atEndOfMonth();

        @Test
        @DisplayName("Should calculate monthly summary correctly when profitable")
        void testGetMonthlySummary_Profitable() {
            BigDecimal totalIncome = new BigDecimal("30000000");
            BigDecimal totalExpense = new BigDecimal("15000000");

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(totalIncome);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(totalExpense);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(25L);

            MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);

            assertNotNull(summary);
            assertEquals(testMonth, summary.getMonth());
            assertEquals(new BigDecimal("30000000"), summary.getTotalIncome());
            assertEquals(new BigDecimal("15000000"), summary.getTotalExpense());
            assertEquals(new BigDecimal("15000000"), summary.getNetProfit());
            assertEquals(25L, summary.getActiveDays());
            assertEquals(new BigDecimal("600000"), summary.getAverageDailyProfit());
            assertTrue(summary.isProfitable());
            assertEquals("30.000.000 đ", summary.getFormattedTotalIncome());
            assertEquals("15.000.000 đ", summary.getFormattedTotalExpense());
            assertEquals("15.000.000 đ", summary.getFormattedNetProfit());
            assertEquals("600.000 đ", summary.getFormattedAverageDailyProfit());
        }

        @Test
        @DisplayName("Should calculate negative net profit and daily profit on loss")
        void testGetMonthlySummary_Loss() {
            BigDecimal totalIncome = new BigDecimal("10000000");
            BigDecimal totalExpense = new BigDecimal("16000000");

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(totalIncome);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(totalExpense);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(20L);

            MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);

            assertNotNull(summary);
            assertEquals(new BigDecimal("-6000000"), summary.getNetProfit());
            assertEquals(new BigDecimal("-300000"), summary.getAverageDailyProfit());
            assertFalse(summary.isProfitable());
            assertEquals("-6.000.000 đ", summary.getFormattedNetProfit());
            assertEquals("-300.000 đ", summary.getFormattedAverageDailyProfit());
        }

        @Test
        @DisplayName("Should return 0 averageDailyProfit when activeDays is 0")
        void testGetMonthlySummary_ZeroActiveDays() {
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(0L);

            MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);

            assertNotNull(summary);
            assertEquals(0L, summary.getActiveDays());
            assertEquals(BigDecimal.ZERO, summary.getAverageDailyProfit());
            assertEquals("0 đ", summary.getFormattedAverageDailyProfit());
        }

        @Test
        @DisplayName("Should handle null repository returns as zero")
        void testGetMonthlySummary_NullSumsFromRepo() {
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(null);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(null);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(0L);

            MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);

            assertNotNull(summary);
            assertEquals(BigDecimal.ZERO, summary.getTotalIncome());
            assertEquals(BigDecimal.ZERO, summary.getTotalExpense());
            assertEquals(BigDecimal.ZERO, summary.getNetProfit());
            assertEquals(0L, summary.getActiveDays());
            assertEquals(BigDecimal.ZERO, summary.getAverageDailyProfit());
        }

        @Test
        @DisplayName("Should default to current month when month argument is null")
        void testGetMonthlySummary_NullMonth_DefaultsToCurrentMonth() {
            YearMonth now = YearMonth.now();
            LocalDate curStart = now.atDay(1);
            LocalDate curEnd = now.atEndOfMonth();

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(curStart), eq(curEnd)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(curStart), eq(curEnd)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countActiveDaysBetween(eq(curStart), eq(curEnd)))
                    .thenReturn(0L);

            MonthlySummaryDto summary = transactionService.getMonthlySummary(null);

            assertNotNull(summary);
            assertEquals(now, summary.getMonth());
            verify(transactionRepository).sumAmountByTypeAndDateRange(TransactionType.INCOME, curStart, curEnd);
            verify(transactionRepository).sumAmountByTypeAndDateRange(TransactionType.EXPENSE, curStart, curEnd);
            verify(transactionRepository).countActiveDaysBetween(curStart, curEnd);
        }
    }

    @Nested
    @DisplayName("Tests for getMonthlyExpenseBreakdown")
    class GetMonthlyExpenseBreakdownTests {

        private final YearMonth testMonth = YearMonth.of(2026, 11);
        private final LocalDate startDate = testMonth.atDay(1);
        private final LocalDate endDate = testMonth.atEndOfMonth();

        @Test
        @DisplayName("Should calculate monthly breakdown percentages and format amounts correctly")
        void testGetMonthlyExpenseBreakdown_MultipleCategories() {
            BigDecimal totalExpense = new BigDecimal("10000000");

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(totalExpense);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(10L);

            List<Object[]> queryResults = List.of(
                    new Object[]{"Thịt bò", new BigDecimal("6000000")},
                    new Object[]{"Bún tươi", new BigDecimal("2000000")},
                    new Object[]{"Gia vị", new BigDecimal("2000000")}
            );
            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);

            assertNotNull(breakdown);
            assertEquals(3, breakdown.size());

            assertEquals("Thịt bò", breakdown.get(0).getItemName());
            assertEquals(new BigDecimal("6000000"), breakdown.get(0).getAmount());
            assertEquals(60.0, breakdown.get(0).getPercentage());
            assertEquals("6.000.000 đ", breakdown.get(0).getFormattedAmount());

            assertEquals("Bún tươi", breakdown.get(1).getItemName());
            assertEquals(new BigDecimal("2000000"), breakdown.get(1).getAmount());
            assertEquals(20.0, breakdown.get(1).getPercentage());
            assertEquals("2.000.000 đ", breakdown.get(1).getFormattedAmount());

            assertEquals("Gia vị", breakdown.get(2).getItemName());
            assertEquals(new BigDecimal("2000000"), breakdown.get(2).getAmount());
            assertEquals(20.0, breakdown.get(2).getPercentage());
            assertEquals("2.000.000 đ", breakdown.get(2).getFormattedAmount());
        }

        @Test
        @DisplayName("Should round percentage to 1 decimal place using HALF_UP")
        void testGetMonthlyExpenseBreakdown_Rounding() {
            BigDecimal totalExpense = new BigDecimal("300000");

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(totalExpense);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(5L);

            List<Object[]> queryResults = List.of(
                    new Object[]{"Thịt bò", new BigDecimal("100000")}, // 33.3%
                    new Object[]{"Bún", new BigDecimal("200000")}       // 66.7%
            );
            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);

            assertEquals(2, breakdown.size());
            assertEquals(33.3, breakdown.get(0).getPercentage());
            assertEquals(66.7, breakdown.get(1).getPercentage());
        }

        @Test
        @DisplayName("Should return 0.0 percentage when total monthly expense is 0")
        void testGetMonthlyExpenseBreakdown_ZeroTotalExpense() {
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(0L);

            List<Object[]> queryResults = List.<Object[]>of(
                    new Object[]{"Chi 0đ", BigDecimal.ZERO}
            );
            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(queryResults);

            List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);

            assertEquals(1, breakdown.size());
            assertEquals(0.0, breakdown.get(0).getPercentage());
        }

        @Test
        @DisplayName("Should return empty list when repository returns empty or null")
        void testGetMonthlyExpenseBreakdown_EmptyOrNull() {
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countActiveDaysBetween(eq(startDate), eq(endDate)))
                    .thenReturn(0L);

            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(List.of());

            List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);
            assertTrue(breakdown.isEmpty());

            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(startDate), eq(endDate)))
                    .thenReturn(null);
            List<CategoryBreakdownDto> nullBreakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);
            assertTrue(nullBreakdown.isEmpty());
        }

        @Test
        @DisplayName("Should default to current month when month is null")
        void testGetMonthlyExpenseBreakdown_NullMonth() {
            YearMonth now = YearMonth.now();
            LocalDate curStart = now.atDay(1);
            LocalDate curEnd = now.atEndOfMonth();

            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.INCOME), eq(curStart), eq(curEnd)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumAmountByTypeAndDateRange(eq(TransactionType.EXPENSE), eq(curStart), eq(curEnd)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countActiveDaysBetween(eq(curStart), eq(curEnd)))
                    .thenReturn(0L);
            when(transactionRepository.findCategoryBreakdownBetween(eq(TransactionType.EXPENSE), eq(curStart), eq(curEnd)))
                    .thenReturn(List.of());

            List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(null);
            assertNotNull(breakdown);
            verify(transactionRepository).findCategoryBreakdownBetween(TransactionType.EXPENSE, curStart, curEnd);
        }
    }

    @Nested
    @DisplayName("Tests for getTransactionsInMonth")
    class GetTransactionsInMonthTests {

        private final YearMonth testMonth = YearMonth.of(2026, 11);
        private final LocalDate startDate = testMonth.atDay(1);
        private final LocalDate endDate = testMonth.atEndOfMonth();

        @Test
        @DisplayName("Should return transactions between start and end of month")
        void testGetTransactionsInMonth_ReturnsList() {
            Transaction tx1 = Transaction.builder().id(1L).itemName("Tô bún").amount(new BigDecimal("35000")).transactionDate(startDate).build();
            Transaction tx2 = Transaction.builder().id(2L).itemName("Thịt bò").amount(new BigDecimal("200000")).transactionDate(endDate).build();

            when(transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(startDate, endDate))
                    .thenReturn(List.of(tx1, tx2));

            List<Transaction> result = transactionService.getTransactionsInMonth(testMonth);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
            verify(transactionRepository).findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(startDate, endDate);
        }

        @Test
        @DisplayName("Should default to current month when month parameter is null")
        void testGetTransactionsInMonth_NullMonth() {
            YearMonth now = YearMonth.now();
            LocalDate curStart = now.atDay(1);
            LocalDate curEnd = now.atEndOfMonth();

            when(transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(curStart, curEnd))
                    .thenReturn(List.of());

            List<Transaction> result = transactionService.getTransactionsInMonth(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(transactionRepository).findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(curStart, curEnd);
        }
    }
}
