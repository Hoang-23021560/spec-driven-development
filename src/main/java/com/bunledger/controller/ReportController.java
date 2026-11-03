package com.bunledger.controller;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.MonthlySummaryDto;
import com.bunledger.model.Transaction;
import com.bunledger.service.CsvExportService;
import com.bunledger.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

/**
 * Controller xử lý màn hình Báo cáo tổng hợp theo tháng và chức năng Xuất dữ liệu CSV.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final TransactionService transactionService;
    private final CsvExportService csvExportService;

    /**
     * GET /reports : Hiển thị trang báo cáo tổng hợp theo tháng.
     * Nhận tham số `month` (định dạng YYYY-MM), mặc định lấy tháng hiện tại nếu không truyền.
     */
    @GetMapping("/reports")
    public String getMonthlyReport(
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            Model model) {

        YearMonth targetMonth = (month != null) ? month : YearMonth.now();
        YearMonth currentMonth = targetMonth;
        YearMonth prevMonth = targetMonth.minusMonths(1);
        YearMonth nextMonth = targetMonth.plusMonths(1);
        boolean isCurrentMonth = targetMonth.equals(YearMonth.now());

        MonthlySummaryDto summary = transactionService.getMonthlySummary(targetMonth);
        List<CategoryBreakdownDto> expenseBreakdown = transactionService.getMonthlyExpenseBreakdown(targetMonth);
        List<Transaction> transactions = transactionService.getTransactionsInMonth(targetMonth);

        model.addAttribute("summary", summary);
        model.addAttribute("expenseBreakdown", expenseBreakdown);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("prevMonth", prevMonth);
        model.addAttribute("nextMonth", nextMonth);
        model.addAttribute("isCurrentMonth", isCurrentMonth);

        log.info("Xem báo cáo tháng: {}, tổng thu: {}, tổng chi: {}, số giao dịch: {}",
                targetMonth, summary.getTotalIncome(), summary.getTotalExpense(), transactions.size());

        return "reports";
    }

    /**
     * GET /export/csv : Xuất danh sách giao dịch trong tháng ra file CSV chuẩn UTF-8 kèm BOM.
     * Đặt Content-Disposition attachment với tên file động `so_thu_chi_YYYY-MM.csv`.
     */
    @GetMapping("/export/csv")
    public void exportCsv(
            @RequestParam(name = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            HttpServletResponse response) throws IOException {

        YearMonth targetMonth = (month != null) ? month : YearMonth.now();
        log.info("Yêu cầu xuất CSV cho tháng: {}", targetMonth);

        List<Transaction> transactions = transactionService.getTransactionsInMonth(targetMonth);
        byte[] csvBytes = csvExportService.exportTransactionsToCsv(transactions);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"so_thu_chi_" + targetMonth + ".csv\"");
        response.getOutputStream().write(csvBytes);
        response.getOutputStream().flush();
    }
}
