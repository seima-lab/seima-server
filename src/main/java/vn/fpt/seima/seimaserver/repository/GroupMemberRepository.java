package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Integer> {
    boolean existsByUserUserIdAndGroupGroupId(Integer userId, Integer groupId);

    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMember gm " +
            "WHERE gm.group.groupId = :groupId AND gm.user.userId = :userId AND gm.role = :role")
    boolean existsByGroupAndUserAndRole(@Param("groupId") Integer groupId,
                                        @Param("userId") Integer userId,
                                        @Param("role") GroupMemberRole role);

    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group.groupId = :groupId AND gm.role = :role AND gm.status = :status")
    Optional<GroupMember> findGroupLeader(@Param("groupId") Integer groupId, 
                                         @Param("role") GroupMemberRole role,
                                         @Param("status") GroupMemberStatus status);

    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "ORDER BY gm.joinDate ASC")
    List<GroupMember> findActiveGroupMembers(@Param("groupId") Integer groupId,
                                           @Param("status") GroupMemberStatus status);

    @Query("SELECT COUNT(gm) FROM GroupMember gm " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status")
    Long countActiveGroupMembers(@Param("groupId") Integer groupId,
                                @Param("status") GroupMemberStatus status);
} 