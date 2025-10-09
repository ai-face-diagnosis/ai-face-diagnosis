package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.UserSettings;
import com.pdiagnosis.applicationService.services.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users/{userId}/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * Получить настройки пользователя
     */
    @GetMapping
    public ResponseEntity<?> getUserSettings(@PathVariable Long userId) {
        Optional<UserSettings> settingsOpt = userSettingsService.findByUserId(userId);
        return settingsOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Settings not found"));
    }

    /**
     * Создать или обновить настройки пользователя
     */
    @PostMapping
    public ResponseEntity<?> saveUserSettings(@PathVariable Long userId,
                                              @RequestBody UserSettings newSettings) {
        newSettings.setUserId(userId);

        Optional<UserSettings> existingOpt = userSettingsService.findByUserId(userId);
        if (existingOpt.isPresent()) {
            UserSettings existing = existingOpt.get();
            existing.setMaxIterations(newSettings.getMaxIterations());
            existing.setThreshold(newSettings.getThreshold());
            existing.setEnableAugmentation(newSettings.isEnableAugmentation());
            existing.setPreferredModel(newSettings.getPreferredModel());
            existing.setAdditionalParams(newSettings.getAdditionalParams());
            return ResponseEntity.ok(userSettingsService.save(existing));
        } else {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userSettingsService.save(newSettings));
        }
    }

    /**
     * Удалить настройки пользователя
     */
    @DeleteMapping
    public ResponseEntity<?> deleteUserSettings(@PathVariable Long userId) {
        Optional<UserSettings> settingsOpt = userSettingsService.findByUserId(userId);
        if (settingsOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Settings not found");
        }

        userSettingsService.delete(settingsOpt.get().getId());
        return ResponseEntity.noContent().build();
    }
}
