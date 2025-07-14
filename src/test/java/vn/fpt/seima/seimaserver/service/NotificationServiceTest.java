package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.response.notification.NotificationResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.impl.NotificationServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private User senderUser;
    private Notification testNotification;
    private Group testGroup;
    private GroupMember testGroupMember;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUserEmail("test@example.com");
        testUser.setUserFullName("Test User");
        testUser.setUserIsActive(true);

        // Setup sender user
        senderUser = new User();
        senderUser.setUserId(2);
        senderUser.setUserEmail("sender@example.com");
        senderUser.setUserFullName("Sender User");
        senderUser.setUserIsActive(true);

        // Setup test group
        testGroup = new Group();
        testGroup.setGroupId(1);
        testGroup.setGroupName("Test Group");

        // Setup test group member
        testGroupMember = new GroupMember();
        testGroupMember.setUser(testUser);
        testGroupMember.setGroup(testGroup);
        testGroupMember.setRole(GroupMemberRole.ADMIN);
        testGroupMember.setStatus(GroupMemberStatus.ACTIVE);

        // Setup test notification
        testNotification = new Notification();
        testNotification.setNotificationId(1);
        testNotification.setReceiver(testUser);
        testNotification.setSender(senderUser);
        testNotification.setTitle("Test Notification");
        testNotification.setMessage("Test message");
        testNotification.setNotificationType(NotificationType.GROUP_JOIN_REQUEST);
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());
        testNotification.setLinkToEntity("seimaapp://groups/1");

        // Setup pageable
        testPageable = PageRequest.of(0, 10);
    }

    // Tests for getNotificationsWithFilters
    @Test
    void getNotificationsWithFilters_WhenValidRequest_ShouldReturnPagedNotifications() {
        // Given
        Integer userId = 1;
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(testNotification), testPageable, 1);
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.findNotificationsWithFilters(
                eq(userId), eq(false), eq(NotificationType.GROUP_JOIN_REQUEST), 
                any(LocalDateTime.class), any(LocalDateTime.class), eq(testPageable)))
                .thenReturn(notificationPage);

        // When
        Page<NotificationResponse> result = notificationService.getNotificationsWithFilters(
                userId, false, NotificationType.GROUP_JOIN_REQUEST, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testNotification.getNotificationId(), result.getContent().get(0).getNotificationId());
        assertEquals(testNotification.getTitle(), result.getContent().get(0).getTitle());
        verify(userRepository).existsById(userId);
        verify(notificationRepository).findNotificationsWithFilters(
                eq(userId), eq(false), eq(NotificationType.GROUP_JOIN_REQUEST), 
                any(LocalDateTime.class), any(LocalDateTime.class), eq(testPageable));
    }

    @Test
    void getNotificationsWithFilters_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getNotificationsWithFilters(
                        userId, null, null, null, null, testPageable)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    @Test
    void getNotificationsWithFilters_WhenInvalidDateRange_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 1;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);
        when(userRepository.existsById(userId)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getNotificationsWithFilters(
                        userId, null, null, startDate, endDate, testPageable)
        );
        assertEquals("Start date cannot be after end date", exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for getUnreadNotificationCount
    @Test
    void getUnreadNotificationCount_WhenUserExists_ShouldReturnCount() {
        // Given
        Integer userId = 1;
        long expectedCount = 5L;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.countUnreadByReceiverId(userId)).thenReturn(expectedCount);

        // When
        long result = notificationService.getUnreadNotificationCount(userId);

        // Then
        assertEquals(expectedCount, result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).countUnreadByReceiverId(userId);
    }

    @Test
    void getUnreadNotificationCount_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getUnreadNotificationCount(userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for markNotificationAsRead
    @Test
    void markNotificationAsRead_WhenValidRequest_ShouldReturnTrue() {
        // Given
        Integer notificationId = 1;
        Integer userId = 1;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.markAsReadById(notificationId, userId)).thenReturn(1);

        // When
        boolean result = notificationService.markNotificationAsRead(notificationId, userId);

        // Then
        assertTrue(result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).markAsReadById(notificationId, userId);
    }

    @Test
    void markNotificationAsRead_WhenNotificationNotFound_ShouldReturnFalse() {
        // Given
        Integer notificationId = 999;
        Integer userId = 1;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.markAsReadById(notificationId, userId)).thenReturn(0);

        // When
        boolean result = notificationService.markNotificationAsRead(notificationId, userId);

        // Then
        assertFalse(result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).markAsReadById(notificationId, userId);
    }

    @Test
    void markNotificationAsRead_WhenNotificationIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markNotificationAsRead(null, 1)
        );
        assertEquals("Notification ID and User ID cannot be null", exception.getMessage());
    }

    @Test
    void markNotificationAsRead_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markNotificationAsRead(1, null)
        );
        assertEquals("Notification ID and User ID cannot be null", exception.getMessage());
    }

    @Test
    void markNotificationAsRead_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer notificationId = 1;
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markNotificationAsRead(notificationId, userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for markAllNotificationsAsRead
    @Test
    void markAllNotificationsAsRead_WhenValidRequest_ShouldReturnCount() {
        // Given
        Integer userId = 1;
        int expectedCount = 3;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.markAllAsReadByReceiverId(userId)).thenReturn(expectedCount);

        // When
        int result = notificationService.markAllNotificationsAsRead(userId);

        // Then
        assertEquals(expectedCount, result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).markAllAsReadByReceiverId(userId);
    }

    @Test
    void markAllNotificationsAsRead_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAllNotificationsAsRead(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void markAllNotificationsAsRead_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAllNotificationsAsRead(userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for deleteNotification
    @Test
    void deleteNotification_WhenValidRequest_ShouldReturnTrue() {
        // Given
        Integer notificationId = 1;
        Integer userId = 1;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.deleteByIdAndReceiverId(notificationId, userId)).thenReturn(1);

        // When
        boolean result = notificationService.deleteNotification(notificationId, userId);

        // Then
        assertTrue(result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).deleteByIdAndReceiverId(notificationId, userId);
    }

    @Test
    void deleteNotification_WhenNotificationNotFound_ShouldReturnFalse() {
        // Given
        Integer notificationId = 999;
        Integer userId = 1;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.deleteByIdAndReceiverId(notificationId, userId)).thenReturn(0);

        // When
        boolean result = notificationService.deleteNotification(notificationId, userId);

        // Then
        assertFalse(result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).deleteByIdAndReceiverId(notificationId, userId);
    }

    @Test
    void deleteNotification_WhenNotificationIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.deleteNotification(null, 1)
        );
        assertEquals("Notification ID and User ID cannot be null", exception.getMessage());
    }

    @Test
    void deleteNotification_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.deleteNotification(1, null)
        );
        assertEquals("Notification ID and User ID cannot be null", exception.getMessage());
    }

    @Test
    void deleteNotification_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer notificationId = 1;
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.deleteNotification(notificationId, userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for deleteAllNotifications
    @Test
    void deleteAllNotifications_WhenValidRequest_ShouldReturnCount() {
        // Given
        Integer userId = 1;
        int expectedCount = 5;
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.deleteAllByReceiverId(userId)).thenReturn(expectedCount);

        // When
        int result = notificationService.deleteAllNotifications(userId);

        // Then
        assertEquals(expectedCount, result);
        verify(userRepository).existsById(userId);
        verify(notificationRepository).deleteAllByReceiverId(userId);
    }

    @Test
    void deleteAllNotifications_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.deleteAllNotifications(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void deleteAllNotifications_WhenUserNotExists_ShouldThrowIllegalArgumentException() {
        // Given
        Integer userId = 999;
        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.deleteAllNotifications(userId)
        );
        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository).existsById(userId);
    }

    // Tests for sendNotificationToGroupMembers
    @Test
    void sendNotificationToGroupMembers_WhenValidRequest_ShouldProcessSuccessfully() {
        // Given
        Integer groupId = 1;
        Integer senderUserId = 2;
        String senderUserName = "Sender User";
        NotificationType notificationType = NotificationType.GROUP_JOIN_REQUEST;
        String title = "Test Title";
        String message = "Test Message";
        String linkToEntity = "seimaapp://groups/1";

        List<GroupMember> adminMembers = Arrays.asList(testGroupMember);
        
        when(groupMemberRepository.findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(adminMembers);
        when(userRepository.findById(senderUserId)).thenReturn(Optional.of(senderUser));
        when(notificationRepository.saveAll(anyList())).thenReturn(Arrays.asList(testNotification));
        when(userDeviceRepository.findFcmTokensByUserIds(anyList())).thenReturn(Arrays.asList("token1"));

        // When
        notificationService.sendNotificationToGroupMembers(groupId, senderUserId, senderUserName, 
                notificationType, title, message, linkToEntity);

        // Then
        verify(groupMemberRepository).findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE);
        verify(userRepository).findById(senderUserId);
    }

    @Test
    void sendNotificationToGroupMembers_WhenGroupIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(null, 1, "User", 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link")
        );
        assertEquals("Group ID must be positive", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenGroupIdIsZero_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(0, 1, "User", 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link")
        );
        assertEquals("Group ID must be positive", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenSenderUserIdIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, null, "User", 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link")
        );
        assertEquals("Sender user ID must be positive", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenSenderUserNameIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, 1, null, 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link")
        );
        assertEquals("Sender user name cannot be empty", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenSenderUserNameIsEmpty_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, 1, "", 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link")
        );
        assertEquals("Sender user name cannot be empty", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenNotificationTypeIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, 1, "User", 
                        null, "Title", "Message", "Link")
        );
        assertEquals("Notification type cannot be null", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenTitleIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, 1, "User", 
                        NotificationType.GROUP_JOIN_REQUEST, null, "Message", "Link")
        );
        assertEquals("Title cannot be empty", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenMessageIsNull_ShouldThrowIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.sendNotificationToGroupMembers(1, 1, "User", 
                        NotificationType.GROUP_JOIN_REQUEST, "Title", null, "Link")
        );
        assertEquals("Message cannot be empty", exception.getMessage());
    }

    @Test
    void sendNotificationToGroupMembers_WhenNoAdminOrOwnerFound_ShouldReturnEarly() {
        // Given
        Integer groupId = 1;
        Integer senderUserId = 2;
        String senderUserName = "Sender User";
        
        when(groupMemberRepository.findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When
        notificationService.sendNotificationToGroupMembers(groupId, senderUserId, senderUserName, 
                NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link");

        // Then
        verify(groupMemberRepository).findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void sendNotificationToGroupMembers_WhenSenderUserNotFound_ShouldReturnEarly() {
        // Given
        Integer groupId = 1;
        Integer senderUserId = 999;
        String senderUserName = "Sender User";
        
        List<GroupMember> adminMembers = Arrays.asList(testGroupMember);
        
        when(groupMemberRepository.findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(adminMembers);
        when(userRepository.findById(senderUserId)).thenReturn(Optional.empty());

        // When
        notificationService.sendNotificationToGroupMembers(groupId, senderUserId, senderUserName, 
                NotificationType.GROUP_JOIN_REQUEST, "Title", "Message", "Link");

        // Then
        verify(groupMemberRepository).findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE);
        verify(userRepository).findById(senderUserId);
    }

    // Tests for sendGroupJoinRequestNotification
    @Test
    void sendGroupJoinRequestNotification_WhenValidRequest_ShouldCallSendNotificationToGroupMembers() {
        // Given
        Integer groupId = 1;
        Integer requestUserId = 2;
        String requestUserName = "Request User";
        
        List<GroupMember> adminMembers = Arrays.asList(testGroupMember);
        
        when(groupMemberRepository.findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(adminMembers);
        when(userRepository.findById(requestUserId)).thenReturn(Optional.of(senderUser));
        when(notificationRepository.saveAll(anyList())).thenReturn(Arrays.asList(testNotification));
        when(userDeviceRepository.findFcmTokensByUserIds(anyList())).thenReturn(Arrays.asList("token1"));

        // When
        notificationService.sendGroupJoinRequestNotification(groupId, requestUserId, requestUserName);

        // Then
        verify(groupMemberRepository).findAdminAndOwnerMembers(groupId, GroupMemberStatus.ACTIVE);
        verify(userRepository).findById(requestUserId);
    }

    // Test for convertToNotificationResponse private method through public method
    @Test
    void getNotificationsWithFilters_WhenNotificationHasSender_ShouldIncludeSenderInfo() {
        // Given
        Integer userId = 1;
        testNotification.setSender(senderUser);
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(testNotification), testPageable, 1);
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.findNotificationsWithFilters(
                eq(userId), isNull(), isNull(), isNull(), isNull(), eq(testPageable)))
                .thenReturn(notificationPage);

        // When
        Page<NotificationResponse> result = notificationService.getNotificationsWithFilters(
                userId, null, null, null, null, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        NotificationResponse response = result.getContent().get(0);
        assertNotNull(response.getSender());
        assertEquals(senderUser.getUserId(), response.getSender().getUserId());
        assertEquals(senderUser.getUserFullName(), response.getSender().getUserFullName());
    }

    @Test
    void getNotificationsWithFilters_WhenNotificationHasNoSender_ShouldHaveNullSender() {
        // Given
        Integer userId = 1;
        testNotification.setSender(null); // System notification
        Page<Notification> notificationPage = new PageImpl<>(Arrays.asList(testNotification), testPageable, 1);
        
        when(userRepository.existsById(userId)).thenReturn(true);
        when(notificationRepository.findNotificationsWithFilters(
                eq(userId), isNull(), isNull(), isNull(), isNull(), eq(testPageable)))
                .thenReturn(notificationPage);

        // When
        Page<NotificationResponse> result = notificationService.getNotificationsWithFilters(
                userId, null, null, null, null, testPageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        NotificationResponse response = result.getContent().get(0);
        assertNull(response.getSender());
    }
}

