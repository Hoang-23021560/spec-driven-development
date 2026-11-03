package com.bunledger;

import com.bunledger.dto.CategoryBreakdownDto;
import com.bunledger.dto.DailySummaryDto;
import com.bunledger.dto.MonthlySummaryDto;
import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import com.bunledger.repository.TransactionRepository;
import com.bunledger.service.CsvExportService;
import com.bunledger.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-End Integration Test cho Phase 1, Phase 2, Phase 3 & Phase 4.
 * Kiểm tra toàn diện từ Web MVC, Service, Repository đến CSDL MySQL bunledger_db.
 * Đối chiếu tuần tự theo bảng Test Matrix trong validation.md:
 * - Phase 1: TC-01 đến TC-07 (Nhập nhanh giao dịch & Tổng hợp ngày)
 * - Phase 2: TC-P2-01 đến TC-P2-06 (Biểu đồ tròn trực quan & Điều hướng ngày)
 * - Phase 3: TC-P3-01 đến TC-P3-05 (Quick Chips, Smart Steppers, Chỉnh sửa giao dịch & Mobile UX Polish)
 * - Phase 4: TC-P4-01 đến TC-P4-05 (Báo cáo tháng, Lợi nhuận TB/ngày, Cơ cấu chi tiêu tháng, Xuất CSV UTF-8 BOM, Tab Navigation)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BunLedger Phase 1, 2, 3 & 4 - End-to-End Integration & Scorecard Verification")
public class BunledgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // Lưu ID của khoản chi "Thịt bò" để kiểm tra xóa ở TC-06
    private static Long expenseTxId;

    /**
     * TC-01: Kết nối CSDL MySQL & Khởi tạo bảng.
     * Kỳ vọng: Spring Boot khởi động thành công, kết nối MySQL bunledger_db ổn định,
     * bảng 'transactions' được tự động tạo với đầy đủ các cột và index.
     */
    @Test
    @Order(1)
    @DisplayName("TC-01: Khởi động Spring Boot và kết nối MySQL bunledger_db thành công")
    void testTC01_DatabaseConnectionAndSchemaCreation() throws Exception {
        // 1. Kiểm tra kết nối DataSource
        assertThat(dataSource).isNotNull();
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.isValid(5)).isTrue();
            assertThat(conn.getCatalog()).isEqualToIgnoringCase("bunledger_db");
        }

        // 2. Kiểm tra bảng 'transactions' đã được Hibernate tạo trong bunledger_db
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'bunledger_db' AND table_name = 'transactions'",
                Integer.class
        );
        assertThat(tableCount).isNotNull().isGreaterThan(0);

        // 3. Kiểm tra các cột thiết yếu của bảng transactions
        List<String> columnNames = jdbcTemplate.query(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'bunledger_db' AND table_name = 'transactions'",
                (rs, rowNum) -> rs.getString("column_name").toLowerCase()
        );
        assertThat(columnNames).contains("id", "type", "item_name", "amount", "description", "transaction_date", "created_at");

        // 4. Kiểm tra HTTP server đang chạy trên cổng ngẫu nhiên
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("SỔ THU CHI QUÁN BÚN");

        // Dọn dẹp dữ liệu ngày hôm nay để bắt đầu chuỗi kiểm thử TC-02 -> TC-06 đồng nhất
        transactionRepository.deleteAll();
    }

    /**
     * TC-02: Nhập nhanh Khoản Chi đầu ngày.
     * Kịch bản: Chi "Thịt bò" 300.000đ -> Lưu -> Tổng chi = 300.000đ, Lợi nhuận = -300.000đ.
     */
    @Test
    @Order(2)
    @DisplayName("TC-02: Nhập khoản Chi 'Thịt bò' 300.000đ -> Lưu -> Tổng chi = 300.000đ, Lợi nhuận = -300.000đ")
    void testTC02_CreateExpenseTransaction() throws Exception {
        // 1. Gửi POST request lưu khoản chi
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "Thịt bò")
                        .param("amount", "300000")
                        .param("description", "Mua 1kg thịt bò tươi sáng sớm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + LocalDate.now()))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Kiểm tra trực tiếp trong CSDL MySQL
        List<Transaction> transactions = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(LocalDate.now());
        assertThat(transactions).hasSize(1);

        Transaction expense = transactions.get(0);
        assertThat(expense.getItemName()).isEqualTo("Thịt bò");
        assertThat(expense.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(expense.getAmount().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(expense.getDescription()).isEqualTo("Mua 1kg thịt bò tươi sáng sớm");

        // Lưu ID cho TC-06 xóa
        expenseTxId = expense.getId();
        assertThat(expenseTxId).isNotNull();

        // 3. Kiểm tra tính toán tóm tắt tài chính (DailySummaryDto)
        DailySummaryDto summary = transactionService.getDailySummary(LocalDate.now());
        assertThat(summary.getTotalIncome().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(summary.getTotalExpense().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("-300000"))).isZero();

        // 4. Kiểm tra trang chủ hiển thị đúng thông tin
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("summary", "transactions"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Thịt bò");
        assertThat(html).contains("300.000");
        assertThat(html).contains("-300.000");
    }

    /**
     * TC-03: Nhập nhanh Khoản Thu trong ngày.
     * Kịch bản: Thu "Bún bò buổi sáng" 500.000đ -> Lưu -> Tổng thu = 500.000đ, Tổng chi = 300.000đ, Lợi nhuận = +200.000đ.
     */
    @Test
    @Order(3)
    @DisplayName("TC-03: Nhập khoản Thu 'Bún bò buổi sáng' 500.000đ -> Lưu -> Tổng thu = 500.000đ, Tổng chi = 300.000đ, Lợi nhuận = +200.000đ")
    void testTC03_CreateIncomeTransaction() throws Exception {
        // 1. Gửi POST request lưu khoản thu
        mockMvc.perform(post("/transactions/save")
                        .param("type", "INCOME")
                        .param("itemName", "Bún bò buổi sáng")
                        .param("amount", "500000")
                        .param("description", "Bán 10 tô bún bò đặc biệt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + LocalDate.now()))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Kiểm tra trực tiếp trong CSDL MySQL có 2 giao dịch
        List<Transaction> transactions = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(LocalDate.now());
        assertThat(transactions).hasSize(2);

        // Khoản thu mới nhập phải nằm trên cùng (ORDER BY createdAt DESC)
        Transaction latestTx = transactions.get(0);
        assertThat(latestTx.getItemName()).isEqualTo("Bún bò buổi sáng");
        assertThat(latestTx.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(latestTx.getAmount().compareTo(new BigDecimal("500000"))).isZero();

        // 3. Kiểm tra tính toán tóm tắt tài chính
        DailySummaryDto summary = transactionService.getDailySummary(LocalDate.now());
        assertThat(summary.getTotalIncome().compareTo(new BigDecimal("500000"))).isZero();
        assertThat(summary.getTotalExpense().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("200000"))).isZero();
        assertThat(summary.isProfitable()).isTrue();

        // 4. Kiểm tra trang chủ hiển thị đúng các số liệu và thẻ
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Bún bò buổi sáng");
        assertThat(html).contains("500.000");
        assertThat(html).contains("300.000");
        assertThat(html).contains("200.000");
    }

    /**
     * TC-04: Tự động ghi nhận ngày hôm nay (LocalDate.now()).
     * Kỳ vọng: Giao dịch được lưu với transactionDate = LocalDate.now(), người dùng không phải chọn ngày thủ công.
     */
    @Test
    @Order(4)
    @DisplayName("TC-04: Kiểm tra ngày giao dịch tự động ghi nhận là ngày hôm nay")
    void testTC04_AutomaticCurrentDateAssignment() {
        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).isNotEmpty();

        LocalDate today = LocalDate.now();
        for (Transaction tx : transactions) {
            assertThat(tx.getTransactionDate())
                    .as("Ngày giao dịch của tx ID %d phải là ngày hôm nay", tx.getId())
                    .isEqualTo(today);
            assertThat(tx.getCreatedAt())
                    .as("Thời điểm tạo phải được tự động gán")
                    .isNotNull();
            assertThat(tx.getCreatedAt().toLocalDate()).isEqualTo(today);
        }
    }

    /**
     * TC-05: Ràng buộc dữ liệu & Validation (Tên trống hoặc tiền <= 0 bị chặn).
     * Kỳ vọng: Không lưu vào CSDL, trả về view index với lỗi binding.
     */
    @Test
    @Order(5)
    @DisplayName("TC-05: Kiểm tra Validation (Tên trống hoặc tiền <= 0 bị chặn)")
    void testTC05_ValidationConstraints() throws Exception {
        int initialCount = transactionRepository.findAll().size();

        // 1. Tên mặt hàng trống
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "")
                        .param("amount", "100000"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("transactionForm", "itemName"));

        // 2. Số tiền bằng 0
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "Hành tây")
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("transactionForm", "amount"));

        // 3. Số tiền âm
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "Rau thơm")
                        .param("amount", "-50000"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("transactionForm", "amount"));

        // 4. Đảm bảo CSDL không tăng thêm bản ghi nào
        int afterCount = transactionRepository.findAll().size();
        assertThat(afterCount).isEqualTo(initialCount);
    }

    /**
     * TC-06: Xóa giao dịch nhập sai và tự động cập nhật lại chỉ số.
     * Kịch bản: Xóa khoản chi 'Thịt bò' -> Tổng chi quay về 0đ, Lợi nhuận cập nhật = +500.000đ.
     */
    @Test
    @Order(6)
    @DisplayName("TC-06: Xóa giao dịch nhập sai -> Chỉ số tự động cập nhật lại")
    void testTC06_DeleteTransactionAndAutoRecalculate() throws Exception {
        // Đảm bảo có expenseTxId
        if (expenseTxId == null) {
            List<Transaction> expenses = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(LocalDate.now())
                    .stream()
                    .filter(tx -> tx.getType() == TransactionType.EXPENSE)
                    .toList();
            assertThat(expenses).isNotEmpty();
            expenseTxId = expenses.get(0).getId();
        }

        // 1. Gửi POST request xóa khoản chi Thịt bò
        mockMvc.perform(post("/transactions/" + expenseTxId + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + LocalDate.now()))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Kiểm tra bản ghi đã bị xóa khỏi CSDL MySQL
        assertThat(transactionRepository.findById(expenseTxId)).isEmpty();

        // 3. Kiểm tra các chỉ số tự động cập nhật lại
        DailySummaryDto summary = transactionService.getDailySummary(LocalDate.now());
        assertThat(summary.getTotalExpense().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(summary.getTotalIncome().compareTo(new BigDecimal("500000"))).isZero();
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("500000"))).isZero();

        // 4. Kiểm tra trên giao diện HTML
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        // Khoản thịt bò không còn xuất hiện trong danh sách
        assertThat(html).doesNotContain("Mua 1kg thịt bò tươi sáng sớm");
        // Tổng thu và lợi nhuận đều là 500.000đ
        assertThat(html).contains("500.000");
    }

    /**
     * TC-07: Kiểm tra cấu trúc Responsive Mobile & UX constraints.
     * Kỳ vọng: Có thẻ meta viewport, inputmode="numeric", touch target min-height 48px, container max-w-lg.
     */
    @Test
    @Order(7)
    @DisplayName("TC-07: Kiểm tra Responsive Mobile, Touch Targets & Attributes")
    void testTC07_ResponsiveMobileAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Viewport hỗ trợ mobile
        assertThat(html).contains("name=\"viewport\"");
        assertThat(html).contains("width=device-width");

        // 2. Thuộc tính bàn phím số
        assertThat(html).contains("inputmode=\"numeric\"");

        // 3. Khung layout Web Dashboard rộng rãi (max-w-7xl)
        assertThat(html).contains("max-w-7xl");

        // 4. Touch target quy chuẩn tối thiểu 48px
        assertThat(html).contains("min-h-[48px]");

        // 5. Hai nút gạt Thu/Chi lớn
        assertThat(html).contains("[ + THU TIỀN ]");
        assertThat(html).contains("[ - CHI TIỀN ]");
    }

    /**
     * TC-P2-01: Lọc dữ liệu theo ngày (?date=YYYY-MM-DD).
     * Kỳ vọng: Chỉ hiển thị các giao dịch phát sinh trong ngày chỉ định, các thẻ Tổng Thu, Tổng Chi, Lợi Nhuận khớp với ngày đó.
     */
    @Test
    @Order(8)
    @DisplayName("TC-P2-01: Lọc dữ liệu theo ngày (?date=YYYY-MM-DD) chỉ trả về giao dịch và chỉ số khớp ngày")
    void testTCP201_FilterDataByDate() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 11, 2);
        LocalDate otherDate = LocalDate.of(2026, 10, 31);

        // 1. Chuẩn bị dữ liệu thực tế trong CSDL MySQL bunledger_db
        Transaction txIncome = transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bún bò đặc biệt 2026-11-02")
                .amount(new BigDecimal("450000"))
                .description("Bán bún sáng 2026-11-02")
                .transactionDate(targetDate)
                .build());

        Transaction txExpense = transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Rau sống giá đỗ 2026-11-02")
                .amount(new BigDecimal("50000"))
                .description("Mua rau thơm sáng")
                .transactionDate(targetDate)
                .build());

        // Giao dịch ở ngày khác (phải bị loại trừ khi xem ngày 2026-11-02)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Chi phí ngày khác 2026-10-31")
                .amount(new BigDecimal("200000"))
                .description("Khoản chi ngày khác")
                .transactionDate(otherDate)
                .build());

        // 2. Gửi request GET /?date=2026-11-02
        MvcResult result = mockMvc.perform(get("/").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentDate", targetDate))
                .andExpect(model().attributeExists("summary", "transactions"))
                .andReturn();

        // 3. Kiểm tra Model attributes
        DailySummaryDto summary = (DailySummaryDto) result.getModelAndView().getModel().get("summary");
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalIncome().compareTo(new BigDecimal("450000"))).isZero();
        assertThat(summary.getTotalExpense().compareTo(new BigDecimal("50000"))).isZero();
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("400000"))).isZero();

        @SuppressWarnings("unchecked")
        List<Transaction> txList = (List<Transaction>) result.getModelAndView().getModel().get("transactions");
        assertThat(txList).isNotNull().hasSize(2);
        assertThat(txList).extracting(Transaction::getItemName)
                .containsExactlyInAnyOrder("Rau sống giá đỗ 2026-11-02", "Bún bò đặc biệt 2026-11-02")
                .doesNotContain("Chi phí ngày khác 2026-10-31");

        // 4. Kiểm tra trên HTML rendered
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Bún bò đặc biệt 2026-11-02");
        assertThat(html).contains("450.000");
        assertThat(html).contains("Rau sống giá đỗ 2026-11-02");
        assertThat(html).contains("50.000");
        assertThat(html).contains("400.000");
        assertThat(html).doesNotContain("Chi phí ngày khác 2026-10-31");

        // Đồng bộ kiểm tra endpoint /transactions?date=2026-11-02
        mockMvc.perform(get("/transactions").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentDate", targetDate));
    }

    /**
     * TC-P2-02: Điều hướng ngày (Lùi / Tiến / Hôm nay).
     * Kỳ vọng: Bấm [◀] chuyển sang ngày hôm qua (target - 1), [▶] chuyển sang ngày mai (target + 1), nút Hôm nay quay về ngày hiện tại.
     */
    @Test
    @Order(9)
    @DisplayName("TC-P2-02: Kiểm tra các link điều hướng ngày prevDate, nextDate và nút Hôm nay")
    void testTCP202_DateNavigationLinks() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 11, 2);
        LocalDate today = LocalDate.now();

        // 1. Khi xem ngày 2026-11-02 (khác hôm nay -> isToday = false)
        MvcResult resultPast = mockMvc.perform(get("/").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentDate", targetDate))
                .andExpect(model().attribute("prevDate", LocalDate.of(2026, 11, 1)))
                .andExpect(model().attribute("nextDate", LocalDate.of(2026, 11, 3)))
                .andExpect(model().attribute("isToday", false))
                .andReturn();

        String htmlPast = resultPast.getResponse().getContentAsString();
        // Nút Lùi 1 ngày [◀] trỏ tới ngày 2026-11-01
        assertThat(htmlPast).contains("[◀]");
        assertThat(htmlPast).contains("/?date=2026-11-01");
        // Nút Tiến 1 ngày [▶] trỏ tới ngày 2026-11-03
        assertThat(htmlPast).contains("[▶]");
        assertThat(htmlPast).contains("/?date=2026-11-03");
        // Nút Hôm nay xuất hiện khi không phải hôm nay
        assertThat(htmlPast).contains("Hôm nay");
        assertThat(htmlPast).contains("/?date=" + today);

        // 2. Khi xem ngày hôm nay (isToday = true)
        MvcResult resultToday = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentDate", today))
                .andExpect(model().attribute("prevDate", today.minusDays(1)))
                .andExpect(model().attribute("nextDate", today.plusDays(1)))
                .andExpect(model().attribute("isToday", true))
                .andReturn();

        String htmlToday = resultToday.getResponse().getContentAsString();
        assertThat(htmlToday).contains("/?date=" + today.minusDays(1));
        assertThat(htmlToday).contains("/?date=" + today.plusDays(1));
    }

    /**
     * TC-P2-03: Tính toán cơ cấu chi phí theo mặt hàng.
     * Kỳ vọng: Tạo 2 khoản chi (Thịt bò 300.000đ, Bún tươi 100.000đ), cơ cấu chi phí trả về chính xác 75% và 25%.
     */
    @Test
    @Order(10)
    @DisplayName("TC-P2-03: Tạo 2 khoản chi và kiểm tra cơ cấu chi phí trả về chính xác 75% và 25%")
    void testTCP203_ExpenseBreakdownCalculation() throws Exception {
        LocalDate breakdownDate = LocalDate.of(2026, 11, 15);

        // 1. Lưu 2 khoản chi vào CSDL MySQL bunledger_db
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Thịt bò")
                .amount(new BigDecimal("300000"))
                .transactionDate(breakdownDate)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Bún tươi")
                .amount(new BigDecimal("100000"))
                .transactionDate(breakdownDate)
                .build());

        // 2. Kiểm tra trực tiếp qua TransactionService
        List<CategoryBreakdownDto> breakdown = transactionService.getExpenseBreakdown(breakdownDate);
        assertThat(breakdown).hasSize(2);

        CategoryBreakdownDto item1 = breakdown.get(0);
        assertThat(item1.getItemName()).isEqualTo("Thịt bò");
        assertThat(item1.getAmount().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(item1.getPercentage()).isEqualTo(75.0);
        assertThat(item1.getFormattedAmount()).isEqualTo("300.000 đ");

        CategoryBreakdownDto item2 = breakdown.get(1);
        assertThat(item2.getItemName()).isEqualTo("Bún tươi");
        assertThat(item2.getAmount().compareTo(new BigDecimal("100000"))).isZero();
        assertThat(item2.getPercentage()).isEqualTo(25.0);
        assertThat(item2.getFormattedAmount()).isEqualTo("100.000 đ");

        // 3. Kiểm tra dữ liệu được truyền vào Thymeleaf Model & HTML qua MockMvc
        MvcResult result = mockMvc.perform(get("/").param("date", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("expenseBreakdown"))
                .andExpect(model().attributeExists("chartExpenseLabelsJson"))
                .andExpect(model().attributeExists("chartExpenseAmountsJson"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Thịt bò");
        assertThat(html).contains("Bún tươi");
        assertThat(html).contains("300.000 đ");
        assertThat(html).contains("100.000 đ");
        assertThat(html).contains("75");
        assertThat(html).contains("25");
    }

    /**
     * TC-P2-04: Biểu đồ tròn Thu vs Chi.
     * Kỳ vọng: Nhập Thu 600.000đ, Chi 400.000đ -> Biểu đồ tròn vành khuyên chia tỷ lệ Thu 60%, Chi 40%.
     */
    @Test
    @Order(11)
    @DisplayName("TC-P2-04: Tạo Thu 600.000đ, Chi 400.000đ và kiểm tra tỷ lệ Thu vs Chi (60% vs 40%)")
    void testTCP204_IncomeVsExpenseChartData() throws Exception {
        LocalDate chartDate = LocalDate.of(2026, 11, 16);

        // 1. Tạo khoản Thu 600.000đ và Chi 400.000đ trong CSDL MySQL bunledger_db
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bán bún bò đặc biệt")
                .amount(new BigDecimal("600000"))
                .transactionDate(chartDate)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Chi tiền sườn và thịt")
                .amount(new BigDecimal("400000"))
                .transactionDate(chartDate)
                .build());

        // 2. Gửi request GET /?date=2026-11-16
        MvcResult result = mockMvc.perform(get("/").param("date", "2026-11-16"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("chartIncome", new BigDecimal("600000")))
                .andExpect(model().attribute("chartExpense", new BigDecimal("400000")))
                .andReturn();

        // 3. Kiểm tra tính toán tỷ lệ Thu vs Chi: Thu 600k / Tổng 1000k = 60%, Chi 400k / Tổng 1000k = 40%
        DailySummaryDto summary = (DailySummaryDto) result.getModelAndView().getModel().get("summary");
        BigDecimal total = summary.getTotalIncome().add(summary.getTotalExpense());
        assertThat(total.compareTo(new BigDecimal("1000000"))).isZero();

        BigDecimal incomeRatio = summary.getTotalIncome().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        BigDecimal expenseRatio = summary.getTotalExpense().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
        assertThat(incomeRatio.compareTo(new BigDecimal("60.0"))).isZero();
        assertThat(expenseRatio.compareTo(new BigDecimal("40.0"))).isZero();

        // 4. Kiểm tra HTML chứa canvas biểu đồ Thu vs Chi và các số liệu
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("id=\"incomeExpenseChart\"");
        assertThat(html).contains("600.000");
        assertThat(html).contains("400.000");
        assertThat(html).contains("200.000");
    }

    /**
     * TC-P2-05: Biểu đồ rỗng khi ngày không có dữ liệu (Zero-State).
     * Kỳ vọng: Chọn 1 ngày hoàn toàn trống dữ liệu, hiển thị zero-state 'Chưa có dữ liệu thu chi ngày này' mà không bị lỗi JS.
     */
    @Test
    @Order(12)
    @DisplayName("TC-P2-05: Truy vấn ngày trống dữ liệu, xác minh HTML hiển thị zero-state không bị lỗi")
    void testTCP205_ZeroStateForEmptyDate() throws Exception {
        LocalDate emptyDate = LocalDate.of(2029, 12, 31);

        // Đảm bảo không có bản ghi nào ngày này
        List<Transaction> existing = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(emptyDate);
        assertThat(existing).isEmpty();

        // Gửi request GET /?date=2029-12-31
        MvcResult result = mockMvc.perform(get("/").param("date", "2029-12-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("currentDate", emptyDate))
                .andExpect(model().attribute("chartIncome", BigDecimal.ZERO))
                .andExpect(model().attribute("chartExpense", BigDecimal.ZERO))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // Xác minh zero-state hiển thị rõ ràng thông điệp chuẩn xác
        assertThat(html).contains("Chưa có giao dịch nào trong ngày này");

        // Biểu đồ tròn luôn sẵn sàng hiển thị trên giao diện theo yêu cầu Web Dashboard
        assertThat(html).contains("id=\"incomeExpenseChart\"");
    }

    /**
     * TC-P2-06: Nhập bù giao dịch cho ngày đã chọn và bảo toàn ngày.
     * Kỳ vọng: Đang xem ngày 2026-11-01, nhập chi "Tiền gas 450.000đ" -> lưu đúng ngày 2026-11-01 và redirect về /?date=2026-11-01.
     */
    @Test
    @Order(13)
    @DisplayName("TC-P2-06: Gửi form nhập khoản chi mới với ngày quá khứ, xác minh lưu đúng CSDL và redirect bảo toàn ngày")
    void testTCP206_SaveTransactionForPastDateAndRedirect() throws Exception {
        LocalDate pastDate = LocalDate.of(2026, 11, 1);

        // 1. Gửi POST request lưu khoản chi với transactionDate = 2026-11-01
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "Tiền gas 450.000đ")
                        .param("amount", "450000")
                        .param("description", "Thay bình gas đỏ đun nước lèo")
                        .param("transactionDate", "2026-11-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=2026-11-01"))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Xác minh trực tiếp trong CSDL MySQL bunledger_db
        List<Transaction> pastTransactions = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(pastDate);
        assertThat(pastTransactions).isNotEmpty();

        Transaction savedTx = pastTransactions.stream()
                .filter(tx -> "Tiền gas 450.000đ".equals(tx.getItemName()))
                .findFirst()
                .orElse(null);

        assertThat(savedTx).isNotNull();
        assertThat(savedTx.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(savedTx.getAmount().compareTo(new BigDecimal("450000"))).isZero();
        assertThat(savedTx.getTransactionDate()).isEqualTo(pastDate);
        assertThat(savedTx.getDescription()).isEqualTo("Thay bình gas đỏ đun nước lèo");

        // 3. Truy cập trang /?date=2026-11-01 xác nhận khoản chi hiển thị trên giao diện
        MvcResult result = mockMvc.perform(get("/").param("date", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentDate", pastDate))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Tiền gas 450.000đ");
        assertThat(html).contains("450.000");
        assertThat(html).contains("-450.000");
    }

    /**
     * TC-P3-01: Gợi ý mặt hàng 1 chạm (Quick Chips) theo ngữ cảnh Thu/Chi.
     * Kỳ vọng: Giao diện HTML chứa đầy đủ các nhóm chips expenseChips và incomeChips,
     * các chip quen thuộc của quán bún, hàm JS chuyển đổi ngữ cảnh và touch target >= 44px.
     */
    @Test
    @Order(14)
    @DisplayName("TC-P3-01: Kiểm tra sự hiện diện và thuộc tính của nhóm Quick Chips (Thu/Chi)")
    void testTCP301_QuickChipsPresenceAndAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Kiểm tra 2 container chip: expenseChips và incomeChips
        assertThat(html).contains("id=\"expenseChips\"");
        assertThat(html).contains("id=\"incomeChips\"");

        // 2. Kiểm tra danh sách mặt hàng Chi
        assertThat(html).contains("Thịt bò");
        assertThat(html).contains("Bún tươi");
        assertThat(html).contains("Xương hầm");
        assertThat(html).contains("Rau sống");
        assertThat(html).contains("Hành ngò");
        assertThat(html).contains("Tiền gas");
        assertThat(html).contains("Gia vị");
        assertThat(html).contains("Đá lạnh");
        assertThat(html).contains("Mặt bằng");

        // 3. Kiểm tra danh sách mặt hàng Thu
        assertThat(html).contains("Bún tái gầu");
        assertThat(html).contains("Bún giò heo");
        assertThat(html).contains("Bún đặc biệt");
        assertThat(html).contains("Bún mang về");
        assertThat(html).contains("Nước ngọt");
        assertThat(html).contains("Khăn lạnh");
        assertThat(html).contains("Trà đá");

        // 4. Kiểm tra sự kiện onclick selectQuickChip
        assertThat(html).contains("selectQuickChip(this, 'Thịt bò')");
        assertThat(html).contains("selectQuickChip(this, 'Bún tái gầu')");

        // 5. Kiểm tra hàm JavaScript xử lý và chuyển theme ngữ cảnh
        assertThat(html).contains("function selectQuickChip");
        assertThat(html).contains("function updateFormTheme");

        // 6. Touch target chuẩn mobile: min-h-[44px] và class quick-chip
        assertThat(html).contains("quick-chip");
        assertThat(html).contains("min-h-[44px]");
    }

    /**
     * TC-P3-02: Bàn phím trợ lực tiền tệ (Smart Steppers) & cơ chế cộng dồn.
     * Kỳ vọng: Giao diện HTML có các nút cộng nhanh +20k, +50k, +100k, +200k, +500k và nút xóa trắng [C],
     * gọi hàm setQuickAmount và clearAmount với touch target >= 44px.
     */
    @Test
    @Order(15)
    @DisplayName("TC-P3-02: Kiểm tra bàn phím tiền tệ thông minh (Smart Steppers +20k..+500k, [C])")
    void testTCP302_SmartMoneySteppersPresenceAndAttributes() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // 1. Kiểm tra sự hiện diện của các nút trợ lực
        assertThat(html).contains("+20k");
        assertThat(html).contains("+50k");
        assertThat(html).contains("+100k");
        assertThat(html).contains("+200k");
        assertThat(html).contains("+500k");
        assertThat(html).contains("[C]");

        // 2. Kiểm tra gọi hàm onclick đúng số tiền VNĐ tương ứng
        assertThat(html).contains("setQuickAmount(20000)");
        assertThat(html).contains("setQuickAmount(50000)");
        assertThat(html).contains("setQuickAmount(100000)");
        assertThat(html).contains("setQuickAmount(200000)");
        assertThat(html).contains("setQuickAmount(500000)");
        assertThat(html).contains("clearAmount()");

        // 3. Kiểm tra logic xử lý JavaScript cho cộng dồn và xóa trắng
        assertThat(html).contains("function setQuickAmount");
        assertThat(html).contains("function clearAmount");
        assertThat(html).contains("current + Number(val)");
    }

    /**
     * TC-P3-03: Chỉnh sửa giao dịch (Edit Transaction) & Tự động tính toán lại.
     * Kịch bản:
     * 1. Tạo 1 giao dịch trong MySQL ("Thịt bò" 300.000đ).
     * 2. Gửi request POST /transactions/{id}/update đổi số tiền thành 350.000đ và đổi ghi chú thành "Mua thêm 1kg".
     * 3. Kiểm tra CSDL: bản ghi được cập nhật đúng số tiền 350.000đ và ghi chú mới.
     * 4. Kiểm tra tổng chi ngày đó tăng lên 350.000đ, lợi nhuận tính toán lại chuẩn xác.
     * 5. Kiểm tra HTML: hiển thị số tiền mới, nút [Sửa] và cấu trúc Modal Chỉnh sửa.
     */
    @Test
    @Order(16)
    @DisplayName("TC-P3-03: Chỉnh sửa giao dịch 'Thịt bò' từ 300k -> 350k, kiểm tra CSDL và tổng hợp tính lại")
    void testTCP303_EditTransactionSuccessAndRecalculate() throws Exception {
        LocalDate editTestDate = LocalDate.of(2026, 11, 20);

        // 1. Chuẩn bị 1 giao dịch ban đầu trong MySQL bunledger_db: Chi "Thịt bò" 300.000đ
        Transaction initialTx = transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Thịt bò")
                .amount(new BigDecimal("300000"))
                .description("Mua 1kg thịt bò tươi sáng sớm")
                .transactionDate(editTestDate)
                .build());
        Long txId = initialTx.getId();
        assertThat(txId).isNotNull();

        // Kiểm tra số liệu trước khi sửa: Tổng chi = 300.000đ, Lợi nhuận = -300.000đ
        DailySummaryDto summaryBefore = transactionService.getDailySummary(editTestDate);
        assertThat(summaryBefore.getTotalExpense().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(summaryBefore.getNetProfit().compareTo(new BigDecimal("-300000"))).isZero();

        // 2. Gửi request POST /transactions/{id}/update đổi số tiền thành 350.000đ và đổi ghi chú
        mockMvc.perform(post("/transactions/" + txId + "/update")
                        .param("date", editTestDate.toString())
                        .param("type", "EXPENSE")
                        .param("itemName", "Thịt bò")
                        .param("amount", "350000")
                        .param("description", "Mua thêm 1kg thịt bò tươi")
                        .param("transactionDate", editTestDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + editTestDate))
                .andExpect(flash().attribute("successMessage", "Đã cập nhật giao dịch thành công!"));

        // 3. Kiểm tra trực tiếp trong CSDL MySQL bunledger_db
        Transaction updatedTx = transactionRepository.findById(txId).orElse(null);
        assertThat(updatedTx).isNotNull();
        assertThat(updatedTx.getId()).isEqualTo(txId);
        assertThat(updatedTx.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(updatedTx.getItemName()).isEqualTo("Thịt bò");
        assertThat(updatedTx.getAmount().compareTo(new BigDecimal("350000"))).isZero();
        assertThat(updatedTx.getDescription()).isEqualTo("Mua thêm 1kg thịt bò tươi");
        assertThat(updatedTx.getTransactionDate()).isEqualTo(editTestDate);

        // 4. Kiểm tra tổng chi ngày đó tăng lên 350.000đ, lợi nhuận tự tính lại chuẩn xác
        DailySummaryDto summaryAfter = transactionService.getDailySummary(editTestDate);
        assertThat(summaryAfter.getTotalExpense().compareTo(new BigDecimal("350000"))).isZero();
        assertThat(summaryAfter.getNetProfit().compareTo(new BigDecimal("-350000"))).isZero();

        // 5. Kiểm tra trên giao diện HTML rendered
        MvcResult result = mockMvc.perform(get("/").param("date", editTestDate.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        // Hiển thị số tiền mới và ghi chú mới
        assertThat(html).contains("350.000");
        assertThat(html).contains("Mua thêm 1kg thịt bò tươi");
        // Có nút Sửa giao dịch với data attributes
        assertThat(html).contains("openEditModalFromButton(this)");
        assertThat(html).contains("data-id=\"" + txId + "\"");
        assertThat(html).contains("data-item-name=\"Thịt bò\"");
        // Có Modal chỉnh sửa và Form editTxForm
        assertThat(html).contains("id=\"editModal\"");
        assertThat(html).contains("id=\"editTxForm\"");
        assertThat(html).contains("CẬP NHẬT GIAO DỊCH");
    }

    /**
     * TC-P3-04: Kiểm tra Validation khi Chỉnh sửa (Tên rỗng hoặc số tiền <= 0).
     * Kịch bản:
     * 1. Gửi request update với tên rỗng.
     * 2. Gửi request update với số tiền bằng 0 hoặc âm.
     * 3. Gửi request update với id không tồn tại.
     * Kỳ vọng: Hệ thống chuyển hướng với flash errorMessage, CSDL không bị cập nhật dữ liệu sai.
     */
    @Test
    @Order(17)
    @DisplayName("TC-P3-04: Kiểm tra validation khi Chỉnh sửa (Tên rỗng hoặc tiền <= 0 bị chặn)")
    void testTCP304_EditTransactionValidationFailure() throws Exception {
        LocalDate validationDate = LocalDate.of(2026, 11, 20);

        // Tìm giao dịch đã tạo ở TC-P3-03
        List<Transaction> txList = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(validationDate);
        assertThat(txList).isNotEmpty();
        Transaction originalTx = txList.get(0);
        Long txId = originalTx.getId();
        BigDecimal originalAmount = originalTx.getAmount();
        String originalItemName = originalTx.getItemName();

        // 1. Thử cập nhật với tên mặt hàng rỗng
        mockMvc.perform(post("/transactions/" + txId + "/update")
                        .param("date", validationDate.toString())
                        .param("type", "EXPENSE")
                        .param("itemName", "")
                        .param("amount", "200000")
                        .param("transactionDate", validationDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + validationDate))
                .andExpect(flash().attributeExists("errorMessage"));

        // 2. Thử cập nhật với số tiền bằng 0
        mockMvc.perform(post("/transactions/" + txId + "/update")
                        .param("date", validationDate.toString())
                        .param("type", "EXPENSE")
                        .param("itemName", "Thịt bò")
                        .param("amount", "0")
                        .param("transactionDate", validationDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + validationDate))
                .andExpect(flash().attributeExists("errorMessage"));

        // 3. Thử cập nhật với số tiền âm
        mockMvc.perform(post("/transactions/" + txId + "/update")
                        .param("date", validationDate.toString())
                        .param("type", "EXPENSE")
                        .param("itemName", "Thịt bò")
                        .param("amount", "-50000")
                        .param("transactionDate", validationDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + validationDate))
                .andExpect(flash().attributeExists("errorMessage"));

        // 4. Thử cập nhật giao dịch có ID không tồn tại
        mockMvc.perform(post("/transactions/999999/update")
                        .param("date", validationDate.toString())
                        .param("type", "EXPENSE")
                        .param("itemName", "Thịt bò")
                        .param("amount", "200000")
                        .param("transactionDate", validationDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + validationDate))
                .andExpect(flash().attributeExists("errorMessage"));

        // 5. Kiểm tra CSDL MySQL: Dữ liệu bản ghi ban đầu được bảo toàn nguyên vẹn
        Transaction checkTx = transactionRepository.findById(txId).orElseThrow();
        assertThat(checkTx.getItemName()).isEqualTo(originalItemName);
        assertThat(checkTx.getAmount().compareTo(originalAmount)).isZero();
    }

    /**
     * TC-P3-05: Nhập nhanh trong vòng 5 giây & Kiểm tra tiêu chuẩn Mobile UX Polish (DoD).
     * Kịch bản: Mô phỏng người dùng hoàn tất nhập liệu nhanh 3 chạm (Bún tươi -> +100k -> Lưu).
     * Kỳ vọng: Giao dịch được lưu và hiển thị ngay tức thì, đáp ứng đầy đủ tiêu chuẩn UX Mobile Polish.
     */
    @Test
    @Order(18)
    @DisplayName("TC-P3-05: Nhập nhanh 3 chạm (< 5s) và xác minh tiêu chuẩn Mobile UX Polish DoD")
    void testTCP305_FastInputEndToEndAndMobilePolishDoD() throws Exception {
        LocalDate fastDate = LocalDate.of(2026, 11, 21);

        // 1. Thực hiện luồng lưu giao dịch nhập nhanh 3 chạm: "Bún tươi" + "100.000"
        mockMvc.perform(post("/transactions/save")
                        .param("type", "EXPENSE")
                        .param("itemName", "Bún tươi")
                        .param("amount", "100000")
                        .param("description", "Nhập bún tươi sáng sớm")
                        .param("transactionDate", fastDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?date=" + fastDate))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Xác minh bản ghi đã lưu vào MySQL bunledger_db
        List<Transaction> transactions = transactionRepository.findByTransactionDateOrderByCreatedAtDesc(fastDate);
        assertThat(transactions).isNotEmpty();
        Transaction fastTx = transactions.get(0);
        assertThat(fastTx.getItemName()).isEqualTo("Bún tươi");
        assertThat(fastTx.getAmount().compareTo(new BigDecimal("100000"))).isZero();
        assertThat(fastTx.getType()).isEqualTo(TransactionType.EXPENSE);

        // 3. Kiểm tra trang hiển thị giao dịch với đầy đủ nút Sửa, định dạng tiền tệ và UX Polish
        MvcResult result = mockMvc.perform(get("/").param("date", fastDate.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String html = result.getResponse().getContentAsString();

        // Giao dịch hiển thị với số tiền định dạng rõ ràng
        assertThat(html).contains("Bún tươi");
        assertThat(html).contains("100.000");

        // Các thuộc tính Mobile UX Polish theo Definition of Done:
        // - Danh sách Quick Chips cuộn ngang mượt mà, ẩn scrollbar
        assertThat(html).contains("overflow-x-auto");
        assertThat(html).contains("scrollbar-none");
        // - Modal có backdrop mờ và đóng bằng Escape
        assertThat(html).contains("backdrop-blur-sm");
        assertThat(html).contains("Escape");
        // - Nút submit to rõ, touch target tối thiểu 48px
        assertThat(html).contains("min-h-[50px]");
        assertThat(html).contains("min-h-[48px]");
    }

    /**
     * TC-P4-01: Báo cáo tài chính theo tháng (GET /reports?month=YYYY-MM).
     * Kịch bản:
     * 1. Chuẩn bị giao dịch ở 2 ngày khác nhau trong tháng (ví dụ: ngày 1 và ngày 3 tháng 2026-08).
     * 2. Gọi GET /reports?month=2026-08.
     * Kỳ vọng:
     * - Trả về HTTP 200, view "reports".
     * - Tính toán chính xác Tổng Thu (1.400.000đ), Tổng Chi (800.000đ), Lợi Nhuận Tháng (600.000đ).
     * - Số ngày hoạt động trong tháng activeDays = 2.
     * - HTML hiển thị chính xác các chỉ số và danh sách giao dịch.
     */
    @Test
    @Order(19)
    @DisplayName("TC-P4-01: Báo cáo tài chính tháng (Tổng Thu, Tổng Chi, Lợi Nhuận, activeDays = 2)")
    void testTCP401_MonthlyFinancialReportSummary() throws Exception {
        YearMonth testMonth = YearMonth.of(2026, 8);
        LocalDate day1 = testMonth.atDay(1); // 2026-08-01
        LocalDate day2 = testMonth.atDay(3); // 2026-08-03

        // Xóa sạch dữ liệu tháng test nếu có từ trước để đảm bảo tính cô lập
        List<Transaction> oldData = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(
                testMonth.atDay(1), testMonth.atEndOfMonth());
        transactionRepository.deleteAll(oldData);

        // 1. Tạo giao dịch ở ngày 1 (2026-08-01)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bún bò bán sáng")
                .amount(new BigDecimal("1000000"))
                .description("Bán 20 tô sáng")
                .transactionDate(day1)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Thịt bò")
                .amount(new BigDecimal("600000"))
                .description("Nhập thịt bắp bò")
                .transactionDate(day1)
                .build());

        // 2. Tạo giao dịch ở ngày 2 (2026-08-03)
        transactionRepository.save(Transaction.builder()
                .type(TransactionType.INCOME)
                .itemName("Bún tái bán chiều")
                .amount(new BigDecimal("400000"))
                .description("Bán 8 tô chiều")
                .transactionDate(day2)
                .build());

        transactionRepository.save(Transaction.builder()
                .type(TransactionType.EXPENSE)
                .itemName("Bún tươi")
                .amount(new BigDecimal("200000"))
                .description("Nhập 40kg bún tươi")
                .transactionDate(day2)
                .build());

        // 3. Kiểm tra trực tiếp qua TransactionService
        MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);
        assertThat(summary).isNotNull();
        assertThat(summary.getMonth()).isEqualTo(testMonth);
        assertThat(summary.getTotalIncome().compareTo(new BigDecimal("1400000"))).isZero();
        assertThat(summary.getTotalExpense().compareTo(new BigDecimal("800000"))).isZero();
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("600000"))).isZero();
        assertThat(summary.getActiveDays()).isEqualTo(2L);
        assertThat(summary.isProfitable()).isTrue();

        // 4. Kiểm tra qua HTTP GET /reports?month=2026-08
        MvcResult result = mockMvc.perform(get("/reports").param("month", testMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andExpect(model().attributeExists("summary", "expenseBreakdown", "transactions"))
                .andExpect(model().attribute("currentMonth", testMonth))
                .andExpect(model().attribute("prevMonth", testMonth.minusMonths(1)))
                .andExpect(model().attribute("nextMonth", testMonth.plusMonths(1)))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        // Kiểm tra 4 thẻ chỉ số
        assertThat(html).contains("1.400.000"); // Tổng Thu
        assertThat(html).contains("800.000");   // Tổng Chi
        assertThat(html).contains("600.000");   // Lợi Nhuận
        assertThat(html).contains("2 ngày bán"); // activeDays
        assertThat(html).contains("Có lãi ròng");
        // Kiểm tra danh sách giao dịch
        assertThat(html).contains("Bún bò bán sáng");
        assertThat(html).contains("Thịt bò");
        assertThat(html).contains("Bún tái bán chiều");
        assertThat(html).contains("Bún tươi");
    }

    /**
     * TC-P4-02: Tính Lợi nhuận trung bình mỗi ngày bán (netProfit / activeDays).
     * Kịch bản:
     * 1. Tháng 2026-08 có 2 ngày bán, tổng lãi 600.000đ.
     * 2. Kiểm tra chỉ số lãi TB/ngày = 300.000đ (600.000 / 2).
     * 3. Kiểm tra trường hợp tháng chưa có giao dịch (activeDays = 0) -> trả về 0đ an toàn, không bị chia cho 0.
     */
    @Test
    @Order(20)
    @DisplayName("TC-P4-02: Kiểm tra tính Lợi nhuận trung bình mỗi ngày bán (netProfit / activeDays)")
    void testTCP402_AverageDailyProfitCalculation() throws Exception {
        YearMonth testMonth = YearMonth.of(2026, 8);

        // 1. Kiểm tra tính toán từ Service
        MonthlySummaryDto summary = transactionService.getMonthlySummary(testMonth);
        assertThat(summary.getActiveDays()).isEqualTo(2L);
        assertThat(summary.getNetProfit().compareTo(new BigDecimal("600000"))).isZero();
        assertThat(summary.getAverageDailyProfit().compareTo(new BigDecimal("300000"))).isZero();
        assertThat(summary.getFormattedAverageDailyProfit()).isEqualTo("300.000 đ");

        // 2. Kiểm tra trên HTML view
        MvcResult result = mockMvc.perform(get("/reports").param("month", testMonth.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Lãi TB / Ngày");
        assertThat(html).contains("300.000");
        assertThat(html).contains("2 ngày bán");

        // 3. Kiểm tra Edge case: Tháng hoàn toàn không có giao dịch (activeDays = 0)
        YearMonth emptyMonth = YearMonth.of(2028, 1);
        MonthlySummaryDto emptySummary = transactionService.getMonthlySummary(emptyMonth);
        assertThat(emptySummary.getActiveDays()).isZero();
        assertThat(emptySummary.getAverageDailyProfit().compareTo(BigDecimal.ZERO)).isZero();
        assertThat(emptySummary.getFormattedAverageDailyProfit()).isEqualTo("0 đ");

        MvcResult emptyResult = mockMvc.perform(get("/reports").param("month", emptyMonth.toString()))
                .andExpect(status().isOk())
                .andReturn();
        String emptyHtml = emptyResult.getResponse().getContentAsString();
        assertThat(emptyHtml).contains("0 ngày bán");
        assertThat(emptyHtml).contains("Chưa có giao dịch nào trong tháng này");
    }

    /**
     * TC-P4-03: Cơ cấu chi tiêu toàn tháng (gom nhóm các khoản chi theo mặt hàng và tỷ lệ %).
     * Kịch bản:
     * - Tháng 2026-08 có: "Thịt bò" 600.000đ (75.0%), "Bún tươi" 200.000đ (25.0%). Tổng chi: 800.000đ.
     * Kỳ vọng:
     * - Gom nhóm chính xác tất cả các khoản chi trong tháng theo mặt hàng.
     * - Tính chính xác tỷ lệ phần trăm (Thịt bò 75%, Bún tươi 25%).
     * - HTML hiển thị danh mục và thanh tỷ lệ tương ứng.
     */
    @Test
    @Order(21)
    @DisplayName("TC-P4-03: Cơ cấu chi tiêu toàn tháng (gom nhóm mặt hàng & tỷ lệ %)")
    void testTCP403_MonthlyExpenseBreakdown() throws Exception {
        YearMonth testMonth = YearMonth.of(2026, 8);

        // 1. Kiểm tra trực tiếp qua Service
        List<CategoryBreakdownDto> breakdown = transactionService.getMonthlyExpenseBreakdown(testMonth);
        assertThat(breakdown).hasSize(2);

        // Mặt hàng chi nhiều nhất đứng đầu
        CategoryBreakdownDto item1 = breakdown.get(0);
        assertThat(item1.getItemName()).isEqualTo("Thịt bò");
        assertThat(item1.getAmount().compareTo(new BigDecimal("600000"))).isZero();
        assertThat(item1.getPercentage()).isEqualTo(75.0);
        assertThat(item1.getFormattedAmount()).isEqualTo("600.000 đ");

        CategoryBreakdownDto item2 = breakdown.get(1);
        assertThat(item2.getItemName()).isEqualTo("Bún tươi");
        assertThat(item2.getAmount().compareTo(new BigDecimal("200000"))).isZero();
        assertThat(item2.getPercentage()).isEqualTo(25.0);
        assertThat(item2.getFormattedAmount()).isEqualTo("200.000 đ");

        // 2. Kiểm tra trên HTML rendered
        MvcResult result = mockMvc.perform(get("/reports").param("month", testMonth.toString()))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Cơ Cấu Chi Tiêu Tháng");
        assertThat(html).contains("2 mặt hàng");
        assertThat(html).contains("Thịt bò");
        assertThat(html).contains("600.000 đ");
        assertThat(html).contains("75.0%");
        assertThat(html).contains("Bún tươi");
        assertThat(html).contains("200.000 đ");
        assertThat(html).contains("25.0%");
    }

    /**
     * TC-P4-04: Xuất file CSV tiếng Việt chuẩn UTF-8 BOM và RFC 4180.
     * Kịch bản:
     * 1. Gọi GET /export/csv?month=2026-08.
     * Kỳ vọng:
     * - HTTP Status 200 OK.
     * - Content-Type: "text/csv; charset=UTF-8".
     * - Content-Disposition: header attachment; filename="so_thu_chi_2026-08.csv".
     * - 3 byte đầu tiên là UTF-8 BOM: 0xEF, 0xBB, 0xBF (chuẩn mở Excel không lỗi font tiếng Việt).
     * - Nội dung chứa dòng tiêu đề tiếng Việt chuẩn: Mã GD,Ngày,Loại,Mặt hàng,Số tiền (VND),Ghi chú,Thời gian ghi.
     * - Nội dung chứa đầy đủ các dòng dữ liệu giao dịch đã ghi nhận.
     * - Kiểm tra tính năng escape trường CSV an toàn khi có ký tự đặc biệt (dấu phẩy, dấu nháy kép).
     */
    @Test
    @Order(22)
    @DisplayName("TC-P4-04: Xuất file CSV tiếng Việt chuẩn UTF-8 BOM và RFC 4180")
    void testTCP404_ExportCsvWithUtf8BomAndVietnameseHeaders() throws Exception {
        YearMonth testMonth = YearMonth.of(2026, 8);

        // 1. Gửi request GET /export/csv?month=2026-08
        MvcResult result = mockMvc.perform(get("/export/csv").param("month", testMonth.toString()))
                .andExpect(status().isOk())
                .andReturn();

        // 2. Kiểm tra Content-Type và Content-Disposition
        String contentType = result.getResponse().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType).contains("text/csv");
        assertThat(contentType).contains("UTF-8");

        String contentDisposition = result.getResponse().getHeader("Content-Disposition");
        assertThat(contentDisposition).isNotNull();
        assertThat(contentDisposition).isEqualTo("attachment; filename=\"so_thu_chi_2026-08.csv\"");

        // 3. Kiểm tra 3 byte đầu tiên là UTF-8 BOM (0xEF, 0xBB, 0xBF)
        byte[] responseBytes = result.getResponse().getContentAsByteArray();
        assertThat(responseBytes).isNotNull().hasSizeGreaterThan(3);
        assertThat(responseBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(responseBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(responseBytes[2]).isEqualTo((byte) 0xBF);

        // 4. Giải mã chuỗi nội dung từ byte thứ 3 trở đi
        String csvContent = new String(responseBytes, 3, responseBytes.length - 3, StandardCharsets.UTF_8);

        // Kiểm tra dòng tiêu đề cột tiếng Việt
        assertThat(csvContent).startsWith("Mã GD,Ngày,Loại,Mặt hàng,Số lượng,Số tiền (VND),Ghi chú,Thời gian ghi");

        // Kiểm tra các dòng giao dịch
        assertThat(csvContent).contains("2026-08-01");
        assertThat(csvContent).contains("Thu");
        assertThat(csvContent).contains("Bún bò bán sáng");
        assertThat(csvContent).contains("1000000");

        assertThat(csvContent).contains("Chi");
        assertThat(csvContent).contains("Thịt bò");
        assertThat(csvContent).contains("600000");

        assertThat(csvContent).contains("2026-08-03");
        assertThat(csvContent).contains("Bún tái bán chiều");
        assertThat(csvContent).contains("400000");

        assertThat(csvContent).contains("Bún tươi");
        assertThat(csvContent).contains("200000");

        // 5. Kiểm tra hàm escapeCsvField với các ký tự đặc biệt
        CsvExportService csvService = new CsvExportService();
        assertThat(csvService.escapeCsvField("Bún bò, chả cua")).isEqualTo("\"Bún bò, chả cua\"");
        assertThat(csvService.escapeCsvField("Bún \"đặc biệt\"")).isEqualTo("\"Bún \"\"đặc biệt\"\"\"");
        assertThat(csvService.escapeCsvField("Ghi chú\ndòng 2")).isEqualTo("\"Ghi chú\ndòng 2\"");
        assertThat(csvService.escapeCsvField("Bình thường")).isEqualTo("Bình thường");
        assertThat(csvService.escapeCsvField(null)).isEqualTo("");
    }

    /**
     * TC-P4-05: Điều hướng qua lại giữa Sổ Ngày (/) và Báo Cáo Tháng (/reports).
     * Kịch bản:
     * 1. Truy cập /: Kiểm tra Header có nút chuyển đổi tới Báo Cáo Tháng (/reports).
     * 2. Truy cập /reports: Kiểm tra Header có nút chuyển đổi quay về Sổ Hàng Ngày (/),
     *    có bộ điều hướng tháng [◀] [▶], input month picker và nút xuất CSV.
     * Kỳ vọng: Giao diện chuyển đổi liền mạch giữa 2 màn hình, touch target đạt chuẩn.
     */
    @Test
    @Order(23)
    @DisplayName("TC-P4-05: Điều hướng qua lại giữa Sổ Ngày (/) và Báo Cáo Tháng (/reports)")
    void testTCP405_NavigationBetweenDailyAndMonthlyViews() throws Exception {
        // 1. Kiểm tra trên trang chủ Sổ Hàng Ngày (/)
        MvcResult dailyResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andReturn();

        String dailyHtml = dailyResult.getResponse().getContentAsString();
        // Có tab Sổ Hàng Ngày
        assertThat(dailyHtml).contains("🍜 Sổ Hàng Ngày");
        // Có tab Báo Cáo Tháng dẫn sang /reports
        assertThat(dailyHtml).contains("📊 Báo Cáo Tháng");
        assertThat(dailyHtml).contains("href=\"/reports\"");

        // 2. Kiểm tra trên trang Báo Cáo Tháng (/reports)
        MvcResult reportResult = mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andReturn();

        String reportHtml = reportResult.getResponse().getContentAsString();
        // Có nút quay về Sổ Hàng Ngày
        assertThat(reportHtml).contains("🍜 Sổ Hàng Ngày");
        assertThat(reportHtml).contains("href=\"/\"");
        // Có tab Báo Cáo đang active
        assertThat(reportHtml).contains("📊 Báo Cáo");
        // Có nút Xuất file CSV
        assertThat(reportHtml).contains("XUẤT FILE CSV (EXCEL)");
        assertThat(reportHtml).contains("/export/csv");
        // Có bộ chọn và điều hướng tháng
        assertThat(reportHtml).contains("[◀]");
        assertThat(reportHtml).contains("[▶]");
        assertThat(reportHtml).contains("id=\"monthPicker\"");
        assertThat(reportHtml).contains("type=\"month\"");
    }
}
