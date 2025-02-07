package com.quartz.checkin.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@Slf4j
public class LoginBlockCache extends ConcurrentMapCache {

    private final Map<String, Long> cache = new ConcurrentHashMap<>();
    private static final long TTL = 30 * 60 * 1000; //30분
    public LoginBlockCache(String name) {
        super(name);
    }

    public long getBlockTimeLeft(String key) {
        long time = (Long) get(key).get();
        long now = System.currentTimeMillis();
        return (time + TTL) - now;
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
            log.info("{}에 대한 로그인 잠금 기록이 없습니다.",k);
            return null;
        }
        if (isExpired(blockedAt)) {
            log.info("{}에 대한 로그인 잠금 유효기간이 지났습니다. 캐시에서 제거합니다.");
            cache.remove(key);
            return null;
        }
        return blockedAt;
    }


    @Override
    public void put(Object key, Object value) {
        String k = (String) key;
        log.info("{}를 로그인 잠금 캐시에 기록합니다.", k);
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
        log.info("로그인 잠금 캐시에 대한 정리를 시작합니다.");
        cache.keySet()
                .stream()
                .filter(k -> isExpired(cache.get(k)))
                .forEach(k -> {
                    log.info("키 {}를 로그인 잠금 캐시에서 삭제합니다.", k);
                    this.evict(k);
                });
    }
}
