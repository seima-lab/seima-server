package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.repository.GroupMemberRepository;
import vn.fpt.seima.seimaserver.service.impl.GroupValidationServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupValidationServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private GroupValidationServiceImpl groupValidationService;

    private static final Integer TEST_USER_ID = 1;
    private static final Integer TEST_GROUP_ID = 1;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(groupMemberRepository);
    }

    @Test
    void validateUserCanJoinMoreGroups_WhenUserHasLessThanMaxGroups_ShouldNotThrowException() {
        // Given
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(5L); // User has 5 groups, max is 10

        // When & Then
        assertDoesNotThrow(() -> 
            groupValidationService.validateUserCanJoinMoreGroups(TEST_USER_ID)
        );
        
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateUserCanJoinMoreGroups_WhenUserHasMaxGroups_ShouldThrowException() {
        // Given
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(10L); // User has 10 groups, max is 10

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinMoreGroups(TEST_USER_ID)
        );
        
        assertEquals("User has reached the maximum number of groups (10). Cannot join more groups.", 
            exception.getMessage());
        
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateUserCanJoinMoreGroups_WhenUserHasMoreThanMaxGroups_ShouldThrowException() {
        // Given
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(15L); // User has 15 groups, max is 10

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinMoreGroups(TEST_USER_ID)
        );
        
        assertEquals("User has reached the maximum number of groups (10). Cannot join more groups.", 
            exception.getMessage());
        
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateUserCanJoinMoreGroups_WhenUserIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinMoreGroups(null)
        );
        
        assertEquals("User ID cannot be null", exception.getMessage());
        
        verify(groupMemberRepository, never()).countUserActiveGroups(any(), any());
    }

    @Test
    void validateGroupCanAcceptMoreMembers_WhenGroupHasLessThanMaxMembers_ShouldNotThrowException() {
        // Given
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(15L); // Group has 15 members, max is 20

        // When & Then
        assertDoesNotThrow(() -> 
            groupValidationService.validateGroupCanAcceptMoreMembers(TEST_GROUP_ID)
        );
        
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateGroupCanAcceptMoreMembers_WhenGroupHasMaxMembers_ShouldThrowException() {
        // Given
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(20L); // Group has 20 members, max is 20

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateGroupCanAcceptMoreMembers(TEST_GROUP_ID)
        );
        
        assertEquals("Group has reached the maximum number of members (20). Cannot accept more members.", 
            exception.getMessage());
        
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateGroupCanAcceptMoreMembers_WhenGroupHasMoreThanMaxMembers_ShouldThrowException() {
        // Given
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(25L); // Group has 25 members, max is 20

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateGroupCanAcceptMoreMembers(TEST_GROUP_ID)
        );
        
        assertEquals("Group has reached the maximum number of members (20). Cannot accept more members.", 
            exception.getMessage());
        
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateGroupCanAcceptMoreMembers_WhenGroupIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateGroupCanAcceptMoreMembers(null)
        );
        
        assertEquals("Group ID cannot be null", exception.getMessage());
        
        verify(groupMemberRepository, never()).countActiveGroupMembers(any(), any());
    }

    @Test
    void validateUserCanJoinGroup_WhenAllValidationsPass_ShouldNotThrowException() {
        // Given
        when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(false); // User is not already a member
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(5L); // User has 5 groups
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(15L); // Group has 15 members

        // When & Then
        assertDoesNotThrow(() -> 
            groupValidationService.validateUserCanJoinGroup(TEST_USER_ID, TEST_GROUP_ID)
        );
        
        verify(groupMemberRepository).existsByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void validateUserCanJoinGroup_WhenUserIsAlreadyMember_ShouldThrowException() {
        // Given
        when(groupMemberRepository.existsByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(true); // User is already a member

        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinGroup(TEST_USER_ID, TEST_GROUP_ID)
        );
        
        assertEquals("User is already an active member of this group", exception.getMessage());
        
        verify(groupMemberRepository).existsByUserAndGroupAndStatus(TEST_USER_ID, TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
        verify(groupMemberRepository, never()).countUserActiveGroups(any(), any());
        verify(groupMemberRepository, never()).countActiveGroupMembers(any(), any());
    }

    @Test
    void validateUserCanJoinGroup_WhenUserIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinGroup(null, TEST_GROUP_ID)
        );
        
        assertEquals("User ID cannot be null", exception.getMessage());
        
        verify(groupMemberRepository, never()).existsByUserAndGroupAndStatus(any(), any(), any());
    }

    @Test
    void validateUserCanJoinGroup_WhenGroupIdIsNull_ShouldThrowException() {
        // When & Then
        GroupException exception = assertThrows(GroupException.class, () ->
            groupValidationService.validateUserCanJoinGroup(TEST_USER_ID, null)
        );
        
        assertEquals("Group ID cannot be null", exception.getMessage());
        
        verify(groupMemberRepository, never()).existsByUserAndGroupAndStatus(any(), any(), any());
    }

    @Test
    void getUserActiveGroupCount_WhenValidUserId_ShouldReturnCount() {
        // Given
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(7L);

        // When
        int result = groupValidationService.getUserActiveGroupCount(TEST_USER_ID);

        // Then
        assertEquals(7, result);
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void getUserActiveGroupCount_WhenUserIdIsNull_ShouldReturnZero() {
        // When
        int result = groupValidationService.getUserActiveGroupCount(null);

        // Then
        assertEquals(0, result);
        verify(groupMemberRepository, never()).countUserActiveGroups(any(), any());
    }

    @Test
    void getUserActiveGroupCount_WhenRepositoryReturnsNull_ShouldReturnZero() {
        // Given
        when(groupMemberRepository.countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(null);

        // When
        int result = groupValidationService.getUserActiveGroupCount(TEST_USER_ID);

        // Then
        assertEquals(0, result);
        verify(groupMemberRepository).countUserActiveGroups(TEST_USER_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void getGroupActiveMemberCount_WhenValidGroupId_ShouldReturnCount() {
        // Given
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(12L);

        // When
        int result = groupValidationService.getGroupActiveMemberCount(TEST_GROUP_ID);

        // Then
        assertEquals(12, result);
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }

    @Test
    void getGroupActiveMemberCount_WhenGroupIdIsNull_ShouldReturnZero() {
        // When
        int result = groupValidationService.getGroupActiveMemberCount(null);

        // Then
        assertEquals(0, result);
        verify(groupMemberRepository, never()).countActiveGroupMembers(any(), any());
    }

    @Test
    void getGroupActiveMemberCount_WhenRepositoryReturnsNull_ShouldReturnZero() {
        // Given
        when(groupMemberRepository.countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE))
                .thenReturn(null);

        // When
        int result = groupValidationService.getGroupActiveMemberCount(TEST_GROUP_ID);

        // Then
        assertEquals(0, result);
        verify(groupMemberRepository).countActiveGroupMembers(TEST_GROUP_ID, GroupMemberStatus.ACTIVE);
    }
} 