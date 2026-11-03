package com.bunledger.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DailySummaryDtoTest {

    @Test
    @DisplayName("Should test isProfitable logic correctly")
    void testIsProfitable() {
        DailySummaryDto positiveProfit = DailySummaryDto.builder()
                .netProfit(new BigDecimal("1000"))
                .build();
        assertTrue(positiveProfit.isProfitable());

        DailySummaryDto zeroProfit = DailySummaryDto.builder()
                .netProfit(BigDecimal.ZERO)
                .build();
        assertTrue(zeroProfit.isProfitable());

        DailySummaryDto negativeProfit = DailySummaryDto.builder()
                .netProfit(new BigDecimal("-500"))
                .build();
        assertFalse(negativeProfit.isProfitable());

        DailySummaryDto nullProfit = DailySummaryDto.builder()
                .netProfit(null)
                .build();
        assertFalse(nullProfit.isProfitable());
    }

    @Test
    @DisplayName("Should format currency with Vietnamese dot grouping and suffix correctly")
    void testCurrencyFormatting() {
        assertEquals("0 đ", DailySummaryDto.formatCurrency(null));
        assertEquals("0 đ", DailySummaryDto.formatCurrency(BigDecimal.ZERO));
        assertEquals("1.250.000 đ", DailySummaryDto.formatCurrency(new BigDecimal("1250000")));
        assertEquals("-350.000 đ", DailySummaryDto.formatCurrency(new BigDecimal("-350000")));
    }

    @Test
    @DisplayName("Should test all getters, setters and builder properly")
    void testDtoBuilderAndGetters() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        DailySummaryDto dto = DailySummaryDto.builder()
                .date(date)
                .totalIncome(new BigDecimal("2000000"))
                .totalExpense(new BigDecimal("1200000"))
                .netProfit(new BigDecimal("800000"))
                .build();

        assertEquals(date, dto.getDate());
        assertEquals(new BigDecimal("2000000"), dto.getTotalIncome());
        assertEquals(new BigDecimal("1200000"), dto.getTotalExpense());
        assertEquals(new BigDecimal("800000"), dto.getNetProfit());
        assertEquals("2.000.000 đ", dto.getFormattedTotalIncome());
        assertEquals("1.200.000 đ", dto.getFormattedTotalExpense());
        assertEquals("800.000 đ", dto.getFormattedNetProfit());
    }
}
