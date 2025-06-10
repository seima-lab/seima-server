package vn.fpt.seima.seimaserver.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.service.RedisService;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final HashOperations<Object, Object, Object> hashOperations;
    private final ObjectMapper objectMapper;

    public RedisServiceImpl(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash(); // Khởi tạo HashOperations từ RedisTemplate
        this.objectMapper = objectMapper;
    }

    @Override
    public void set(Object key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void setTimeToLive(Object key, long timeoutInDays) {
        redisTemplate.expire(key, timeoutInDays, TimeUnit.SECONDS);
    }

    @Override
    public void hashSet(Object key, String field, Object value) {
        hashOperations.put(key, field, value);
    }

    @Override
    public boolean hashExists(Object key, String field) {
        return hashOperations.hasKey(key, field);
    }

    @Override
    public Object get(Object key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T getObject(Object key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        
        try {
            // If the value is already of the expected type, return it directly
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }
            
            // If it's a LinkedHashMap or other Map type, convert it using ObjectMapper
            if (value instanceof Map) {
                return objectMapper.convertValue(value, clazz);
            }
            
            // For other types, try to convert using ObjectMapper
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Redis value to " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public Map<Object, Object> getField(Object key) {
        return hashOperations.entries(key);
    }

    @Override
    public Object hashGet(Object key, String field) {
        return hashOperations.get(key, field);
    }

    @Override
    public List<Object> hashGetByFieldPrefix(Object key, String fieldPrefix) {
        List<Object> objects = new ArrayList<>();
        Map<Object, Object> hashEntries = hashOperations.entries(key);
        for (Map.Entry<Object, Object> entry : hashEntries.entrySet()) {
            if (entry.getKey().toString().startsWith(fieldPrefix)) {
                objects.add(entry.getValue());
            }
        }
        return objects;
    }

    @Override
    public Set<Object> getFieldPrefixes(Object key) {
        return hashOperations.entries(key).keySet();
    }

    @Override
    public void delete(Object key) {
        redisTemplate.delete(key);
    }

    @Override
    public void delete(Object key, String field) {
        hashOperations.delete(key, field);
    }

    @Override
    public void delete(Object key, List<String> fields) {
        for (String field : fields) {
            hashOperations.delete(key, field);
        }
    }

    @Override
    public void setTimeToLiveInMinutes(Object key, long timeoutInMinutes) {
        redisTemplate.expire(key, timeoutInMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void hashMultiSet(Object key, Map<String, ?> map) {
        hashOperations.putAll(key, map);
    }

}
