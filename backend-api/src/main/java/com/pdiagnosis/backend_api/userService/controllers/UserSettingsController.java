package com.pdiagnosis.backend_api.userService.controllers;

import com.pdiagnosis.backend_api.userService.model.settings.UserNeuronNetworkSettings;
import com.pdiagnosis.backend_api.userService.model.users.User;
import com.pdiagnosis.backend_api.userService.services.UserSettingsService;
import com.pdiagnosis.backend_api.userService.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user-settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService settingsService;
    private final UserService userService;

    /**
     * Получить настройки пользователя по имени
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserNeuronNetworkSettings> getSettings(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        return user.flatMap(settingsService::getSettingsByUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Создать или сохранить новые настройки пользователя
     */
    @PostMapping("/{username}")
    public ResponseEntity<UserNeuronNetworkSettings> createSettings(
            @PathVariable String username,
            @RequestBody UserNeuronNetworkSettings settings) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            settings.setUser(user.get());
            return ResponseEntity.ok(settingsService.saveSettings(settings));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Обновить существующие настройки пользователя
     */
    @PutMapping("/{username}")
    public ResponseEntity<UserNeuronNetworkSettings> updateSettings(
            @PathVariable String username,
            @RequestBody UserNeuronNetworkSettings newSettings) {
        Optional<User> user = userService.findByUsername(username);
        return user.map(value -> ResponseEntity.ok(settingsService.updateSettings(value, newSettings)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удалить настройки пользователя
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteSettings(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            settingsService.deleteSettings(user.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
