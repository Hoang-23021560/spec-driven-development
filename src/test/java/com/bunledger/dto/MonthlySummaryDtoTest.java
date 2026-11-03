package com.bunledger.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonthlySummaryDto Unit Tests")
class MonthlySummaryDtoTest {

    @Test
    @DisplayName("Should test isProfitable logic correctly")
    void testIsProfitable() {
        MonthlySummaryDto positiveProfit = MonthlySummaryDto.builder()
                .netProfit(new BigDecimal("1000"))
                .build();
        assertTrue(positiveProfit.isProfitable());

        MonthlySummaryDto zeroProfit = MonthlySummaryDto.builder()
                .netProfit(BigDecimal.ZERO)
                .build();
        assertTrue(zeroProfit.isProfitable());

        MonthlySummaryDto negativeProfit = MonthlySummaryDto.builder()
                .netProfit(new BigDecimal("-500"))
                .build();
        assertFalse(negativeProfit.isProfitable());

        MonthlySummaryDto nullProfit = MonthlySummaryDto.builder()
                .netProfit(null)
                .build();
        assertFalse(nullProfit.isProfitable());
    }

    @Test
    @DisplayName("Should format currency with Vietnamese dot grouping and suffix correctly")
    void testCurrencyFormatting() {
        assertEquals("0 đ", MonthlySummaryDto.formatCurrency(null));
        assertEquals("0 đ", MonthlySummaryDto.formatCurrency(BigDecimal.ZERO));
        assertEquals("1.250.000 đ", MonthlySummaryDto.formatCurrency(new BigDecimal("1250000")));
        assertEquals("-350.000 đ", MonthlySummaryDto.formatCurrency(new BigDecimal("-350000")));
    }

    @Test
    @DisplayName("Should test all getters, setters and builder properly")
    void testDtoBuilderAndGetters() {
        YearMonth month = YearMonth.of(2026, 11);
        MonthlySummaryDto dto = MonthlySummaryDto.builder()
                .month(month)
                .totalIncome(new BigDecimal("35000000"))
                .totalExpense(new BigDecimal("20000000"))
                .netProfit(new BigDecimal("15000000"))
                .activeDays(25L)
                .averageDailyProfit(new BigDecimal("600000"))
                .build();

        assertEquals(month, dto.getMonth());
        assertEquals(new BigDecimal("35000000"), dto.getTotalIncome());
        assertEquals(new BigDecimal("20000000"), dto.getTotalExpense());
        assertEquals(new BigDecimal("15000000"), dto.getNetProfit());
        assertEquals(25L, dto.getActiveDays());
        assertEquals(new BigDecimal("600000"), dto.getAverageDailyProfit());
        assertTrue(dto.isProfitable());
        assertEquals("35.000.000 đ", dto.getFormattedTotalIncome());
        assertEquals("20.000.000 đ", dto.getFormattedTotalExpense());
        assertEquals("15.000.000 đ", dto.getFormattedNetProfit());
        assertEquals("600.000 đ", dto.getFormattedAverageDailyProfit());
    }
}
