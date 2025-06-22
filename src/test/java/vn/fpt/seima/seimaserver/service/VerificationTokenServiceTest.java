package vn.fpt.seima.seimaserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.fpt.seima.seimaserver.service.impl.VerificationTokenServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private VerificationTokenServiceImpl verificationTokenService;

    private String testEmail;
    private String testToken;
    private String tokenKey;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testToken = "test-token-123";
        tokenKey = "verification-token:" + testToken;
    }

    // ===== GENERATE VERIFICATION TOKEN TESTS =====

    @Test
    void generateVerificationToken_Success_ReturnsTokenAndStoresInRedis() {
        // Given
        doNothing().when(redisService).set(anyString(), eq(testEmail));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), eq(15L));

        // When
        String result = verificationTokenService.generateVerificationToken(testEmail);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify Redis interactions
        verify(redisService).set(startsWith("verification-token:"), eq(testEmail));
        verify(redisService).setTimeToLiveInMinutes(startsWith("verification-token:"), eq(15L));
    }

    @Test
    void generateVerificationToken_WithNullEmail_StoresNullInRedis() {
        // Given
        doNothing().when(redisService).set(anyString(), isNull());
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), eq(15L));

        // When
        String result = verificationTokenService.generateVerificationToken(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify Redis interactions
        verify(redisService).set(startsWith("verification-token:"), isNull());
        verify(redisService).setTimeToLiveInMinutes(startsWith("verification-token:"), eq(15L));
    }

    @Test
    void generateVerificationToken_WithEmptyEmail_StoresEmptyStringInRedis() {
        // Given
        String emptyEmail = "";
        doNothing().when(redisService).set(anyString(), eq(emptyEmail));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), eq(15L));

        // When
        String result = verificationTokenService.generateVerificationToken(emptyEmail);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify Redis interactions
        verify(redisService).set(startsWith("verification-token:"), eq(emptyEmail));
        verify(redisService).setTimeToLiveInMinutes(startsWith("verification-token:"), eq(15L));
    }

    // ===== VALIDATE AND EXTRACT EMAIL TESTS =====

    @Test
    void validateAndExtractEmail_Success_ReturnsEmail() {
        // Given
        when(redisService.getObject(tokenKey, String.class)).thenReturn(testEmail);

        // When
        String result = verificationTokenService.validateAndExtractEmail(testToken);

        // Then
        assertEquals(testEmail, result);
        verify(redisService).getObject(tokenKey, String.class);
    }

    @Test
    void validateAndExtractEmail_WithNullToken_ReturnsNull() {
        // When
        String result = verificationTokenService.validateAndExtractEmail(null);

        // Then
        assertNull(result);
        verify(redisService, never()).getObject(anyString(), any());
    }

    @Test
    void validateAndExtractEmail_WithEmptyToken_ReturnsNull() {
        // When
        String result = verificationTokenService.validateAndExtractEmail("");

        // Then
        assertNull(result);
        verify(redisService, never()).getObject(anyString(), any());
    }

    @Test
    void validateAndExtractEmail_WithWhitespaceToken_ReturnsNull() {
        // When
        String result = verificationTokenService.validateAndExtractEmail("   ");

        // Then
        assertNull(result);
        verify(redisService, never()).getObject(anyString(), any());
    }

    @Test
    void validateAndExtractEmail_WithExpiredToken_ReturnsNull() {
        // Given - Redis returns null for expired/non-existent token
        when(redisService.getObject(tokenKey, String.class)).thenReturn(null);

        // When
        String result = verificationTokenService.validateAndExtractEmail(testToken);

        // Then
        assertNull(result);
        verify(redisService).getObject(tokenKey, String.class);
    }

    @Test
    void validateAndExtractEmail_WithInvalidToken_ReturnsNull() {
        // Given
        String invalidToken = "invalid-token";
        String invalidTokenKey = "verification-token:" + invalidToken;
        when(redisService.getObject(invalidTokenKey, String.class)).thenReturn(null);

        // When
        String result = verificationTokenService.validateAndExtractEmail(invalidToken);

        // Then
        assertNull(result);
        verify(redisService).getObject(invalidTokenKey, String.class);
    }

    // ===== IS TOKEN VALID TESTS =====

    @Test
    void isTokenValid_WithValidToken_ReturnsTrue() {
        // Given
        when(redisService.getObject(tokenKey, String.class)).thenReturn(testEmail);

        // When
        boolean result = verificationTokenService.isTokenValid(testToken);

        // Then
        assertTrue(result);
        verify(redisService).getObject(tokenKey, String.class);
    }

    @Test
    void isTokenValid_WithInvalidToken_ReturnsFalse() {
        // Given
        when(redisService.getObject(tokenKey, String.class)).thenReturn(null);

        // When
        boolean result = verificationTokenService.isTokenValid(testToken);

        // Then
        assertFalse(result);
        verify(redisService).getObject(tokenKey, String.class);
    }

    @Test
    void isTokenValid_WithNullToken_ReturnsFalse() {
        // When
        boolean result = verificationTokenService.isTokenValid(null);

        // Then
        assertFalse(result);
        verify(redisService, never()).getObject(anyString(), any());
    }

    @Test
    void isTokenValid_WithEmptyToken_ReturnsFalse() {
        // When
        boolean result = verificationTokenService.isTokenValid("");

        // Then
        assertFalse(result);
        verify(redisService, never()).getObject(anyString(), any());
    }

    // ===== INVALIDATE TOKEN TESTS =====

    @Test
    void invalidateToken_Success_DeletesTokenFromRedis() {
        // Given
        doNothing().when(redisService).delete(tokenKey);

        // When
        verificationTokenService.invalidateToken(testToken);

        // Then
        verify(redisService).delete(tokenKey);
    }

    @Test
    void invalidateToken_WithNullToken_DoesNotCallRedis() {
        // When
        verificationTokenService.invalidateToken(null);

        // Then
        verify(redisService, never()).delete(anyString());
    }

    @Test
    void invalidateToken_WithEmptyToken_DoesNotCallRedis() {
        // When
        verificationTokenService.invalidateToken("");

        // Then
        verify(redisService, never()).delete(anyString());
    }

    @Test
    void invalidateToken_WithWhitespaceToken_DoesNotCallRedis() {
        // When
        verificationTokenService.invalidateToken("   ");

        // Then
        verify(redisService, never()).delete(anyString());
    }

    // ===== INTEGRATION-STYLE TESTS =====

    @Test
    void fullTokenLifecycle_GenerateValidateInvalidate_WorksCorrectly() {
        // This test simulates the full lifecycle of a token
        
        // 1. Generate token
        doNothing().when(redisService).set(anyString(), eq(testEmail));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), eq(15L));
        
        String generatedToken = verificationTokenService.generateVerificationToken(testEmail);
        assertNotNull(generatedToken);
        
        // 2. Validate token (simulate Redis has the token)
        String generatedTokenKey = "verification-token:" + generatedToken;
        when(redisService.getObject(generatedTokenKey, String.class)).thenReturn(testEmail);
        
        String extractedEmail = verificationTokenService.validateAndExtractEmail(generatedToken);
        assertEquals(testEmail, extractedEmail);
        
        boolean isValid = verificationTokenService.isTokenValid(generatedToken);
        assertTrue(isValid);
        
        // 3. Invalidate token
        doNothing().when(redisService).delete(generatedTokenKey);
        verificationTokenService.invalidateToken(generatedToken);
        
        // 4. Validate token after invalidation (simulate Redis returns null)
        when(redisService.getObject(generatedTokenKey, String.class)).thenReturn(null);
        
        String extractedEmailAfterInvalidation = verificationTokenService.validateAndExtractEmail(generatedToken);
        assertNull(extractedEmailAfterInvalidation);
        
        boolean isValidAfterInvalidation = verificationTokenService.isTokenValid(generatedToken);
        assertFalse(isValidAfterInvalidation);
        
        // Verify all interactions
        verify(redisService).set(generatedTokenKey, testEmail);
        verify(redisService).setTimeToLiveInMinutes(generatedTokenKey, 15L);
        verify(redisService, times(4)).getObject(generatedTokenKey, String.class);
        verify(redisService).delete(generatedTokenKey);
    }

    @Test
    void tokenExpiration_SimulatesRedisExpiration() {
        // Generate token
        doNothing().when(redisService).set(anyString(), eq(testEmail));
        doNothing().when(redisService).setTimeToLiveInMinutes(anyString(), eq(15L));
        
        String generatedToken = verificationTokenService.generateVerificationToken(testEmail);
        String generatedTokenKey = "verification-token:" + generatedToken;
        
        // Initially token is valid
        when(redisService.getObject(generatedTokenKey, String.class)).thenReturn(testEmail);
        assertTrue(verificationTokenService.isTokenValid(generatedToken));
        
        // Simulate token expiration (Redis returns null)
        when(redisService.getObject(generatedTokenKey, String.class)).thenReturn(null);
        assertFalse(verificationTokenService.isTokenValid(generatedToken));
        assertNull(verificationTokenService.validateAndExtractEmail(generatedToken));
    }
} 