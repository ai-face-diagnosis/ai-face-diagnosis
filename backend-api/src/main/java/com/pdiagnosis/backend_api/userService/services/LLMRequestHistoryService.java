package com.pdiagnosis.backend_api.userService.services;

import com.pdiagnosis.backend_api.userService.model.history.LLMRequestHistory;
import com.pdiagnosis.backend_api.userService.model.users.User;
import com.pdiagnosis.backend_api.userService.repositories.LLMRequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LLMRequestHistoryService {

    private final LLMRequestHistoryRepository llmRequestHistoryRepository;

    /**
     * Сохраняет новую запись о запросе к LLM.
     */
    public LLMRequestHistory saveRequestHistory(LLMRequestHistory history) {
        return llmRequestHistoryRepository.save(history);
    }

    /**
     * Возвращает список всех запросов пользователя.
     */
    public List<LLMRequestHistory> getHistoryByUser(User user) {
        return llmRequestHistoryRepository.findByUser(user);
    }

    /**
     * Проверяет, есть ли уже запись с таким хэшем изображения для пользователя.
     */
    public Optional<LLMRequestHistory> findByImageHashAndUser(String imageHash, User user) {
        return llmRequestHistoryRepository.findByImageHashAndUser(imageHash, user);
    }

    /**
     * Возвращает все записи (для администратора или аналитики).
     */
    public List<LLMRequestHistory> getAllHistory() {
        return llmRequestHistoryRepository.findAll();
    }

    /**
     * Удаляет запись по ID.
     */
    public void deleteById(Long id) {
        llmRequestHistoryRepository.deleteById(id);
    }

    /**
     * Обновляет существующую запись (например, если нужно исправить ответ LLM).
     */
    public LLMRequestHistory updateHistory(LLMRequestHistory history) {
        if (history.getId() == null) {
            throw new IllegalArgumentException("Cannot update history without ID");
        }
        return llmRequestHistoryRepository.save(history);
    }
}
