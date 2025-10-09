package com.pdiagnosis.applicationService.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Вместо связи с сущностью User

    private Integer maxIterations;
    private Double threshold;
    private boolean enableAugmentation;
    private String preferredModel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String additionalParams;
}
