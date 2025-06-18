package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
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
import vn.fpt.seima.seimaserver.service.impl.GroupServiceImpl;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMapper groupMapper;

    @InjectMocks
    private GroupServiceImpl groupService;

    private CreateGroupRequest validRequest;
    private User mockUser;
    private Group mockGroup;
    private GroupResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = new CreateGroupRequest();
        validRequest.setGroupName("Test Group");
        validRequest.setGroupAvatarUrl("http://example.com/avatar.jpg");

        mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setUserFullName("Test User");

        mockGroup = new Group();
        mockGroup.setGroupId(1);
        mockGroup.setGroupName("Test Group");
        mockGroup.setGroupIsActive(true);
        mockGroup.setGroupCreatedDate(LocalDateTime.now());

        mockResponse = new GroupResponse();
        mockResponse.setGroupId(1);
        mockResponse.setGroupName("Test Group");
        mockResponse.setGroupIsActive(true);
    }

    @Test
    void createGroup_Success() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.createGroup(validRequest);

            // Then
            assertNotNull(result);
            assertEquals("Test Group", result.getGroupName());
            assertEquals(1, result.getGroupId());
            assertTrue(result.getGroupIsActive());

            // Verify interactions
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(argThat(member -> 
                member.getRole() == GroupMemberRole.ADMIN &&
                member.getStatus() == GroupMemberStatus.ACTIVE &&
                member.getUser().equals(mockUser)
            ));
        }
    }

    @Test
    void createGroup_NullRequest_ThrowsException() {
        // Given
        CreateGroupRequest nullRequest = null;

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroup(nullRequest));
        assertEquals("Group request cannot be null", exception.getMessage());

        // Verify no repository interactions
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void createGroup_EmptyGroupName_ThrowsException() {
        // Given
        validRequest.setGroupName("");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroup(validRequest));
        assertEquals("Group name is required and cannot be empty", exception.getMessage());

        // Verify no repository interactions
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void createGroup_NullGroupName_ThrowsException() {
        // Given
        validRequest.setGroupName(null);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroup(validRequest));
        assertEquals("Group name is required and cannot be empty", exception.getMessage());

        // Verify no repository interactions
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void createGroup_GroupNameTooLong_ThrowsException() {
        // Given
        String longName = "a".repeat(101); // 101 characters
        validRequest.setGroupName(longName);

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroup(validRequest));
        assertEquals("Group name cannot exceed 100 characters", exception.getMessage());

        // Verify no repository interactions
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void createGroup_NoCurrentUser_ThrowsException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(null);

            // When & Then
            GroupException exception = assertThrows(GroupException.class, 
                () -> groupService.createGroup(validRequest));
            assertEquals("Unable to identify the current user", exception.getMessage());

            // Verify no repository interactions
            verifyNoInteractions(groupRepository, groupMemberRepository);
        }
    }

    @Test
    void createGroup_RepositoryException_PropagatesException() {
        // Given
        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(groupRepository.save(any(Group.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> groupService.createGroup(validRequest));
            assertEquals("Database error", exception.getMessage());

            // Verify group repository was called but member repository wasn't
            verify(groupRepository).save(any(Group.class));
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Test
    void createGroup_WithWhitespaceGroupName_ThrowsException() {
        // Given
        validRequest.setGroupName("   ");

        // When & Then
        GroupException exception = assertThrows(GroupException.class, 
            () -> groupService.createGroup(validRequest));
        assertEquals("Group name is required and cannot be empty", exception.getMessage());

        // Verify no repository interactions
        verifyNoInteractions(groupRepository, groupMemberRepository);
    }

    @Test
    void createGroup_ValidatesGroupNameLength_EdgeCase() {
        // Given - exactly 100 characters (should pass)
        String exactLength = "a".repeat(100);
        validRequest.setGroupName(exactLength);

        try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
            userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(mockUser);
            when(groupMapper.toEntity(validRequest)).thenReturn(mockGroup);
            when(groupRepository.save(any(Group.class))).thenReturn(mockGroup);
            when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(new GroupMember());
            when(groupMapper.toResponse(mockGroup)).thenReturn(mockResponse);

            // When
            GroupResponse result = groupService.createGroup(validRequest);

            // Then
            assertNotNull(result);
            verify(groupRepository).save(any(Group.class));
            verify(groupMemberRepository).save(any(GroupMember.class));
        }
    }
} 