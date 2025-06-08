package vn.fpt.seima.seimaserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Wallet;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    @Query("SELECT w FROM Wallet w WHERE w.isDeleted = false")
    List<Wallet> findAllActive();

    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.isDeleted = false")
    Optional<Wallet> findByIdAndNotDeleted(@Param("id") Integer id);

    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId AND w.isDeleted = false")
    List<Wallet> findAllActiveByUserId(@Param("userId") Integer userId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Wallet w WHERE w.id = :id AND w.isDeleted = false")
    boolean existsByIdAndNotDeleted(@Param("id") Integer id);
}