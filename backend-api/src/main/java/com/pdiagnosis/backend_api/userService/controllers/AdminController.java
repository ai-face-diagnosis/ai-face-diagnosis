package com.pdiagnosis.backend_api.userService.controllers;

import com.pdiagnosis.backend_api.userService.model.users.Admin;
import com.pdiagnosis.backend_api.userService.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Создать нового администратора
     */
    @PostMapping
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        return ResponseEntity.ok(adminService.saveAdmin(admin));
    }

    /**
     * Получить всех администраторов
     */
    @GetMapping
    public ResponseEntity<List<Admin>> getAllAdmins() {
        return ResponseEntity.ok(adminService.findAll());
    }

    /**
     * Найти администратора по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        Optional<Admin> admin = adminService.findById(id);
        return admin.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Найти администратора по email
     */
    @GetMapping("/search")
    public ResponseEntity<Admin> getAdminByEmail(@RequestParam String email) {
        Optional<Admin> admin = adminService.findByEmail(email);
        return admin.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновить администратора
     */
    @PutMapping("/{id}")
    public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @RequestBody Admin adminDetails) {
        Optional<Admin> existingAdmin = adminService.findById(id);
        if (existingAdmin.isPresent()) {
            Admin adminToUpdate = existingAdmin.get();
            adminToUpdate.setUsername(adminDetails.getUsername());
            adminToUpdate.setEmail(adminDetails.getEmail());
            adminToUpdate.setPassword(adminDetails.getPassword());
            adminToUpdate.setAdminLevel(adminDetails.getAdminLevel());
            adminToUpdate.setPermissions(adminDetails.getPermissions());
            return ResponseEntity.ok(adminService.updateAdmin(adminToUpdate));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Удалить администратора
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
