package com.pdiagnosis.orchestration.controllers;

import com.pdiagnosis.orchestration.MultipartInputStreamFileResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.face-detection.url:http://localhost:8083/api/llm/analyze}")
    private String faceDetectionUrl; // эндпоинт face-detection сервиса (заглушка)

    @Value("${services.llm-fastapi.url:http://localhost:8000/analyze}")
    private String llmFastApiUrl; // эндпоинт LLM-сервиса (FastAPI, заглушка)

    @Value("${services.llm-spring.url:http://localhost:8083/api/llm/chat}")
    private String llmSpringUrl; // эндпоинт Spring LLM сервиса

    // --- Временное хранилище чатов (заглушка, потом заменим на БД)
    private final Map<Long, ChatSession> chats = new HashMap<>();
    private long chatCounter = 1L;

    /**
     * 📸 Создание нового чата.
     * Принимает изображение (и опционально текст),
     * проверяет наличие лица, передаёт в FastAPI, потом в Spring LLM.
     */
    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createNewChat(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            // 🔹 1. Проверка изображения на наличие лица
            boolean faceDetected = checkFaceOnImage(file);
            if (!faceDetected) {
                return ResponseEntity.badRequest().body(Map.of("error", "No face detected on the image"));
            }

            // 🔹 2. Отправка изображения в LLM FastAPI (анализ)
            String llmResult = sendToFastApiLLM(file, prompt);

            // 🔹 3. Отправка результата анализа и запроса пользователя в Spring LLM
            String finalResponse = sendToSpringLLM(llmResult, prompt);

            // 🔹 4. Создание чата
            long chatId = chatCounter++;
            ChatSession session = new ChatSession(chatId, "Chat " + chatId, LocalDateTime.now());
            session.addMessage("user", prompt != null ? prompt : "(image only)");
            session.addMessage("llm", finalResponse);
            chats.put(chatId, session);

            return ResponseEntity.ok(Map.of(
                    "chatId", chatId,
                    "response", finalResponse
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📄 Получение списка чатов (заголовок + дата)
     */
    @GetMapping("/list")
    public ResponseEntity<?> getChatList() {
        List<Map<String, Object>> list = chats.values().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("title", c.getTitle());
                    map.put("createdAt", c.getCreatedAt());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(list);
    }


    /**
     * 💬 Получение истории конкретного чата
     */
    @GetMapping("/{chatId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long chatId) {
        ChatSession session = chats.get(chatId);
        if (session == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Chat not found"));

        return ResponseEntity.ok(session);
    }

    // ====================================================================
    // 🔧 Вспомогательные методы (в реальной версии — вынести в сервисы)
    // ====================================================================

    private boolean checkFaceOnImage(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // заглушка: всегда true, но можно раскомментировать для реального вызова

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(faceDetectionUrl, request, Map.class);
            return response.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(response.getBody().get("faceDetected"));

        } catch (Exception e) {
            return false;
        }
    }

    private String sendToFastApiLLM(MultipartFile file, String prompt) {
        // Заглушка — пока возвращаем фейковый результат анализа
        return "На фото обнаружено лицо. Кожа имеет нормальный цвет без явных признаков воспалений.";
    }

    private String sendToSpringLLM(String llmResult, String userPrompt) {
        try {
            Map<String, String> requestBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "prompt", llmResult + "\n\nВопрос пользователя: " + (userPrompt != null ? userPrompt : "")
            );
            ResponseEntity<Map> response = restTemplate.postForEntity(llmSpringUrl, requestBody, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception ignored) {}
        return "Ошибка при получении ответа от LLM.";
    }

    // ====================================================================
    // 🔹 Вспомогательный класс для хранения чата
    // ====================================================================

    static class ChatSession {
        private final Long id;
        private final String title;
        private final LocalDateTime createdAt;
        private final List<Map<String, String>> messages = new ArrayList<>();

        public ChatSession(Long id, String title, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
        }

        public void addMessage(String role, String content) {
            messages.add(Map.of("role", role, "content", content));
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public List<Map<String, String>> getMessages() { return messages; }
    }
}
