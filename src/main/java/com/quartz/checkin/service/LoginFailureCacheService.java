package com.quartz.checkin.service;

import com.quartz.checkin.common.cache.LoginFailureInfo;
import com.quartz.checkin.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoginFailureCacheService {

    @Cacheable(cacheNames = {CacheConfig.LOGIN_FAILURE_CACHE})
    public LoginFailureInfo getLoginFailureInfo(String key) {
        return new LoginFailureInfo();
    }

    @CacheEvict(cacheNames = {CacheConfig.LOGIN_FAILURE_CACHE})
    public void evictLoginFailureInfo(String key) {
    }
}
