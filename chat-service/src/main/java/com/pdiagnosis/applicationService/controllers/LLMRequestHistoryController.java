package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.Chat;
import com.pdiagnosis.applicationService.model.LLMRequestHistory;
import com.pdiagnosis.applicationService.services.ChatService;
import com.pdiagnosis.applicationService.services.LLMRequestHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chats/{chatId}/history")
@RequiredArgsConstructor
public class LLMRequestHistoryController {

    private final LLMRequestHistoryService llmRequestHistoryService;
    private final ChatService chatService;

    /**
     * Получить все записи истории для конкретного чата
     */
    @GetMapping("/getAll")
    public ResponseEntity<List<LLMRequestHistory>> getAllHistory(@PathVariable Long chatId) {
        return chatService.findById(chatId)
                .map(chat -> ResponseEntity.ok(llmRequestHistoryService.findByChat(chat)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получить запись по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getHistoryById(@PathVariable Long id) {
        Optional<LLMRequestHistory> historyOpt = llmRequestHistoryService.findById(id);
        if (historyOpt.isPresent()) {
            return ResponseEntity.ok(historyOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("History record not found");
        }
    }

    /**
     * Создать новую запись истории
     */
    @PostMapping("/create")
    public ResponseEntity<?> createHistory(@PathVariable Long chatId,
                                           @RequestBody LLMRequestHistory history) {
        Optional<Chat> chatOpt = chatService.findById(chatId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Chat not found");
        }

        history.setChat(chatOpt.get());
        LLMRequestHistory created = llmRequestHistoryService.create(history);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Обновить запись
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateHistory(@PathVariable Long id,
                                           @RequestBody LLMRequestHistory updated) {
        Optional<LLMRequestHistory> existingOpt = llmRequestHistoryService.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("History record not found");
        }

        LLMRequestHistory existing = existingOpt.get();
        existing.setLlmResponse(updated.getLlmResponse());
        existing.setImageUrl(updated.getImageUrl());
        existing.setImageHash(updated.getImageHash());

        LLMRequestHistory saved = llmRequestHistoryService.update(existing);
        return ResponseEntity.ok(saved);
    }

    /**
     * Удалить запись
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteHistory(@PathVariable Long id) {
        Optional<LLMRequestHistory> existingOpt = llmRequestHistoryService.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("History record not found");
        }

        llmRequestHistoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
