package vn.fpt.seima.seimaserver.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

/**
 * Service for handling group permission logic
 * Centralizes all group-related permission checks
 */
@Service
@Slf4j
public class GroupPermissionService {


    public boolean canRemoveMember(GroupMemberRole currentRole, GroupMemberRole targetRole) {
        if (currentRole == null || targetRole == null) {
            log.warn("Null roles provided - currentRole: {}, targetRole: {}", currentRole, targetRole);
            return false;
        }
        
        return switch (currentRole) {
            case OWNER -> targetRole != GroupMemberRole.OWNER; // Owner can remove ADMIN/MEMBER but not other OWNERs
            case ADMIN -> targetRole == GroupMemberRole.MEMBER; // Admin can only remove MEMBERs
            case MEMBER -> false; // Members cannot remove anyone
        };
    }

    public boolean canUpdateGroupInfo(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER;
    }

    public boolean canInviteMembers(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }

    public boolean canPromoteToAdmin(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER;
    }

    public boolean canDemoteAdmin(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER;
    }

    /**
     * Check if user can update member role
     * Only OWNER can update member roles
     */
    public boolean canUpdateMemberRole(GroupMemberRole currentRole, GroupMemberRole targetCurrentRole, GroupMemberRole newRole) {
        // Only owner can update roles
        if (currentRole != GroupMemberRole.OWNER) {
            return false;
        }
        
        // Cannot change owner role
        if (targetCurrentRole == GroupMemberRole.OWNER) {
            return false;
        }
        
        // Cannot promote to owner (prevent multiple owners)
        if (newRole == GroupMemberRole.OWNER) {
            return false;
        }
        
        return true;
    }

    public boolean canRemoveLastAdmin(GroupMemberRole currentRole, boolean hasOwner, int adminCount) {
        if (currentRole != GroupMemberRole.OWNER) {
            return false;
        }

        return hasOwner || adminCount > 1;
    }

    public boolean canManageGroupSettings(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }

    public boolean canViewGroupMembers(GroupMemberRole role) {
        // All group members can view the member list
        return role != null;
    }
    public boolean canSendMessages(GroupMemberRole role) {
        // All group members can send messages
        return role != null;
    }

    /**
     * Check if user can view pending group member requests
     * Only OWNER and ADMIN can view pending requests
     */
    public boolean canViewPendingRequests(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }

    /**
     * Check if user can accept/reject pending group member requests
     * Only OWNER and ADMIN can accept/reject requests
     */
    public boolean canAcceptGroupMemberRequests(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }

    /**
     * Check if user can reject pending group member requests
     * Only OWNER and ADMIN can reject requests
     */
    public boolean canRejectGroupMemberRequests(GroupMemberRole role) {
        return role == GroupMemberRole.OWNER || role == GroupMemberRole.ADMIN;
    }
    
    public String getPermissionDescription(String operation, GroupMemberRole currentRole, GroupMemberRole targetRole) {
        return String.format("Operation: %s, Current Role: %s, Target Role: %s", 
            operation, currentRole, targetRole);
    }
} 