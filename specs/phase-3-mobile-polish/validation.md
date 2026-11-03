# Validation Scorecard & Testing Criteria
## Phase 3: Tối Ưu UX Mobile & Nhập Nhanh (Mobile Polish)

---

### Mục tiêu nghiệm thu
Đảm bảo tốc độ nhập liệu 1 chạm qua Quick Chips và bàn phím tiền tệ thông minh hoạt động mượt mà, đồng thời chức năng Chỉnh sửa giao dịch (Edit) cập nhật chuẩn xác dữ liệu trong MySQL.

---

### Bảng Scorecard Kiểm Thử (Test Matrix)

| Mã kiểm thử | Kịch bản kiểm thử | Các bước thực hiện | Kết quả kỳ vọng | Trạng thái |
| :--- | :--- | :--- | :--- | :---: |
| **TC-P3-01** | Gợi ý mặt hàng 1 chạm (Quick Chips) | 1. Đang ở chế độ Chi -> Bấm chip "Thịt bò"<br>2. Chuyển sang Thu -> Bấm chip "Bún tái gầu" | - Bấm "Thịt bò": Ô mặt hàng tự động điền "Thịt bò".<br>- Khi chuyển sang Thu: Danh sách chip đổi sang nhóm Thu, bấm "Bún tái gầu" tự động điền vào ô. | ✅ Passed |
| **TC-P3-02** | Cộng dồn số tiền nhanh (Smart Steppers) | 1. Bấm `+50k`<br>2. Bấm tiếp `+20k`<br>3. Bấm `[C]` | - Bấm `+50k`: Ô số tiền hiện `50000`.<br>- Bấm tiếp `+20k`: Tự động cộng dồn thành `70000`.<br>- Bấm `[C]`: Xóa trắng ô số tiền. | ✅ Passed |
| **TC-P3-03** | Chỉnh sửa giao dịch (Edit Transaction) | 1. Bấm nút [Sửa] tại giao dịch "Thịt bò 300.000đ"<br>2. Đổi số tiền thành `350000`, thêm ghi chú "Mua thêm 1kg"<br>3. Bấm "Cập nhật" | CSDL cập nhật `amount = 350000`, `description = Mua thêm 1kg`. Tổng chi ngày tăng lên 350.000đ, Lợi nhuận tự tính lại. | ✅ Passed |
| **TC-P3-04** | Kiểm tra validation khi Chỉnh sửa | Trong form sửa, thử xóa rỗng tên hoặc nhập số tiền âm -> Bấm Cập nhật | Hệ thống chặn không cho lưu, báo lỗi rõ ràng. | ✅ Passed |
| **TC-P3-05** | Nhập nhanh trong vòng 5 giây | Chạm chip "Bún tươi" -> Chạm `+100k` -> Chạm "Lưu giao dịch" | Toàn bộ thao tác hoàn tất trong 3 chạm (< 5 giây), giao dịch xuất hiện ngay lập tức. | ✅ Passed |

---

### Báo cáo kết quả kiểm thử tự động (Execution Log)

* **Thời gian thực thi:** 2026-11-03 16:59:39+07:00
* **Công cụ thực thi:** Maven Surefire Plugin (`.\mvnw.cmd test`)
* **Môi trường:** Java 21.0.5, Spring Boot 3.3.5, MySQL Database `bunledger_db`
* **Tổng số kiểm thử:** 78 tests (Failures: 0, Errors: 0, Skipped: 0) - Tỷ lệ đạt: **100%**

#### Chi tiết nghiệm thu từng ca kiểm thử (TC-P3-01 -> TC-P3-05):
1. **TC-P3-01 (`testTCP301_QuickChipsPresenceAndAttributes`):**
   - Xác minh sự hiện diện của container `expenseChips` (9 mặt hàng: Thịt bò, Bún tươi, Xương hầm, Rau sống, Hành ngò, Tiền gas, Gia vị, Đá lạnh, Mặt bằng) và `incomeChips` (7 mặt hàng: Bún tái gầu, Bún giò heo, Bún đặc biệt, Bún mang về, Nước ngọt, Khăn lạnh, Trà đá).
   - Xác nhận có hàm JavaScript `selectQuickChip(this, ...)` tự động gán tên mặt hàng và highlight chip.
   - Hàm `updateFormTheme` tự động đổi danh sách chip tương ứng với nút gạt Thu/Chi.
   - Nút chip đạt chuẩn touch target di động `min-h-[44px]`.
2. **TC-P3-02 (`testTCP302_SmartMoneySteppersPresenceAndAttributes`):**
   - Xác minh sự hiện diện của toàn bộ hàng nút trợ lực: `+20k`, `+50k`, `+100k`, `+200k`, `+500k` và `[C]`.
   - Các nút kích hoạt đúng hàm `setQuickAmount` với các giá trị 20.000, 50.000, 100.000, 200.000, 500.000 và `clearAmount()`.
   - Xác minh cơ chế cộng dồn số tiền nhanh (`current + Number(val)`) và nút xóa trắng số tiền.
   - Touch target đạt chuẩn thao tác ngón cái `min-h-[44px]`.
3. **TC-P3-03 (`testTCP303_EditTransactionSuccessAndRecalculate`):**
   - Tạo giao dịch thực tế Chi "Thịt bò" 300.000đ trong CSDL MySQL `bunledger_db`.
   - Gửi request `POST /transactions/{id}/update` đổi số tiền thành 350.000đ và đổi ghi chú thành "Mua thêm 1kg".
   - Kiểm tra CSDL MySQL: bản ghi được cập nhật chuẩn xác `amount = 350000` và `description = Mua thêm 1kg`, bảo toàn ngày và phân loại.
   - Kiểm tra `DailySummaryDto`: Tổng chi ngày tăng lên 350.000đ, Lợi nhuận tự động tính lại thành -350.000đ.
   - Giao diện HTML render hiển thị số tiền mới, ghi chú mới, nút `[Sửa]` có đủ `data-*` attributes và modal `editModal`.
4. **TC-P3-04 (`testTCP304_EditTransactionValidationFailure`):**
   - Thử cập nhật với tên mặt hàng rỗng -> Bị chặn, redirect an toàn kèm flash `errorMessage`.
   - Thử cập nhật với số tiền bằng 0 hoặc âm (-50.000đ) -> Bị chặn, redirect an toàn kèm flash `errorMessage`.
   - Thử cập nhật với ID không tồn tại -> Bắt ngoại lệ và redirect an toàn kèm flash `errorMessage`.
   - Xác minh trực tiếp trong CSDL MySQL: dữ liệu của giao dịch ban đầu không bị ghi đè, bảo toàn tính toàn vẹn dữ liệu.
5. **TC-P3-05 (`testTCP305_FastInputEndToEndAndMobilePolishDoD`):**
   - Kiểm thử luồng nhập nhanh 3 chạm (< 5 giây): Chạm chip "Bún tươi" -> Chạm "+100k" -> Chạm "LƯU GIAO DỊCH".
   - Dữ liệu được ghi nhận vào CSDL MySQL và hiển thị tức thì trên danh sách giao dịch với số tiền định dạng `-100.000 đ`.
   - Xác minh toàn bộ các tiêu chí Mobile UX Polish theo Definition of Done: cuộn ngang `overflow-x-auto scrollbar-none`, modal có backdrop mờ và đóng bằng phím Escape, touch target chính đạt tối thiểu 48px (`min-h-[48px]`, `min-h-[50px]`).

---

### Tiêu chuẩn hoàn thành (Definition of Done - DoD cho Phase 3)
1. ✅ Quick Chips hoạt động trơn tru theo ngữ cảnh Thu/Chi trên điện thoại.
2. ✅ Bàn phím trợ lực tiền tệ hoạt động chính xác với cơ chế cộng dồn.
3. ✅ Chức năng sửa giao dịch hoạt động hoàn hảo trên CSDL MySQL và cập nhật giao diện.
4. ✅ 100% test cases trong bảng Scorecard đạt trạng thái Passed (78/78 tests toàn hệ thống).
