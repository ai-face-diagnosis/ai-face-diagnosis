package com.pdiagnosis.backend_api.userService.model.history;

import com.pdiagnosis.backend_api.userService.model.users.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_request_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LLMRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime requestTime = LocalDateTime.now();

    @Column(nullable = false, length = 64)
    private String imageHash; // хэш изображения (например, SHA-256)

    @Column(nullable = true, length = 255)
    private String imageUrl; // ссылка на изображение

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String llmResponse; // текст, который вернула LLM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // пользователь, который сделал запрос

    @PrePersist
    protected void onCreate() {
        requestTime = LocalDateTime.now();
    }
}
