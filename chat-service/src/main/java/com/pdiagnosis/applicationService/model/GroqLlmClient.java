package com.pdiagnosis.applicationService.model;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdiagnosis.applicationService.repositories.MedicalCardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GroqLlmClient implements LlmInterface {

    @Value("${llm.groq.api-url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${llm.groq.api-key}")
    private String apiKey;

    private final MedicalCardRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqLlmClient(MedicalCardRepository repository) {
        this.repository = repository;
    }

    @Override
    public String sendChatCompletion(String modelName, List<Map<String, String>> messages, Map<String, Object> options) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        if (options != null) {
            body.putAll(options);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            var choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        throw new RuntimeException("Failed to get LLM response: " + response.getStatusCode());
    }

    @Override
    public void updateMedicalCard(String modelName, List<Map<String, String>> messages, Map<String, Object> options, int userId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", messages);
        if (options != null) {
            body.putAll(options);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, Map.class);
        var choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            editCard((String) message.get("content"), userId);
        }
    }
    private static final Pattern PROBABILITY_PATTERN = Pattern.compile("\\d+");
    private List<String> parseIllness(String illness) {
        if (illness == null || illness.isBlank()) {
            return Collections.emptyList();
        }

        String[] parts = illness.split(":", 2);
        if (parts.length != 2) {
            return Collections.emptyList();
        }

        String disease = parts[0].trim();

        String[] probAndDesc = parts[1].split(";", 2);
        if (probAndDesc.length != 2) {
            return Collections.emptyList();
        }

        String probabilityStr = probAndDesc[0].trim();
        String description   = probAndDesc[1].trim();

        Matcher m = PROBABILITY_PATTERN.matcher(probabilityStr);
        String probability = m.find() ? m.group() : "0";

        return List.of(disease, probability, description);
    }

    private void editCard(String llm, int userId) {
        var diagnosis = repository.findByUserId(userId);
        String[] illnesses = llm.split("Ответ:")[1].split("\\n");
        Map<String, List<String>> illnessMap = new HashMap<>();
        for (var illness : illnesses) {
            if(!illness.strip().isEmpty()){
                List<String> illnessList = parseIllness(illness);
                illnessMap.put(illnessList.getFirst(), illnessList);
            }
        }
        if (!diagnosis.isEmpty()) {
            for (var diagnose : diagnosis) {
                var update = illnessMap.get(diagnose.getDiseas());
                diagnose.setDiseas(update.getFirst());
                diagnose.setDescription(update.get(2));
                diagnose.setPossibility(Integer.parseInt(update.get(1)));
                repository.save(diagnose);
            }
        }else{
            for(var illness : illnessMap.keySet()) {
                MedicalCard newCard = new MedicalCard();
                newCard.setUserId(userId);
                newCard.setDiseas(illnessMap.get(illness).getFirst());
                newCard.setDescription(illnessMap.get(illness).get(2));
                newCard.setPossibility(Integer.parseInt(illnessMap.get(illness).get(1)));
                repository.save(newCard);
            }
        }
    }
}
