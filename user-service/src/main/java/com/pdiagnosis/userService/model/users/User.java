package com.pdiagnosis.userService.model.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
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
