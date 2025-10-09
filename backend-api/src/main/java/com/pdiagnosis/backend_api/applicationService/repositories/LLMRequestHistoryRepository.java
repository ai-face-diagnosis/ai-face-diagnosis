package com.pdiagnosis.backend_api.applicationService.repositories;

import com.pdiagnosis.backend_api.applicationService.model.Chat;
import com.pdiagnosis.backend_api.applicationService.model.LLMRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LLMRequestHistoryRepository extends JpaRepository<LLMRequestHistory, Long> {

    // Получить все записи истории по конкретному чату
    List<LLMRequestHistory> findByChat(Chat chat);
}
