# Functional Requirements & UX Specifications
## Phase 3: Tối Ưu UX Mobile & Nhập Nhanh (Mobile Polish)

---

### 1. Tổng quan tính năng (Feature Overview)
Phase 3 tập trung tối ưu hóa tốc độ và trải nghiệm thao tác trên màn hình điện thoại di động của chủ quán bún:
1. **Quick Chips (Gợi ý mặt hàng 1 chạm):** Các thẻ chip gợi ý mặt hàng quen thuộc của quán bún, tự động thay đổi theo trạng thái Thu hoặc Chi. Chạm vào chip sẽ tự động điền tên mặt hàng vào ô nhập.
2. **Bàn phím tiền tệ thông minh (Smart Money Steppers & Auto-format):** Hỗ trợ các nút cộng tiền nhanh (`+20k`, `+50k`, `+100k`, `+200k`, `+500k`, `Xóa trắng [C]`) giúp chủ quán không cần gõ từng số 0.
3. **Chỉnh sửa giao dịch (Edit Transaction):** Cho phép sửa nhanh tên mặt hàng, số tiền hoặc ghi chú của một khoản đã lưu khi phát hiện nhầm lẫn.
4. **Tối ưu tốc độ 1 tay:** Thao tác ghi nhận một khoản thu hoặc chi hoàn tất dưới 5 giây.

---

### 2. Yêu cầu chức năng chi tiết (Functional Requirements)

#### FR-3.1: Gợi ý mặt hàng nhanh (Quick Chips)
* **Khi chọn `[ - CHI TIỀN ]`:** Hiển thị danh sách chip màu đỏ cam nhạt:
  * `Thịt bò`, `Bún tươi`, `Xương hầm`, `Rau sống`, `Hành ngò`, `Tiền gas`, `Gia vị`, `Đá lạnh`, `Mặt bằng`.
* **Khi chọn `[ + THU TIỀN ]`:** Hiển thị danh sách chip màu xanh ngọc nhạt:
  * `Bún tái gầu`, `Bún giò heo`, `Bún đặc biệt`, `Bún mang về`, `Nước ngọt`, `Khăn lạnh`, `Trà đá`.
* **Hành vi:**
  * Bấm vào chip -> Giá trị của chip lập tức được gán vào ô input `itemName`.
  * Highlight nhẹ chip vừa chọn để nhận biết trực quan.

#### FR-3.2: Bàn phím trợ lực tiền tệ (Quick Amount Steppers)
* Phía dưới ô nhập số tiền, hiển thị hàng nút bấm trợ lực:
  * `+20k` (+20.000 đ)
  * `+50k` (+50.000 đ)
  * `+100k` (+100.000 đ)
  * `+200k` (+200.000 đ)
  * `+500k` (+500.000 đ)
  * `[C]` (Xóa về 0 hoặc làm trống)
* **Hành vi cộng dồn thông minh:** Nếu ô tiền đang có `50.000`, bấm `+20k` sẽ tự động thành `70.000`.

#### FR-3.3: Chỉnh sửa giao dịch (Edit Transaction)
* Tại mỗi dòng trong danh sách giao dịch, thêm icon **[Sửa]** bên cạnh nút [Xóa].
* Bấm nút [Sửa] sẽ mở một **Modal chỉnh sửa nhanh (Quick Edit Sheet)** ngay trên màn hình mà không rời trang:
  * Hiển thị thông tin hiện tại: Loại (Thu/Chi), Tên mặt hàng, Số tiền, Ghi chú.
  * Cho phép sửa đổi và bấm **"CẬP NHẬT GIAO DỊCH"**.
  * Endpoint backend: `POST /transactions/{id}/update` (hoặc `POST /transactions/{id}/edit`).
  * Sau khi cập nhật, lưu lại vào MySQL và redirect bảo toàn ngày đang xem kèm thông báo flash message "Đã cập nhật giao dịch thành công".

---

### 3. Ràng buộc UX Mobile (UX Constraints)
* Chiều cao toàn bộ các nút bấm chip và phím số $\ge 44\text{px} - 48\text{px}$.
* Danh sách chip cuộn ngang mượt mà (`overflow-x-auto whitespace-nowrap scrollbar-none`) vừa khít với ngón tay cái.
* Modal chỉnh sửa có backdrop mờ và nút đóng to rõ ràng, hỗ trợ đóng khi chạm ra ngoài hoặc bấm phím Escape.

