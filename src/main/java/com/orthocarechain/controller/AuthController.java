package com.orthocarechain.controller;

import com.orthocarechain.dto.request.LoginRequest;
import com.orthocarechain.dto.request.RegisterRequest;
import com.orthocarechain.dto.response.ApiResponse;
import com.orthocarechain.dto.response.JwtResponse;
import com.orthocarechain.dto.response.UserResponse;
import com.orthocarechain.security.UserDetailsImpl;
import com.orthocarechain.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    /**
     * POST /api/auth/login
     * Public — No authentication required
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        JwtResponse jwtResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
    }

    /**
     * POST /api/auth/register
     * Public — No authentication required
     * Cannot register as ADMIN
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful. Welcome to OrthoCareChain!", user));
    }

    /**
     * POST /api/auth/refresh-token
     * Public — Uses refresh token to get new access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refresh token is required", 400));
        }
        JwtResponse jwtResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", jwtResponse));
    }

    /**
     * POST /api/auth/logout
     * Requires: Any authenticated user
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        authService.logout(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * GET /api/auth/me
     * Requires: Any authenticated user
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Map<String, Object> userInfo = Map.of(
                "id", currentUser.getId(),
                "username", currentUser.getUsername(),
                "email", currentUser.getEmail(),
                "role", currentUser.getRole()
        );
        return ResponseEntity.ok(ApiResponse.success("Current user info", userInfo));
    }
}
