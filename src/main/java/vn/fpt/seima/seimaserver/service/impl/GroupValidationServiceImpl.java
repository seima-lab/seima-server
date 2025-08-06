package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.service.GroupValidationService;

/**
 * Implementation of GroupValidationService
 * Handles validation for group business rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupValidationServiceImpl implements GroupValidationService {
    
    private static final int MAX_GROUPS_PER_USER = 10;
    private static final int MAX_MEMBERS_PER_GROUP = 20;
    
    private final GroupMemberRepository groupMemberRepository;
    
    @Override
    public void validateUserCanJoinMoreGroups(Integer userId) {
        if (userId == null) {
            throw new GroupException("User ID cannot be null");
        }
        
        int currentGroupCount = getUserActiveGroupCount(userId);
        
        if (currentGroupCount >= MAX_GROUPS_PER_USER) {
            throw new GroupException(
                String.format("User has reached the maximum number of groups (%d). Cannot join more groups.", 
                    MAX_GROUPS_PER_USER)
            );
        }
        
        log.debug("User {} can join more groups. Current count: {}, Max allowed: {}", 
            userId, currentGroupCount, MAX_GROUPS_PER_USER);
    }
    
    @Override
    public void validateGroupCanAcceptMoreMembers(Integer groupId) {
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        
        int currentMemberCount = getGroupActiveMemberCount(groupId);
        
        if (currentMemberCount >= MAX_MEMBERS_PER_GROUP) {
            throw new GroupException(
                String.format("Group has reached the maximum number of members (%d). Cannot accept more members.", 
                    MAX_MEMBERS_PER_GROUP)
            );
        }
        
        log.debug("Group {} can accept more members. Current count: {}, Max allowed: {}", 
            groupId, currentMemberCount, MAX_MEMBERS_PER_GROUP);
    }
    
    @Override
    public void validateUserCanJoinGroup(Integer userId, Integer groupId) {
        if (userId == null) {
            throw new GroupException("User ID cannot be null");
        }
        if (groupId == null) {
            throw new GroupException("Group ID cannot be null");
        }
        
        // Check if user is already a member of this group
        boolean isAlreadyMember = groupMemberRepository.existsByUserAndGroupAndStatus(
            userId, groupId, GroupMemberStatus.ACTIVE);
        
        if (isAlreadyMember) {
            throw new GroupException("User is already an active member of this group");
        }
        
        // Validate user can join more groups
        validateUserCanJoinMoreGroups(userId);
        
        // Validate group can accept more members
        validateGroupCanAcceptMoreMembers(groupId);
        
        log.debug("User {} can join group {}. All validations passed.", userId, groupId);
    }
    
    @Override
    public int getUserActiveGroupCount(Integer userId) {
        if (userId == null) {
            return 0;
        }
        
        Long count = groupMemberRepository.countUserActiveGroups(userId, GroupMemberStatus.ACTIVE);
        return count != null ? count.intValue() : 0;
    }
    
    @Override
    public int getGroupActiveMemberCount(Integer groupId) {
        if (groupId == null) {
            return 0;
        }
        
        Long count = groupMemberRepository.countActiveGroupMembers(groupId, GroupMemberStatus.ACTIVE);
        return count != null ? count.intValue() : 0;
    }
} 