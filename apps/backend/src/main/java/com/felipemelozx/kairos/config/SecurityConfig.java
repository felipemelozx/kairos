package com.felipemelozx.kairos.config;

import com.felipemelozx.kairos.security.filters.CsrfValidationFilter;
import com.felipemelozx.kairos.security.filters.OriginValidationFilter;
import com.felipemelozx.kairos.security.jwt.JwtCookieAuthenticationFilter;
import com.felipemelozx.kairos.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtCookieAuthenticationFilter jwtCookieFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OriginValidationFilter originValidationFilter;
    private final CsrfValidationFilter csrfValidationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtCookieAuthenticationFilter jwtCookieFilter,
                          @org.springframework.context.annotation.Lazy OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
                          OriginValidationFilter originValidationFilter,
                          CsrfValidationFilter csrfValidationFilter,
                          @Value("${app.security.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
        this.jwtCookieFilter = jwtCookieFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.originValidationFilter = originValidationFilter;
        this.csrfValidationFilter = csrfValidationFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/error",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler)
            )
            .addFilterBefore(jwtCookieFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(originValidationFilter, JwtCookieAuthenticationFilter.class)
            .addFilterAfter(csrfValidationFilter, OriginValidationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-Token"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
