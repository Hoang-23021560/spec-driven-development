package com.bunledger.service;

import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvExportService Unit Tests")
class CsvExportServiceTest {

    private CsvExportService csvExportService;

    @BeforeEach
    void setUp() {
        csvExportService = new CsvExportService();
    }

    @Nested
    @DisplayName("UTF-8 BOM & Header Tests")
    class BomAndHeaderTests {

        @Test
        @DisplayName("File CSV phải bắt đầu bằng 3 byte UTF-8 BOM (0xEF, 0xBB, 0xBF)")
        void testUtf8BomPresence() {
            byte[] result = csvExportService.exportTransactionsToCsv(Collections.emptyList());

            assertThat(result).isNotNull();
            assertThat(result.length).isGreaterThanOrEqualTo(3);
            assertThat(result[0]).isEqualTo((byte) 0xEF);
            assertThat(result[1]).isEqualTo((byte) 0xBB);
            assertThat(result[2]).isEqualTo((byte) 0xBF);
        }

        @Test
        @DisplayName("File CSV phải chứa đúng dòng header tiếng Việt chuẩn RFC 4180")
        void testHeaderLineContent() {
            byte[] result = csvExportService.exportTransactionsToCsv(Collections.emptyList());

            // Bỏ 3 bytes BOM đầu để đọc nội dung text
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);
            assertThat(csvText).isEqualTo(CsvExportService.CSV_HEADER);
            assertThat(csvText).startsWith("Mã GD,Ngày,Loại,Mặt hàng,Số lượng,Số tiền (VND),Ghi chú,Thời gian ghi\r\n");
        }

        @Test
        @DisplayName("Xử lý an toàn khi danh sách giao dịch là null")
        void testExportWithNullList() {
            byte[] result = csvExportService.exportTransactionsToCsv(null);

            assertThat(result).isNotNull();
            assertThat(result[0]).isEqualTo((byte) 0xEF);
            assertThat(result[1]).isEqualTo((byte) 0xBB);
            assertThat(result[2]).isEqualTo((byte) 0xBF);

            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);
            assertThat(csvText).isEqualTo(CsvExportService.CSV_HEADER);
        }
    }

    @Nested
    @DisplayName("Transaction Data Formatting Tests")
    class DataFormattingTests {

        @Test
        @DisplayName("Định dạng đúng các trường thông tin: id, ngày, loại tiếng Việt, mặt hàng, số tiền sạch, ghi chú, thời gian")
        void testExportValidTransactions() {
            LocalDateTime createdAt1 = LocalDateTime.of(2026, 11, 1, 8, 30, 15);
            LocalDateTime createdAt2 = LocalDateTime.of(2026, 11, 2, 14, 45, 0);

            Transaction tx1 = Transaction.builder()
                    .id(1L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Thịt bò tươi")
                    .amount(BigDecimal.valueOf(350000))
                    .description("Mua 1kg thịt bò bắp")
                    .transactionDate(LocalDate.of(2026, 11, 1))
                    .createdAt(createdAt1)
                    .build();

            Transaction tx2 = Transaction.builder()
                    .id(2L)
                    .type(TransactionType.INCOME)
                    .itemName("Bán 20 tô bún bò")
                    .amount(BigDecimal.valueOf(800000))
                    .description(null)
                    .transactionDate(LocalDate.of(2026, 11, 2))
                    .createdAt(createdAt2)
                    .build();

            byte[] result = csvExportService.exportTransactionsToCsv(List.of(tx1, tx2));
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);

            String expectedLine1 = "1,2026-11-01,Khoản Chi,Thịt bò tươi,1,350000,Mua 1kg thịt bò bắp,2026-11-01 08:30:15\r\n";
            String expectedLine2 = "2,2026-11-02,Khoản Thu,Bán 20 tô bún bò,1,800000,,2026-11-02 14:45:00\r\n";

            assertThat(csvText).contains(expectedLine1);
            assertThat(csvText).contains(expectedLine2);
        }

        @Test
        @DisplayName("Xử lý an toàn khi các trường của giao dịch là null")
        void testExportTransactionWithNullFields() {
            Transaction tx = Transaction.builder()
                    .id(null)
                    .type(null)
                    .itemName(null)
                    .amount(null)
                    .description(null)
                    .transactionDate(null)
                    .createdAt(null)
                    .build();

            byte[] result = csvExportService.exportTransactionsToCsv(List.of(tx));
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);

            // id='', date='', type='', item='', quantity='1', amount='0', desc='', createdAt=''
            String expectedLine = ",,,,1,0,,\r\n";
            assertThat(csvText).contains(expectedLine);
        }
    }

    @Nested
    @DisplayName("RFC 4180 Escaping Tests")
    class EscapingTests {

        @Test
        @DisplayName("Escape chính xác khi trường chứa dấu phẩy (,)")
        void testEscapeComma() {
            Transaction tx = Transaction.builder()
                    .id(10L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Bún tươi, rau sống, giá")
                    .amount(BigDecimal.valueOf(150000))
                    .description("Mua tại chợ Hòa Hưng, quận 10")
                    .transactionDate(LocalDate.of(2026, 11, 3))
                    .createdAt(LocalDateTime.of(2026, 11, 3, 7, 0, 0))
                    .build();

            byte[] result = csvExportService.exportTransactionsToCsv(List.of(tx));
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);

            assertThat(csvText).contains("\"Bún tươi, rau sống, giá\"");
            assertThat(csvText).contains("\"Mua tại chợ Hòa Hưng, quận 10\"");
        }

        @Test
        @DisplayName("Escape chính xác khi trường chứa dấu nháy kép (\") - nhân đôi dấu nháy kép và bao ngoài bằng dấu nháy")
        void testEscapeDoubleQuotes() {
            Transaction tx = Transaction.builder()
                    .id(11L)
                    .type(TransactionType.INCOME)
                    .itemName("Tô bún \"đặc biệt\"")
                    .amount(BigDecimal.valueOf(65000))
                    .description("Khách gọi món \"VIP\" của quán")
                    .transactionDate(LocalDate.of(2026, 11, 3))
                    .createdAt(LocalDateTime.of(2026, 11, 3, 9, 15, 0))
                    .build();

            byte[] result = csvExportService.exportTransactionsToCsv(List.of(tx));
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);

            assertThat(csvText).contains("\"Tô bún \"\"đặc biệt\"\"\"");
            assertThat(csvText).contains("\"Khách gọi món \"\"VIP\"\" của quán\"");
        }

        @Test
        @DisplayName("Escape chính xác khi trường chứa ký tự xuống dòng (\\n)")
        void testEscapeNewline() {
            Transaction tx = Transaction.builder()
                    .id(12L)
                    .type(TransactionType.EXPENSE)
                    .itemName("Nguyên liệu lẩu")
                    .amount(BigDecimal.valueOf(500000))
                    .description("Dòng 1: Xương bò\nDòng 2: Nấm hương")
                    .transactionDate(LocalDate.of(2026, 11, 3))
                    .createdAt(LocalDateTime.of(2026, 11, 3, 11, 0, 0))
                    .build();

            byte[] result = csvExportService.exportTransactionsToCsv(List.of(tx));
            String csvText = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);

            assertThat(csvText).contains("\"Dòng 1: Xương bò\nDòng 2: Nấm hương\"");
        }

        @Test
        @DisplayName("Unit test trực tiếp hàm escapeCsvField với các trường hợp biên")
        void testEscapeCsvFieldDirectly() {
            assertThat(csvExportService.escapeCsvField(null)).isEqualTo("");
            assertThat(csvExportService.escapeCsvField("")).isEqualTo("");
            assertThat(csvExportService.escapeCsvField("Thịt bò")).isEqualTo("Thịt bò");
            assertThat(csvExportService.escapeCsvField("Bún, Phở")).isEqualTo("\"Bún, Phở\"");
            assertThat(csvExportService.escapeCsvField("Tô \"VIP\"")).isEqualTo("\"Tô \"\"VIP\"\"\"");
            assertThat(csvExportService.escapeCsvField("Dòng 1\nDòng 2")).isEqualTo("\"Dòng 1\nDòng 2\"");
            assertThat(csvExportService.escapeCsvField("Dòng 1\r\nDòng 2")).isEqualTo("\"Dòng 1\r\nDòng 2\"");
            assertThat(csvExportService.escapeCsvField("Có \"nháy\" và ,phẩy")).isEqualTo("\"Có \"\"nháy\"\" và ,phẩy\"");
        }
    }
}
