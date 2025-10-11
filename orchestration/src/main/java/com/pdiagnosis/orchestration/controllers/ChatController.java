package com.pdiagnosis.orchestration.controllers;

import com.pdiagnosis.Chat;
import com.pdiagnosis.orchestration.MultipartInputStreamFileResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
            @RequestParam("chatId") Long chatId

    ) {
        return sendQuestion(file, prompt, chatId);
    }

    @PostMapping(value = "/new/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> messageWithVoice(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("voice") MultipartFile voiceFile,
            @RequestParam("chatId") Long chatId
    ) {
        try {
            // 1️⃣ Конвертируем голос в текст
            String voiceText = convertVoiceToText(voiceFile);

            // 2️⃣ Используем метод sendQuestion с изображением и распознанным текстом
            return sendQuestion(imageFile, voiceText, chatId);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getChatList(@RequestParam("userId") Long userId) {
        try {
            // Формируем URL к chat-service
            String url = chatGetter + "?userId=" + userId;

            // Типизированный запрос — RestTemplate должен знать, что мы получаем List<Chat>
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // Проверяем успешность
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No chats found for userId " + userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch chats: " + e.getMessage()));
        }
    }


    @GetMapping("/{chatId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long chatId) {
        try {
            // 1️⃣ Формируем URL для запроса истории
            String url = UriComponentsBuilder
                    .fromHttpUrl(chatHistoryGetter)
                    .queryParam("chatId", chatId)
                    .toUriString();

            // 2️⃣ Запрос к сервису истории
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No history found for chatId " + chatId));
            }

            List<Map<String, Object>> rawHistory = response.getBody();

            // 3️⃣ Преобразуем записи истории, подгружая изображения в Base64
            List<Map<String, Object>> historyWithImages = rawHistory.stream().map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", entry.get("id"));
                map.put("chatId", entry.get("chat") != null ? ((Map<?, ?>) entry.get("chat")).get("id") : null);
                map.put("requestTime", entry.get("requestTime"));
                map.put("llmResponse", entry.get("llmResponse"));

                String imageUrl = (String) entry.get("imageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    try {
                        Path filePath = Paths.get("src/main/resources/photo", Paths.get(imageUrl).getFileName().toString());
                        if (Files.exists(filePath)) {
                            byte[] bytes = Files.readAllBytes(filePath);
                            String base64 = Base64.getEncoder().encodeToString(bytes);
                            map.put("imageBase64", base64);
                            map.put("imageUrl", "/api/chats/history/image?imageUrl=" + imageUrl);
                        }
                    } catch (Exception e) {
                        map.put("imageBase64", null);
                    }
                } else {
                    map.put("imageBase64", null);
                }

                return map;
            }).toList();

            return ResponseEntity.ok(historyWithImages);

        } catch (Exception e) {
            System.err.println("Error fetching chat history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch chat history: " + e.getMessage()));
        }
    }





    @GetMapping("/create")
    @PostMapping("/create")
    public ResponseEntity<?> createChat(
            @RequestParam("userId") Long userId,
            @RequestParam("title") String title
    ) {
        try {
            Chat newChat = new Chat();
            newChat.setMessages(new ArrayList<>());
            newChat.setUserId(userId);
            newChat.setTitle(title);

            // Просто используем URL сервиса без {userId}
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chatCreation,  // URL без {userId}
                    newChat,       // тело запроса
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


    private ResponseEntity<?> sendQuestion(MultipartFile imageFile, String prompt, Long chatId) {
        try {
            if (imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing image file"));
            }
            boolean faceDetected = true;
            // 🔹 Проверка изображения на наличие лица
            if (imageFile != null) {
                faceDetected = checkFaceOnImage(imageFile);
            }
            if (!faceDetected) {
                return ResponseEntity.badRequest().body(Map.of("error", "No face detected on the image"));
            }
            String llmResult = "";
            // 🔹 Отправка изображения в LLM FastAPI (анализ)
            if (imageFile != null)
                 llmResult = sendToFastApiLLM(imageFile);

            // 🔹 Отправка результата анализа и запроса пользователя в Spring LLM
            String finalResponse = sendToSpringLLM(llmResult, prompt);

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
        return true;
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
//            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
//            ResponseEntity<Map> response = restTemplate.postForEntity(faceDetectionUrl, request, Map.class);
//            return response.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(response.getBody().get("faceDetected"));
//
//        } catch (Exception e) {
//            return false;
//        }
    }

    private String sendToFastApiLLM(MultipartFile file) {
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
//            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
//            ResponseEntity<Map> response = restTemplate.postForEntity(llmFastApiUrl, request, Map.class);
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                return (String) response.getBody().get("response");
//            }
//            return "Ошибка при анализе изображения.";
//        } catch (Exception e) {
//            return "Ошибка при анализе изображения: " + e.getMessage();
//        }
        return "Типо работает";
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
            String imageUrl = null;

            // 📸 Если файл передан — сохраняем его на диск
            if (imageFile != null && !imageFile.isEmpty()) {
                // Папка для хранения изображений
                Path uploadDir = Paths.get("src/main/resources/photo");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Уникальное имя файла (чтобы избежать коллизий)
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);

                // Сохраняем файл
                Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Относительный путь, который можно потом отдать клиенту
                imageUrl = "src/main/resources/photo/" + fileName;
            }

            // 🧾 Формируем объект истории
            Map<String, Object> history = new HashMap<>();
            history.put("chatId", chatId);
            history.put("prompt", prompt != null ? prompt : "");
            history.put("llmResponse", response);
            history.put("imageUrl", imageUrl);

            // Отправляем запись в сервис чатов
            restTemplate.postForEntity(
                    chatHistoryGetter.replace("{chatId}", chatId.toString()).replace("/{id}", "/create"),
                    history,
                    Map.class
            );

        } catch (Exception e) {
            System.err.println("❌ Error saving chat history: " + e.getMessage());
        }
    }

}