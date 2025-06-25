package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberListResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.GroupMemberService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {
    
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional(readOnly = true)
    public GroupMemberListResponse getActiveGroupMembers(Integer groupId) {
        log.info("Getting active members for group ID: {}", groupId);

        // Validate input
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Find and validate group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupException("Group not found"));

        // Check if group is active
        if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
            throw new GroupException("Group not found");
        }

        // Check if current user is an active member
        if (!groupMemberRepository.existsByUserAndGroupAndStatus(
                currentUser.getUserId(), groupId, GroupMemberStatus.ACTIVE)) {
            throw new GroupException("You don't have permission to view this group's members");
        }

        // Get group leader
        GroupMember leader = groupMemberRepository.findGroupLeader(
                groupId, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new GroupException("Group leader not found for group ID: " + groupId));

        // Check if leader account is still active
        if (!Boolean.TRUE.equals(leader.getUser().getUserIsActive())) {
            throw new GroupException("Group leader account is no longer active");
        }

        GroupMemberResponse leaderResponse = mapToGroupMemberResponse(leader);

        // Get all active members
        List<GroupMember> allMembers = groupMemberRepository.findActiveGroupMembers(
                groupId, GroupMemberStatus.ACTIVE);

        // Filter out inactive user accounts and exclude leader from members list
        List<GroupMemberResponse> memberResponses = allMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive())) // Only active user accounts
                .filter(member -> !member.getUser().getUserId().equals(leader.getUser().getUserId()))
                .map(this::mapToGroupMemberResponse)
                .collect(Collectors.toList());

        // Get total member count (only active users)
        Long totalActiveUserCount = allMembers.stream()
                .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                .count();

        // Get current user's role
        GroupMemberRole currentUserRole = allMembers.stream()
                .filter(member -> member.getUser().getUserId().equals(currentUser.getUserId()))
                .map(GroupMember::getRole)
                .findFirst()
                .orElse(GroupMemberRole.MEMBER);

        return GroupMemberListResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupAvatarUrl(group.getGroupAvatarUrl())
                .totalMembersCount(totalActiveUserCount.intValue())
                .groupLeader(leaderResponse)
                .members(memberResponses)
                .currentUserRole(currentUserRole)
                .build();
    }

    /**
     * Map GroupMember entity to GroupMemberResponse DTO
     */
    private GroupMemberResponse mapToGroupMemberResponse(GroupMember groupMember) {
        User user = groupMember.getUser();
        GroupMemberResponse response = new GroupMemberResponse();
        response.setUserId(user.getUserId());
        response.setUserFullName(user.getUserFullName());
        response.setUserAvatarUrl(user.getUserAvatarUrl());
        response.setRole(groupMember.getRole());
        return response;
    }

    /**
     * Get current user with validation
     */
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new GroupException("Unable to identify the current user");
        }
        return currentUser;
    }

    @Override
    @Transactional
    public void handleUserAccountDeactivation(Integer userId) {
        log.info("Handling account deactivation for user ID: {}", userId);

        if (userId == null) {
            throw new GroupException("User ID cannot be null");
        }

        // Find all groups where this user is a leader
        List<GroupMember> leadershipRoles = groupMemberRepository.findByUserIdAndRole(userId, GroupMemberRole.ADMIN);

        for (GroupMember leaderRole : leadershipRoles) {
            Group group = leaderRole.getGroup();
            
            // Skip if group is already inactive
            if (!Boolean.TRUE.equals(group.getGroupIsActive())) {
                continue;
            }

            log.info("Processing leadership transfer for group ID: {} (user {} was leader)", 
                    group.getGroupId(), userId);

            // Find other active admins in the group
            List<GroupMember> otherActiveAdmins = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.ADMIN)
                    .filter(member -> !member.getUser().getUserId().equals(userId))
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!otherActiveAdmins.isEmpty()) {
                // There are other active admins, no action needed
                log.info("Group {} has other active admins, no leadership transfer needed", group.getGroupId());
                continue;
            }

            // Find active members to promote
            List<GroupMember> activeMembers = groupMemberRepository.findActiveGroupMembers(
                    group.getGroupId(), GroupMemberStatus.ACTIVE)
                    .stream()
                    .filter(member -> member.getRole() == GroupMemberRole.MEMBER)
                    .filter(member -> Boolean.TRUE.equals(member.getUser().getUserIsActive()))
                    .collect(Collectors.toList());

            if (!activeMembers.isEmpty()) {
                // Promote the first active member (could be based on join date, etc.)
                GroupMember newLeader = activeMembers.get(0);
                newLeader.setRole(GroupMemberRole.ADMIN);
                groupMemberRepository.save(newLeader);
                
                log.info("Promoted user {} to admin for group {}", 
                        newLeader.getUser().getUserId(), group.getGroupId());
            } else {
                // No active members left, deactivate the group
                group.setGroupIsActive(false);
                groupRepository.save(group);
                
                log.info("No active members left in group {}, group has been deactivated", group.getGroupId());
            }
        }

        log.info("Account deactivation handling completed for user ID: {}", userId);
    }
} 