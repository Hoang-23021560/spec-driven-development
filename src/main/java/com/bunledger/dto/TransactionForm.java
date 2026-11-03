package com.bunledger.dto;

import com.bunledger.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Form DTO hứng dữ liệu từ màn hình nhập nhanh giao dịch Thu/Chi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionForm {

    @NotNull(message = "Vui lòng chọn loại giao dịch")
    @Builder.Default
    private TransactionType type = TransactionType.EXPENSE;

    @NotBlank(message = "Vui lòng nhập tên mặt hàng")
    @Size(max = 100, message = "Tên mặt hàng tối đa 100 ký tự")
    private String itemName;

    @NotNull(message = "Vui lòng nhập số tiền")
    @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @DecimalMin(value = "1", message = "Số lượng phải lớn hơn hoặc bằng 1")
    @Builder.Default
    private Integer quantity = 1;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate transactionDate;
}
