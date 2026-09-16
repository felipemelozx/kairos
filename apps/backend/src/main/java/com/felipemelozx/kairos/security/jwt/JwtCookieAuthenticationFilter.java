package com.felipemelozx.kairos.security.jwt;

import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtCookieAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = CookieUtils.getAccessTokenFromCookies(request.getCookies());

        if (token != null && jwtService.validateToken(token) && jwtService.isAccessToken(token)) {
            try {
                String userId = jwtService.getUserIdFromToken(token);
                int tokenVersion = jwtService.getTokenVersionFromToken(token);
                UUID id = UUID.fromString(userId);
                boolean versionMatches = userRepository.findById(id)
                        .map(user -> {
                            int current = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
                            return tokenVersion == current;
                        })
                        .orElse(false);
                if (versionMatches) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            new User(userId, "", Collections.emptyList()),
                            null,
                            Collections.emptyList()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
