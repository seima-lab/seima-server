package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {
    boolean existsByUserUserIdAndGroupGroupId(Integer userId, Integer groupId);

    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMember gm " +
            "WHERE gm.group.groupId = :groupId AND gm.user.userId = :userId AND gm.role = :role")
    boolean existsByGroupAndUserAndRole(@Param("groupId") Integer groupId,
                                        @Param("userId") Integer userId,
                                        @Param("role") GroupMemberRole role);

} 