package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.GroqLlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final GroqLlmClient llmClient;
    @Value("${llm.groq.api-key}")
    String key;
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    public LlmController(GroqLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    // 🧠 Промпт от разработчиков (системная роль)
    private static final String SYSTEM_PROMPT = """
            Ты — интеллектуальный ассистент медицинского приложения.
            Отвечай кратко, точно и профессионально.
            Если вопрос не относится к медицине или диагностике — отвечай нейтрально или откажись.
            Никогда не выдавай непроверенные диагнозы.Отвечай на языке запроса пользователя
            """;

    /**
     * Простой текстовый запрос.
     * POST /api/llm/chat
     * {
     *   "model": "llama-3.3-70b-versatile",
     *   "prompt": "Объясни простыми словами, что такое диабет 2 типа"
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<?> sendChat(@RequestBody Map<String, String> requestBody) {
        try {
            String modelName = requestBody.getOrDefault("model", "llama-3.3-70b-versatile");
            String prompt = requestBody.get("prompt");

            if (prompt == null || prompt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field"));
            }

            // Формируем контекст с системным промптом
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", prompt)
            );

            String response = llmClient.sendChatCompletion(modelName, messages, Map.of());
            return ResponseEntity.ok(Map.of("response", response));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📸 Метод для отправки фото и текста.
     * Принимает multipart/form-data с файлом и текстом.
     *
     * Пример запроса:
     * POST /api/llm/analyze
     * Content-Type: multipart/form-data
     *
     * Параметры:
     * - file: (изображение)
     * - prompt: "Опиши состояние кожи на фото"
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            // Конвертируем изображение в Base64
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String imageData = "data:image/jpeg;base64," + base64Image;

            // Формируем JSON для Groq API
            Map<String, Object> requestBody = Map.of(
                    "model", "meta-llama/llama-4-maverick-17b-128e-instruct",
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "text", "text", "Есть ли человеческое лицо на изображении"),
                                            Map.of("type", "image_url", "image_url", Map.of("url", imageData))
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(key); // Используем сам ключ, без getenv

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> groqResponse = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    request,
                    Map.class
            );

            return ResponseEntity.status(groqResponse.getStatusCode()).body(groqResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🎙 Метод для отправки аудиофайла и получения транскрипции.
     * Принимает multipart/form-data с аудиофайлом.
     *
     * Пример запроса:
     * POST /api/llm/transcribe
     * Content-Type: multipart/form-data
     *
     * Параметры:
     * - file: (аудиофайл)
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribeAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            String url = "https://api.groq.com/openai/v1/audio/transcriptions";
            String modelName = "whisper-large-v3-turbo";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(key);  // твой ключ

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            body.add("model", modelName);
            body.add("temperature", "0");
            body.add("response_format", "verbose_json");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "transcription", response.getBody()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }


}
