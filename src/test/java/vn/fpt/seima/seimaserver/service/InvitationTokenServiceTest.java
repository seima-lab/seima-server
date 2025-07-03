package vn.fpt.seima.seimaserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.dto.request.group.InvitationTokenData;
import vn.fpt.seima.seimaserver.service.impl.InvitationTokenServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationTokenServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InvitationTokenServiceImpl invitationTokenService;

    private InvitationTokenData validTokenData;
    private String validToken;
    private String validTokenKey;
    private String validUserGroupKey;
    private String validTokenDataJson;
    
    // Test constants
    private static final String TEST_TOKEN = "abcd1234efgh5678ijkl9012";
    private static final Integer TEST_USER_ID = 123;
    private static final Integer TEST_GROUP_ID = 456;
    private static final Integer TEST_INVITER_ID = 789;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_GROUP_NAME = "Test Group";
    private static final String TEST_INVITER_NAME = "Test Inviter";
    private static final String TEST_STATUS = "INVITED";
    private static final String UPDATED_STATUS = "ACCEPTED";
    private static final String TOKEN_PREFIX = "invitation:token:";
    private static final String USER_GROUP_PREFIX = "invitation:user_group:";
    private static final long TOKEN_EXPIRATION_MINUTES = 30 * 24 * 60; // 30 days

    @BeforeEach
    void setUp() {
        // Setup test data
        LocalDateTime now = LocalDateTime.now();
        
        validTokenData = InvitationTokenData.builder()
                .groupId(TEST_GROUP_ID)
                .inviterId(TEST_INVITER_ID)
                .invitedUserId(TEST_USER_ID)
                .invitedUserEmail(TEST_EMAIL)
                .status(TEST_STATUS)
                .createdAt(now)
                .expiresAt(now.plusDays(30))
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_INVITER_NAME)
                .build();

        validToken = TEST_TOKEN;
        validTokenKey = TOKEN_PREFIX + validToken;
        validUserGroupKey = USER_GROUP_PREFIX + TEST_USER_ID + ":" + TEST_GROUP_ID;
        validTokenDataJson = "{\"groupId\":456,\"inviterId\":789,\"invitedUserId\":123,\"invitedUserEmail\":\"test@example.com\",\"status\":\"INVITED\",\"groupName\":\"Test Group\",\"inviterName\":\"Test Inviter\"}";
    }

    // ===== CREATE INVITATION TOKEN TESTS =====

    @Test
    void createInvitationToken_Success_WithValidData() throws Exception {
        // Given
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid = mock(UUID.class);
            when(mockUuid.toString()).thenReturn("abcd-1234-efgh-5678-ijkl-9012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUuid);
            
            when(objectMapper.writeValueAsString(any(InvitationTokenData.class))).thenReturn(validTokenDataJson);

            // When
            String result = invitationTokenService.createInvitationToken(validTokenData);

            // Then
            assertNotNull(result);
            assertEquals(TEST_TOKEN, result);
            
            // Verify Redis operations
            verify(redisService).set(eq(validTokenKey), anyString());
            verify(redisService).setTimeToLiveInMinutes(validTokenKey, TOKEN_EXPIRATION_MINUTES);
            verify(redisService).set(eq(validUserGroupKey), eq(TEST_TOKEN));
            verify(redisService).setTimeToLiveInMinutes(validUserGroupKey, TOKEN_EXPIRATION_MINUTES);
            
            // Verify token data was updated with timestamps
            assertNotNull(validTokenData.getCreatedAt());
            assertNotNull(validTokenData.getExpiresAt());
        }
    }

    @Test
    void createInvitationToken_ThrowsException_WhenObjectMapperFails() throws Exception {
        // Given
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid = mock(UUID.class);
            when(mockUuid.toString()).thenReturn("abcd-1234-efgh-5678-ijkl-9012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUuid);
            
            when(objectMapper.writeValueAsString(any(InvitationTokenData.class)))
                    .thenThrow(new RuntimeException("JSON serialization failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> invitationTokenService.createInvitationToken(validTokenData));
            
            assertEquals("Failed to create invitation token", exception.getMessage());
            
            // Verify no Redis operations were performed
            verify(redisService, never()).set(anyString(), anyString());
            verify(redisService, never()).setTimeToLiveInMinutes(anyString(), anyLong());
        }
    }

    @Test
    void createInvitationToken_ThrowsException_WhenRedisOperationFails() throws Exception {
        // Given
        try (MockedStatic<UUID> uuidMock = mockStatic(UUID.class)) {
            UUID mockUuid = mock(UUID.class);
            when(mockUuid.toString()).thenReturn("abcd-1234-efgh-5678-ijkl-9012");
            uuidMock.when(UUID::randomUUID).thenReturn(mockUuid);
            
            when(objectMapper.writeValueAsString(any(InvitationTokenData.class))).thenReturn(validTokenDataJson);
            doThrow(new RuntimeException("Redis connection failed")).when(redisService).set(anyString(), anyString());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> invitationTokenService.createInvitationToken(validTokenData));
            
            assertEquals("Failed to create invitation token", exception.getMessage());
        }
    }

    // ===== GET INVITATION TOKEN DATA TESTS =====

    @Test
    void getInvitationTokenData_Success_WithValidToken() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(validTokenData);

        // When
        Optional<InvitationTokenData> result = invitationTokenService.getInvitationTokenData(validToken);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validTokenData, result.get());
        verify(redisService).getObject(validTokenKey, String.class);
        verify(objectMapper).readValue(validTokenDataJson, InvitationTokenData.class);
    }

    @Test
    void getInvitationTokenData_ReturnsEmpty_WhenTokenNotFound() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(null);

        // When
        Optional<InvitationTokenData> result = invitationTokenService.getInvitationTokenData(validToken);

        // Then
        assertTrue(result.isEmpty());
        verify(redisService).getObject(validTokenKey, String.class);
        verify(objectMapper, never()).readValue(anyString(), eq(InvitationTokenData.class));
    }

    @Test
    void getInvitationTokenData_ReturnsEmpty_WhenTokenExpired() throws Exception {
        // Given
        InvitationTokenData expiredTokenData = InvitationTokenData.builder()
                .groupId(TEST_GROUP_ID)
                .inviterId(TEST_INVITER_ID)
                .invitedUserId(TEST_USER_ID)
                .invitedUserEmail(TEST_EMAIL)
                .status(TEST_STATUS)
                .createdAt(LocalDateTime.now().minusDays(35))
                .expiresAt(LocalDateTime.now().minusDays(5)) // Expired 5 days ago
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_INVITER_NAME)
                .build();

        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(expiredTokenData);

        // When
        Optional<InvitationTokenData> result = invitationTokenService.getInvitationTokenData(validToken);

        // Then
        assertTrue(result.isEmpty());
        // Should be called twice: once for getting token data, once for removing expired token
        verify(redisService, times(2)).getObject(validTokenKey, String.class);
    }

    @Test
    void getInvitationTokenData_ReturnsEmpty_WhenDeserializationFails() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class))
                .thenThrow(new RuntimeException("JSON deserialization failed"));

        // When
        Optional<InvitationTokenData> result = invitationTokenService.getInvitationTokenData(validToken);

        // Then
        assertTrue(result.isEmpty());
        verify(redisService).getObject(validTokenKey, String.class);
        verify(objectMapper).readValue(validTokenDataJson, InvitationTokenData.class);
    }

    // ===== UPDATE INVITATION TOKEN STATUS TESTS =====

    @Test
    void updateInvitationTokenStatus_Success_WithValidToken() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(validTokenData);
        when(objectMapper.writeValueAsString(any(InvitationTokenData.class))).thenReturn(validTokenDataJson);

        // When
        boolean result = invitationTokenService.updateInvitationTokenStatus(validToken, UPDATED_STATUS);

        // Then
        assertTrue(result);
        assertEquals(UPDATED_STATUS, validTokenData.getStatus());
        verify(redisService).set(eq(validTokenKey), anyString());
        verify(redisService).setTimeToLiveInMinutes(eq(validTokenKey), anyLong());
    }

    @Test
    void updateInvitationTokenStatus_ReturnsFalse_WhenTokenNotFound() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(null);

        // When
        boolean result = invitationTokenService.updateInvitationTokenStatus(validToken, UPDATED_STATUS);

        // Then
        assertFalse(result);
        verify(redisService).getObject(validTokenKey, String.class);
        verify(redisService, never()).set(anyString(), anyString());
    }

    @Test
    void updateInvitationTokenStatus_ReturnsFalse_WhenTokenExpired() throws Exception {
        // Given
        InvitationTokenData expiredTokenData = InvitationTokenData.builder()
                .groupId(TEST_GROUP_ID)
                .inviterId(TEST_INVITER_ID)
                .invitedUserId(TEST_USER_ID)
                .invitedUserEmail(TEST_EMAIL)
                .status(TEST_STATUS)
                .createdAt(LocalDateTime.now().minusDays(35))
                .expiresAt(LocalDateTime.now().minusDays(5)) // Expired 5 days ago
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_INVITER_NAME)
                .build();

        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(expiredTokenData);

        // When
        boolean result = invitationTokenService.updateInvitationTokenStatus(validToken, UPDATED_STATUS);

        // Then
        assertFalse(result);
        verify(redisService, never()).set(anyString(), anyString());
    }

    @Test
    void updateInvitationTokenStatus_ReturnsFalse_WhenSerializationFails() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(validTokenData);
        when(objectMapper.writeValueAsString(any(InvitationTokenData.class)))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        // When
        boolean result = invitationTokenService.updateInvitationTokenStatus(validToken, UPDATED_STATUS);

        // Then
        assertFalse(result);
        verify(redisService, never()).set(anyString(), anyString());
    }

    // ===== REMOVE INVITATION TOKEN TESTS =====

    @Test
    void removeInvitationToken_Success_WithValidToken() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class)).thenReturn(validTokenData);

        // When
        boolean result = invitationTokenService.removeInvitationToken(validToken);

        // Then
        assertTrue(result);
        verify(redisService).getObject(validTokenKey, String.class);
        verify(redisService).delete(validUserGroupKey);
        verify(redisService).delete(validTokenKey);
    }

    @Test
    void removeInvitationToken_ReturnsFalse_WhenTokenNotFound() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(null);

        // When
        boolean result = invitationTokenService.removeInvitationToken(validToken);

        // Then
        assertFalse(result);
        verify(redisService).getObject(validTokenKey, String.class);
        verify(redisService, never()).delete(anyString());
    }

    @Test
    void removeInvitationToken_Success_WhenUserGroupKeyCleanupFails() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        when(objectMapper.readValue(validTokenDataJson, InvitationTokenData.class))
                .thenThrow(new RuntimeException("JSON deserialization failed"));

        // When
        boolean result = invitationTokenService.removeInvitationToken(validToken);

        // Then
        assertTrue(result); // Should still succeed even if user-group key cleanup fails
        verify(redisService).getObject(validTokenKey, String.class);
        verify(redisService).delete(validTokenKey);
        // User-group key deletion should not be called due to deserialization failure
        verify(redisService, never()).delete(validUserGroupKey);
    }

    @Test
    void removeInvitationToken_ReturnsFalse_WhenRedisDeleteFails() throws Exception {
        // Given
        when(redisService.getObject(validTokenKey, String.class)).thenReturn(validTokenDataJson);
        doThrow(new RuntimeException("Redis delete failed")).when(redisService).delete(validTokenKey);

        // When
        boolean result = invitationTokenService.removeInvitationToken(validToken);

        // Then
        assertFalse(result);
        verify(redisService).getObject(validTokenKey, String.class);
    }

    // ===== REMOVE INVITATION TOKEN BY USER AND GROUP TESTS =====

    @Test
    void removeInvitationTokenByUserAndGroup_Success_WithValidData() throws Exception {
        // Given
        when(redisService.getObject(validUserGroupKey, String.class)).thenReturn(validToken);

        // When
        boolean result = invitationTokenService.removeInvitationTokenByUserAndGroup(TEST_USER_ID, TEST_GROUP_ID);

        // Then
        assertTrue(result);
        verify(redisService).getObject(validUserGroupKey, String.class);
        verify(redisService).delete(validUserGroupKey);
        verify(redisService).delete(validTokenKey);
    }

    @Test
    void removeInvitationTokenByUserAndGroup_ReturnsFalse_WhenTokenNotFound() throws Exception {
        // Given
        when(redisService.getObject(validUserGroupKey, String.class)).thenReturn(null);

        // When
        boolean result = invitationTokenService.removeInvitationTokenByUserAndGroup(TEST_USER_ID, TEST_GROUP_ID);

        // Then
        assertFalse(result);
        verify(redisService).getObject(validUserGroupKey, String.class);
        verify(redisService, never()).delete(anyString());
    }

    @Test
    void removeInvitationTokenByUserAndGroup_ReturnsFalse_WhenRedisOperationFails() throws Exception {
        // Given
        when(redisService.getObject(validUserGroupKey, String.class)).thenReturn(validToken);
        doThrow(new RuntimeException("Redis delete failed")).when(redisService).delete(validUserGroupKey);

        // When
        boolean result = invitationTokenService.removeInvitationTokenByUserAndGroup(TEST_USER_ID, TEST_GROUP_ID);

        // Then
        assertFalse(result);
        verify(redisService).getObject(validUserGroupKey, String.class);
    }

    // ===== UTILITY METHOD TESTS =====

    @Test
    void generateTokenKey_Success_WithValidToken() throws Exception {
        // When
        String result = invitationTokenService.generateTokenKey(validToken);

        // Then
        assertEquals(TOKEN_PREFIX + validToken, result);
    }

    @Test
    void generateTokenKeyByUserAndGroup_Success_WithValidData() throws Exception {
        // When
        String result = invitationTokenService.generateTokenKeyByUserAndGroup(TEST_USER_ID, TEST_GROUP_ID);

        // Then
        assertEquals(USER_GROUP_PREFIX + TEST_USER_ID + ":" + TEST_GROUP_ID, result);
    }

    // ===== HELPER METHODS =====

    private InvitationTokenData createTestTokenData(Integer groupId, Integer inviterId, Integer invitedUserId, String status) {
        LocalDateTime now = LocalDateTime.now();
        return InvitationTokenData.builder()
                .groupId(groupId)
                .inviterId(inviterId)
                .invitedUserId(invitedUserId)
                .invitedUserEmail("test@example.com")
                .status(status)
                .createdAt(now)
                .expiresAt(now.plusDays(30))
                .groupName("Test Group")
                .inviterName("Test Inviter")
                .build();
    }

    private InvitationTokenData createExpiredTokenData() {
        LocalDateTime now = LocalDateTime.now();
        return InvitationTokenData.builder()
                .groupId(TEST_GROUP_ID)
                .inviterId(TEST_INVITER_ID)
                .invitedUserId(TEST_USER_ID)
                .invitedUserEmail(TEST_EMAIL)
                .status(TEST_STATUS)
                .createdAt(now.minusDays(35))
                .expiresAt(now.minusDays(5))
                .groupName(TEST_GROUP_NAME)
                .inviterName(TEST_INVITER_NAME)
                .build();
    }
} 