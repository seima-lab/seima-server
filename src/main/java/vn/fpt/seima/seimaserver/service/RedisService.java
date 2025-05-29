package vn.fpt.seima.seimaserver.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisService {
    void set(Object key, Object value);

    void setTimeToLive(Object key, long timeoutInDays);

    void setTimeToLiveInMinutes(Object key, long timeoutInDays);

    void hashSet(Object key, String field, Object value);

    boolean hashExists(Object key, String field);

    Object get(Object key);

    Map<Object, Object> getField(Object key);

    Object hashGet(Object key, String field);

    List<Object> hashGetByFieldPrefix(Object key, String fieldPrefix);

    Set<Object> getFieldPrefixes(Object key);

    void delete(Object key);

    void delete(Object key, String field);

    void delete(Object key, List<String> fields);
    void hashMultiSet(Object key, Map<String, ?> map);

}
