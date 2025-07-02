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

    /**
     * Find the group owner (creator) - there should be exactly one OWNER per group
     * @param groupId the group ID
     * @param status the membership status
     * @return Optional of GroupMember who is the owner
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
     * Check if user is already a member of the group with active status
     * @param userId the user ID
     * @param groupId the group ID
     * @param status the membership status
     * @return true if user is active member, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMember gm " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId AND gm.status = :status")
    boolean existsByUserAndGroupAndStatus(@Param("userId") Integer userId,
                                         @Param("groupId") Integer groupId,
                                         @Param("status") GroupMemberStatus status);
    
    /**
     * Find group member by user and group with specific status
     * @param userId the user ID
     * @param groupId the group ID
     * @param status the membership status
     * @return Optional of GroupMember
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId AND gm.status = :status")
    Optional<GroupMember> findByUserAndGroupAndStatus(@Param("userId") Integer userId,
                                                      @Param("groupId") Integer groupId,
                                                      @Param("status") GroupMemberStatus status);
    
    /**
     * Find group member by user and group
     * @param userId the user ID
     * @param groupId the group ID
     * @return Optional of GroupMember
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "WHERE gm.user.userId = :userId AND gm.group.groupId = :groupId")
    Optional<GroupMember> findByUserIdAndGroupId(@Param("userId") Integer userId,
                                                 @Param("groupId") Integer groupId);

    /**
     * Find all groups that a user has joined (active membership in active groups only)
     *
     * @param userId        the user ID
     * @param memberStatus  the membership status (should be ACTIVE)
     * @param groupIsActive the group active status (should be true)
     * @return List of GroupMember with group and user eagerly loaded, ordered by join date desc
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
     * @param userId the user ID
     * @param role the role to filter by
     * @return List of GroupMember with group and user eagerly loaded
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.group g " +
            "JOIN FETCH gm.user u " +
            "WHERE gm.user.userId = :userId AND gm.role = :role")
    List<GroupMember> findByUserIdAndRole(@Param("userId") Integer userId,
                                         @Param("role") GroupMemberRole role);

    /**
     * Find all pending group members for a specific group
     * @param groupId the group ID
     * @param status the membership status (should be PENDING_APPROVAL)
     * @return List of GroupMember with pending approval status, ordered by join date asc
     */
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user u " +
            "JOIN FETCH gm.group g " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND u.userIsActive = true " +
            "ORDER BY gm.joinDate ASC")
    List<GroupMember> findPendingGroupMembers(@Param("groupId") Integer groupId,
                                             @Param("status") GroupMemberStatus status);

    /**
     * Count pending group members for a specific group
     * @param groupId the group ID
     * @param status the membership status (should be PENDING_APPROVAL)
     * @return count of pending members
     */
    @Query("SELECT COUNT(gm) FROM GroupMember gm " +
            "JOIN gm.user u " +
            "WHERE gm.group.groupId = :groupId AND gm.status = :status " +
            "AND u.userIsActive = true")
    Long countPendingGroupMembers(@Param("groupId") Integer groupId,
                                 @Param("status") GroupMemberStatus status);
} 