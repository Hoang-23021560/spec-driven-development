package com.bunledger.repository;

import com.bunledger.model.Transaction;
import com.bunledger.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Tìm danh sách giao dịch theo ngày, mới nhất lên đầu theo thời gian tạo.
     */
    List<Transaction> findByTransactionDateOrderByCreatedAtDesc(LocalDate transactionDate);

    /**
     * Tìm danh sách giao dịch theo ngày, sắp xếp theo ID giảm dần (dự phòng).
     */
    List<Transaction> findByTransactionDateOrderByIdDesc(LocalDate transactionDate);

    /**
     * Tính tổng số tiền theo loại giao dịch (INCOME/EXPENSE) và ngày phát sinh.
     * Trả về 0 nếu chưa có giao dịch nào thỏa mãn điều kiện.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND t.transactionDate = :date")
    BigDecimal sumAmountByTypeAndDate(@Param("type") TransactionType type, @Param("date") LocalDate date);

    /**
     * Gom nhóm chi phí theo mặt hàng trong ngày, tính tổng chi và sắp xếp giảm dần.
     */
    @Query("SELECT t.itemName, SUM(t.amount) FROM Transaction t " +
           "WHERE t.type = :type AND t.transactionDate = :date " +
           "GROUP BY t.itemName ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategoryBreakdown(@Param("type") TransactionType type, @Param("date") LocalDate date);

    /**
     * Tính tổng số tiền theo loại giao dịch (INCOME/EXPENSE) trong một khoảng ngày.
     * Trả về 0 nếu không có giao dịch nào thỏa mãn điều kiện.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByTypeAndDateRange(@Param("type") TransactionType type,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Đếm số ngày có phát sinh giao dịch trong một khoảng ngày (distinct date).
     */
    @Query("SELECT COUNT(DISTINCT t.transactionDate) FROM Transaction t " +
           "WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    long countActiveDaysBetween(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    /**
     * Gom nhóm chi phí theo mặt hàng trong khoảng ngày, tính tổng chi và sắp xếp giảm dần.
     */
    @Query("SELECT t.itemName, SUM(t.amount) FROM Transaction t " +
           "WHERE t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY t.itemName ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategoryBreakdownBetween(@Param("type") TransactionType type,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * Lấy tất cả giao dịch trong khoảng ngày, sắp xếp theo ngày tăng dần và ID tăng dần (phục vụ xuất CSV/Excel).
     */
    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateAscIdAsc(LocalDate startDate, LocalDate endDate);
}

