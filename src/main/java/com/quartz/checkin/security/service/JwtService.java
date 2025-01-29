package com.quartz.checkin.security.service;


import com.quartz.checkin.dto.response.AuthenticationResponse;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.common.ServletResponseUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtService {

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String PASSWORD_RESET_TOKEN_SUBJECT = "PasswordReset";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final Long ACCESS_TOKEN_EXPIRATION_PERIOD = 3600000L;
    private static final Long PASSWORD_RESET_TOKEN_EXPIRATION_PERIOD = 300000L;
    private static final Long REFRESH_TOKEN_EXPIRATION_PERIOD = 604800000L;
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 604800;

    private static final String ACCESS_TOKEN_HEADER = "Authorization";
    private static final String REFRESH_TOKEN_COOKIE = "Refresh";
    private static final String BEARER = "Bearer ";

    public static final String ID_CLAIM = "id";
    public static final String ROLE_CLAIM = "role";
    public static final String PROFILE_PIC_CLAIM = "profilePic";
    public static final String USERNAME_CLAIM = "username";

    @Value("${jwt.secretKey}")
    private String secretKey;
    private SecretKey key;

    @PostConstruct
    void initializeKey() {
        key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long id, String username, String profilePic, Role role) {
        Date now = new Date();

        return Jwts
                .builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .claim(ID_CLAIM, id)
                .claim(USERNAME_CLAIM, username)
                .claim(PROFILE_PIC_CLAIM, profilePic)
                .claim(ROLE_CLAIM, role)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_PERIOD))
                .signWith(key)
                .compact();
    }

    public String createAccessToken(CustomUser customUser) {
        Date now = new Date();

        return Jwts
                .builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .claim(ID_CLAIM, customUser.getId())
                .claim(USERNAME_CLAIM, customUser.getUsername())
                .claim(PROFILE_PIC_CLAIM, customUser.getProfilePic())
                .claim(ROLE_CLAIM, customUser.getRole().getValue())
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_PERIOD))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken() {
        Date now = new Date();

        return Jwts
                .builder()
                .subject(REFRESH_TOKEN_SUBJECT)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_PERIOD))
                .signWith(key)
                .compact();
    }

    public String createPasswordResetToken(Long id) {
        Date now = new Date();

        return Jwts
                .builder()
                .subject(PASSWORD_RESET_TOKEN_SUBJECT)
                .claim(ID_CLAIM, id)
                .expiration(new Date(now.getTime() + PASSWORD_RESET_TOKEN_EXPIRATION_PERIOD))
                .signWith(key)
                .compact();
    }

    public Optional<String> extractAccessTokenFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(ACCESS_TOKEN_HEADER))
                .filter(token -> token.startsWith(BEARER))
                .map(token -> token.replace(BEARER, ""));
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                String value = cookie.getValue();
                if (cookieName.equals(REFRESH_TOKEN_COOKIE)) {
                    return Optional.ofNullable(value);
                }
            }
        }

        return Optional.empty();
    }

    public void setAuthenticationResponse(HttpServletResponse response, CustomUser customUser) {
        String accessToken = createAccessToken(customUser);
        String passwordResetToken =
                customUser.getPasswordChangedAt() == null ? createPasswordResetToken(customUser.getId()) : null;
        ServletResponseUtils.writeApiResponseWithData(response,
                AuthenticationResponse.from(customUser, accessToken, passwordResetToken));
    }

    public void setRefreshToken(HttpServletResponse response, String refreshToken) {
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
    }

    public void expireRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");

        response.addCookie(refreshTokenCookie);
    }

    public boolean isValidToken(String token) {
        try {
            Claims claims = decodeToken(token);

            if (!claims.getExpiration().after(new Date())) {
                log.warn("만료된 토큰입니다.");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("해독할 수 없는 토큰입니다. {}", e.getMessage());
            return false;
        }
    }

    public Claims decodeToken(String token) throws Exception {
        return Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
