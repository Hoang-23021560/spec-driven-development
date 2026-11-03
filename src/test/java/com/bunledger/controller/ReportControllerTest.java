package com.bunledger.controller;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.MonthlySummaryDto;
import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import com.bunledger.service.CsvExportService;
import com.bunledger.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ReportController.class)
@DisplayName("ReportController WebMvc Tests")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private CsvExportService csvExportService;

    @Nested
    @DisplayName("GET /reports (Monthly Report Page)")
    class GetMonthlyReportTests {

        @Test
        @DisplayName("GET /reports không truyền tháng: mặc định nạp tháng hiện tại và trả về view 'reports'")
        void testGetReportsDefaultCurrentMonth() throws Exception {
            YearMonth currentMonth = YearMonth.now();

            MonthlySummaryDto mockSummary = MonthlySummaryDto.builder()
                    .month(currentMonth)
                    .totalIncome(BigDecimal.valueOf(15000000))
                    .totalExpense(BigDecimal.valueOf(6000000))
                    .netProfit(BigDecimal.valueOf(9000000))
                    .activeDays(20)
                    .averageDailyProfit(BigDecimal.valueOf(450000))
                    .build();

            List<CategoryBreakdownDto> mockBreakdown = List.of(
                    CategoryBreakdownDto.builder()
                            .itemName("Thịt bò")
                            .amount(BigDecimal.valueOf(4000000))
                            .percentage(66.7)
                            .formattedAmount("4.000.000 đ")
                            .build()
            );

            Transaction mockTx = Transaction.builder()
                    .id(1L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò")
                    .amount(BigDecimal.valueOf(4000000))
                    .transactionDate(currentMonth.atDay(1))
                    .build();

            when(transactionService.getMonthlySummary(currentMonth)).thenReturn(mockSummary);
            when(transactionService.getMonthlyExpenseBreakdown(currentMonth)).thenReturn(mockBreakdown);
            when(transactionService.getTransactionsInMonth(currentMonth)).thenReturn(List.of(mockTx));

            mockMvc.perform(get("/reports"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reports"))
                    .andExpect(model().attributeExists("summary"))
                    .andExpect(model().attributeExists("expenseBreakdown"))
                    .andExpect(model().attributeExists("transactions"))
                    .andExpect(model().attribute("currentMonth", currentMonth))
                    .andExpect(model().attribute("prevMonth", currentMonth.minusMonths(1)))
                    .andExpect(model().attribute("nextMonth", currentMonth.plusMonths(1)))
                    .andExpect(model().attribute("isCurrentMonth", true))
                    .andExpect(model().attribute("summary", mockSummary))
                    .andExpect(model().attribute("expenseBreakdown", hasSize(1)))
                    .andExpect(model().attribute("transactions", hasSize(1)))
                    .andExpect(content().string(containsString("BÁO CÁO TÀI CHÍNH THÁNG")))
                    .andExpect(content().string(containsString("Sổ Hàng Ngày")))
                    .andExpect(content().string(containsString("XUẤT FILE CSV (EXCEL)")))
                    .andExpect(content().string(containsString("Cơ Cấu Chi Tiêu Tháng")))
                    .andExpect(content().string(containsString("Thịt bò")))
                    .andExpect(content().string(containsString("15.000.000 đ")))
                    .andExpect(content().string(containsString("6.000.000 đ")))
                    .andExpect(content().string(containsString("9.000.000 đ")))
                    .andExpect(content().string(containsString("20 ngày bán")));

            verify(transactionService).getMonthlySummary(currentMonth);
            verify(transactionService).getMonthlyExpenseBreakdown(currentMonth);
            verify(transactionService).getTransactionsInMonth(currentMonth);
        }

        @Test
        @DisplayName("GET /reports?month=2026-10: nạp đúng tháng chỉ định, tính đúng prevMonth, nextMonth, isCurrentMonth=false")
        void testGetReportsWithCustomMonth() throws Exception {
            YearMonth customMonth = YearMonth.of(2026, 10);

            MonthlySummaryDto mockSummary = MonthlySummaryDto.builder()
                    .month(customMonth)
                    .totalIncome(BigDecimal.valueOf(10000000))
                    .totalExpense(BigDecimal.valueOf(4000000))
                    .netProfit(BigDecimal.valueOf(6000000))
                    .activeDays(15)
                    .averageDailyProfit(BigDecimal.valueOf(400000))
                    .build();

            when(transactionService.getMonthlySummary(customMonth)).thenReturn(mockSummary);
            when(transactionService.getMonthlyExpenseBreakdown(customMonth)).thenReturn(Collections.emptyList());
            when(transactionService.getTransactionsInMonth(customMonth)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/reports").param("month", "2026-10"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reports"))
                    .andExpect(model().attribute("currentMonth", customMonth))
                    .andExpect(model().attribute("prevMonth", YearMonth.of(2026, 9)))
                    .andExpect(model().attribute("nextMonth", YearMonth.of(2026, 11)))
                    .andExpect(model().attribute("isCurrentMonth", customMonth.equals(YearMonth.now())))
                    .andExpect(model().attribute("summary", mockSummary))
                    .andExpect(model().attribute("expenseBreakdown", hasSize(0)))
                    .andExpect(model().attribute("transactions", hasSize(0)))
                    .andExpect(content().string(containsString("Tháng 10/2026")))
                    .andExpect(content().string(containsString("Tháng này chưa phát sinh chi phí nào")))
                    .andExpect(content().string(containsString("Chưa có giao dịch nào trong tháng này")))
                    .andExpect(content().string(containsString("10.000.000 đ")))
                    .andExpect(content().string(containsString("4.000.000 đ")))
                    .andExpect(content().string(containsString("6.000.000 đ")))
                    .andExpect(content().string(containsString("15 ngày bán")));

            verify(transactionService).getMonthlySummary(customMonth);
            verify(transactionService).getMonthlyExpenseBreakdown(customMonth);
            verify(transactionService).getTransactionsInMonth(customMonth);
        }
    }

    @Nested
    @DisplayName("GET /export/csv (CSV Export Endpoint)")
    class ExportCsvTests {

        @Test
        @DisplayName("GET /export/csv không truyền tháng: xuất CSV tháng hiện tại, Content-Type, Content-Disposition đúng chuẩn")
        void testExportCsvDefaultCurrentMonth() throws Exception {
            YearMonth currentMonth = YearMonth.now();

            Transaction tx = Transaction.builder()
                    .id(1L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò tươi")
                    .amount(BigDecimal.valueOf(350000))
                    .description("Mua chợ sớm")
                    .transactionDate(currentMonth.atDay(1))
                    .createdAt(LocalDateTime.of(currentMonth.getYear(), currentMonth.getMonthValue(), 1, 6, 30))
                    .build();

            List<Transaction> transactions = List.of(tx);
            byte[] mockCsvBytes = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'M', 'a', ' ', 'G', 'D'};

            when(transactionService.getTransactionsInMonth(currentMonth)).thenReturn(transactions);
            when(csvExportService.exportTransactionsToCsv(transactions)).thenReturn(mockCsvBytes);

            byte[] responseBytes = mockMvc.perform(get("/export/csv"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("text/csv")))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"so_thu_chi_" + currentMonth + ".csv\""))
                    .andReturn().getResponse().getContentAsByteArray();

            assertThat(responseBytes).isEqualTo(mockCsvBytes);

            verify(transactionService).getTransactionsInMonth(currentMonth);
            verify(csvExportService).exportTransactionsToCsv(transactions);
        }

        @Test
        @DisplayName("GET /export/csv?month=2026-10: xuất CSV theo tháng chỉ định, tên file so_thu_chi_2026-10.csv")
        void testExportCsvWithCustomMonth() throws Exception {
            YearMonth customMonth = YearMonth.of(2026, 10);
            byte[] mockCsvBytes = ("\uFEFF" + CsvExportService.CSV_HEADER).getBytes(StandardCharsets.UTF_8);

            when(transactionService.getTransactionsInMonth(customMonth)).thenReturn(Collections.emptyList());
            when(csvExportService.exportTransactionsToCsv(Collections.emptyList())).thenReturn(mockCsvBytes);

            byte[] responseBytes = mockMvc.perform(get("/export/csv").param("month", "2026-10"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", containsString("text/csv")))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"so_thu_chi_2026-10.csv\""))
                    .andReturn().getResponse().getContentAsByteArray();

            assertThat(responseBytes).isEqualTo(mockCsvBytes);

            verify(transactionService).getTransactionsInMonth(customMonth);
            verify(csvExportService).exportTransactionsToCsv(Collections.emptyList());
        }
    }
}
