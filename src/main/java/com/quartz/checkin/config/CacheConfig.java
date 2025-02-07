package com.quartz.checkin.config;

import com.quartz.checkin.common.cache.LoginBlockCache;
import com.quartz.checkin.common.cache.LoginFailureCache;
import com.quartz.checkin.common.cache.TokenBlacklistCache;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LOGIN_BLOCK_CACHE = "loginBlock";
    public static final String LOGIN_FAILURE_CACHE = "loginFailure";
    public static final String TOKEN_BLACKLIST_CACHE = "tokenBlacklist";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(List.of(
                new LoginFailureCache(LOGIN_FAILURE_CACHE),
                new LoginBlockCache(LOGIN_BLOCK_CACHE),
                new TokenBlacklistCache(TOKEN_BLACKLIST_CACHE)
        ));
        simpleCacheManager.afterPropertiesSet();
        return simpleCacheManager;
    }
}
