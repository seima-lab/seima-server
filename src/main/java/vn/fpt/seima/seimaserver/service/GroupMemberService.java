package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;

/**
 * Service interface for handling group member operations
 * Follows Single Responsibility Principle - only handles member-related operations
 */
public interface GroupMemberService {
    
    /**
     * Get all active members of a group.
     * Only returns members with ACTIVE status from groups that are active.
     * Current user must be an active member of the group to access this information.
     * 
     * @param groupId the ID of the group to get members for
     * @return list of active group members with group info and current user's role
     * @throws GroupException if group not found, user not authorized, or group is inactive
     */
    GroupMemberListResponse getActiveGroupMembers(Integer groupId);

    /**
     * Handle user account deactivation - update group leadership if needed
     * When a user deactivates their account, this method ensures group integrity:
     * - If user is group leader, transfer leadership to another admin or promote a member
     * - Keep membership records for audit purposes
     *
     * @param userId the ID of the user being deactivated
     * @throws GroupException if leadership transfer fails
     */
    void handleUserAccountDeactivation(Integer userId);
} 