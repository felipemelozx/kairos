package com.felipemelozx.kairos.security;

import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.csrf.CsrfTokenService;
import com.felipemelozx.kairos.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthCookieService {

    private final JwtService jwtService;
    private final CsrfTokenService csrfTokenService;
    private final UserRepository userRepository;

    public AuthCookieService(JwtService jwtService, CsrfTokenService csrfTokenService,
                             UserRepository userRepository) {
        this.jwtService = jwtService;
        this.csrfTokenService = csrfTokenService;
        this.userRepository = userRepository;
    }

    public void issueAuthCookies(HttpServletResponse response, UUID userId) {
        int tokenVersion = userRepository.findById(userId)
                .map(User::getTokenVersion)
                .orElse(0);
        issueAuthCookies(response, userId, tokenVersion);
    }

    public void issueAuthCookies(HttpServletResponse response, UUID userId, int tokenVersion) {
        String accessToken = jwtService.generateAccessToken(userId, tokenVersion);
        String refreshToken = jwtService.generateRefreshToken(userId, tokenVersion);
        CookieUtils.addAccessTokenCookie(response, accessToken);
        CookieUtils.addRefreshTokenCookie(response, refreshToken);
        CookieUtils.addCsrfTokenCookie(response, csrfTokenService.generateToken());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        CookieUtils.clearCookies(response);
    }
}
