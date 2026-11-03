package com.bunledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.util.Locale;

/**
 * DTO chứa thông tin tóm tắt tài chính trong tháng:
 * Tổng Thu, Tổng Chi, Lợi Nhuận Tháng, Số Ngày Hoạt Động và Lợi Nhuận Trung Bình Mỗi Ngày.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryDto {

    private YearMonth month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private long activeDays;
    private BigDecimal averageDailyProfit;

    private static final DecimalFormatSymbols VND_SYMBOLS;

    static {
        VND_SYMBOLS = new DecimalFormatSymbols(Locale.getDefault());
        VND_SYMBOLS.setGroupingSeparator('.');
    }

    /**
     * Kiểm tra có lợi nhuận trong tháng hay không (netProfit >= 0).
     */
    public boolean isProfitable() {
        return netProfit != null && netProfit.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Định dạng số tiền sang chuẩn hiển thị VNĐ (VD: 1.250.000 đ).
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        DecimalFormat df = new DecimalFormat("#,##0", VND_SYMBOLS);
        return df.format(amount) + " đ";
    }

    public String getFormattedTotalIncome() {
        return formatCurrency(totalIncome);
    }

    public String getFormattedTotalExpense() {
        return formatCurrency(totalExpense);
    }

    public String getFormattedNetProfit() {
        return formatCurrency(netProfit);
    }

    public String getFormattedAverageDailyProfit() {
        return formatCurrency(averageDailyProfit);
    }
}
