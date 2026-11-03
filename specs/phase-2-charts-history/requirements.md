# Functional Requirements & UX Specifications
## Phase 2: Biểu Đồ Tròn Trực Quan (Chart.js) & Xem Lại Theo Ngày

---

### 1. Tổng quan tính năng (Feature Overview)
Phase 2 nâng cấp ứng dụng từ sổ ghi đơn thuần thành công cụ phân tích tài chính trực quan cho chủ quán bún:
1. **Bộ điều hướng ngày (Date Navigator):** Cho phép xem sổ thu chi của bất kỳ ngày nào trong quá khứ hoặc tương lai, hỗ trợ phím chuyển ngày nhanh (Hôm qua, Hôm nay, Lùi 1 ngày `[◀]`, Tiến 1 ngày `[▶]`).
2. **Biểu đồ tròn Thu vs Chi:** Sử dụng thư viện Chart.js hiển thị tỷ trọng phần trăm giữa Tổng Thu và Tổng Chi trong ngày.
3. **Biểu đồ tròn Cơ cấu Chi phí:** Thống kê chi tiết quán đã chi bao nhiêu % cho từng mặt hàng (Thịt bò, Bún, Rau, Tiền gas...) để tối ưu chi phí nguyên liệu.
4. **Nhập bù giao dịch đúng ngày:** Khi đang xem ngày nào, thêm giao dịch mới sẽ tự động gắn ngày đó và giữ nguyên trang đang xem.

---

### 2. Yêu cầu chức năng chi tiết (Functional Requirements)

#### FR-2.1: Bộ điều hướng ngày linh hoạt (Date Navigation Bar)
* **Thanh công cụ ngày trên đầu trang:**
  * Nút `[◀] Lùi ngày`: Chuyển sang ngày trước đó liền kề.
  * Ô chọn ngày (`Date Picker`): Bấm vào mở lịch native của điện thoại/trình duyệt để nhảy đến ngày bất kỳ.
  * Nút `[▶] Tiến ngày`: Chuyển sang ngày kế tiếp.
  * Nút nhanh `Hôm nay`: Nhảy ngay về ngày hiện tại nếu đang xem ngày khác.
* **Định dạng hiển thị:** Luôn có nhãn tiếng Việt rõ ràng:
  * Ví dụ: *"Hôm nay, Thứ Ba, 03/11/2026"* hoặc *"Hôm qua, Thứ Hai, 02/11/2026"*, hoặc *"Thứ Sáu, 28/10/2026"*.
* **Đồng bộ URL:** Đường dẫn tương ứng dạng `/?date=YYYY-MM-DD` hoặc `/transactions?date=YYYY-MM-DD`.

#### FR-2.2: Phân tích cơ cấu chi tiêu (Expense Category Breakdown)
* Hệ thống tự động gom nhóm (`GROUP BY`) các khoản chi (`type = EXPENSE`) theo mặt hàng (`itemName`) trong ngày đang chọn.
* Trả về danh sách gồm:
  * Tên mặt hàng (`categoryName`)
  * Tổng tiền chi cho mặt hàng đó (`amount`)
  * Tỷ trọng phần trăm (`percentage = (amount / totalExpense) * 100%`)
  * Số tiền định dạng hiển thị tiếng Việt (`formattedAmount`)

#### FR-2.3: Biểu đồ tròn Thu vs Chi (Income vs Expense Chart)
* **Loại biểu đồ:** Doughnut Chart (Biểu đồ vành khuyên) của Chart.js.
* **Màu sắc nhận diện:**
  * Thu (Income): Xanh ngọc lục bảo (`#059669` / Tailwind Emerald-600).
  * Chi (Expense): Đỏ cam nổi bật (`#e11d48` / Tailwind Rose-600).
* **Số liệu trung tâm:** Hiển thị tổng thu, tổng chi và tỷ lệ % khi hover / tap trên điện thoại.
* **Trạng thái rỗng (Zero state):** Nếu ngày đó chưa có cả thu lẫn chi, hiển thị vành xám trung tính với thông báo *"Chưa có dữ liệu thu chi ngày này"*.

#### FR-2.4: Biểu đồ tròn Cơ cấu Chi Phí (Expense Distribution Chart)
* **Loại biểu đồ:** Doughnut / Pie Chart phân bổ theo từng mặt hàng chi.
* Bảng màu trực quan, phân biệt rõ các loại chi phí (Thịt, Bún, Rau, Gas, Mặt bằng...).
* Chú giải (Legend) ngắn gọn, chạm vào từng phần trên mobile hiển thị số tiền chính xác.

#### FR-2.5: Nhập bù giao dịch theo ngày đang xem
* Khi chủ quán đang ở trang ngày `2026-11-02` (Hôm qua), form nhập nhanh sẽ mặc định lưu cho ngày `2026-11-02`.
* Sau khi submit thành công, hệ thống redirect về đúng ngày đang xem (`redirect:/?date=2026-11-02`) thay vì nhảy về hôm nay, giúp việc nhập bù nhiều khoản liên tiếp diễn ra liền mạch.

---

### 3. Ràng buộc UX & Kỹ thuật
* **Hiệu năng:** Biểu đồ vẽ trực tiếp trên trình duyệt bằng Chart.js qua Canvas HTML5, dữ liệu JSON nhúng trực tiếp qua Thymeleaf không cần gọi API phụ để tránh nhấp nháy giật lag.
* **Mobile Friendly:** Kích thước biểu đồ co giãn tỷ lệ (responsive aspect ratio) phù hợp với màn hình smartphone, không chiếm quá nửa màn hình làm đẩy form nhập xuống quá sâu.

