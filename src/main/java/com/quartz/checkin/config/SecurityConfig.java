package com.quartz.checkin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.cache.LoginBlockCache;
import com.quartz.checkin.security.filter.CustomUsernamePasswordAuthenticationFilter;
import com.quartz.checkin.security.filter.JwtAuthenticationFilter;
import com.quartz.checkin.security.handler.CustomAccessDeniedHandler;
import com.quartz.checkin.security.handler.CustomAuthenticationEntryPoint;
import com.quartz.checkin.security.handler.CustomLoginFailureHandler;
import com.quartz.checkin.security.handler.CustomLoginSuccessHandler;
import com.quartz.checkin.security.handler.CustomLogoutHandler;
import com.quartz.checkin.security.handler.CustomLogoutSuccessHandler;
import com.quartz.checkin.security.service.CustomUserDetailsService;
import com.quartz.checkin.service.LoginBlockCacheService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserService;
    private final CustomLoginFailureHandler customLoginFailureHandler;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomLogoutHandler customLogoutHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginBlockCacheService loginBlockCacheService;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(
                        FrameOptionsConfig::disable
                ))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/members/check-*").permitAll()
                        .requestMatchers("/members/password-reset", "/members/*/password-reset").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                );

        http.addFilterAfter(customUsernamePasswordAuthenticationFilter(), LogoutFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, CustomUsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserService);

        // UserNotFoundException을 BadCredentialException으로 변환하지 않게 한다.
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
    }

    @Bean
    public CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter() {
        CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter =
                new CustomUsernamePasswordAuthenticationFilter(objectMapper, loginBlockCacheService);

        customUsernamePasswordAuthenticationFilter.setAuthenticationManager(authenticationManager());
        customUsernamePasswordAuthenticationFilter.setAuthenticationSuccessHandler(customLoginSuccessHandler);
        customUsernamePasswordAuthenticationFilter.setAuthenticationFailureHandler(customLoginFailureHandler);
        return customUsernamePasswordAuthenticationFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:8080")); //Vue.js 기본 포트
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}