package com.pdiagnosis.backend_api.userService.model.settings;

import com.pdiagnosis.backend_api.userService.model.users.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserNeuronNetworkSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Пример настроек нейросети
    @Column(nullable = false)
    private int maxIterations = 100; // количество итераций/эпох

    @Column(nullable = false)
    private double threshold = 0.5; // порог срабатывания модели (например, для классификации)

    @Column(nullable = false)
    private boolean enableAugmentation = true; // использовать аугментацию изображений

    @Column(length = 100)
    private String preferredModel = "default-model"; // название модели для пользователя

    @Column(length = 255)
    private String additionalParams; // любые дополнительные параметры JSON
}
