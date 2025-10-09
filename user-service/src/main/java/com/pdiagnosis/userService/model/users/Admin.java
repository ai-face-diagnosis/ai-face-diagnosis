package com.pdiagnosis.userService.model.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Admin extends BaseUser {

    @Column(nullable = false, length = 30)
    private String adminLevel; // GLOBAL, DEPARTMENTAL и т.п.

    @Lob
    @Column(columnDefinition = "TEXT")
    private String permissions; // JSON-строка или список прав (можно вынести в отдельную таблицу позже)

    public boolean canManageUsers() {
        return true;
    }
}
