package com.bunledger.dto;

import com.bunledger.model.TransactionType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransactionFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid TransactionForm should have no constraint violations")
    void testValidForm() {
        TransactionForm form = TransactionForm.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Hành lá")
                .amount(new BigDecimal("15000"))
                .description("Chợ sáng")
                .transactionDate(LocalDate.now())
                .build();

        Set<ConstraintViolation<TransactionForm>> violations = validator.validate(form);
        assertTrue(violations.isEmpty(), "Form should be valid");
    }

    @Test
    @DisplayName("Should detect missing type")
    void testInvalidType() {
        TransactionForm form = TransactionForm.builder()
                .type(null)
                .itemName("Hành lá")
                .amount(new BigDecimal("15000"))
                .build();

        Set<ConstraintViolation<TransactionForm>> violations = validator.validate(form);
        assertEquals(1, violations.size());
        assertEquals("Vui lòng chọn loại giao dịch", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("Should detect blank itemName")
    void testBlankItemName() {
        TransactionForm form = TransactionForm.builder()
                .type(TransactionType.INCOME)
                .itemName("   ")
                .amount(new BigDecimal("50000"))
                .build();

        Set<ConstraintViolation<TransactionForm>> violations = validator.validate(form);
        assertEquals(1, violations.size());
        assertEquals("Vui lòng nhập tên mặt hàng", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("Should detect null or invalid amount (zero or negative)")
    void testInvalidAmount() {
        TransactionForm nullAmount = TransactionForm.builder()
                .type(TransactionType.INCOME)
                .itemName("Bún riêu")
                .amount(null)
                .build();

        Set<ConstraintViolation<TransactionForm>> violations1 = validator.validate(nullAmount);
        assertEquals(1, violations1.size());
        assertEquals("Vui lòng nhập số tiền", violations1.iterator().next().getMessage());

        TransactionForm zeroAmount = TransactionForm.builder()
                .type(TransactionType.INCOME)
                .itemName("Bún riêu")
                .amount(BigDecimal.ZERO)
                .build();

        Set<ConstraintViolation<TransactionForm>> violations2 = validator.validate(zeroAmount);
        assertEquals(1, violations2.size());
        assertEquals("Số tiền phải lớn hơn 0", violations2.iterator().next().getMessage());
    }
}
