package com.bunledger.service;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.DailySummaryDto;
import com.bunledger.dto.MonthlySummaryDto;
import com.bunledger.dto.TransactionForm;
import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import com.bunledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service xử lý nghiệp vụ giao dịch thu chi và tính toán tóm tắt tài chính.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Lưu một giao dịch mới từ form nhập liệu.
     * Tự động gán ngày hiện tại nếu form không cung cấp transactionDate.
     */
    @Transactional
    public Transaction saveTransaction(TransactionForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu form giao dịch không được để trống");
        }

        LocalDate txDate = form.getTransactionDate() != null ? form.getTransactionDate() : LocalDate.now();
        int qty = (form.getQuantity() != null && form.getQuantity() > 0) ? form.getQuantity() : 1;

        Transaction transaction = Transaction.builder()
                .type(form.getType())
                .itemName(form.getItemName())
                .amount(form.getAmount())
                .quantity(qty)
                .description(form.getDescription())
                .transactionDate(txDate)
                .build();

        log.info("Lưu giao dịch mới: loại={}, mặt hàng={}, số tiền={}, ngày={}",
                transaction.getType(), transaction.getItemName(), transaction.getAmount(), transaction.getTransactionDate());

        return transactionRepository.save(transaction);
    }

    /**
     * Lấy tóm tắt tài chính (Tổng thu, Tổng chi, Lợi nhuận) của một ngày cụ thể.
     * Mặc định là ngày hiện tại nếu date là null.
     */
    public DailySummaryDto getDailySummary(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDate(TransactionType.INCOME, targetDate);
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        BigDecimal totalExpense = transactionRepository.sumAmountByTypeAndDate(TransactionType.EXPENSE, targetDate);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        return DailySummaryDto.builder()
                .date(targetDate)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .build();
    }

    /**
     * Lấy danh sách giao dịch gần nhất trong ngày, sắp xếp theo thời gian tạo mới nhất lên đầu.
     */
    public List<Transaction> getRecentTransactions(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return transactionRepository.findByTransactionDateOrderByCreatedAtDesc(targetDate);
    }

    /**
     * Tìm kiếm một giao dịch theo id.
     * Ném ngoại lệ IllegalArgumentException nếu không tìm thấy giao dịch.
     */
    public Transaction getTransactionById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch với id: " + id);
        }
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch với id: " + id));
    }

    /**
     * Cập nhật thông tin giao dịch hiện có.
     * Cập nhật các trường: type, itemName, amount, description.
     * Nếu form có transactionDate thì cập nhật, ngược lại giữ nguyên ngày cũ.
     */
    @Transactional
    public Transaction updateTransaction(Long id, TransactionForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu form giao dịch không được để trống");
        }

        Transaction transaction = getTransactionById(id);

        transaction.setType(form.getType());
        transaction.setItemName(form.getItemName());
        transaction.setAmount(form.getAmount());
        int qty = (form.getQuantity() != null && form.getQuantity() > 0) ? form.getQuantity() : 1;
        transaction.setQuantity(qty);
        transaction.setDescription(form.getDescription());

        if (form.getTransactionDate() != null) {
            transaction.setTransactionDate(form.getTransactionDate());
        }

        log.info("Cập nhật giao dịch id={}: loại={}, mặt hàng={}, số tiền={}, ngày={}",
                id, transaction.getType(), transaction.getItemName(), transaction.getAmount(), transaction.getTransactionDate());

        return transactionRepository.save(transaction);
    }

    /**
     * Xóa một giao dịch theo id.
     */
    @Transactional
    public void deleteTransaction(Long id) {
        if (id != null) {
            log.info("Xóa giao dịch với id: {}", id);
            transactionRepository.deleteById(id);
        }
    }

    /**
     * Lấy danh sách phân tích cơ cấu chi tiêu theo từng mặt hàng trong ngày,
     * sắp xếp giảm dần theo số tiền chi, tính tỷ lệ % và định dạng tiền tệ VNĐ.
     */
    public List<CategoryBreakdownDto> getExpenseBreakdown(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        DailySummaryDto summary = getDailySummary(targetDate);
        BigDecimal totalExpense = summary != null && summary.getTotalExpense() != null
                ? summary.getTotalExpense()
                : BigDecimal.ZERO;

        List<Object[]> rows = transactionRepository.findCategoryBreakdown(TransactionType.EXPENSE, targetDate);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CategoryBreakdownDto> breakdownList = new ArrayList<>();
        for (Object[] row : rows) {
            String itemName = row[0] != null ? row[0].toString() : "";
            BigDecimal amount = BigDecimal.ZERO;
            if (row.length > 1 && row[1] != null) {
                if (row[1] instanceof BigDecimal) {
                    amount = (BigDecimal) row[1];
                } else if (row[1] instanceof Number) {
                    amount = new BigDecimal(row[1].toString());
                }
            }

            Double percentage = 0.0;
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            String formattedAmount = CategoryBreakdownDto.formatCurrency(amount);

            breakdownList.add(CategoryBreakdownDto.builder()
                    .itemName(itemName)
                    .amount(amount)
                    .percentage(percentage)
                    .formattedAmount(formattedAmount)
                    .build());
        }

        return breakdownList;
    }

    /**
     * Lấy tóm tắt tài chính của một tháng (Tổng thu, Tổng chi, Lợi nhuận tháng, Số ngày hoạt động, Lợi nhuận TB/ngày).
     * Mặc định là tháng hiện tại nếu month là null.
     */
    public MonthlySummaryDto getMonthlySummary(YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDateRange(TransactionType.INCOME, startDate, endDate);
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        BigDecimal totalExpense = transactionRepository.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, startDate, endDate);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        long activeDays = transactionRepository.countActiveDaysBetween(startDate, endDate);

        BigDecimal averageDailyProfit = BigDecimal.ZERO;
        if (activeDays > 0) {
            averageDailyProfit = netProfit.divide(BigDecimal.valueOf(activeDays), 0, RoundingMode.HALF_UP);
        }

        return MonthlySummaryDto.builder()
                .month(targetMonth)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .activeDays(activeDays)
                .averageDailyProfit(averageDailyProfit)
                .build();
    }

    /**
     * Lấy danh sách cơ cấu chi tiêu cho từng mặt hàng trong tháng,
     * sắp xếp giảm dần theo số tiền chi, tính tỷ lệ % và định dạng tiền tệ VNĐ.
     */
    public List<CategoryBreakdownDto> getMonthlyExpenseBreakdown(YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        MonthlySummaryDto summary = getMonthlySummary(targetMonth);
        BigDecimal totalExpense = summary != null && summary.getTotalExpense() != null
                ? summary.getTotalExpense()
                : BigDecimal.ZERO;

        List<Object[]> rows = transactionRepository.findCategoryBreakdownBetween(TransactionType.EXPENSE, startDate, endDate);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<CategoryBreakdownDto> breakdownList = new ArrayList<>();
        for (Object[] row : rows) {
            String itemName = row[0] != null ? row[0].toString() : "";
            BigDecimal amount = BigDecimal.ZERO;
            if (row.length > 1 && row[1] != null) {
                if (row[1] instanceof BigDecimal) {
                    amount = (BigDecimal) row[1];
                } else if (row[1] instanceof Number) {
                    amount = new BigDecimal(row[1].toString());
                }
            }

            Double percentage = 0.0;
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            String formattedAmount = CategoryBreakdownDto.formatCurrency(amount);

            breakdownList.add(CategoryBreakdownDto.builder()
                    .itemName(itemName)
                    .amount(amount)
                    .percentage(percentage)
                    .formattedAmount(formattedAmount)
                    .build());
        }

        return breakdownList;
    }

    /**
     * Lấy danh sách tất cả các giao dịch trong tháng, sắp xếp theo ngày tăng dần và ID tăng dần
     * (phục vụ hiển thị chi tiết tháng và xuất CSV).
     */
    public List<Transaction> getTransactionsInMonth(YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        return transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(startDate, endDate);
    }
}
