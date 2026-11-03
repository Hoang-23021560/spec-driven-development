package com.bunledger.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryBreakdownDto Unit Tests")
class CategoryBreakdownDtoTest {

    @Test
    @DisplayName("Should test builder, getters, and field assignments")
    void testBuilderAndGetters() {
        CategoryBreakdownDto dto = CategoryBreakdownDto.builder()
                .itemName("Thịt bò tái")
                .amount(new BigDecimal("350000"))
                .percentage(45.5)
                .formattedAmount("350.000 đ")
                .build();

        assertEquals("Thịt bò tái", dto.getItemName());
        assertEquals(new BigDecimal("350000"), dto.getAmount());
        assertEquals(45.5, dto.getPercentage());
        assertEquals("350.000 đ", dto.getFormattedAmount());
    }

    @Test
    @DisplayName("Should test no-args constructor and setters")
    void testNoArgsConstructorAndSetters() {
        CategoryBreakdownDto dto = new CategoryBreakdownDto();
        dto.setItemName("Bún tươi");
        dto.setAmount(new BigDecimal("150000"));
        dto.setPercentage(19.5);
        dto.setFormattedAmount("150.000 đ");

        assertEquals("Bún tươi", dto.getItemName());
        assertEquals(new BigDecimal("150000"), dto.getAmount());
        assertEquals(19.5, dto.getPercentage());
        assertEquals("150.000 đ", dto.getFormattedAmount());
    }

    @Test
    @DisplayName("Should fallback to formatCurrency when formattedAmount is not explicitly set")
    void testFormattedAmountFallback() {
        CategoryBreakdownDto dtoWithAmount = CategoryBreakdownDto.builder()
                .itemName("Rau sống")
                .amount(new BigDecimal("50000"))
                .build();

        assertEquals("50.000 đ", dtoWithAmount.getFormattedAmount());

        CategoryBreakdownDto dtoWithNullAmount = CategoryBreakdownDto.builder()
                .itemName("Chưa rõ")
                .amount(null)
                .build();

        assertEquals("0 đ", dtoWithNullAmount.getFormattedAmount());
    }

    @Test
    @DisplayName("Should return 0.0 when percentage is null")
    void testPercentageNullSafe() {
        CategoryBreakdownDto dto = CategoryBreakdownDto.builder()
                .itemName("Gia vị")
                .amount(new BigDecimal("30000"))
                .percentage(null)
                .build();

        assertNotNull(dto.getPercentage());
        assertEquals(0.0, dto.getPercentage());
    }

    @Test
    @DisplayName("Should format currency with Vietnamese dot grouping and suffix correctly")
    void testFormatCurrency() {
        assertEquals("0 đ", CategoryBreakdownDto.formatCurrency(null));
        assertEquals("0 đ", CategoryBreakdownDto.formatCurrency(BigDecimal.ZERO));
        assertEquals("1.250.000 đ", CategoryBreakdownDto.formatCurrency(new BigDecimal("1250000")));
        assertEquals("35.000 đ", CategoryBreakdownDto.formatCurrency(new BigDecimal("35000")));
        assertEquals("-50.000 đ", CategoryBreakdownDto.formatCurrency(new BigDecimal("-50000")));
    }

    @Test
    @DisplayName("Should verify equals, hashCode, and toString contract")
    void testEqualsAndHashCode() {
        CategoryBreakdownDto dto1 = CategoryBreakdownDto.builder()
                .itemName("Tiền gas")
                .amount(new BigDecimal("400000"))
                .percentage(52.0)
                .formattedAmount("400.000 đ")
                .build();

        CategoryBreakdownDto dto2 = CategoryBreakdownDto.builder()
                .itemName("Tiền gas")
                .amount(new BigDecimal("400000"))
                .percentage(52.0)
                .formattedAmount("400.000 đ")
                .build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertTrue(dto1.toString().contains("Tiền gas"));
    }
}

