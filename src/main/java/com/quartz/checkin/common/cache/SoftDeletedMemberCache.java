package com.quartz.checkin.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@Slf4j
public class SoftDeletedMemberCache extends ConcurrentMapCache {

    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public SoftDeletedMemberCache(String name) {
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
            log.info("{}에 대한 소프트 딜리트 기록이 없습니다.", k);
            return null;
        }
        log.info("{}에 대한 소프트 딜리트 기록이 존재합니다.", k);
        return value;
    }


    @Override
    public void put(Object key, Object value) {
        String k = (String) key;
        log.info("{}의 소프트 딜리트 기록을 등록합니다.", k);
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
