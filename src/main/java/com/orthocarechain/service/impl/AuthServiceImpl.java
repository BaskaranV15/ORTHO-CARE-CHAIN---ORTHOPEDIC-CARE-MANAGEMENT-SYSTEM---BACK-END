package com.orthocarechain.service.impl;

import com.orthocarechain.dto.request.LoginRequest;
import com.orthocarechain.dto.request.RegisterRequest;
import com.orthocarechain.dto.response.JwtResponse;
import com.orthocarechain.dto.response.UserResponse;
import com.orthocarechain.entity.User;
import com.orthocarechain.enums.Role;
import com.orthocarechain.exception.DuplicateResourceException;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.repository.UserRepository;
import com.orthocarechain.security.JwtUtils;
import com.orthocarechain.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration.ms}")
    private Long jwtExpirationMs;

    @Transactional
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Save refresh token to user
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("User logged in successfully: {}", userDetails.getUsername());

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Check uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use.");
        }

        // Prevent unauthorized admin self-registration
        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .isActive(true)
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role: {}", savedUser.getUsername(), savedUser.getRole());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public JwtResponse refreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token."));

        if (!jwtUtils.validateJwtToken(refreshToken)) {
            user.setRefreshToken(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Refresh token is expired. Please log in again.");
        }

        String newAccessToken = jwtUtils.generateJwtTokenFromUsername(user.getUsername());

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setRefreshToken(null);
        userRepository.save(user);
        SecurityContextHolder.clearContext();
        log.info("User logged out: {}", user.getUsername());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
