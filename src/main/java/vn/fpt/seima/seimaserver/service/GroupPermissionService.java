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
    public String getPermissionDescription(String operation, GroupMemberRole currentRole, GroupMemberRole targetRole) {
        return String.format("Operation: %s, Current Role: %s, Target Role: %s", 
            operation, currentRole, targetRole);
    }
} 