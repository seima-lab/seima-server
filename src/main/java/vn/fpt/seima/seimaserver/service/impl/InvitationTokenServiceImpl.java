package vn.fpt.seima.seimaserver.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.group.InvitationTokenData;
import vn.fpt.seima.seimaserver.service.InvitationTokenService;
import vn.fpt.seima.seimaserver.service.RedisService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of InvitationTokenService using Redis for token storage
 * Uses userId:groupId as key pattern for efficient lookups
 */
@Service
public class InvitationTokenServiceImpl implements InvitationTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvitationTokenServiceImpl.class);
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Token expiration: 30 days (in minutes)
    private static final long TOKEN_EXPIRATION_MINUTES = 30 * 24 * 60; // 30 days
    private static final String TOKEN_PREFIX = "invitation:token:";
    private static final String USER_GROUP_PREFIX = "invitation:user_group:";
    
    @Override
    public String createInvitationToken(InvitationTokenData tokenData) {
        try {
            // Generate unique token
            String token = generateUniqueToken();
            
            // Set creation and expiration times
            LocalDateTime now = LocalDateTime.now();
            tokenData.setCreatedAt(now);
            tokenData.setExpiresAt(now.plusDays(30));
            
            // Serialize token data to JSON
            String tokenDataJson = objectMapper.writeValueAsString(tokenData);
            
            // Store in Redis with both key patterns:
            // 1. Traditional token key for web link access
            String tokenKey = generateTokenKey(token);
            redisService.set(tokenKey, tokenDataJson);
            redisService.setTimeToLiveInMinutes(tokenKey, TOKEN_EXPIRATION_MINUTES);
            
            // 2. User-Group key for direct lookup during accept/reject
            String userGroupKey = generateTokenKeyByUserAndGroup(tokenData.getInvitedUserId(), tokenData.getGroupId());
            redisService.set(userGroupKey, token); // Store the token as value
            redisService.setTimeToLiveInMinutes(userGroupKey, TOKEN_EXPIRATION_MINUTES);
            
            logger.info("Created invitation token for user {} to join group {} (expires in 30 days)", 
                    tokenData.getInvitedUserId(), tokenData.getGroupId());
            
            return token;
            
        } catch (Exception e) {
            logger.error("Failed to create invitation token for user {} to group {}", 
                    tokenData.getInvitedUserId(), tokenData.getGroupId(), e);
            throw new RuntimeException("Failed to create invitation token", e);
        }
    }
    
    @Override
    public Optional<InvitationTokenData> getInvitationTokenData(String token) {
        try {
            String redisKey = generateTokenKey(token);
            String tokenDataJson = redisService.getObject(redisKey, String.class);
            
            if (tokenDataJson == null) {
                logger.debug("Invitation token not found or expired: {}", token);
                return Optional.empty();
            }
            
            // Deserialize token data from JSON
            InvitationTokenData tokenData = objectMapper.readValue(tokenDataJson, InvitationTokenData.class);
            
            // Check if token is expired
            if (tokenData.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.debug("Invitation token expired: {}", token);
                // Remove expired token
                removeInvitationToken(token);
                return Optional.empty();
            }
            
            return Optional.of(tokenData);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve invitation token data: {}", token, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean updateInvitationTokenStatus(String token, String newStatus) {
        try {
            Optional<InvitationTokenData> tokenDataOpt = getInvitationTokenData(token);
            
            if (tokenDataOpt.isEmpty()) {
                logger.debug("Cannot update status - token not found: {}", token);
                return false;
            }
            
            InvitationTokenData tokenData = tokenDataOpt.get();
            tokenData.setStatus(newStatus);
            
            // Check if token is not expired
            if (tokenData.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.debug("Cannot update status - token expired: {}", token);
                return false;
            }
            
            // Update in Redis - maintain original expiration
            String tokenDataJson = objectMapper.writeValueAsString(tokenData);
            String redisKey = generateTokenKey(token);
            
            // Calculate remaining time until expiration in minutes
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = tokenData.getExpiresAt();
            long remainingMinutes = java.time.Duration.between(now, expiresAt).toMinutes();
            
            if (remainingMinutes <= 0) {
                logger.debug("Cannot update status - token expired: {}", token);
                return false;
            }
            
            // Set value with remaining TTL
            redisService.set(redisKey, tokenDataJson);
            redisService.setTimeToLiveInMinutes(redisKey, remainingMinutes);
            
            logger.info("Updated invitation token status to {} for token: {}", newStatus, token);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to update invitation token status: {}", token, e);
            return false;
        }
    }
    
    @Override
    public boolean removeInvitationToken(String token) {
        try {
            String redisKey = generateTokenKey(token);
            
            // Check if token exists and get token data to find user-group key
            String tokenDataJson = redisService.getObject(redisKey, String.class);
            if (tokenDataJson == null) {
                logger.debug("Invitation token not found for removal: {}", token);
                return false;
            }
            
            // Parse token data to get userId and groupId for cleanup
            try {
                InvitationTokenData tokenData = objectMapper.readValue(tokenDataJson, InvitationTokenData.class);
                String userGroupKey = generateTokenKeyByUserAndGroup(tokenData.getInvitedUserId(), tokenData.getGroupId());
                redisService.delete(userGroupKey);
            } catch (Exception e) {
                logger.warn("Failed to cleanup user-group key for token: {}", token, e);
            }
            
            // Delete the main token
            redisService.delete(redisKey);
            logger.info("Removed invitation token: {}", token);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to remove invitation token: {}", token, e);
            return false;
        }
    }
    
    @Override
    public boolean removeInvitationTokenByUserAndGroup(Integer userId, Integer groupId) {
        try {
            String userGroupKey = generateTokenKeyByUserAndGroup(userId, groupId);
            
            // Get the token from user-group key
            String token = redisService.getObject(userGroupKey, String.class);
            if (token == null) {
                logger.debug("No invitation token found for user {} in group {}", userId, groupId);
                return false;
            }
            
            // Delete both keys
            redisService.delete(userGroupKey);
            String tokenKey = generateTokenKey(token);
            redisService.delete(tokenKey);
            
            logger.info("Removed invitation token for user {} in group {}", userId, groupId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to remove invitation token for user {} in group {}", userId, groupId, e);
            return false;
        }
    }

    
    @Override
    public String generateTokenKey(String token) {
        return TOKEN_PREFIX + token;
    }
    
    @Override
    public String generateTokenKeyByUserAndGroup(Integer userId, Integer groupId) {
        return USER_GROUP_PREFIX + userId + ":" + groupId;
    }
    

    private String generateUniqueToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 