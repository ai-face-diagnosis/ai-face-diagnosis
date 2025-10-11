package com.pdiagnosis.orchestration.controllers;

import com.pdiagnosis.AuthenticationUser;
import com.pdiagnosis.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final RestTemplate restTemplate;

    // DTO для запроса логина
    public static class LoginRequest {
        public String username;
        public String password;

        // Геттеры и сеттеры
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private Long userId;

        public LoginResponse() {} // нужен для Jackson

        public LoginResponse(String token, Long userId) {
            this.token = token;
            this.userId = userId;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }


    // DTO для запроса регистрации
    public static class RegisterRequest {
        public String username;
        public String password;
        public String email;
//        public String fullName;
//        public Integer age;
//        public String gender; // "MALE", "FEMALE", "OTHER"

        // Геттеры и сеттеры
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
//        public String getFullName() { return fullName; }
//        public void setFullName(String fullName) { this.fullName = fullName; }
//        public Integer getAge() { return age; }
//        public void setAge(Integer age) { this.age = age; }
//        public String getGender() { return gender; }
//        public void setGender(String gender) { this.gender = gender; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Пересылка запроса логина в authentication-service
        String authServiceUrl = "http://localhost:8080/api/auth/login";
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                authServiceUrl, loginRequest, LoginResponse.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(response.getBody());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Неверное имя пользователя или пароль");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // Создаем AuthenticationUser для authentication-service
        AuthenticationUser authUser = new AuthenticationUser();
        authUser.setUsername(registerRequest.getUsername());
        authUser.setPassword(registerRequest.getPassword());

        // Регистрация в authentication-service
        String authServiceUrl = "http://localhost:8080/api/auth/register";
        ResponseEntity<AuthenticationUser> authResponse = restTemplate.postForEntity(
                authServiceUrl, authUser, AuthenticationUser.class);

        if (authResponse.getStatusCode() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Имя пользователя уже существует");
        }

        // Если пользователь успешно создан в authentication-service, регистрируем в user-service
        if (authResponse.getStatusCode() == HttpStatus.OK) {
            // Создаем User для user-service
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
//            user.setFullName(registerRequest.getFullName());
//            user.setAge(registerRequest.getAge());
//            user.setGender(registerRequest.getGender());
            user.setActive(true); // Устанавливаем по умолчанию, как в модели User

            String userServiceUrl = "http://localhost:8081/api/users/register";
            ResponseEntity<User> userResponse = restTemplate.postForEntity(
                    userServiceUrl, user, User.class);

            if (userResponse.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.CREATED).body(userResponse.getBody());
            } else {
                // Откат регистрации в authentication-service
                // В продакшене рекомендуется использовать распределенные транзакции
                String deleteAuthUserUrl = "http://localhost:8080/api/auth/delete/" + registerRequest.getUsername();
                restTemplate.delete(deleteAuthUserUrl);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Не удалось зарегистрировать пользователя в user-service");
            }
        }

        return ResponseEntity.status(authResponse.getStatusCode()).body(authResponse.getBody());
    }
}