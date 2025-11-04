package com.pdiagnosis.applicationService.controllers;

import com.pdiagnosis.applicationService.model.LlmInterface;
import com.pdiagnosis.applicationService.model.MedicalCard;
import com.pdiagnosis.applicationService.repositories.MedicalCardRepository;
import com.pdiagnosis.applicationService.services.LlmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/llm")
public class LlmController {
    private LlmService llmService;
    private final LlmInterface llmClient;
    private final MedicalCardRepository repository;
    @Value("${llm.groq.api-key}")
    String key;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    public LlmController(LlmInterface llmClient, MedicalCardRepository repository,LlmService llmService) {
        this.llmClient = llmClient;
        this.repository = repository;
        this.llmService = llmService;
    }

    private static final String SYSTEM_PROMPT = """
            Ты — интеллектуальный ассистент медицинского приложения.
            Отвечай кратко, точно и профессионально.
            Если вопрос не относится к медицине или диагностике — отвечай нейтрально или откажись.
            Никогда не выдавай непроверенные диагнозы.Отвечай на языке запроса пользователя.Будет прикреплена медицинская
            карта пользователя с историей болезней, которые ты ранее диагностировал.Также будет прикреплено описание состояние 
            кожи,глаз,эмоционального состояния и т.д.
            """;
    private static final String PROMPT_TO_EDIT_CARD = """
    ТЫ — СИСТЕМА ОБНОВЛЕНИЯ МЕДИЦИНСКОЙ КАРТЫ. НЕ РАЗМЫШЛЯЙ. НЕ АНАЛИЗИРУЙ. НЕ ОБЪЯСНЯЙ.
    
    ЗАПРЕЩЕНО:
    - Писать <think>, </think>
    - Делать анализ, выводы, рассуждения
    - Добавлять "я думаю", "на основании", "возможно"
    - Использовать \n\n, лишние пробелы, переносы
    - Изменять формат
    - Добавлять что-либо до или после "Ответ:"
    
    РАЗРЕШЕНО ТОЛЬКО:
    СТРОГО ОДНА СТРОКА НА КАЖДОЕ ЗАБОЛЕВАНИЕ:
    Заболевание: вероятность;Описание.\n
    
    ПРАВИЛА:
    1. Вероятность — ЧИСЛО 0–100, ТОЛЬКО если УКАЗАНО ЯВНО в отчёте
    2. Если вероятность НЕ указана — ПРОПУСТИ запись
    3. Описание — КРАТКОЕ, ТОЛЬКО симптомы из отчёта
    4. Если заболевание НЕ упомянуто — НЕ ДОБАВЛЯЙ
    5. Если карта пустая — создавай ТОЛЬКО с явной вероятностью
    6. НИКАКИХ 100%, если не сказано "определённо"
    
    ФОРМАТ — СТРОГО:
    Ответ:
    Заболевание: 70;Жирная кожа, блеск.\n
    Заболевание: 95;Сухость, жажда.\n
    
    НАЧИНАЙ СРАЗУ С "Ответ:"
    ЕСЛИ НАРУШИШЬ — ТЫ БУДЕШЬ УДАЛЁН ИЗ СИСТЕМЫ.
    """;

    /**
     * Простой текстовый запрос.
     * POST /api/llm/chat
     * {
     * "model": "llama-3.3-70b-versatile",
     * "prompt": "Объясни простыми словами, что такое диабет 2 типа"
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<?> sendChat(@RequestBody Map<String, String> requestBody) {
        try {
            String modelName = requestBody.getOrDefault("model", "llama-3.3-70b-versatile");
            List<MedicalCard> cards=new ArrayList<>();
            Integer userId = Integer.parseInt(requestBody.getOrDefault("userId", "-1"));
            if(userId==-1){
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }else{

               cards= repository.findByUserId(userId);
            }
            StringBuilder cardsString = new StringBuilder();
            cardsString.append("Медицинская карта пользователя ").append(userId);
            for(var card:cards){
                cardsString.append(card.getFullDescription());
            }
            String prompt = requestBody.get("prompt");

            if (prompt == null || prompt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field"));
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", cardsString.toString()),
                    Map.of("role", "user", "content", prompt)
            );

            String response = llmClient.sendChatCompletion(modelName, messages, Map.of());
            modelName="qwen/qwen3-32b";
            messages = List.of(
                    Map.of("role", "system", "content", PROMPT_TO_EDIT_CARD),
                    Map.of("role", "user", "content", cardsString.toString()),
                    Map.of("role", "user", "content",response)
                    );
            llmClient.updateMedicalCard(modelName,messages,Map.of("temperature", 0.3),userId);
            return ResponseEntity.ok(Map.of("response", response));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Метод для отправки фото и текста.
     * Принимает multipart/form-data с файлом и текстом.
     * <p>
     * Пример запроса:
     * POST /api/llm/analyze
     * Content-Type: multipart/form-data
     * <p>
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
            String imageUrl = llmService.uploadImage(file);
            ResponseEntity<Map> groqResponse = restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    llmService.formPostForAnalyzeImage(key, imageUrl),
                    Map.class
            );
            Map<String, Object> body = groqResponse.getBody();
            String firstWord = Optional.ofNullable(body)
                    .map(b -> (List<?>) b.get("choices"))
                    .filter(list -> !list.isEmpty())
                    .map(list -> (Map<String, Object>) list.get(0))
                    .map(choice -> (Map<String, Object>) choice.get("message"))
                    .map(msg -> (String) msg.get("content"))
                    .map(str -> str.trim().split("\\.", 2)[0])
                    .orElse("");
            body.put("faceDetected", "Да".equalsIgnoreCase(firstWord));
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🎙 Метод для отправки аудиофайла и получения транскрипции.
     * Принимает multipart/form-data с аудиофайлом.
     * <p>
     * Пример запроса:
     * POST /api/llm/transcribe
     * Content-Type: multipart/form-data
     * <p>
     * Параметры:
     * - file: (аудиофайл)
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribeAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'file'"));
            }

            var map = llmService.formPostForTranscribeAudio(key, file);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(map.get("url").toString(), map.get("request"), Map.class);

            return ResponseEntity.ok(Map.of(
                    "fileName", file.getOriginalFilename(),
                    "transcription", response.getBody()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}