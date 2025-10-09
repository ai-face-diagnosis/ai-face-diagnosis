package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.Chat;
import com.pdiagnosis.applicationService.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/{userId}/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Получить все чаты пользователя
     */
    @GetMapping
    public ResponseEntity<List<Chat>> getChatsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.findByUserId(userId));
    }

    /**
     * Получить чат по ID
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<?> getChatById(@PathVariable Long userId, @PathVariable Long chatId) {
        Optional<Chat> chatOpt = chatService.findByIdAndUserId(chatId, userId);
        return chatOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat not found"));
    }

    /**
     * Создать новый чат
     */
    @PostMapping
    public ResponseEntity<Chat> createChat(@PathVariable Long userId, @RequestBody Chat chat) {
        chat.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.create(chat));
    }

    /**
     * Обновить чат
     */
    @PutMapping("/{chatId}")
    public ResponseEntity<?> updateChat(@PathVariable Long userId,
                                        @PathVariable Long chatId,
                                        @RequestBody Chat updatedChat) {
        Optional<Chat> existingOpt = chatService.findByIdAndUserId(chatId, userId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat not found");
        }

        Chat existing = existingOpt.get();
        existing.setTitle(updatedChat.getTitle());
        return ResponseEntity.ok(chatService.update(existing));
    }

    /**
     * Удалить чат
     */
    @DeleteMapping("/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable Long userId, @PathVariable Long chatId) {
        Optional<Chat> chatOpt = chatService.findByIdAndUserId(chatId, userId);
        if (chatOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat not found");
        }

        chatService.delete(chatId);
        return ResponseEntity.noContent().build();
    }
}
