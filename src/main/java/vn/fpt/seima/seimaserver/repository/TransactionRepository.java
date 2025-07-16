package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;import vn.fpt.seima.seimaserver.entity.Transaction;
import vn.fpt.seima.seimaserver.entity.TransactionType;
import vn.fpt.seima.seimaserver.entity.User;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findAllByUserAndTransactionDateBetween(User user, LocalDateTime start, LocalDateTime end);

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
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Page<Transaction> findByDate(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);


    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND (:categoryId IS NULL OR t.category.categoryId = :categoryId) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.transactionType != 'INACTIVE'")
    List<Transaction> listReportByUserAndCategoryAndTransactionDateBetween(
            @Param("user") User user,
            @Param("categoryId") Integer categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    void deleteByCategory_CategoryId(Integer categoryId);
    List<Transaction> findAllByCategory_CategoryId(Integer categoryId);
}

