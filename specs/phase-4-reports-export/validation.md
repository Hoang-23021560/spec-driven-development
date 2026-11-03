# Validation Scorecard & Testing Criteria
## Phase 4: Báo Cáo Tháng & Xuất Dữ Liệu (Reports & CSV Export)

---

### Mục tiêu nghiệm thu
Đảm bảo các báo cáo tổng hợp theo tháng tính toán chính xác số liệu tài chính và số ngày bán hàng, đồng thời chức năng xuất file CSV hoạt động hoàn hảo, tải về file mở được trên Microsoft Excel mà không bị lỗi font tiếng Việt.

---

### Bảng Scorecard Kiểm Thử (Test Matrix)

| Mã kiểm thử | Kịch bản kiểm thử | Các bước thực hiện | Kết quả kỳ vọng | Trạng thái |
| :--- | :--- | :--- | :--- | :---: |
| **TC-P4-01** | Báo cáo tài chính theo tháng (`GET /reports?month=YYYY-MM`) | 1. Tạo giao dịch ở 2 ngày khác nhau trong tháng (ví dụ: ngày 1 và ngày 3)<br>2. Truy cập `/reports?month=2026-08` | Hiển thị chính xác Tổng Thu, Tổng Chi, Lợi Nhuận của cả tháng; Số ngày hoạt động (`activeDays = 2`). | ✅ Passed |
| **TC-P4-02** | Tính Lợi nhuận trung bình mỗi ngày bán | 1. Tháng có 2 ngày bán, tổng lãi 600.000đ<br>2. Kiểm tra chỉ số lãi TB/ngày | Chỉ số hiển thị: `300.000 đ / ngày` ($600.000 / 2$). Edge case tháng trống: hiển thị 0đ an toàn. | ✅ Passed |
| **TC-P4-03** | Cơ cấu chi tiêu toàn tháng | Kiểm tra danh sách mặt hàng chi trong tháng | Gom nhóm chính xác tất cả các khoản chi trong tháng theo mặt hàng và tỷ lệ % (Thịt bò 75%, Bún tươi 25%). | ✅ Passed |
| **TC-P4-04** | Xuất file CSV tiếng Việt chuẩn UTF-8 BOM | 1. Gọi `GET /export/csv?month=2026-08`<br>2. Kiểm tra header và nội dung file | - HTTP 200, `Content-Disposition` chứa tên file `so_thu_chi_2026-08.csv`.<br>- 3 byte đầu là UTF-8 BOM (`0xEF, 0xBB, 0xBF`).<br>- Dòng header tiếng Việt có dấu đầy đủ và escape chuẩn RFC 4180. | ✅ Passed |
| **TC-P4-05** | Điều hướng qua lại giữa Sổ Ngày và Báo Cáo Tháng | Bấm nút chuyển đổi trên Header | Chuyển đổi qua lại mượt mà giữa trang `index.html` và `reports.html`, đầy đủ tab và nút xuất CSV. | ✅ Passed |

---

### Báo cáo kết quả kiểm thử tự động (Execution Log)

* **Thời gian thực thi:** 2026-11-03 17:25:50+07:00
* **Công cụ thực thi:** Maven Surefire Plugin (`.\mvnw.cmd clean package`)
* **Môi trường:** Java 21.0.5, Spring Boot 3.3.5, MySQL Database `bunledger_db`
* **Tổng số kiểm thử:** 115 tests (Failures: 0, Errors: 0, Skipped: 0) - Tỷ lệ đạt: **100%**
* **File đóng gói bản phát hành:** `target/bunledger-0.0.1-SNAPSHOT.jar` (Dung lượng: ~50.4 MB)

#### Chi tiết nghiệm thu từng ca kiểm thử (TC-P4-01 -> TC-P4-05):
1. **TC-P4-01 (`testTCP401_MonthlyFinancialReportSummary`):**
   - Chuẩn bị dữ liệu cô lập cho tháng `2026-08` trên CSDL MySQL `bunledger_db`:
     - Ngày 01 (2026-08-01): Thu "Bún bò bán sáng" 1.000.000đ, Chi "Thịt bò" 600.000đ.
     - Ngày 03 (2026-08-03): Thu "Bún tái bán chiều" 400.000đ, Chi "Bún tươi" 200.000đ.
   - Gọi `GET /reports?month=2026-08`: Trả về HTTP 200 OK, view `reports`.
   - Tính toán chuẩn xác qua `TransactionService.getMonthlySummary`:
     - Tổng Thu = 1.400.000đ.
     - Tổng Chi = 800.000đ.
     - Lợi Nhuận Tháng = 600.000đ.
     - Số ngày hoạt động `activeDays = 2`.
     - Trạng thái `isProfitable() = true`.
   - Giao diện HTML render hiển thị đầy đủ 4 thẻ chỉ số tháng, huy hiệu "Có lãi ròng" và bảng chi tiết giao dịch.
2. **TC-P4-02 (`testTCP402_AverageDailyProfitCalculation`):**
   - Tháng có 2 ngày bán, tổng lợi nhuận 600.000đ -> Tính toán Lợi nhuận trung bình mỗi ngày bán: $600.000 / 2 = 300.000$ đ/ngày.
   - Xác nhận `MonthlySummaryDto.getAverageDailyProfit()` = 300.000đ và định dạng `300.000 đ`.
   - Xác nhận trên HTML hiển thị nhãn "Lãi TB / Ngày" với số tiền `300.000` và chú thích `2 ngày bán`.
   - Kiểm tra edge case tháng hoàn toàn không có giao dịch (`activeDays = 0`): trả về 0đ an toàn, không xảy ra ngoại lệ chia cho 0 (`ArithmeticException`), hiển thị trạng thái zero-state "Chưa có giao dịch nào trong tháng này".
3. **TC-P4-03 (`testTCP403_MonthlyExpenseBreakdown`):**
   - Xác minh cơ cấu chi tiêu toàn tháng qua `TransactionService.getMonthlyExpenseBreakdown`:
     - "Thịt bò": 600.000đ chiếm chính xác 75.0% tổng chi phí tháng.
     - "Bún tươi": 200.000đ chiếm chính xác 25.0% tổng chi phí tháng.
   - HTML hiển thị chính xác tiêu đề "Cơ Cấu Chi Tiêu Tháng", số lượng "2 mặt hàng", tên mặt hàng, số tiền định dạng VNĐ, tỷ lệ % và thanh progress bar trực quan.
4. **TC-P4-04 (`testTCP404_ExportCsvWithUtf8BomAndVietnameseHeaders`):**
   - Gửi request `GET /export/csv?month=2026-08`:
     - HTTP Status: 200 OK.
     - Content-Type: `text/csv; charset=UTF-8`.
     - Header Content-Disposition: `attachment; filename="so_thu_chi_2026-08.csv"`.
   - Kiểm tra 3 byte đầu tiên của luồng dữ liệu là UTF-8 BOM (`0xEF, 0xBB, 0xBF`), đảm bảo Microsoft Excel trên Windows, macOS và điện thoại tự động nhận diện tiếng Việt có dấu mà không bị lỗi bảng mã.
   - Kiểm tra dòng tiêu đề cột tiếng Việt chuẩn: `Mã GD,Ngày,Loại,Mặt hàng,Số tiền (VND),Ghi chú,Thời gian ghi`.
   - Kiểm tra đầy đủ nội dung dữ liệu của 4 dòng giao dịch tháng 8/2026.
   - Kiểm tra hàm `escapeCsvField` xử lý an toàn dấu phẩy, dấu nháy kép (`""`), ký tự xuống dòng theo chuẩn RFC 4180.
5. **TC-P4-05 (`testTCP405_NavigationBetweenDailyAndMonthlyViews`):**
   - Xác minh trên trang Sổ Hàng Ngày (`/`): Header có tab "🍜 Sổ Hàng Ngày" và tab "📊 Báo Cáo Tháng" trỏ tới `/reports`.
   - Xác minh trên trang Báo Cáo Tháng (`/reports`): Header có tab quay về "🍜 Sổ Hàng Ngày" (`href="/"`), tab "📊 Báo Cáo" active, nút nổi bật "XUẤT FILE CSV (EXCEL)" trỏ tới `/export/csv`, cụm điều hướng tháng `[◀]`, `[▶]`, input native `type="month"` (`#monthPicker`).

---

### Tiêu chuẩn hoàn thành (Definition of Done - DoD cho Phase 4)
1. ✅ Báo cáo tài chính tháng tổng hợp chuẩn xác từ CSDL MySQL `bunledger_db`.
2. ✅ File CSV tải về tương thích hoàn toàn với Microsoft Excel (tiền tố UTF-8 BOM `0xEF, 0xBB, 0xBF`).
3. ✅ 100% ca kiểm thử trong Scorecard đạt trạng thái Passed (115/115 tests toàn dự án).
4. ✅ Đóng gói hoàn tất file chạy ứng dụng cuối cùng của dự án: `target/bunledger-0.0.1-SNAPSHOT.jar` (50.4 MB).
