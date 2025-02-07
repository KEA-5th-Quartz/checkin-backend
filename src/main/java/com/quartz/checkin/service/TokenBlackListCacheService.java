package com.quartz.checkin.service;

import com.quartz.checkin.common.cache.TokenBlacklistCache;
import com.quartz.checkin.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TokenBlackListCacheService {

    private final TokenBlacklistCache tokenBlacklistCache;

    public TokenBlackListCacheService(CacheManager cacheManager) {
        this.tokenBlacklistCache = (TokenBlacklistCache) cacheManager.getCache(CacheConfig.TOKEN_BLACKLIST_CACHE);
    }

    public boolean isBlackList(String key) {
        ValueWrapper wrapper = tokenBlacklistCache.get(key);
        return wrapper != null;
    }

    public void addBlacklist(String key) {
        tokenBlacklistCache.put(key, System.currentTimeMillis());
    }
}
