package com.bunledger.controller;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.DailySummaryDto;
import com.bunledger.dto.TransactionForm;
import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import com.bunledger.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController WebMvc Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Nested
    @DisplayName("GET / (Index Page & Date Navigation)")
    class IndexPageTests {

        @Test
        @DisplayName("GET / thành công trả về view 'index' và các model attributes điều hướng, tóm tắt, biểu đồ")
        void testGetIndexSuccess() throws Exception {
            LocalDate today = LocalDate.now();
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(today)
                    .totalIncome(BigDecimal.valueOf(1000000))
                    .totalExpense(BigDecimal.valueOf(400000))
                    .netProfit(BigDecimal.valueOf(600000))
                    .build();

            Transaction mockTx = Transaction.builder()
                    .id(1L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò tươi")
                    .amount(BigDecimal.valueOf(400000))
                    .transactionDate(today)
                    .build();

            when(transactionService.getDailySummary(today)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(today)).thenReturn(List.of(mockTx));
            when(transactionService.getExpenseBreakdown(today)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("summary"))
                    .andExpect(model().attributeExists("transactions"))
                    .andExpect(model().attributeExists("transactionForm"))
                    .andExpect(model().attribute("currentDate", today))
                    .andExpect(model().attribute("today", today))
                    .andExpect(model().attribute("isToday", true))
                    .andExpect(model().attribute("prevDate", today.minusDays(1)))
                    .andExpect(model().attribute("nextDate", today.plusDays(1)))
                    .andExpect(model().attribute("chartIncome", BigDecimal.valueOf(1000000)))
                    .andExpect(model().attribute("chartExpense", BigDecimal.valueOf(400000)))
                    .andExpect(model().attributeExists("expenseBreakdown"))
                    .andExpect(model().attributeExists("chartExpenseLabels"))
                    .andExpect(model().attributeExists("chartExpenseAmounts"))
                    .andExpect(model().attributeExists("chartExpensePercentages"))
                    .andExpect(model().attributeExists("chartExpenseLabelsJson"))
                    .andExpect(model().attributeExists("chartExpenseAmountsJson"))
                    .andExpect(model().attributeExists("chartExpensePercentagesJson"))
                    .andExpect(model().attributeExists("expenseBreakdownJson"))
                    .andExpect(model().attribute("transactions", hasSize(1)))
                    .andExpect(content().string(containsString("Báo Cáo Tháng")))
                    .andExpect(content().string(containsString("/reports")));

            verify(transactionService).getDailySummary(today);
            verify(transactionService).getRecentTransactions(today);
            verify(transactionService).getExpenseBreakdown(today);
        }

        @Test
        @DisplayName("GET /?date=2026-11-01 lọc theo ngày cụ thể, tính đúng prevDate, nextDate, isToday=false")
        void testGetIndexWithCustomDate() throws Exception {
            LocalDate customDate = LocalDate.of(2026, 11, 1);
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(customDate)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build();

            when(transactionService.getDailySummary(customDate)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(customDate)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(customDate)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/").param("date", "2026-11-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("currentDate", customDate))
                    .andExpect(model().attribute("today", LocalDate.now()))
                    .andExpect(model().attribute("isToday", false))
                    .andExpect(model().attribute("prevDate", LocalDate.of(2026, 10, 31)))
                    .andExpect(model().attribute("nextDate", LocalDate.of(2026, 11, 2)))
                    .andExpect(model().attribute("summary", mockSummary))
                    .andExpect(model().attribute("transactions", hasSize(0)))
                    .andExpect(model().attributeExists("expenseBreakdown"));

            verify(transactionService).getDailySummary(customDate);
            verify(transactionService).getRecentTransactions(customDate);
            verify(transactionService).getExpenseBreakdown(customDate);
        }

        @Test
        @DisplayName("GET /transactions?date=2026-11-01 hoạt động đồng bộ với GET /")
        void testGetTransactionsWithCustomDate() throws Exception {
            LocalDate customDate = LocalDate.of(2026, 11, 1);
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(customDate)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build();

            when(transactionService.getDailySummary(customDate)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(customDate)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(customDate)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/transactions").param("date", "2026-11-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("currentDate", customDate))
                    .andExpect(model().attribute("isToday", false));

            verify(transactionService).getDailySummary(customDate);
            verify(transactionService).getRecentTransactions(customDate);
            verify(transactionService).getExpenseBreakdown(customDate);
        }

        @Test
        @DisplayName("GET / với dữ liệu phân tích chi phí: nạp đúng chart labels, amounts và JSON")
        void testGetIndexWithExpenseBreakdownData() throws Exception {
            LocalDate today = LocalDate.now();
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(today)
                    .totalIncome(BigDecimal.valueOf(1500000))
                    .totalExpense(BigDecimal.valueOf(500000))
                    .netProfit(BigDecimal.valueOf(1000000))
                    .build();

            List<CategoryBreakdownDto> breakdown = List.of(
                    CategoryBreakdownDto.builder()
                            .itemName("Thịt bò tươi")
                            .amount(BigDecimal.valueOf(350000))
                            .percentage(70.0)
                            .formattedAmount("350.000 đ")
                            .build(),
                    CategoryBreakdownDto.builder()
                            .itemName("Bún tươi")
                            .amount(BigDecimal.valueOf(150000))
                            .percentage(30.0)
                            .formattedAmount("150.000 đ")
                            .build()
            );

            when(transactionService.getDailySummary(today)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(today)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(today)).thenReturn(breakdown);

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("expenseBreakdown", hasSize(2)))
                    .andExpect(model().attribute("chartExpenseLabels", List.of("Thịt bò tươi", "Bún tươi")))
                    .andExpect(model().attribute("chartExpenseAmounts", List.of(BigDecimal.valueOf(350000), BigDecimal.valueOf(150000))))
                    .andExpect(model().attribute("chartExpensePercentages", List.of(70.0, 30.0)))
                    .andExpect(model().attribute("chartExpenseLabelsJson", containsString("Thịt bò tươi")))
                    .andExpect(model().attribute("chartExpenseAmountsJson", containsString("350000")));
        }
    }

    @Nested
    @DisplayName("POST /transactions/save (Save Transaction & Date Preservation)")
    class SaveTransactionTests {

        @Test
        @DisplayName("POST /transactions/save hợp lệ không truyền ngày: lưu thành công, redirect bảo toàn ngày hôm nay")
        void testSaveTransactionValid() throws Exception {
            LocalDate today = LocalDate.now();

            mockMvc.perform(post("/transactions/save")
                            .param("type", "EXPENSE")
                            .param("itemName", "Thịt bò")
                            .param("amount", "300000")
                            .param("description", "Mua 1kg thịt bò"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + today))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(transactionService).saveTransaction(any(TransactionForm.class));
        }

        @Test
        @DisplayName("POST /transactions/save với ngày cụ thể: lưu thành công, redirect bảo toàn đúng ngày đã chọn")
        void testSaveTransactionWithSpecificDate() throws Exception {
            LocalDate targetDate = LocalDate.of(2026, 11, 1);

            mockMvc.perform(post("/transactions/save")
                            .param("type", "EXPENSE")
                            .param("itemName", "Bún sợi to")
                            .param("amount", "120000")
                            .param("description", "Nhập bổ sung bún hôm qua")
                            .param("transactionDate", "2026-11-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + targetDate))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(transactionService).saveTransaction(any(TransactionForm.class));
        }

        @Test
        @DisplayName("POST /transactions/save không hợp lệ (amount <= 0 và itemName trống): nạp lại dữ liệu Model, trả về view 'index'")
        void testSaveTransactionInvalid() throws Exception {
            LocalDate today = LocalDate.now();
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(today)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build();

            when(transactionService.getDailySummary(today)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(today)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(today)).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/transactions/save")
                            .param("type", "EXPENSE")
                            .param("itemName", "")
                            .param("amount", "0"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeHasFieldErrors("transactionForm", "itemName"))
                    .andExpect(model().attributeHasFieldErrors("transactionForm", "amount"))
                    .andExpect(model().attributeExists("summary"))
                    .andExpect(model().attributeExists("transactions"))
                    .andExpect(model().attributeExists("expenseBreakdown"))
                    .andExpect(model().attribute("currentDate", today))
                    .andExpect(model().attribute("today", today))
                    .andExpect(model().attribute("isToday", true));

            verify(transactionService, never()).saveTransaction(any(TransactionForm.class));
            verify(transactionService).getDailySummary(today);
            verify(transactionService).getRecentTransactions(today);
            verify(transactionService).getExpenseBreakdown(today);
        }

        @Test
        @DisplayName("POST /transactions/save không hợp lệ với ngày cụ thể: nạp lại Model của đúng ngày đó")
        void testSaveTransactionInvalidWithCustomDate() throws Exception {
            LocalDate customDate = LocalDate.of(2026, 11, 1);
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(customDate)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build();

            when(transactionService.getDailySummary(customDate)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(customDate)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(customDate)).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/transactions/save")
                            .param("type", "EXPENSE")
                            .param("itemName", "")
                            .param("amount", "-50000")
                            .param("transactionDate", "2026-11-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().hasErrors())
                    .andExpect(model().attribute("currentDate", customDate))
                    .andExpect(model().attribute("isToday", false));

            verify(transactionService, never()).saveTransaction(any(TransactionForm.class));
            verify(transactionService).getDailySummary(customDate);
            verify(transactionService).getRecentTransactions(customDate);
            verify(transactionService).getExpenseBreakdown(customDate);
        }

        @Test
        @DisplayName("POST /transactions/save thiếu trường bắt buộc: trả về view 'index' và không gọi service")
        void testSaveTransactionMissingRequiredFields() throws Exception {
            LocalDate today = LocalDate.now();
            when(transactionService.getDailySummary(today)).thenReturn(DailySummaryDto.builder().date(today).build());
            when(transactionService.getRecentTransactions(today)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(today)).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/transactions/save"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().hasErrors());

            verify(transactionService, never()).saveTransaction(any(TransactionForm.class));
        }
    }

    @Nested
    @DisplayName("POST /transactions/{id}/delete (Delete Transaction & Date Preservation)")
    class DeleteTransactionTests {

        @Test
        @DisplayName("POST /transactions/{id}/delete không truyền ngày: xóa thành công, redirect về ngày hôm nay")
        void testDeleteTransactionSuccess() throws Exception {
            Long txId = 42L;
            LocalDate today = LocalDate.now();

            mockMvc.perform(post("/transactions/{id}/delete", txId))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + today))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(transactionService).deleteTransaction(txId);
        }

        @Test
        @DisplayName("POST /transactions/{id}/delete?date=2026-11-01: xóa thành công, redirect bảo toàn đúng ngày đang xem")
        void testDeleteTransactionWithDateParam() throws Exception {
            Long txId = 42L;
            LocalDate customDate = LocalDate.of(2026, 11, 1);

            mockMvc.perform(post("/transactions/{id}/delete", txId)
                            .param("date", "2026-11-01"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + customDate))
                    .andExpect(flash().attributeExists("successMessage"));

            verify(transactionService).deleteTransaction(txId);
        }
    }

    @Nested
    @DisplayName("POST /transactions/{id}/update (Update Transaction & Date Preservation)")
    class UpdateTransactionTests {

        @Test
        @DisplayName("POST /transactions/{id}/update hợp lệ có date param: cập nhật thành công, redirect bảo toàn ngày")
        void testUpdateTransactionValidWithDateParam() throws Exception {
            Long txId = 15L;
            LocalDate targetDate = LocalDate.of(2026, 11, 2);

            mockMvc.perform(post("/transactions/{id}/update", txId)
                            .param("date", "2026-11-02")
                            .param("type", "INCOME")
                            .param("itemName", "Bún đặc biệt tô lớn")
                            .param("amount", "65000")
                            .param("description", "Khách gọi thêm chả"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + targetDate))
                    .andExpect(flash().attribute("successMessage", "Đã cập nhật giao dịch thành công!"));

            verify(transactionService).updateTransaction(eq(txId), any(TransactionForm.class));
        }

        @Test
        @DisplayName("POST /transactions/{id}/update hợp lệ không có date param nhưng có transactionDate trong form")
        void testUpdateTransactionValidWithFormDate() throws Exception {
            Long txId = 15L;
            LocalDate formDate = LocalDate.of(2026, 11, 3);

            mockMvc.perform(post("/transactions/{id}/update", txId)
                            .param("type", "EXPENSE")
                            .param("itemName", "Hành lá")
                            .param("amount", "25000")
                            .param("transactionDate", "2026-11-03"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + formDate))
                    .andExpect(flash().attribute("successMessage", "Đã cập nhật giao dịch thành công!"));

            verify(transactionService).updateTransaction(eq(txId), any(TransactionForm.class));
        }

        @Test
        @DisplayName("POST /transactions/{id}/update không hợp lệ (tên mặt hàng trống): redirect bảo toàn ngày kèm errorMessage flash")
        void testUpdateTransactionInvalidBlankItemName() throws Exception {
            Long txId = 15L;
            LocalDate targetDate = LocalDate.of(2026, 11, 2);

            mockMvc.perform(post("/transactions/{id}/update", txId)
                            .param("date", "2026-11-02")
                            .param("type", "EXPENSE")
                            .param("itemName", "")
                            .param("amount", "50000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + targetDate))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", containsString("Dữ liệu cập nhật không hợp lệ:")));

            verify(transactionService, never()).updateTransaction(any(), any());
        }

        @Test
        @DisplayName("POST /transactions/{id}/update không hợp lệ (số tiền âm hoặc 0): redirect bảo toàn ngày hôm nay nếu không có date")
        void testUpdateTransactionInvalidNonPositiveAmount() throws Exception {
            Long txId = 15L;
            LocalDate today = LocalDate.now();

            mockMvc.perform(post("/transactions/{id}/update", txId)
                            .param("type", "EXPENSE")
                            .param("itemName", "Thịt bò")
                            .param("amount", "0"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + today))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", containsString("Dữ liệu cập nhật không hợp lệ:")));

            verify(transactionService, never()).updateTransaction(any(), any());
        }

        @Test
        @DisplayName("POST /transactions/{id}/update khi service ném ngoại lệ không tìm thấy id: redirect bảo toàn ngày và nạp errorMessage flash")
        void testUpdateTransactionServiceThrowsNotFound() throws Exception {
            Long txId = 999L;
            LocalDate targetDate = LocalDate.of(2026, 11, 1);

            when(transactionService.updateTransaction(eq(txId), any(TransactionForm.class)))
                    .thenThrow(new IllegalArgumentException("Không tìm thấy giao dịch với id: 999"));

            mockMvc.perform(post("/transactions/{id}/update", txId)
                            .param("date", "2026-11-01")
                            .param("type", "INCOME")
                            .param("itemName", "Bún tái")
                            .param("amount", "50000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/?date=" + targetDate))
                    .andExpect(flash().attribute("errorMessage", "Không tìm thấy giao dịch với id: 999"));

            verify(transactionService).updateTransaction(eq(txId), any(TransactionForm.class));
        }
    }

    @Nested
    @DisplayName("Task Group 3: View Thymeleaf + Chart.js Integration Tests")
    class ViewThymeleafAndChartJsTests {

        @Test
        @DisplayName("Giao diện HTML nhúng thư viện Chart.js qua CDN và có Date Navigation Bar hoàn chỉnh")
        void testIndexContainsChartJsAndDateNavigation() throws Exception {
            LocalDate customDate = LocalDate.of(2026, 11, 1);
            LocalDate today = LocalDate.now();
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(customDate)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO)
                    .build();

            when(transactionService.getDailySummary(customDate)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(customDate)).thenReturn(Collections.emptyList());
            when(transactionService.getExpenseBreakdown(customDate)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/").param("date", "2026-11-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    // 1. Nhúng Chart.js CDN
                    .andExpect(content().string(containsString("https://cdn.jsdelivr.net/npm/chart.js")))
                    // 2. Nút [◀] lùi 1 ngày và [▶] tiến 1 ngày
                    .andExpect(content().string(containsString("[◀]")))
                    .andExpect(content().string(containsString("/?date=2026-10-31")))
                    .andExpect(content().string(containsString("[▶]")))
                    .andExpect(content().string(containsString("/?date=2026-11-02")))
                    // 3. Ô input date native với onchange điều hướng
                    .andExpect(content().string(containsString("type=\"date\"")))
                    .andExpect(content().string(containsString("window.location.href='/?date=' + this.value")))
                    // 4. Nút nhanh 'Hôm nay' khi đang ở ngày khác (!isToday)
                    .andExpect(content().string(containsString("/?date=" + today)))
                    .andExpect(content().string(containsString("Hôm nay")))
                    // 5. Zero-state khi chưa có dữ liệu thu chi ngày này
                    .andExpect(content().string(containsString("Chưa có giao dịch nào trong ngày này")))
                    // 6. 2 Tab chuyển đổi biểu đồ
                    .andExpect(content().string(containsString("Thu vs Chi")))
                    .andExpect(content().string(containsString("Cơ Cấu Chi Phí")))
                    // 7. Hidden field transactionDate trong form nhập nhanh
                    .andExpect(content().string(containsString("name=\"transactionDate\"")))
                    .andExpect(content().string(containsString("value=\"2026-11-01\"")));
        }

        @Test
        @DisplayName("Giao diện HTML khi có dữ liệu: hiển thị canvas biểu đồ và form xóa kèm query param date")
        void testIndexContainsChartsAndPreservedDeleteDate() throws Exception {
            LocalDate customDate = LocalDate.of(2026, 11, 2);
            DailySummaryDto mockSummary = DailySummaryDto.builder()
                    .date(customDate)
                    .totalIncome(BigDecimal.valueOf(1000000))
                    .totalExpense(BigDecimal.valueOf(350000))
                    .netProfit(BigDecimal.valueOf(650000))
                    .build();

            Transaction mockTx = Transaction.builder()
                    .id(99L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò tươi")
                    .amount(BigDecimal.valueOf(350000))
                    .transactionDate(customDate)
                    .build();

            List<CategoryBreakdownDto> breakdown = List.of(
                    CategoryBreakdownDto.builder()
                            .itemName("Thịt bò tươi")
                            .amount(BigDecimal.valueOf(350000))
                            .percentage(100.0)
                            .formattedAmount("350.000 đ")
                            .build()
            );

            when(transactionService.getDailySummary(customDate)).thenReturn(mockSummary);
            when(transactionService.getRecentTransactions(customDate)).thenReturn(List.of(mockTx));
            when(transactionService.getExpenseBreakdown(customDate)).thenReturn(breakdown);

            mockMvc.perform(get("/").param("date", "2026-11-02"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    // Canvas cho cả 2 biểu đồ
                    .andExpect(content().string(containsString("id=\"incomeExpenseChart\"")))
                    .andExpect(content().string(containsString("id=\"expenseBreakdownChart\"")))
                    // Form xóa bảo toàn ngày đang xem
                    .andExpect(content().string(containsString("action=\"/transactions/99/delete?date=2026-11-02\"")))
                    // Chú giải mặt hàng
                    .andExpect(content().string(containsString("Thịt bò tươi")))
                    .andExpect(content().string(containsString("350.000 đ")));
        }
    }
}
