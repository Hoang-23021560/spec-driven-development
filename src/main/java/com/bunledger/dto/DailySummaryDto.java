package com.bunledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/**
 * DTO chứa thông tin tóm tắt tài chính trong ngày: Tổng Thu, Tổng Chi và Lãi Ròng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySummaryDto {

    private LocalDate date;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;

    private static final DecimalFormatSymbols VND_SYMBOLS;

    static {
        VND_SYMBOLS = new DecimalFormatSymbols(Locale.getDefault());
        VND_SYMBOLS.setGroupingSeparator('.');
    }

    /**
     * Kiểm tra có lợi nhuận trong ngày hay không (netProfit >= 0).
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
}
