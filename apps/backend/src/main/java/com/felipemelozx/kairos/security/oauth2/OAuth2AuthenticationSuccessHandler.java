package com.felipemelozx.kairos.security.oauth2;

import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.entity.User;
import com.felipemelozx.kairos.entity.enums.AuthProvider;
import com.felipemelozx.kairos.repository.UserRepository;
import com.felipemelozx.kairos.security.CookieUtils;
import com.felipemelozx.kairos.security.jwt.JwtService;
import com.felipemelozx.kairos.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final JwtService jwtService;

    public OAuth2AuthenticationSuccessHandler(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        UserResponse userResponse = authService.findOrCreateGoogleUser(email, name, picture);

        String accessToken = jwtService.generateAccessToken(userResponse.id());
        String refreshToken = jwtService.generateRefreshToken(userResponse.id());

        CookieUtils.addAccessTokenCookie(response, accessToken);
        CookieUtils.addRefreshTokenCookie(response, refreshToken);

        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}
