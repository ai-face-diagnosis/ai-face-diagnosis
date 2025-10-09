package com.pdiagnosis.authenticationService.controllers;

import com.pdiagnosis.authenticationService.config.JwtTokenGenerator;
import com.pdiagnosis.authenticationService.model.AuthenticationUser;
import com.pdiagnosis.authenticationService.services.AuthenticationUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationUserService authenticationUserService;
    private final JwtTokenGenerator jwtTokenGenerator;

    private final PasswordEncoder passwordEncoder;

    // DTO для запроса логина
    public static class LoginRequest {
        public String username;
        public String password;
    }

    // DTO для ответа с токеном
    public static class LoginResponse {
        public String token;

        public LoginResponse(String token) {
            this.token = token;

        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Находим AuthenticationUser
        Optional<AuthenticationUser> authUserOpt =
                authenticationUserService.findByUsername(loginRequest.username);

        if (authUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        AuthenticationUser authUser = authUserOpt.get();

        // Проверка пароля
        if (!passwordEncoder.matches(loginRequest.password, authUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        // Генерация JWT
        String token = jwtTokenGenerator.generateToken(authUser.getUsername());

        return ResponseEntity.ok(new LoginResponse(token));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthenticationUser newUser) {
        if (authenticationUserService.existsByUsername(newUser.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        }
        AuthenticationUser created = authenticationUserService.createUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
