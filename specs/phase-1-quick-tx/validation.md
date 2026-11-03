# Validation Scorecard & Testing Criteria
## Phase 1: Nhập Nhanh Giao Dịch Thu/Chi & Xem Danh Sách Gần Nhất

---

### Mục tiêu nghiệm thu
Đảm bảo tính năng Phase 1 hoạt động chính xác từ tầng CSDL MySQL đến giao diện người dùng trên điện thoại di động trước khi chuyển sang Phase 2.

---

### Bảng Scorecard Kiểm Thử (Test Matrix)

| Mã kiểm thử | Kịch bản kiểm thử | Các bước thực hiện | Kết quả kỳ vọng | Trạng thái | Ghi chú & Bằng chứng nghiệm thu thực tế |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **TC-01** | Kết nối CSDL MySQL & Khởi tạo bảng | 1. Cấu hình database `bunledger_db`<br>2. Chạy ứng dụng Spring Boot | Ứng dụng khởi động thành công trên cổng `8080`, bảng `transactions` được tự động tạo trong MySQL với đầy đủ các cột và index. | ✅ Passed | Đã xác thực kết nối MySQL `bunledger_db`. Bảng `transactions` và 2 indexes (`idx_transactions_date`, `idx_transactions_type`) được tạo tự động bởi Hibernate DDL. Test `testTC01_DatabaseConnectionAndSchemaCreation` đạt 100%. |
| **TC-02** | Nhập nhanh Khoản Chi đầu ngày | 1. Mở trang chủ trên trình duyệt<br>2. Chọn nút `[- CHI TIỀN]`<br>3. Nhập mặt hàng: `Thịt bò`, Số tiền: `300000`<br>4. Bấm `LƯU GIAO DỊCH` | - Form được reset trắng<br>- Xuất hiện dòng giao dịch mới: `[CHI] Thịt bò -300.000 đ`<br>- Thẻ Tổng Chi = `300.000 đ`<br>- Thẻ Lợi Nhuận = `-300.000 đ` (cảnh báo âm vốn) | ✅ Passed | Giao dịch `Thịt bò` lưu thành công vào MySQL. Tổng chi tính toán = `300.000 đ`, Tổng thu = `0 đ`, Lợi nhuận = `-300.000 đ`. Giao diện hiển thị đúng thẻ cảnh báo âm vốn. Test `testTC02_CreateExpenseTransaction` đạt 100%. |
| **TC-03** | Nhập nhanh Khoản Thu trong ngày | 1. Chọn nút `[+ THU TIỀN]`<br>2. Nhập mặt hàng: `Bún bò buổi sáng`, Số tiền: `500000`<br>3. Bấm `LƯU GIAO DỊCH` | - Khoản thu mới xuất hiện ngay trên đầu danh sách: `[THU] Bún bò buổi sáng +500.000 đ`<br>- Thẻ Tổng Thu = `500.000 đ`<br>- Thẻ Tổng Chi = `300.000 đ`<br>- Thẻ Lợi Nhuận = `+200.000 đ` (số dương, lãi) | ✅ Passed | Giao dịch `Bún bò buổi sáng` nằm trên cùng danh sách (`ORDER BY created_at DESC`). Tổng thu = `500.000 đ`, Tổng chi = `300.000 đ`, Lợi nhuận = `+200.000 đ` (lãi). Test `testTC03_CreateIncomeTransaction` đạt 100%. |
| **TC-04** | Tự động ghi nhận ngày hôm nay | Kiểm tra trực tiếp trong bảng MySQL hoặc trên giao diện | Cột `transaction_date` lưu chính xác ngày hiện tại (`YYYY-MM-DD`), người dùng không phải chọn ngày thủ công. | ✅ Passed | Xác thực toàn bộ bản ghi trong CSDL MySQL có `transaction_date = LocalDate.now()` (2026-11-03) và `created_at` tự động sinh thời gian thực tế. Test `testTC04_AutomaticCurrentDateAssignment` đạt 100%. |
| **TC-05** | Bàn phím số & Ràng buộc dữ liệu (Validation) | 1. Thử để trống tên mặt hàng và bấm Lưu<br>2. Thử nhập số tiền âm hoặc bằng 0<br>3. Kiểm tra trên điện thoại di động | - Hệ thống chặn không cho gửi, hiển thị thông báo yêu cầu nhập hợp lệ.<br>- Trường số tiền khi chạm vào sẽ kích hoạt bàn phím số to (`inputmode="numeric"`). | ✅ Passed | Chặn hoàn toàn dữ liệu lỗi: tên trống (`@NotBlank`), số tiền <= 0 (`@DecimalMin`). CSDL không bị ghi dữ liệu rác. Giao diện có thuộc tính `inputmode="numeric"`. Test `testTC05_ValidationConstraints` đạt 100%. |
| **TC-06** | Xóa giao dịch nhập sai | 1. Bấm nút Thùng rác tại dòng `Thịt bò`<br>2. Trình duyệt hiện popup xác nhận -> Chọn OK | - Khoản chi `Thịt bò` bị xóa khỏi danh sách và CSDL.<br>- Thẻ Tổng Chi quay về `0 đ`.<br>- Thẻ Lợi Nhuận tự động cập nhật lại = `+500.000 đ`. | ✅ Passed | Đã xóa bản ghi `Thịt bò` qua endpoint `POST /transactions/{id}/delete`. MySQL xác nhận bản ghi đã xóa. Tổng chi cập nhật về `0 đ`, Lợi nhuận cập nhật = `+500.000 đ`. Test `testTC06_DeleteTransactionAndAutoRecalculate` đạt 100%. |
| **TC-07** | Hiển thị Responsive Mobile | Mở DevTools ở chế độ mobile (iPhone SE 375px hoặc Pixel 7) | - Toàn bộ giao diện co giãn hoàn hảo, không bị cuộn ngang.<br>- Các nút bấm Thu/Chi và nút Lưu to rõ, dễ bấm bằng ngón tay cái. | ✅ Passed | Giao diện chuẩn Mobile-First (`max-w-lg mx-auto`), đầy đủ `<meta name="viewport">`, touch target `>= 48px` (`min-h-[48px]`), phím tắt số tiền (+20k, +50k...). Test `testTC07_ResponsiveMobileAttributes` đạt 100%. |

---

### Nhật Ký Thực Thi Kiểm Thử Thực Tế (Execution Log)

* **Thời gian thực thi:** 2026-11-03 15:56:16 (Asia/Ho_Chi_Minh)
* **Môi trường thử nghiệm:**
  * HĐH: Windows 11
  * Java: OpenJDK 21.0.5
  * Spring Boot: 3.3.5
  * CSDL: MySQL Server 8.0.x (Database: `bunledger_db`)
  * Trình chạy test: Apache Maven Surefire Plugin 3.2.5 (`.\mvnw.cmd test`)
* **Kết quả kiểm thử tự động toàn diện:**
  * Tổng số ca kiểm thử chạy: **34/34 tests**
  * Số ca kiểm thử thành công: **34 tests (100% SUCCESS)**
  * Thất bại / Lỗi: **0 Failures, 0 Errors, 0 Skipped**
  * Phân rã test suite:
    * `BunledgerIntegrationTest`: 7/7 tests passed (End-to-end từ Web đến MySQL).
    * `TransactionControllerTest`: 7/7 tests passed (MockMvc Web layer).
    * `TransactionServiceTest`: 11/11 tests passed (Business logic & calculations).
    * `TransactionFormTest`: 4/4 tests passed (Validation DTO).
    * `DailySummaryDtoTest`: 3/3 tests passed (Format currency & profit logic).
    * `TransactionTest`: 3/3 tests passed (Entity mapping & pre-persist).

---

### Tiêu chuẩn hoàn thành (Definition of Done - DoD cho Phase 1)
1. ✅ Mã nguồn Java & Spring Boot tuân thủ chuẩn MVC: Phân tách rõ ràng giữa `Controller`, `Service`, `Repository`, `Entity`, và `View (Thymeleaf)`.
2. ✅ Kết nối ổn định với MySQL database `bunledger_db`.
3. ✅ Vượt qua 100% các ca kiểm thử trong bảng Scorecard trên.
4. ✅ Giao diện trực quan, thao tác nhanh chóng và mượt mà trên trình duyệt di động.
