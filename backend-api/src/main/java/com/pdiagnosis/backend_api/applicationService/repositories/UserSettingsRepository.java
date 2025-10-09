package com.pdiagnosis.backend_api.applicationService.repositories;

import com.pdiagnosis.backend_api.applicationService.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUserId(Long userId);
}
