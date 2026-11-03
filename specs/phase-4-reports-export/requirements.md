# Functional Requirements & UX Specifications
## Phase 4: Báo Cáo Tháng & Xuất Dữ Liệu (Reports & CSV Export)

---

### 1. Tổng quan tính năng (Feature Overview)
Phase 4 hoàn thiện bức tranh quản trị tài chính cho quán bún:
1. **Báo cáo tài chính theo tháng (Monthly Overview):** Giúp chủ quán có cái nhìn tổng quan về kết quả kinh doanh trong cả tháng: Tổng doanh thu, tổng chi phí, tổng lãi ròng và lợi nhuận trung bình mỗi ngày bán.
2. **Cơ cấu chi phí toàn tháng:** Thống kê tổng số tiền đã chi cho từng mặt hàng trong tháng (tháng này đã chi bao nhiêu triệu tiền thịt, tiền bún, tiền gas...).
3. **Xuất file CSV / Excel tiếng Việt (Data Backup & Export):** Xuất toàn bộ sổ sách thu chi trong tháng ra file CSV có BOM UTF-8 (mở chuẩn tiếng Việt trên Microsoft Excel điện thoại và máy tính), phục vụ lưu trữ dự phòng hoặc chia sẻ cho gia đình.

---

### 2. Yêu cầu chức năng chi tiết (Functional Requirements)

#### FR-4.1: Báo Cáo Tài Chính Tổng Hợp Tháng
* **Đường dẫn truy cập:** `GET /reports?month=YYYY-MM` (mặc định lấy tháng hiện tại nếu không truyền tham số).
* **Bộ chọn tháng (Month Picker):**
  * Nút lùi 1 tháng `[◀]`, input native `type="month"`, nút tiến 1 tháng `[▶]`.
  * Tự động tải lại số liệu khi đổi tháng.
* **4 Chỉ số trọng yếu toàn tháng:**
  1. **Tổng Thu Tháng:** $\sum \text{Khoản Thu trong tháng}$
  2. **Tổng Chi Tháng:** $\sum \text{Khoản Chi trong tháng}$
  3. **Lợi Nhuận Tháng:** $\text{Tổng Thu Tháng} - \text{Tổng Chi Tháng}$
  4. **Lợi Nhuận TB/Ngày:** $\frac{\text{Lợi Nhuận Tháng}}{\text{Số ngày có phát sinh bán hàng (Active Days)}}$
* **Cơ cấu chi phí tháng:** Bảng hoặc danh sách xếp hạng các mặt hàng chi nhiều nhất trong tháng (Tên món, Số tiền, Tỷ lệ phần trăm).

#### FR-4.2: Xuất Dữ Liệu Ra File CSV / Excel
* **Đường dẫn:** `GET /export/csv?month=YYYY-MM`.
* **Quy chuẩn file tải về:**
  * Định dạng file: `.csv` chuẩn UTF-8 kèm ký tự **BOM (`\uFEFF`)** để Excel không bị lỗi font chữ tiếng Việt có dấu.
  * Tên file động: `so_thu_chi_YYYY-MM.csv` (ví dụ: `so_thu_chi_2026-11.csv`).
  * Header HTTP:
    - `Content-Type: text/csv; charset=UTF-8`
    - `Content-Disposition: attachment; filename="so_thu_chi_YYYY-MM.csv"`
* **Nội dung file CSV:**
  * Dòng 1 (Header): `Mã GD,Ngày,Loại,Mặt hàng,Số tiền (VND),Ghi chú,Thời gian ghi`
  * Các dòng dữ liệu sắp xếp theo ngày tăng dần và thời gian ghi.
  * Cột Số tiền là số nguyên sạch (ví dụ: `350000`) để người dùng dễ dàng dùng hàm `=SUM()` trong Excel.

#### FR-4.3: Điều hướng giao diện (Header Navigation)
* Trên thanh điều hướng Header:
  * Nút chuyển đổi nhanh: **[ Sổ Hàng Ngày ]** và **[ Báo Cáo Tháng ]**.
  * Tại trang Báo cáo: Nút bấm lớn nổi bật **"XUẤT FILE CSV (EXCEL)"** với icon tải xuống.

---

### 3. Ràng buộc Kỹ thuật & Hiệu năng
* Truy vấn gom nhóm tháng thực hiện tối ưu bằng SQL `BETWEEN` theo khoảng ngày đầu tháng đến cuối tháng để tận dụng tối đa chỉ mục `idx_transactions_date`.
* Xuất file CSV dạng streaming hoặc buffer trực tiếp vào `HttpServletResponse`, không ghi tạm file rác lên ổ đĩa máy chủ.

