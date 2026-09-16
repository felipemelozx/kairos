package com.felipemelozx.kairos.api;

import com.felipemelozx.kairos.dto.request.LoginRequest;
import com.felipemelozx.kairos.dto.request.RegisterRequest;
import com.felipemelozx.kairos.dto.response.ApiResponse;
import com.felipemelozx.kairos.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Auth", description = "Authentication endpoints")
public interface AuthApi {

    @Operation(summary = "Register a new user", description = "Creates a new user account with email and password")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    ResponseEntity<ApiResponse<UserResponse>> register(
            RegisterRequest request,
            HttpServletResponse response);

    @Operation(summary = "Login with email and password", description = "Authenticates user with email and password")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    ResponseEntity<ApiResponse<UserResponse>> login(
            LoginRequest request,
            HttpServletResponse response);

    @Operation(summary = "Logout current user", description = "Revokes refresh tokens and clears authentication cookies")
    @SecurityRequirement(name = "cookieAuth")
    ResponseEntity<ApiResponse<Void>> logout(
            UserDetails principal,
            String refreshToken,
            HttpServletResponse response);

    @Operation(summary = "Refresh access token", description = "Issues a new access token using the refresh token cookie")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    ResponseEntity<ApiResponse<Void>> refresh(
            String refreshToken,
            HttpServletResponse response);

    @Operation(summary = "Get current authenticated user", description = "Returns the profile of the currently authenticated user")
    @SecurityRequirement(name = "cookieAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user profile"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    ResponseEntity<ApiResponse<UserResponse>> me(UserDetails principal);
}
