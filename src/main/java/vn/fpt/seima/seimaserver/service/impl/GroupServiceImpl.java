package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.entity.GroupMember;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.mapper.GroupMapper;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.repository.GroupRepository;
import vn.fpt.seima.seimaserver.service.GroupService;
import vn.fpt.seima.seimaserver.util.UserUtils;

@Service
@AllArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;

    @Override
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        // Get current user
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Unable to identify the current user.");
        }

        // Validate request
        if (request == null || request.getGroupName() == null || request.getGroupName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required.");
        }

        // Create group entity
        Group group = groupMapper.toEntity(request);
        
        // Save group
        Group savedGroup = groupRepository.save(group);

        // Add the creator as an ADMIN member
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(savedGroup);
        groupMember.setUser(currentUser);
        groupMember.setRole(GroupMemberRole.ADMIN);
        groupMember.setStatus(GroupMemberStatus.ACTIVE);
        
        groupMemberRepository.save(groupMember);

        return groupMapper.toResponse(savedGroup);
    }
} 