package com.pdiagnosis.backend_api.userService.repositories;

import com.pdiagnosis.backend_api.userService.model.settings.UserNeuronNetworkSettings;
import com.pdiagnosis.backend_api.userService.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserNeuronNetworkSettings, Long> {
    Optional<UserNeuronNetworkSettings> findByUser(User user);
}
