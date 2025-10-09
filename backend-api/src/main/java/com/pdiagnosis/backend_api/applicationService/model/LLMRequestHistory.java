package com.pdiagnosis.backend_api.applicationService.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_request_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime requestTime = LocalDateTime.now();

    @Column(nullable = false, length = 64)
    private String imageHash;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String llmResponse;

    @Column(length = 255)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @PrePersist
    protected void onCreate() {
        requestTime = LocalDateTime.now();
    }
}
