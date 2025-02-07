package com.quartz.checkin.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@Slf4j
public class RoleUpdateCache extends ConcurrentMapCache {

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public RoleUpdateCache(String name) {
        super(name);
    }

    @Override
    public ValueWrapper get(Object key) {
        Object value = lookup(key);
        if (value != null) {
            return new SimpleValueWrapper(value);
        }
        return null;
    }

    @Override
    protected Boolean lookup(Object key) {
        String k = (String) key;
        Boolean value = cache.get(k);
        if (value == null) {
            log.info("{}에 대한 권한 변경 기록이 없습니다.", k);
            return null;
        }
        log.info("{}에 대한 권한 변경 기록이 있습니다. 토큰 재발급이 필요합니다.", k);
        return value;
    }


    @Override
    public void put(Object key, Object value) {
        String k = (String) key;
        log.info("{}를 권한 변경 기록에 등록합니다.", k);
        cache.put(k, true);
    }

    @Override
    public void evict(Object key) {
        cache.remove((String) key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
