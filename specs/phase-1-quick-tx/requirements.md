# Functional Requirements & UX Specifications
## Phase 1: Nhập Nhanh Giao Dịch Thu/Chi & Xem Danh Sách Gần Nhất

---

### 1. Tổng quan tính năng (Feature Overview)
Cung cấp cho chủ quán bún một màn hình thao tác tinh gọn trên điện thoại:
1. Form nhập nhanh luôn mở sẵn ở nửa trên màn hình (Phương án A).
2. Tự động ghi nhận thời gian thực (ngày hôm nay), người dùng chỉ cần thao tác tối thiểu: chọn Thu/Chi, nhập mặt hàng và số tiền.
3. Cập nhật tức thời 3 chỉ số tài chính cốt lõi trong ngày: Tổng Thu, Tổng Chi và Lãi ròng.
4. Hiển thị danh sách các khoản thu/chi phát sinh trong ngày, khoản mới nhất nằm trên cùng, kèm nút xóa nhanh nếu bấm nhầm.

---

### 2. Yêu cầu chức năng chi tiết (Functional Requirements)

#### FR-1: Form Nhập Nhanh (Fixed Quick Entry Form)
* **Vị trí:** Luôn hiển thị cố định ngay ở phần trên trang (Phương án A), không bắt người dùng phải bấm nút để mở pop-up hay modal.
* **Thành phần của Form:**
  * **Nút gạt Loại giao dịch (Toggle Thu/Chi):**
    * Gồm 2 nút bấm to: `[ + THU TIỀN ]` và `[ - CHI TIỀN ]`.
    * Nút đang chọn sẽ nổi bật màu sắc rõ ràng (Thu: Xanh lá cây đậm, Chi: Đỏ cam đậm).
    * Mặc định khi vào trang: Chọn sẵn **Chi** (do buổi sáng quán bún thường chi tiền mua thịt/bún trước) hoặc Thu tùy chọn tiện lợi.
  * **Tên mặt hàng / Nội dung (`itemName`):**
    * Trường bắt buộc (`required`).
    * Placeholder: *"VD: Thịt bò, Bún tươi, Bún tái 5 tô..."*.
  * **Số tiền (`amount`):**
    * Trường bắt buộc (`required`), chỉ nhận số nguyên dương (> 0).
    * Đơn vị mặc định: VNĐ.
    * Thuộc tính input: `type="number"` và `inputmode="numeric"` để kích hoạt ngay bàn phím số cỡ lớn trên điện thoại.
  * **Ghi chú (`description`):**
    * Trường tùy chọn (`optional`).
    * Placeholder: *"Ghi chú thêm (không bắt buộc)..."*.
  * **Ngày giao dịch (`transactionDate`):**
    * **Tự động gán:** Hệ thống tự động lấy ngày hiện tại của máy chủ/thiết bị (`LocalDate.now()`). Chủ quán không cần tốn thao tác chọn ngày.
  * **Nút bấm gửi (`Submit`):**
    * Nút lớn full-width: *"LƯU GIAO DỊCH"*.
    * Khi submit thành công: Form tự động làm mới (reset) để sẵn sàng nhập khoản tiếp theo, con trỏ tự focus hoặc sẵn sàng cho lần gõ mới.

#### FR-2: Thẻ Tóm Tắt Trong Ngày (Daily Summary Cards)
Hiển thị 3 khối số liệu nổi bật ngay phía dưới/trên danh sách:
* **Thẻ Tổng Thu:** $\sum \text{Khoản Thu trong ngày}$ (Màu xanh lá cây, format: `1.250.000 đ`).
* **Thẻ Tổng Chi:** $\sum \text{Khoản Chi trong ngày}$ (Màu đỏ cam, format: `850.000 đ`).
* **Thẻ Lợi Nhuận (Lãi Hôm Nay):**
  $$\text{Lãi} = \text{Tổng Thu} - \text{Tổng Chi}$$
  * Nếu Lãi $\ge 0$: Chữ màu xanh dương/xanh lá biểu thị có lãi.
  * Nếu Lãi $< 0$: Chữ màu đỏ cảnh báo đang thâm hụt vốn trong ngày.

#### FR-3: Danh Sách Giao Dịch Gần Nhất (Recent Feed)
* Hiển thị toàn bộ các giao dịch phát sinh trong ngày hôm nay.
* **Thứ tự sắp xếp:** Giao dịch mới nhập nhất nằm ở vị trí đầu tiên (`ORDER BY created_at DESC`).
* **Thông tin trên mỗi dòng giao dịch:**
  * Huy hiệu phân loại: `[THU]` (nền xanh lá) hoặc `[CHI]` (nền đỏ).
  * Tên mặt hàng và ghi chú (nếu có).
  * Thời gian nhập (ví dụ: `06:30`, `07:15`...).
  * Số tiền: Thu hiển thị `+45.000 đ` (xanh), Chi hiển thị `-300.000 đ` (đỏ).
  * Nút xóa nhanh: Icon thùng rác ở góc phải mỗi dòng.

#### FR-4: Xóa Giao Dịch Nhập Nhầm
* Khi bấm nút Xóa: Hiển thị hộp thoại xác nhận của trình duyệt (`confirm: "Bạn có chắc chắn muốn xóa khoản giao dịch này không?"`).
* Sau khi xác nhận xóa: Dữ liệu được xóa khỏi CSDL MySQL, trang tự tải lại và cập nhật lại chính xác các thẻ Tổng Thu, Tổng Chi, Lợi Nhuận.

---

### 3. Ràng buộc UX & Trải nghiệm Mobile (UX Constraints)
* **Mobile-First Touch Target:** Mọi nút bấm, tab chuyển đổi có chiều cao tối thiểu `48px`, khoảng cách bấm vừa vặn ngón tay cái, tránh bấm trúng nhầm nút.
* **Tối ưu một tay:** Toàn bộ form và nút lưu nằm trong tầm với ngón tay cái khi cầm điện thoại 1 tay.
* **Định dạng tiền tệ tiếng Việt:** Toàn bộ số tiền trên giao diện phải có dấu phân cách hàng nghìn (ví dụ: `250.000 đ`).
* **Tốc độ phản hồi:** Lưu và tải lại danh sách trong vòng dưới 0.5 giây trên mạng nội bộ hoặc máy chủ cục bộ.
* **Không tràn màn hình:** Giao diện co giãn chuẩn từ màn hình nhỏ nhất (chiều rộng 360px trở lên).

