package vn.fpt.seima.seimaserver.service;

public interface TokenBlacklistService {
    /**
     * Add token to blacklist
     * @param token JWT token to blacklist
     * @param expirationTime Token expiration time in milliseconds
     */
    void blacklistToken(String token, long expirationTime);
    
    /**
     * Check if token is blacklisted
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * Remove expired tokens from blacklist
     */
    void cleanupExpiredTokens();
} 