package com.bunledger.service;

import com.bunledger.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xử lý xuất dữ liệu giao dịch ra file định dạng CSV chuẩn RFC 4180
 * với ký tự UTF-8 BOM để tương thích tốt với Microsoft Excel.
 */
@Service
@Slf4j
public class CsvExportService {

    public static final String CSV_HEADER = "Mã GD,Ngày,Loại,Mặt hàng,Số lượng,Số tiền (VND),Ghi chú,Thời gian ghi\r\n";
    public static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Xuất danh sách giao dịch ra mảng byte định dạng CSV UTF-8 có kèm ký tự BOM (Byte Order Mark)
     * ở đầu file, giúp Excel tự động nhận diện và hiển thị tiếng Việt có dấu không bị lỗi font.
     *
     * @param transactions danh sách giao dịch cần xuất
     * @return mảng byte CSV có tiền tố UTF-8 BOM
     */
    public byte[] exportTransactionsToCsv(List<Transaction> transactions) {
        int count = (transactions != null) ? transactions.size() : 0;
        log.info("Bắt đầu xuất CSV cho {} giao dịch", count);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // Ghi UTF-8 BOM: 0xEF, 0xBB, 0xBF
            baos.write(UTF8_BOM);

            StringBuilder sb = new StringBuilder();
            sb.append(CSV_HEADER);

            if (transactions != null) {
                for (Transaction tx : transactions) {
                    String idStr = tx.getId() != null ? String.valueOf(tx.getId()) : "";
                    String dateStr = tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "";
                    String typeStr = (tx.getType() != null && tx.getType().getDisplayName() != null)
                            ? tx.getType().getDisplayName()
                            : "";
                    String itemStr = tx.getItemName() != null ? tx.getItemName() : "";
                    String qtyStr = tx.getQuantity() != null ? String.valueOf(tx.getQuantity()) : "1";
                    String amountStr = tx.getAmount() != null ? tx.getAmount().toPlainString() : "0";
                    String descStr = tx.getDescription() != null ? tx.getDescription() : "";
                    String createdAtStr = tx.getCreatedAt() != null ? tx.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

                    sb.append(escapeCsvField(idStr)).append(",")
                            .append(escapeCsvField(dateStr)).append(",")
                            .append(escapeCsvField(typeStr)).append(",")
                            .append(escapeCsvField(itemStr)).append(",")
                            .append(escapeCsvField(qtyStr)).append(",")
                            .append(escapeCsvField(amountStr)).append(",")
                            .append(escapeCsvField(descStr)).append(",")
                            .append(escapeCsvField(createdAtStr))
                            .append("\r\n");
                }
            }

            baos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Lỗi khi ghi dữ liệu CSV vào OutputStream", e);
            throw new RuntimeException("Lỗi khi xuất file CSV", e);
        }

        return baos.toByteArray();
    }

    /**
     * Xử lý escape trường dữ liệu CSV theo chuẩn RFC 4180:
     * Nếu giá trị chứa dấu phẩy (,), dấu nháy kép ("), hoặc ký tự xuống dòng (\n, \r),
     * trường dữ liệu sẽ được bao quanh bằng dấu nháy kép và nhân đôi các dấu nháy kép bên trong (" -> "").
     *
     * @param field giá trị chuỗi của trường
     * @return chuỗi đã được escape an toàn theo chuẩn CSV
     */
    public String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
