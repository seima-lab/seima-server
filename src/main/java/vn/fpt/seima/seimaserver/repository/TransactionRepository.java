package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;import vn.fpt.seima.seimaserver.entity.Transaction;
import vn.fpt.seima.seimaserver.entity.TransactionType;
import vn.fpt.seima.seimaserver.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.transactionType != :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "and t.user.userId = :userId and t.group.groupId is null")
    List<Transaction> findAllByUserAndTransactionDateBetween(
            @Param("userId") Integer userId,
            @Param("type") TransactionType type,
            @Param("startDate")LocalDateTime startDate,
            @Param("endDate")LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.transactionType != :type and t.user.userId = :userId")
    Page<Transaction> findByType(
                                        @Param("transaction_type") TransactionType type,
                                        @Param("userId") Integer userId,
                                        Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.transactionType != :type and t.group.groupId = :groupId")
    Page<Transaction> findByTypeGroup(
            @Param("transaction_type") TransactionType type,
            @Param("group_id") Integer groupId
            , Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.transactionType != :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND ((:groupId IS NULL AND t.group.groupId IS NULL) OR (:groupId IS NOT NULL AND t.group.groupId = :groupId)) " +
            "AND t.user.userId = :userId")
    Page<Transaction> findByDate(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("groupId") Integer groupId,
            @Param("userId") Integer userId,
            Pageable pageable);


    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND (:categoryId IS NULL OR t.category.categoryId = :categoryId) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.transactionType != 'INACTIVE' " +
            "AND ((:groupId IS NULL AND t.group.groupId IS NULL) " +
            "OR (:groupId IS NOT NULL AND t.group.groupId = :groupId))")
    List<Transaction> listReportByUserAndCategoryAndTransactionDateBetween(
            @Param("user") User user,
            @Param("categoryId") Integer categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("groupId") Integer groupId
    );
    void deleteByCategory_CategoryId(Integer categoryId);

    List<Transaction> findAllByCategory_CategoryId(Integer categoryId);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user.userId = :userId AND t.category.categoryId = :categoryId " +
            "AND t.transactionDate BETWEEN :start AND :end " +
            "AND t.transactionType IN ('EXPENSE', 'INCOME')" +
            "and t.group.groupId is null")
    List<Transaction> findExpensesByUserAndDateRange(
            @Param("categoryId") Integer categoryId,
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user.userId = :userId " +
            "AND t.transactionDate BETWEEN :start AND :end " +
            "AND t.transactionType IN ('EXPENSE', 'INCOME')")
    List<Transaction> findExpensesByUserAndDateRange(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.userId = :userId AND " +
            "t.transactionType = 'EXPENSE' AND t.category.categoryId in :categoryId AND " +
            "t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumExpensesByCategoryAndMonth(@Param("userId") Integer userId,
                                             @Param("categoryId") List<Integer> categoryId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.transactionType != 'INACTIVE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "and t.user.userId = :userId and " +
            "t.category.categoryId = :categoryId and " +
            "t.group.groupId is null ")
    Page<Transaction> getTransactionByBudget(@Param("userId") Integer userId,
                                             @Param("categoryId") Integer categoryId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             Pageable pageable);
}

