package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupPermissionService Tests")
class GroupPermissionServiceTest {

    private GroupPermissionService groupPermissionService;

    @BeforeEach
    void setUp() {
        groupPermissionService = new GroupPermissionService();
    }

    @Test
    @DisplayName("canRemoveMember - Owner can remove Admin and Member but not other Owner")
    void canRemoveMember_Owner_ShouldAllowRemovalExceptOwner() {
        // Owner can remove Admin
        assertTrue(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.ADMIN));
        
        // Owner can remove Member
        assertTrue(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.MEMBER));
        
        // Owner cannot remove another Owner
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, GroupMemberRole.OWNER));
    }

    @Test
    @DisplayName("canRemoveMember - Admin can only remove Member")
    void canRemoveMember_Admin_ShouldOnlyRemoveMember() {
        // Admin cannot remove Owner
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.ADMIN, GroupMemberRole.OWNER));
        
        // Admin cannot remove another Admin
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.ADMIN, GroupMemberRole.ADMIN));
        
        // Admin can remove Member
        assertTrue(groupPermissionService.canRemoveMember(GroupMemberRole.ADMIN, GroupMemberRole.MEMBER));
    }

    @Test
    @DisplayName("canRemoveMember - Member cannot remove anyone")
    void canRemoveMember_Member_ShouldNotRemoveAnyone() {
        // Member cannot remove Owner
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.MEMBER, GroupMemberRole.OWNER));
        
        // Member cannot remove Admin
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.MEMBER, GroupMemberRole.ADMIN));
        
        // Member cannot remove another Member
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.MEMBER, GroupMemberRole.MEMBER));
    }

    @Test
    @DisplayName("canRemoveMember - Should handle null values gracefully")
    void canRemoveMember_WithNullValues_ShouldReturnFalse() {
        assertFalse(groupPermissionService.canRemoveMember(null, GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canRemoveMember(GroupMemberRole.OWNER, null));
        assertFalse(groupPermissionService.canRemoveMember(null, null));
    }

    @Test
    @DisplayName("canUpdateGroupInfo - Only Owner can update")
    void canUpdateGroupInfo_ShouldOnlyAllowOwner() {
        assertTrue(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.OWNER));
        assertFalse(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.ADMIN));
        assertFalse(groupPermissionService.canUpdateGroupInfo(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canUpdateGroupInfo(null));
    }

    @Test
    @DisplayName("canInviteMembers - Owner and Admin can invite")
    void canInviteMembers_ShouldAllowOwnerAndAdmin() {
        assertTrue(groupPermissionService.canInviteMembers(GroupMemberRole.OWNER));
        assertTrue(groupPermissionService.canInviteMembers(GroupMemberRole.ADMIN));
        assertFalse(groupPermissionService.canInviteMembers(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canInviteMembers(null));
    }

    @Test
    @DisplayName("canPromoteToAdmin - Only Owner can promote")
    void canPromoteToAdmin_ShouldOnlyAllowOwner() {
        assertTrue(groupPermissionService.canPromoteToAdmin(GroupMemberRole.OWNER));
        assertFalse(groupPermissionService.canPromoteToAdmin(GroupMemberRole.ADMIN));
        assertFalse(groupPermissionService.canPromoteToAdmin(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canPromoteToAdmin(null));
    }

    @Test
    @DisplayName("canDemoteAdmin - Only Owner can demote")
    void canDemoteAdmin_ShouldOnlyAllowOwner() {
        assertTrue(groupPermissionService.canDemoteAdmin(GroupMemberRole.OWNER));
        assertFalse(groupPermissionService.canDemoteAdmin(GroupMemberRole.ADMIN));
        assertFalse(groupPermissionService.canDemoteAdmin(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canDemoteAdmin(null));
    }

    @Test
    @DisplayName("canRemoveLastAdmin - Owner can remove if has owner or multiple admins")
    void canRemoveLastAdmin_ShouldCheckContext() {
        // Owner can remove last admin if group has owner
        assertTrue(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.OWNER, true, 1));
        
        // Owner can remove admin if there are multiple admins
        assertTrue(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.OWNER, false, 2));
        
        // Owner cannot remove last admin if no owner and only one admin
        assertFalse(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.OWNER, false, 1));
        
        // Non-owner cannot remove admin regardless of context
        assertFalse(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.ADMIN, true, 5));
        assertFalse(groupPermissionService.canRemoveLastAdmin(GroupMemberRole.MEMBER, true, 5));
    }

    @Test
    @DisplayName("canManageGroupSettings - Owner and Admin can manage")
    void canManageGroupSettings_ShouldAllowOwnerAndAdmin() {
        assertTrue(groupPermissionService.canManageGroupSettings(GroupMemberRole.OWNER));
        assertTrue(groupPermissionService.canManageGroupSettings(GroupMemberRole.ADMIN));
        assertFalse(groupPermissionService.canManageGroupSettings(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canManageGroupSettings(null));
    }

    @Test
    @DisplayName("canViewGroupMembers - All members can view")
    void canViewGroupMembers_ShouldAllowAllMembers() {
        assertTrue(groupPermissionService.canViewGroupMembers(GroupMemberRole.OWNER));
        assertTrue(groupPermissionService.canViewGroupMembers(GroupMemberRole.ADMIN));
        assertTrue(groupPermissionService.canViewGroupMembers(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canViewGroupMembers(null));
    }

    @Test
    @DisplayName("canSendMessages - All members can send messages")
    void canSendMessages_ShouldAllowAllMembers() {
        assertTrue(groupPermissionService.canSendMessages(GroupMemberRole.OWNER));
        assertTrue(groupPermissionService.canSendMessages(GroupMemberRole.ADMIN));
        assertTrue(groupPermissionService.canSendMessages(GroupMemberRole.MEMBER));
        assertFalse(groupPermissionService.canSendMessages(null));
    }

    @Test
    @DisplayName("getPermissionDescription - Should format correctly")
    void getPermissionDescription_ShouldFormatCorrectly() {
        String description = groupPermissionService.getPermissionDescription(
            "REMOVE_MEMBER", GroupMemberRole.OWNER, GroupMemberRole.ADMIN);
        
        assertEquals("Operation: REMOVE_MEMBER, Current Role: OWNER, Target Role: ADMIN", description);
    }

    @Test
    @DisplayName("getPermissionDescription - Should handle null values")
    void getPermissionDescription_ShouldHandleNullValues() {
        String description = groupPermissionService.getPermissionDescription(
            "TEST_OP", null, GroupMemberRole.MEMBER);
        
        assertEquals("Operation: TEST_OP, Current Role: null, Target Role: MEMBER", description);
    }
} 