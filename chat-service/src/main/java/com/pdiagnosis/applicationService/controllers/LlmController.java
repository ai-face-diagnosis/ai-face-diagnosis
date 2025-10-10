package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.GroqLlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final GroqLlmClient llmClient;

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
    public ResponseEntity<?> analyzeImage(
            @RequestParam("file") MultipartFile file

    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            // 👇 Пока просто шлём модельке текст с упоминанием, что было передано фото
            String modelName = "meta-llama/llama-4-maverick-17b-128e-instruct";
            String fullPrompt = "Analyze giving image and give an short answer whether " +
                    " it's face or not.Answer ''" + "\n(Изображение передано отдельно: " + file.getOriginalFilename() + ")";

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", fullPrompt)
            );

            String response = llmClient.sendChatCompletion(modelName, messages, Map.of());

            // ⚠️ Здесь можно будет позже добавить анализ изображения через vision-модель
            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "response", response
            ));

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
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            // Формируем запрос для whisper-1
            String modelName = "whisper-large-v3";
            String prompt = "Транскрибируй аудиофайл: " + file.getOriginalFilename();

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", prompt)
            );

            String response = llmClient.sendChatCompletion(modelName, messages, Map.of());

            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "transcription", response
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
