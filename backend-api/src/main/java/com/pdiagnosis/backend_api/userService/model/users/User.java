package com.pdiagnosis.backend_api.userService.model.users;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class User extends BaseUser {

    @Column
    private Integer age;

    @Column(length = 10)
    private String gender; // "MALE", "FEMALE", "OTHER"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String diagnosisHistory; // JSON или текст с прошлой медицинской информацией

    public boolean canSubmitDiagnosisRequest() {
        return isActive();
    }
}
