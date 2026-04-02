package com.orthocarechain.config;

import com.orthocarechain.entity.User;
import com.orthocarechain.enums.Role;
import com.orthocarechain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            // Create default admin if no admin exists
            boolean adminExists = !userRepository.findActiveUsersByRole(Role.ROLE_ADMIN).isEmpty();
            if (!adminExists) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@orthocarechain.com")
                        .password(passwordEncoder.encode("Admin@12345"))
                        .role(Role.ROLE_ADMIN)
                        .firstName("System")
                        .lastName("Administrator")
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();

                userRepository.save(admin);
                log.info("======================================================");
                log.info("Default Admin account created:");
                log.info("  Username: admin");
                log.info("  Password: Admin@12345");
                log.info("  *** CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION ***");
                log.info("======================================================");
            } else {
                log.info("OrthoCareChain started. Admin account already exists.");
            }
        };
    }
}
