package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.NotificationCacheService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCacheServiceImpl implements NotificationCacheService {
    
    private static final String CACHE_KEY_PREFIX = "notification:unread:";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // 24 hours TTL
    
    private final RedisTemplate<Object, Object> redisTemplate;
    
    @Override
    public Long getUnreadCountFromCache(Integer userId) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedValue != null) {
                if (cachedValue instanceof Number) {
                    return ((Number) cachedValue).longValue();
                } else if (cachedValue instanceof String) {
                    return Long.parseLong((String) cachedValue);
                }
            }
            
            log.debug("Cache miss for unread count - userId: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error getting unread count from cache for userId: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public void setUnreadCountInCache(Integer userId, Long count) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            redisTemplate.opsForValue().set(cacheKey, count, CACHE_TTL);
            log.debug("Set unread count in cache - userId: {}, count: {}", userId, count);
        } catch (Exception e) {
            log.error("Error setting unread count in cache for userId: {}", userId, e);
        }
    }
    
    @Override
    public Long incrementUnreadCount(Integer userId) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            Long newCount = redisTemplate.opsForValue().increment(cacheKey);
            
            // Set TTL if this is a new key
            if (newCount != null && newCount == 1) {
                redisTemplate.expire(cacheKey, CACHE_TTL);
            }
            
            log.debug("Incremented unread count in cache - userId: {}, newCount: {}", userId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Error incrementing unread count in cache for userId: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public Long decrementUnreadCount(Integer userId) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            Long newCount = redisTemplate.opsForValue().decrement(cacheKey);
            
            // Remove from cache if count becomes 0 or negative
            if (newCount != null && newCount <= 0) {
                redisTemplate.delete(cacheKey);
                log.debug("Removed unread count from cache (count <= 0) - userId: {}", userId);
                return 0L;
            }
            
            log.debug("Decremented unread count in cache - userId: {}, newCount: {}", userId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Error decrementing unread count in cache for userId: {}", userId, e);
            return null;
        }
    }
    
    @Override
    public void resetUnreadCount(Integer userId) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            redisTemplate.delete(cacheKey);
            log.debug("Reset unread count in cache - userId: {}", userId);
        } catch (Exception e) {
            log.error("Error resetting unread count in cache for userId: {}", userId, e);
        }
    }
    
    @Override
    public void removeUnreadCountFromCache(Integer userId) {
        try {
            String cacheKey = getUnreadCountCacheKey(userId);
            redisTemplate.delete(cacheKey);
            log.debug("Removed unread count from cache - userId: {}", userId);
        } catch (Exception e) {
            log.error("Error removing unread count from cache for userId: {}", userId, e);
        }
    }
    
    @Override
    public String getUnreadCountCacheKey(Integer userId) {
        return CACHE_KEY_PREFIX + userId;
    }
} 