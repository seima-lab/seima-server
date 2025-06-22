package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.Transaction;
import vn.fpt.seima.seimaserver.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findAllByUserAndTransactionDateBetween(User user, LocalDateTime start, LocalDateTime end);

    Page<Transaction> findAllByGroup(Group group, Pageable pageable);

} 