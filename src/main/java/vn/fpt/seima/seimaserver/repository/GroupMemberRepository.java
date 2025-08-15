package vn.fpt.seima.seimaserver.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fpt.seima.seimaserver.entity.Group;
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

    /**
     * Find the group owner (creator) - there should be exactly one OWNER per group
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group.groupId = :groupId AND gm.role = 'OWNER' AND gm.status = :status")
    Optional<GroupMember> findGroupOwner(@Param("groupId") Integer groupId,
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
    
    /**
     * Count active groups for a user
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm " +
            "JOIN gm.group g " +
            "WHERE gm.user.userId = :userId AND gm.status = :status " +
            "AND g.groupIsActive = true")
    Long countUserActiveGroups(@Param("userId") Integer userId,
                              @Param("status") GroupMemberStatus status);
    
    /**
     * Check if user is already a member of the group with active status
     */
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMember gm " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId AND gm.status = :status")
    boolean existsByUserAndGroupAndStatus(@Param("userId") Integer userId,
                                         @Param("groupId") Integer groupId,
                                         @Param("status") GroupMemberStatus status);
    
    /**
     * Find group member by user and group with specific status
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId AND gm.status = :status")
    Optional<GroupMember> findByUserAndGroupAndStatus(@Param("userId") Integer userId,
                                                      @Param("groupId") Integer groupId,
                                                      @Param("status") GroupMemberStatus status);

    /**
     * Find the most recent membership of a user in a specific group
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId " +
            "ORDER BY gm.joinDate DESC LIMIT 1")
    Optional<GroupMember> findMostRecentMembershipByUserIdAndGroupId(@Param("userId") Integer userId,
                                                                     @Param("groupId") Integer groupId);
    /**
     * Find all groups that a user has joined (active membership in active groups only)
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.group g " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.user.userId = :userId " +
            "AND gm.status = :memberStatus " +
            "AND g.groupIsActive = :groupIsActive " +
            "ORDER BY gm.joinDate DESC")
    List<GroupMember> findUserJoinedGroups(@Param("userId") Integer userId,
                                           @Param("memberStatus") GroupMemberStatus memberStatus,
                                           @Param("groupIsActive") Boolean groupIsActive);

    /**
     * Find all group memberships for a user with specific role
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.group g " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.user.userId = :userId AND gm.role = :role")
    List<GroupMember> findByUserIdAndRole(@Param("userId") Integer userId,
                                         @Param("role") GroupMemberRole role);


    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user u " +
            "JOIN FETCH gm.group g " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND u.userIsActive = true " +
            "ORDER BY gm.joinDate ASC")
    List<GroupMember> findPendingGroupMembers(@Param("groupId") Integer groupId,
                                             @Param("status") GroupMemberStatus status);


    @Query("SELECT COUNT(gm) FROM GroupMember gm " +
            "JOIN gm.user u " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND u.userIsActive = true")
    Long countPendingGroupMembers(@Param("groupId") Integer groupId,
                                 @Param("status") GroupMemberStatus status);


    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND (gm.role = 'ADMIN' OR gm.role = 'OWNER') " +
            "AND u.userIsActive = true")
    List<GroupMember> findAdminAndOwnerMembers(@Param("groupId") Integer groupId,
                                              @Param("status") GroupMemberStatus status);

    /**
     * Find all groups that a user has requested to join but are still pending approval
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.group g " +
            "WHERE gm.user.userId = :userId " +
            "AND gm.status = :status " +
            "AND g.groupIsActive = true " +
            "ORDER BY gm.joinDate DESC")
    List<GroupMember> findUserPendingGroups(@Param("userId") Integer userId,
                                           @Param("status") GroupMemberStatus status);

    /**
     * Find all active members of a group excluding a specific user
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND gm.user.userId != :userId " +
            "AND u.userIsActive = true " +
            "ORDER BY gm.joinDate ASC")
    List<GroupMember> findByGroupAndStatusAndUserIdNot(@Param("groupId") Integer groupId,
                                                       @Param("status") GroupMemberStatus status,
                                                       @Param("userId") Integer userId);

    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN gm.group g " +
            "WHERE g.groupIsActive = true and g.groupId = :groupId")
    List<GroupMember> findActiveGroupMembers(@Param("groupId") Integer groupId);
}
