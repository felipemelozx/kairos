package com.felipemelozx.kairos.security.oauth2;

import com.felipemelozx.kairos.dto.response.UserResponse;
import com.felipemelozx.kairos.security.AuthCookieService;
import com.felipemelozx.kairos.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(AuthService authService, AuthCookieService authCookieService,
                                              @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        UserResponse userResponse = authService.findOrCreateGoogleUser(email, name, picture);

        authCookieService.issueAuthCookies(response, userResponse.id());

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/");
    }
}
