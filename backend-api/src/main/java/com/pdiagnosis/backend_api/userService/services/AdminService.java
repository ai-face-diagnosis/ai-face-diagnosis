package com.pdiagnosis.backend_api.userService.services;

import com.pdiagnosis.backend_api.userService.model.users.Admin;
import com.pdiagnosis.backend_api.userService.repositories.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    /**
     * Сохраняет нового администратора.
     */
    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    /**
     * Находит администратора по email.
     */
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    /**
     * Находит администратора по ID.
     */
    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    /**
     * Возвращает всех администраторов.
     */
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    /**
     * Обновляет данные администратора.
     */
    public Admin updateAdmin(Admin admin) {
        if (admin.getId() == null) {
            throw new IllegalArgumentException("Cannot update admin without ID");
        }
        return adminRepository.save(admin);
    }

    /**
     * Удаляет администратора по ID.
     */
    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }
}
