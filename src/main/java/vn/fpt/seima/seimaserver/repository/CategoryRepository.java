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


    @Query("SELECT c FROM Category c WHERE c.categoryType = :categoryType AND (c.user.userId = :userId OR (c.group IS NULL AND c.user IS NULL))")
    List<Category> findByCategoryTypeAndUser_UserIdOrUserIsNull(
            @Param("categoryType") CategoryType categoryType,
            @Param("userId") Integer userId
    );

    @Query("SELECT c FROM Category c WHERE c.categoryType = :categoryType AND (c.group.groupId = :groupId OR (c.group IS NULL AND c.user IS NULL))")
    List<Category> findByCategoryTypeAndGroup_GroupIdOrGroupIsNull(
            @Param("categoryType") CategoryType categoryType,
            @Param("groupId") Integer groupId
    );

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
            "WHERE c.categoryName = :categoryName AND c.categoryType = :type AND c.user.userId = :userId")
    boolean existsByCategoryNameAndTypeAndUser_UserId(@Param("categoryName") String categoryName,
                         @Param("type") CategoryType type,
                         @Param("userId") Integer userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c " +
            "WHERE c.categoryName = :categoryName AND c.categoryType = :type AND c.group.groupId = :groupId")
    boolean existsByCategoryNameAndTypeAndGroup_GroupId(@Param("categoryName") String categoryName,
                          @Param("type") CategoryType type,
                          @Param("groupId") Integer groupId);

}