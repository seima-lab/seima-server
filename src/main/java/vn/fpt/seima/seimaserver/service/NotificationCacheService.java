package vn.fpt.seima.seimaserver.service;

public interface NotificationCacheService {
    
    /** Get unread notification count from cache */
    Long getUnreadCountFromCache(Integer userId);
    
    /** Set unread notification count in cache */
    void setUnreadCountInCache(Integer userId, Long count);
    
    /** Increment unread notification count in cache */
    Long incrementUnreadCount(Integer userId);
    
    /** Decrement unread notification count in cache */
    Long decrementUnreadCount(Integer userId);
    
    /** Reset unread notification count in cache to 0 */
    void resetUnreadCount(Integer userId);
    
    /** Remove unread notification count from cache */
    void removeUnreadCountFromCache(Integer userId);
    
    /** Get cache key for user's unread count */
    String getUnreadCountCacheKey(Integer userId);
} 