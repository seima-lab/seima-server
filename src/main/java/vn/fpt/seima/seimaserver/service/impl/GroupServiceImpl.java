package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.GroupService;
import vn.fpt.seima.seimaserver.util.UserUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;

    @Override
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        log.info("Creating group with name: {}", request.getGroupName());
        
        // Validate request
        validateCreateGroupRequest(request);
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Create and save group
        Group group = createGroupEntity(request);
        Group savedGroup = groupRepository.save(group);
        log.info("Group created with ID: {}", savedGroup.getGroupId());
        
        // Add creator as admin member
        createAdminMembership(savedGroup, currentUser);
        
        GroupResponse response = groupMapper.toResponse(savedGroup);
        log.info("Group creation completed successfully");
        return response;
    }
    
    private void validateCreateGroupRequest(CreateGroupRequest request) {
        if (request == null) {
            throw new GroupException("Group request cannot be null");
        }
        
        if (!StringUtils.hasText(request.getGroupName())) {
            throw new GroupException("Group name is required and cannot be empty");
        }
        
        if (request.getGroupName().trim().length() > 100) {
            throw new GroupException("Group name cannot exceed 100 characters");
        }
    }
    
    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new GroupException("Unable to identify the current user");
        }
        return currentUser;
    }
    
    private Group createGroupEntity(CreateGroupRequest request) {
        Group group = groupMapper.toEntity(request);
        // Ensure default values are set
        if (group.getGroupIsActive() == null) {
            group.setGroupIsActive(true);
        }
        return group;
    }
    
    private void createAdminMembership(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMember.setRole(GroupMemberRole.ADMIN);
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        
        GroupMember savedMember = groupMemberRepository.save(groupMember);
        log.info("Admin membership created for user ID: {} in group ID: {}", 
                user.getUserId(), group.getGroupId());
    }
} 