package com.pdiagnosis.applicationService.services;

import com.pdiagnosis.applicationService.model.UserSettings;
import com.pdiagnosis.applicationService.repositories.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public Optional<UserSettings> findById(Long id) {
        return userSettingsRepository.findById(id);
    }

    public Optional<UserSettings> findByUserId(Long userId) {
        return userSettingsRepository.findByUserId(userId);
    }

    public UserSettings save(UserSettings settings) {
        return userSettingsRepository.save(settings);
    }

    public void delete(Long id) {
        userSettingsRepository.deleteById(id);
    }
}
