package com.quartz.checkin.service;

import com.quartz.checkin.common.cache.SoftDeletedMemberCache;
import com.quartz.checkin.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SoftDeletedMemberCacheService {

    private final SoftDeletedMemberCache softDeletedMemberCache;

    public SoftDeletedMemberCacheService(CacheManager cacheManager) {
        this.softDeletedMemberCache =
                (SoftDeletedMemberCache) cacheManager.getCache(CacheConfig.SOFT_DELETED_MEMBER_CACHE);
    }

    public boolean isSoftDeleted(String key) {
        ValueWrapper wrapper = softDeletedMemberCache.get(key);
        return wrapper != null;
    }

    public Boolean get(String key) {
        return (Boolean) softDeletedMemberCache.get(key).get();
    }

    @CachePut(cacheNames = CacheConfig.ROLE_UPDATE_CACHE)
    public boolean put(String key) {
        return true;
    }

    @CacheEvict(cacheNames = {CacheConfig.ROLE_UPDATE_CACHE})
    public void evict(String key) {
    }
}
