package com.quartz.checkin.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@Slf4j
public class LoginFailureCache extends ConcurrentMapCache {

    private final Map<String, LoginFailureInfo> cache = new ConcurrentHashMap<>();
    private static final long TTL = 5 * 60 * 1000; // 5분
    public LoginFailureCache(String name) {
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
    public void evict(Object key) {
        String k = (String) key;
        cache.remove(k);
        log.info("로그인 실패 캐시 키({}) 초기화", k);
    }

    @Override
    public void clear() {
        cache.clear();
        log.info("로그인 실패 캐시 전체 초기화");
    }

    @Override
    protected Object lookup(Object key) {
        LoginFailureInfo loginFailureInfo = cache.get((String) key);
        if (loginFailureInfo == null) {
            log.info("{}에 대한 로그인 실패 기록 없음", key);
            return null;
        }
        if (isExpired(loginFailureInfo)) {
            log.info("{} 로그인 실패 횟수 유효기간 만료, 새로 카운트 시작", key);
            return new LoginFailureInfo();
        }

        loginFailureInfo.increment();
        log.info("{} 로그인 실패 횟수: {}", key, loginFailureInfo.getCount());
        return loginFailureInfo;
    }

    @Override
    public void put(Object key, Object value) {
        String k = (String) key;
        cache.put(k, new LoginFailureInfo());
    }

    private boolean isExpired(LoginFailureInfo loginFailureInfo) {
        return System.currentTimeMillis() - loginFailureInfo.getFirstFailureTime() > TTL;
    }


}
