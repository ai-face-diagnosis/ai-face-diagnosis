package com.pdiagnosis.backend_api.userService.controllers;

import com.pdiagnosis.backend_api.userService.model.history.LLMRequestHistory;
import com.pdiagnosis.backend_api.userService.model.users.User;
import com.pdiagnosis.backend_api.userService.services.LLMRequestHistoryService;
import com.pdiagnosis.backend_api.userService.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/llm-history")
@RequiredArgsConstructor
public class LLMRequestHistoryController {

    private final LLMRequestHistoryService historyService;
    private final UserService userService;

    /**
     * Создание новой записи о запросе к LLM
     */
    @PostMapping
    public ResponseEntity<LLMRequestHistory> createHistory(@RequestBody LLMRequestHistory history) {
        return ResponseEntity.ok(historyService.saveRequestHistory(history));
    }

    /**
     * Получить всю историю конкретного пользователя
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<List<LLMRequestHistory>> getHistoryByUser(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        return user.map(value -> ResponseEntity.ok(historyService.getHistoryByUser(value)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Проверка, есть ли уже запись с таким хэшем изображения
     */
    @GetMapping("/check")
    public ResponseEntity<LLMRequestHistory> checkDuplicate(@RequestParam String username,
                                                            @RequestParam String imageHash) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            Optional<LLMRequestHistory> existing = historyService.findByImageHashAndUser(imageHash, user.get());
            return existing.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить все записи (для администратора)
     */
    @GetMapping("/all")
    public ResponseEntity<List<LLMRequestHistory>> getAllHistory() {
        return ResponseEntity.ok(historyService.getAllHistory());
    }

    /**
     * Обновить существующую запись
     */
    @PutMapping("/{id}")
    public ResponseEntity<LLMRequestHistory> updateHistory(@PathVariable Long id,
                                                           @RequestBody LLMRequestHistory updatedHistory) {
        Optional<LLMRequestHistory> existing = historyService.getAllHistory().stream()
                .filter(h -> h.getId().equals(id))
                .findFirst();
        if (existing.isPresent()) {
            updatedHistory.setId(id);
            return ResponseEntity.ok(historyService.updateHistory(updatedHistory));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Удалить запись по ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        historyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
