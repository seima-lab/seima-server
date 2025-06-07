package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Category;
import vn.fpt.seima.seimaserver.entity.CategoryType;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByCategoryName(String categoryName);


    @Query("SELECT c FROM Category c WHERE c.categoryType = :categoryType AND (c.user.userId = :userId OR c.user IS NULL)")
    List<Category> findByCategoryTypeAndUser_UserIdOrUserIsNull(
            @Param("categoryType") CategoryType categoryType,
            @Param("userId") Integer userId
    );
}