package com.pdiagnosis.backend_api.authenticationService.repositories;

import com.pdiagnosis.backend_api.authenticationService.model.AuthenticationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthenticationUserRepository extends JpaRepository<AuthenticationUser, Long> {

    Optional<AuthenticationUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
