# Validation Scorecard & Testing Criteria
## Phase 2: Biểu Đồ Tròn Trực Quan (Chart.js) & Xem Lại Theo Ngày

---

### Mục tiêu nghiệm thu
Xác minh ứng dụng hoạt động chính xác khi xem lại lịch sử các ngày khác nhau, tính toán cơ cấu chi phí đúng từng mặt hàng và hiển thị mượt mà 2 biểu đồ tròn Chart.js trên thiết bị di động.

---

### Bảng Scorecard Kiểm Thử (Test Matrix)

| Mã kiểm thử | Kịch bản kiểm thử | Các bước thực hiện | Kết quả kỳ vọng | Trạng thái |
| :--- | :--- | :--- | :--- | :---: |
| **TC-P2-01** | Lọc dữ liệu theo ngày (`?date=YYYY-MM-DD`) | 1. Truy cập `/?date=2026-11-02`<br>2. Kiểm tra dữ liệu hiển thị | Chỉ hiển thị các giao dịch phát sinh trong ngày `2026-11-02`, các thẻ Tổng Thu, Tổng Chi, Lợi Nhuận khớp với ngày đó. | ✅ Passed |
| **TC-P2-02** | Điều hướng ngày (Lùi / Tiến / Hôm nay) | 1. Đang ở ngày hôm nay, bấm `[◀]`<br>2. Bấm nút `Hôm nay` | - Bấm `[◀]` chuyển sang ngày hôm qua (`today - 1`).<br>- Bấm `Hôm nay` chuyển thẳng về ngày hiện tại. | ✅ Passed |
| **TC-P2-03** | Tính toán cơ cấu chi phí theo mặt hàng | 1. Tạo 2 khoản chi: Thịt bò 300.000đ, Bún tươi 100.000đ<br>2. Gọi hàm tính cơ cấu | Trả về 2 mục: Thịt bò (300.000đ, chiếm 75%), Bún tươi (100.000đ, chiếm 25%). | ✅ Passed |
| **TC-P2-04** | Biểu đồ tròn Thu vs Chi | 1. Nhập Thu 600.000đ, Chi 400.000đ<br>2. Kiểm tra dữ liệu truyền vào biểu đồ | Biểu đồ tròn vành khuyên chia tỷ lệ: Thu chiếm 60% (xanh), Chi chiếm 40% (đỏ). | ✅ Passed |
| **TC-P2-05** | Biểu đồ rỗng khi ngày không có dữ liệu | Chọn 1 ngày hoàn toàn trống (ví dụ ngày trong tương lai) | Hiển thị biểu đồ trung tính không bị lỗi JS, hiển thị thông báo "Chưa có dữ liệu thu chi ngày này". | ✅ Passed |
| **TC-P2-06** | Nhập bù giao dịch cho ngày đã chọn | 1. Đang xem ngày `2026-11-01`<br>2. Nhập khoản chi "Tiền gas 450.000đ"<br>3. Bấm Lưu | Bản ghi được lưu với `transaction_date = 2026-11-01`, sau khi lưu trình duyệt vẫn giữ nguyên trang `/?date=2026-11-01`. | ✅ Passed |

---

### Báo cáo kết quả kiểm thử tự động (Execution Log)

* **Thời gian thực thi:** 2026-11-03 16:36:04+07:00
* **Công cụ thực thi:** Maven Surefire Plugin (`.\mvnw.cmd test`)
* **Môi trường:** Java 21.0.5, Spring Boot 3.3.5, MySQL Database `bunledger_db`
* **Tổng số kiểm thử:** 61 tests (Failures: 0, Errors: 0, Skipped: 0) - Tỷ lệ đạt: **100%**

#### Chi tiết nghiệm thu từng ca kiểm thử (TC-P2-01 -> TC-P2-06):
1. **TC-P2-01 (`testTCP201_FilterDataByDate`):** Đã kiểm tra lọc dữ liệu ngày `2026-11-02` với CSDL MySQL thực tế. Tổng thu 450.000đ, Tổng chi 50.000đ, Lợi nhuận 400.000đ. Bản ghi của ngày khác (`2026-10-31`) bị loại trừ hoàn toàn.
2. **TC-P2-02 (`testTCP202_DateNavigationLinks`):** Nút `[◀]` sinh đúng link `/?date=2026-11-01`, nút `[▶]` sinh đúng link `/?date=2026-11-03`, nút `Hôm nay` xuất hiện khi xem ngày quá khứ và trỏ thẳng về `LocalDate.now()`.
3. **TC-P2-03 (`testTCP203_ExpenseBreakdownCalculation`):** Kiểm tra `TransactionService.getExpenseBreakdown` với 2 khoản chi: Thịt bò 300.000đ (75.0%) và Bún tươi 100.000đ (25.0%). Thymeleaf render chính xác số tiền và tỷ lệ phần trăm.
4. **TC-P2-04 (`testTCP204_IncomeVsExpenseChartData`):** Tạo Thu 600.000đ, Chi 400.000đ; Model nhận `chartIncome = 600000`, `chartExpense = 400000`, tỷ trọng chính xác 60.0% vs 40.0%, canvas `incomeExpenseChart` được render thành công.
5. **TC-P2-05 (`testTCP205_ZeroStateForEmptyDate`):** Truy vấn ngày `2029-12-31` hoàn toàn trống dữ liệu; HTML hiển thị trạng thái rỗng "Chưa có dữ liệu thu chi ngày này" và "Chưa có giao dịch nào trong ngày này", không có lỗi JavaScript và canvas được ẩn một cách an toàn.
6. **TC-P2-06 (`testTCP206_SaveTransactionForPastDateAndRedirect`):** Gửi form nhập "Tiền gas 450.000đ" với `transactionDate = 2026-11-01`. Dữ liệu được ghi nhận chuẩn xác vào bảng `transactions` trong CSDL MySQL và hệ thống redirect chuẩn `/?date=2026-11-01`.

---

### Tiêu chuẩn hoàn thành (Definition of Done - DoD cho Phase 2)
1. ✅ Có bộ điều hướng ngày tiện dụng, thân thiện trên màn hình di động.
2. ✅ 2 biểu đồ tròn (Doughnut Chart) hiển thị trực quan, mượt mà bằng thư viện Chart.js.
3. ✅ 100% ca kiểm thử trong bảng Scorecard trên vượt qua kiểm tra tự động.

