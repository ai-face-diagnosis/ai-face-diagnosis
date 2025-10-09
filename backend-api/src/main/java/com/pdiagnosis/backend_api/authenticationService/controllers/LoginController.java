package com.pdiagnosis.backend_api.authenticationService.controllers;

import com.pdiagnosis.backend_api.authenticationService.config.JwtTokenGenerator;

import com.pdiagnosis.backend_api.userService.model.users.User;
import com.pdiagnosis.backend_api.userService.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGenerator jwtTokenProvider;

    // DTO для запроса логина
    public static class LoginRequest {
        public String username;
        public String password;
    }

    // DTO для ответа с токеном
    public static class LoginResponse {
        public String token;
        public LoginResponse(String token) { this.token = token; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Ищем пользователя по username
        Optional<User> userOpt = userService.findByUsername(loginRequest.username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        User user = userOpt.get();

        // Проверяем пароль
        if (!passwordEncoder.matches(loginRequest.password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }

        // Генерируем JWT
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(new LoginResponse(token));
    }
}
