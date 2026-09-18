package com.felipemelozx.kairos.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_VERSION_CLAIM = "tv";
    private static final String TOKEN_TYPE_CLAIM = "typ";

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, accessTokenExpiration, 0, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, refreshTokenExpiration, 0, REFRESH_TOKEN_TYPE);
    }

    public String generateAccessToken(UUID userId, int tokenVersion) {
        return buildToken(userId, accessTokenExpiration, tokenVersion, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(UUID userId, int tokenVersion) {
        return buildToken(userId, refreshTokenExpiration, tokenVersion, REFRESH_TOKEN_TYPE);
    }

    private String buildToken(UUID userId, long expiration, int tokenVersion, String tokenType) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim(TOKEN_VERSION_CLAIM, tokenVersion)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public int getTokenVersionFromToken(String token) {
        Object version = parseToken(token).get(TOKEN_VERSION_CLAIM);
        if (version instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    public String getTokenTypeFromToken(String token) {
        Object type = parseToken(token).get(TOKEN_TYPE_CLAIM);
        return type instanceof String s ? s : "";
    }

    public boolean isAccessToken(String token) {
        try {
            return ACCESS_TOKEN_TYPE.equals(getTokenTypeFromToken(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return REFRESH_TOKEN_TYPE.equals(getTokenTypeFromToken(token));
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
