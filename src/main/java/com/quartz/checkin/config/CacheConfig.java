package com.quartz.checkin.config;

import com.quartz.checkin.common.cache.LoginBlockCache;
import com.quartz.checkin.common.cache.LoginFailureCache;
import com.quartz.checkin.common.cache.RoleUpdateCache;
import com.quartz.checkin.common.cache.TokenBlacklistCache;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LOGIN_BLOCK_CACHE = "loginBlock";
    public static final String LOGIN_FAILURE_CACHE = "loginFailure";
    public static final String ROLE_UPDATE_CACHE = "roleUpdate";
    public static final String TOKEN_BLACKLIST_CACHE = "tokenBlacklist";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(List.of(
                new LoginFailureCache(LOGIN_FAILURE_CACHE),
                new LoginBlockCache(LOGIN_BLOCK_CACHE),
                new RoleUpdateCache(ROLE_UPDATE_CACHE),
                new TokenBlacklistCache(TOKEN_BLACKLIST_CACHE)
        ));
        simpleCacheManager.afterPropertiesSet();
        return simpleCacheManager;
    }

    // 1시간마다 TTL이 포함된 캐시들에서 만료된 데이터들을 정리합니다.
    @Scheduled(cron = "0 0 * * * *")
    public void evictCaches() {
        CacheManager cacheManager = cacheManager();

        LoginFailureCache loginFailureCache =
                (LoginFailureCache) cacheManager.getCache(LOGIN_FAILURE_CACHE);

        LoginBlockCache loginBlockCache =
                (LoginBlockCache) cacheManager.getCache(LOGIN_BLOCK_CACHE);

        TokenBlacklistCache tokenBlacklistCache =
                (TokenBlacklistCache) cacheManager.getCache(TOKEN_BLACKLIST_CACHE);

        loginFailureCache.evictAllExpiredData();
        loginBlockCache.evictAllExpiredData();
        tokenBlacklistCache.evictAllExpiredData();
    }
}
