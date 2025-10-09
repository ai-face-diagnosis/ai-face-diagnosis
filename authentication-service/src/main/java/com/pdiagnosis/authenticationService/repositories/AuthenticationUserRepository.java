package com.pdiagnosis.authenticationService.repositories;

import com.pdiagnosis.authenticationService.model.AuthenticationUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthenticationUserRepository extends JpaRepository<AuthenticationUser, Long> {

    Optional<AuthenticationUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
