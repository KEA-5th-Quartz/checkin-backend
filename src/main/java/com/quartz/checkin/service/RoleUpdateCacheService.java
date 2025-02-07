package com.quartz.checkin.service;

import com.quartz.checkin.common.cache.RoleUpdateCache;
import com.quartz.checkin.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoleUpdateCacheService {

    private final RoleUpdateCache roleUpdateCache;

    public RoleUpdateCacheService(CacheManager cacheManager) {
        this.roleUpdateCache =
                (RoleUpdateCache) cacheManager.getCache(CacheConfig.ROLE_UPDATE_CACHE);
    }

    public boolean isRoleUpdated(String key) {
        ValueWrapper wrapper = roleUpdateCache.get(key);
        return wrapper != null;
    }

    public Boolean get(String key) {
        return (Boolean) roleUpdateCache.get(key).get();
    }

    @CachePut(cacheNames = CacheConfig.ROLE_UPDATE_CACHE)
    public boolean put(String key) {
        return true;
    }

    @CacheEvict(cacheNames = {CacheConfig.ROLE_UPDATE_CACHE})
    public void evict(String key) {
    }

}
