package com.quartz.checkin.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberConfig {

    @Value("${user.profile.defaultImageUrl}")
    private String profilePic;

    public static String defaultProfilePic;

    @PostConstruct
    public void init() {
        defaultProfilePic = profilePic;
    }
}
