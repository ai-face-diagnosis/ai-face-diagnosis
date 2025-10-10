package com.pdiagnosis.orchestration.controllers;

import com.pdiagnosis.Chat;
import com.pdiagnosis.orchestration.MultipartInputStreamFileResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.face-detection.url}")
    private String faceDetectionUrl;

    @Value("${services.llm-fastapi.url}")
    private String llmFastApiUrl;

    @Value("${services.llm.spring.chat.url}")
    private String llmSpringUrl;

    @Value("${services.llm.spring.transcribe.url}")
    private String voiceRecognition;

    @Value("${services.chat.creation.url}")
    private String chatCreation;

    @Value("${services.chat.getter.url}")
    private String chatGetter;

    @Value("${services.chat.history.url}")
    private String chatHistoryGetter;

    @PostMapping(value = "/new/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> messageWithImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam("userId") Long userId
    ) {
        return sendQuestion(file, prompt, userId);
    }

    @PostMapping(value = "/new/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> messageWithVoice(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("voice") MultipartFile voiceFile,
            @RequestParam("userId") Long userId
    ) {
        try {
            // 1️⃣ Конвертируем голос в текст
            String voiceText = convertVoiceToText(voiceFile);

            // 2️⃣ Используем метод sendQuestion с изображением и распознанным текстом
            return sendQuestion(imageFile, voiceText, userId);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getChatList(@RequestParam("userId") Long userId) {
        try {
            // Запрос к сервису для получения списка чатов пользователя
            String url = chatGetter.replace("{userId}", userId.toString());
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok(response.getBody());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No chats found for user"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long chatId) {
        try {
            // Запрос к сервису для получения истории чата
            String url = chatHistoryGetter.replace("{chatId}", chatId.toString()).replace("{id}", "");
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok(response.getBody());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Chat history not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/create")
    public ResponseEntity<?> createChat(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title
    ) {
        try {
            Chat newChat = new Chat();
            newChat.setMessages(new ArrayList<>());
            newChat.setUserId(userId);
            newChat.setTitle(title);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chatCreation.replace("{userId}", userId.toString()),
                    newChat,
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Long chatId = Long.valueOf(response.getBody().get("id").toString());
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("chatId", chatId));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create chat"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating chat: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> sendQuestion(MultipartFile imageFile, String prompt, Long userId) {
        try {
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing image file"));
            }

            // 🔹 Проверка изображения на наличие лица
            boolean faceDetected = checkFaceOnImage(imageFile);
            if (!faceDetected) {
                return ResponseEntity.badRequest().body(Map.of("error", "No face detected on the image"));
            }

            // 🔹 Отправка изображения в LLM FastAPI (анализ)
            String llmResult = sendToFastApiLLM(imageFile);

            // 🔹 Отправка результата анализа и запроса пользователя в Spring LLM
            String finalResponse = sendToSpringLLM(llmResult, prompt);

            // 🔹 Создание нового чата через GET-метод
            String chatTitle = prompt != null ? prompt.substring(0, Math.min(prompt.length(), 50)) : "Image-based chat";
            ResponseEntity<?> createChatResponse = createChat(userId, chatTitle);
            if (createChatResponse.getStatusCode() != HttpStatus.CREATED || createChatResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create chat"));
            }
            Long chatId = (Long) ((Map<String, Object>) createChatResponse.getBody()).get("chatId");

            // 🔹 Сохранение истории
            saveChatHistory(chatId, prompt, finalResponse, imageFile);

            return ResponseEntity.ok(Map.of(
                    "chatId", chatId,
                    "response", finalResponse
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean checkFaceOnImage(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(faceDetectionUrl, request, Map.class);
            return response.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(response.getBody().get("faceDetected"));

        } catch (Exception e) {
            return false;
        }
    }

    private String sendToFastApiLLM(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(llmFastApiUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
            return "Ошибка при анализе изображения.";
        } catch (Exception e) {
            return "Ошибка при анализе изображения: " + e.getMessage();
        }
    }

    private String sendToSpringLLM(String llmResult, String userPrompt) {
        try {
            Map<String, String> requestBody = Map.of(
                    "model", "openai/gpt-oss-120b",
                    "prompt", llmResult + "\n\nВопрос пользователя: " + (userPrompt != null ? userPrompt : "")
            );
            ResponseEntity<Map> response = restTemplate.postForEntity(llmSpringUrl, requestBody, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
            return "Ошибка при получении ответа от LLM.";
        } catch (Exception e) {
            return "Ошибка при получении ответа от LLM: " + e.getMessage();
        }
    }

    private String convertVoiceToText(MultipartFile voiceFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(voiceFile.getInputStream(), voiceFile.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(voiceRecognition, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("transcription");
            }
            return "Ошибка при распознавании голоса.";
        } catch (Exception e) {
            return "Ошибка при распознавании голоса: " + e.getMessage();
        }
    }

    private void saveChatHistory(Long chatId, String prompt, String response, MultipartFile imageFile) {
        try {
            Map<String, Object> history = new HashMap<>();
            history.put("chatId", chatId);
            history.put("prompt", prompt != null ? prompt : "");
            history.put("llmResponse", response);
            if (imageFile != null) {
                history.put("imageUrl", imageFile.getOriginalFilename());
            }

            restTemplate.postForEntity(
                    chatHistoryGetter.replace("{chatId}", chatId.toString()).replace("/{id}", "/create"),
                    history,
                    Map.class
            );
        } catch (Exception e) {
            // Логирование ошибки, но не прерываем выполнение
            System.err.println("Error saving chat history: " + e.getMessage());
        }
    }
}