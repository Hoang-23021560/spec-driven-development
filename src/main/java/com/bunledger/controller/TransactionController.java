package com.bunledger.controller;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.DailySummaryDto;
import com.bunledger.dto.TransactionForm;
import com.bunledger.model.Transaction;
import com.bunledger.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring MVC Controller xử lý các yêu cầu thu/chi và điều hướng ngày cho BunLedger.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    /**
     * GET / hoặc GET /transactions: Hiển thị sổ thu chi của ngày được chọn (mặc định hôm nay).
     */
    @GetMapping({"/", "/transactions"})
    public String index(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        populateModel(model, targetDate);

        if (!model.containsAttribute("transactionForm")) {
            TransactionForm form = new TransactionForm();
            form.setTransactionDate(targetDate);
            model.addAttribute("transactionForm", form);
        }

        return "index";
    }

    /**
     * POST /transactions/save : Nhận TransactionForm, validate, gọi service lưu và redirect bảo toàn ngày.
     * Nếu có lỗi validation, nạp lại toàn bộ dữ liệu ngày targetDate vào Model và trả về view "index".
     */
    @PostMapping("/transactions/save")
    public String saveTransaction(
            @Valid @ModelAttribute("transactionForm") TransactionForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        LocalDate targetDate = (form.getTransactionDate() != null) ? form.getTransactionDate() : LocalDate.now();

        if (bindingResult.hasErrors()) {
            log.warn("Dữ liệu form giao dịch không hợp lệ: {}", bindingResult.getAllErrors());
            if (form.getTransactionDate() == null) {
                form.setTransactionDate(targetDate);
            }
            populateModel(model, targetDate);
            return "index";
        }

        transactionService.saveTransaction(form);

        String typeDisplay = (form.getType() != null) ? form.getType().getDisplayName().toLowerCase() : "giao dịch";
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu khoản " + typeDisplay + " thành công!");

        return "redirect:/?date=" + targetDate;
    }

    /**
     * POST /transactions/{id}/update : Nhận thông tin cập nhật giao dịch, validate, gọi service và redirect bảo toàn ngày.
     */
    @PostMapping("/transactions/{id}/update")
    public String updateTransaction(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("transactionForm") TransactionForm form,
            BindingResult bindingResult,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes,
            Model model) {

        LocalDate targetDate = (date != null) ? date : (form.getTransactionDate() != null ? form.getTransactionDate() : LocalDate.now());

        if (bindingResult.hasErrors()) {
            String defaultMessage = bindingResult.getAllErrors().isEmpty()
                    ? "Dữ liệu không hợp lệ"
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            log.warn("Dữ liệu cập nhật không hợp lệ cho giao dịch id {}: {}", id, defaultMessage);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu cập nhật không hợp lệ: " + defaultMessage);
            return "redirect:/?date=" + targetDate;
        }

        try {
            transactionService.updateTransaction(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giao dịch thành công!");
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi khi cập nhật giao dịch id {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/?date=" + targetDate;
    }

    /**
     * POST /transactions/{id}/delete : Nhận yêu cầu xóa giao dịch theo id, bảo toàn ngày đang xem khi redirect.
     */
    @PostMapping("/transactions/{id}/delete")
    public String deleteTransaction(
            @PathVariable("id") Long id,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Xóa giao dịch có id: {} thuộc ngày: {}", id, targetDate);
        transactionService.deleteTransaction(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khoản giao dịch thành công!");
        return "redirect:/?date=" + targetDate;
    }

    /**
     * Nạp dữ liệu tóm tắt ngày, danh sách giao dịch, cơ cấu chi tiêu và dữ liệu biểu đồ vào Model.
     */
    private void populateModel(Model model, LocalDate targetDate) {
        LocalDate today = LocalDate.now();
        DailySummaryDto summary = transactionService.getDailySummary(targetDate);
        List<Transaction> transactions = transactionService.getRecentTransactions(targetDate);
        List<CategoryBreakdownDto> expenseBreakdown = transactionService.getExpenseBreakdown(targetDate);

        List<String> chartExpenseLabels = expenseBreakdown.stream()
                .map(CategoryBreakdownDto::getItemName)
                .toList();
        List<BigDecimal> chartExpenseAmounts = expenseBreakdown.stream()
                .map(CategoryBreakdownDto::getAmount)
                .toList();
        List<Double> chartExpensePercentages = expenseBreakdown.stream()
                .map(CategoryBreakdownDto::getPercentage)
                .toList();

        model.addAttribute("summary", summary);
        model.addAttribute("transactions", transactions);
        model.addAttribute("expenseBreakdown", expenseBreakdown);
        model.addAttribute("currentDate", targetDate);
        model.addAttribute("today", today);
        model.addAttribute("isToday", targetDate.isEqual(today));
        model.addAttribute("prevDate", targetDate.minusDays(1));
        model.addAttribute("nextDate", targetDate.plusDays(1));

        model.addAttribute("chartIncome", summary.getTotalIncome());
        model.addAttribute("chartExpense", summary.getTotalExpense());
        model.addAttribute("chartExpenseLabels", chartExpenseLabels);
        model.addAttribute("chartExpenseAmounts", chartExpenseAmounts);
        model.addAttribute("chartExpensePercentages", chartExpensePercentages);

        try {
            model.addAttribute("chartExpenseLabelsJson", objectMapper.writeValueAsString(chartExpenseLabels));
            model.addAttribute("chartExpenseAmountsJson", objectMapper.writeValueAsString(chartExpenseAmounts));
            model.addAttribute("chartExpensePercentagesJson", objectMapper.writeValueAsString(chartExpensePercentages));
            model.addAttribute("expenseBreakdownJson", objectMapper.writeValueAsString(expenseBreakdown));
        } catch (JsonProcessingException e) {
            log.error("Lỗi khi serialize dữ liệu biểu đồ sang JSON", e);
            model.addAttribute("chartExpenseLabelsJson", "[]");
            model.addAttribute("chartExpenseAmountsJson", "[]");
            model.addAttribute("chartExpensePercentagesJson", "[]");
            model.addAttribute("expenseBreakdownJson", "[]");
        }
    }
}
