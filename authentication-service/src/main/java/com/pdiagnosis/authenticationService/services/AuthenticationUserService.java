package com.pdiagnosis.authenticationService.services;

import com.pdiagnosis.authenticationService.repositories.AuthenticationUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pdiagnosis.AuthenticationUser;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationUserService {

    private final AuthenticationUserRepository authenticationUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Создает нового пользователя для аутентификации
     */
    public AuthenticationUser createUser(AuthenticationUser user) {
        // Хэшируем пароль перед сохранением
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return authenticationUserRepository.save(user);
    }

    /**
     * Находит пользователя по username
     */
    public Optional<AuthenticationUser> findByUsername(String username) {
        return authenticationUserRepository.findByUsername(username);
    }

    /**
     * Проверяет, существует ли пользователь с таким username
     */
    public boolean existsByUsername(String username) {
        return authenticationUserRepository.existsByUsername(username);
    }

    /**
     * Обновление пароля
     */
    public AuthenticationUser updatePassword(AuthenticationUser user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return authenticationUserRepository.save(user);
    }

    /**
     * Деактивация пользователя
     */
    public void deactivateUser(AuthenticationUser user) {
        user.setActive(false);
        authenticationUserRepository.save(user);
    }
    public List<AuthenticationUser> getAllUsers() {
        return authenticationUserRepository.findAll();
    }

    public Optional<AuthenticationUser> findById(Long id) {
        return authenticationUserRepository.findById(id);
    }

    public AuthenticationUser updateUser(AuthenticationUser user) {
        return authenticationUserRepository.save(user);
    }

}
