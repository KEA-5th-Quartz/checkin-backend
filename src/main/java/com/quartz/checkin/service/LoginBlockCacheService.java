package com.quartz.checkin.service;

import com.quartz.checkin.common.cache.LoginBlockCache;
import com.quartz.checkin.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginBlockCacheService {

    private final LoginBlockCache loginBlockCache;

    public LoginBlockCacheService(CacheManager cacheManager) {
        this.loginBlockCache = (LoginBlockCache) cacheManager.getCache(CacheConfig.LOGIN_BLOCK_CACHE);
    }
    
    public boolean isBlockedMember(String key) {
        ValueWrapper wrapper = loginBlockCache.get(key);
        return wrapper != null;
    }

    public Long getBlockTimeLeft(String key) {
        return loginBlockCache.getBlockTimeLeft(key);
    }



    public void block(String key) {
        loginBlockCache.put(key, System.currentTimeMillis());
    }

    @CacheEvict(cacheNames = {CacheConfig.LOGIN_BLOCK_CACHE})
    public void evict(String key) {
    }
}
