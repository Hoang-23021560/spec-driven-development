package com.bunledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * DTO chứa thông tin cơ cấu chi tiêu cho từng mặt hàng trong ngày:
 * Tên mặt hàng, số tiền chi, tỷ lệ phần trăm trên tổng chi và chuỗi định dạng VNĐ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryBreakdownDto {

    private String itemName;
    private BigDecimal amount;
    private Double percentage;
    private String formattedAmount;

    private static final DecimalFormatSymbols VND_SYMBOLS;

    static {
        VND_SYMBOLS = new DecimalFormatSymbols(Locale.getDefault());
        VND_SYMBOLS.setGroupingSeparator('.');
    }

    /**
     * Định dạng số tiền sang chuẩn hiển thị VNĐ (VD: 350.000 đ).
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        DecimalFormat df = new DecimalFormat("#,##0", VND_SYMBOLS);
        return df.format(amount) + " đ";
    }

    /**
     * Lấy chuỗi định dạng tiền tệ. Tự động format từ amount nếu formattedAmount chưa được set.
     */
    public String getFormattedAmount() {
        if (formattedAmount != null) {
            return formattedAmount;
        }
        return formatCurrency(amount);
    }

    /**
     * Lấy tỷ lệ phần trăm, trả về 0.0 nếu percentage là null.
     */
    public Double getPercentage() {
        return percentage != null ? percentage : 0.0;
    }
}

