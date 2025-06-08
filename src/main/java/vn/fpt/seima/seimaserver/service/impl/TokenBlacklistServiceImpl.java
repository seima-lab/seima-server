package vn.fpt.seima.seimaserver.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.RedisService;
import vn.fpt.seima.seimaserver.service.TokenBlacklistService;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    @Autowired
    private RedisService redisService;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    @Override
    public void blacklistToken(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        // Store with expiration time to automatically cleanup
        long ttlSeconds = (expirationTime - System.currentTimeMillis()) / 1000;
        if (ttlSeconds > 0) {
            redisService.set(key, "blacklisted");
            redisService.setTimeToLive(key, ttlSeconds);
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisService.get(key) != null;
    }

    @Override
    public void cleanupExpiredTokens() {
        // This method is called manually if needed
        // For Redis, expired keys are automatically cleaned up by Redis TTL mechanism
        // But we can implement additional cleanup logic here if needed
    }

    /**
     * Scheduled task to clean up expired tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void scheduledCleanup() {
        cleanupExpiredTokens();
    }
} 