# Project Mission & Specifications
## Sổ Thu Chi Quán Bún (BunLedger)

---

### 1. Vision & Sứ mệnh (Mission)
Xây dựng một ứng dụng web sổ sách thu/chi tinh gọn, chuyên biệt hóa cho chủ quán bún gia đình. Hệ thống giải quyết bài toán nhầm lẫn, mất mát dữ liệu từ sổ tay truyền thống, mang lại sự minh bạch dòng tiền và hiển thị chính xác **lợi nhuận thực tế hàng ngày** chỉ sau vài thao tác chạm trên điện thoại.

---

### 2. User Persona & Ngữ cảnh sử dụng
* **Chủ thể sử dụng (Persona):**
  * Chủ quán bún gia đình.
  * Thói quen: Cần công cụ đơn giản, chữ to, số tiền rõ ràng, không yêu cầu kỹ năng công nghệ phức tạp.
* **Ngữ cảnh sử dụng (Context):**
  * **Thiết bị chính:** Điện thoại thông minh (smartphone) đặt tại quầy hoặc cầm tay; có thể mở trên máy tính bảng/máy tính cá nhân.
  * **Thời điểm nhập liệu:**
    * Nhập khoản chi ngay khi lấy nguyên liệu đầu ngày (thịt, bún, rau củ, đá, gia vị).
    * Nhập khoản thu từng đợt hoặc tổng kết ca/cuối ngày.
    * Xem báo cáo lãi/lỗ và biểu đồ chốt sổ vào cuối ngày bán hàng.

---

### 3. Nghiệp vụ & Dữ liệu cốt lõi (Core Business Rules)

#### A. Khoản Thu (Income)
Mỗi khoản thu đại diện cho dòng tiền đi vào quán, bao gồm:
* **Ngày (`transaction_date`):** Ngày phát sinh giao dịch (mặc định là hôm nay).
* **Mặt hàng / Phân loại (`item_name`):** Tên món bán được (VD: *Bún tái, Bún giò, Bún chả, Nước ngọt, Khăn lạnh...*).
* **Giá tiền (`amount`):** Số tiền thu được (VND, số nguyên dương).
* **Nội dung / Ghi chú (`description`):** Ghi chú bổ sung (VD: *Khách đặt mang về 10 tô, Bán đồ ăn kèm...*).

#### B. Khoản Chi (Expense)
Mỗi khoản chi đại diện cho dòng tiền mua nguyên vật liệu, vận hành, bao gồm:
* **Ngày (`transaction_date`):** Ngày phát sinh chi phí.
* **Mặt hàng / Phân loại (`item_name`):** Loại chi phí (VD: *Thịt bò, Bún tươi, Xương hầm, Rau thơm, Tiền gas, Tiền điện...*).
* **Giá tiền (`amount`):** Số tiền chi ra (VND, số nguyên dương).
* **Nội dung / Ghi chú (`description`):** Chi tiết bổ sung (VD: *Mua ở chợ sớm, Đổi bình gas petrolimex...*).

---

### 4. Báo cáo & Chỉ số trọng yếu (Key Metrics)
Hệ thống tự động tổng hợp theo từng ngày được chọn:
1. **Tổng Thu trong ngày ($Total\_Income$):** $\sum \text{Khoản Thu}$.
2. **Tổng Chi trong ngày ($Total\_Expense$):** $\sum \text{Khoản Chi}$.
3. **Tổng Lãi trong ngày ($Daily\_Profit$):**
   $$\text{Lãi ròng} = \text{Tổng Thu} - \text{Tổng Chi}$$
4. **Trực quan hóa (Visual Chart):**
   * **Biểu đồ tròn (Pie Chart) Thu vs Chi:** So sánh tỷ trọng giữa dòng tiền vào và dòng tiền ra để chủ quán thấy ngay tình trạng chi tiêu chiếm bao nhiêu % doanh thu.
   * **Biểu đồ tròn theo Danh mục:** Xem cơ cấu chi phí (chi cho thịt, bún hay mặt bằng chiếm phần lớn).

---

### 5. Tiêu chuẩn thành công (Success Criteria)
* Giao diện chuẩn Mobile-first, thao tác 1 tay thuận tiện.
* Tốc độ phản hồi tức thì, dữ liệu được lưu trữ an toàn trong cơ sở dữ liệu MySQL.
* Bất kỳ lúc nào mở app ra, chủ quán đều thấy ngay 3 con số: **Thu**, **Chi**, và **Lãi hôm nay**.

