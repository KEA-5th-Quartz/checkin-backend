package com.quartz.checkin.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@Slf4j
public class TokenBlacklistCache extends ConcurrentMapCache {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();
    private static final long TTL = 60 * 60 * 1000; // 1시간

    public TokenBlacklistCache(String name) {
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
    protected Object lookup(Object key) {
        String k = (String) key;
        Long blockedAt = cache.get(k);
        if (blockedAt == null) {
            log.info("토큰({})은 블랙리스트에 존재하지 않습니다.",k);
            return null;
        }
        if (isExpired(blockedAt)) {
            log.info("토큰({})에 대한 블랙리스트 유효기간이 지났습니다. 캐시에서 제거합니다.");
            cache.remove(key);
            return null;
        }
        return blockedAt;
    }


    @Override
    public void put(Object key, Object value) {
        String k = (String) key;
        log.info("토큰{{}}를 블랙리스트에 기록합니다.", k);
        cache.put(k, (Long) value);
    }

    @Override
    public void evict(Object key) {
        cache.remove((String) key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    private boolean isExpired(Long blockedAt) {
        return System.currentTimeMillis() - blockedAt > TTL;
    }

    public void evictAllExpiredData() {
        log.info("토큰 블랙리스트 캐시에 대한 정리를 시작합니다.");
        cache.keySet()
                .stream()
                .filter(k -> isExpired(cache.get(k)))
                .forEach(k -> {
                    log.info("키 {}를 블랙리스트 캐시에서 삭제합니다.", k);
                    this.evict(k);
                });
    }
}
