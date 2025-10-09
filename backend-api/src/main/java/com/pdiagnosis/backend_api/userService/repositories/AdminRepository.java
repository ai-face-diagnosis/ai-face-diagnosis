package com.pdiagnosis.backend_api.userService.repositories;

import com.pdiagnosis.backend_api.userService.model.users.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
}
