package com.pdiagnosis.backend_api.userService.services;

import com.pdiagnosis.backend_api.userService.model.settings.UserNeuronNetworkSettings;
import com.pdiagnosis.backend_api.userService.model.users.User;
import com.pdiagnosis.backend_api.userService.repositories.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    /**
     * Сохраняет новые настройки для пользователя.
     */
    public UserNeuronNetworkSettings saveSettings(UserNeuronNetworkSettings settings) {
        return userSettingsRepository.save(settings);
    }

    /**
     * Получает настройки пользователя по объекту User.
     */
    public Optional<UserNeuronNetworkSettings> getSettingsByUser(User user) {
        return userSettingsRepository.findByUser(user);
    }

    /**
     * Обновляет существующие настройки пользователя.
     * Если настроек нет — создает новые.
     */
    public UserNeuronNetworkSettings updateSettings(User user, UserNeuronNetworkSettings newSettings) {
        Optional<UserNeuronNetworkSettings> existing = userSettingsRepository.findByUser(user);
        if (existing.isPresent()) {
            UserNeuronNetworkSettings settings = existing.get();
            settings.setMaxIterations(newSettings.getMaxIterations());
            settings.setThreshold(newSettings.getThreshold());
            settings.setEnableAugmentation(newSettings.isEnableAugmentation());
            settings.setPreferredModel(newSettings.getPreferredModel());
            settings.setAdditionalParams(newSettings.getAdditionalParams());
            return userSettingsRepository.save(settings);
        } else {
            newSettings.setUser(user);
            return userSettingsRepository.save(newSettings);
        }
    }

    /**
     * Удаляет настройки пользователя.
     */
    public void deleteSettings(User user) {
        userSettingsRepository.findByUser(user).ifPresent(userSettingsRepository::delete);
    }
}
