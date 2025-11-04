package com.pdiagnosis.applicationService.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@ApplicationScope
@Service
public class LlmService {
    @Value("${upload.api.key}")
    private  String keyUpload;
    RestTemplate restTemplate = new RestTemplate();
    public Map<String,Object> formPostForTranscribeAudio(String key, MultipartFile file) throws IOException {
        String url = "https://api.groq.com/openai/v1/audio/transcriptions";
        String modelName = "whisper-large-v3-turbo";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(key);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
        body.add("model", modelName);
        body.add("temperature", "0");
        body.add("response_format", "verbose_json");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String,Object> result=new HashMap<>();
        result.put("url", url);
        result.put("request", request);
        return result;
    }
    public String uploadImage(MultipartFile file) throws IOException, InterruptedException {
        String realContentType = detectRealContentType(file.getBytes());
        if ("image/avif".equals(realContentType)) {
            file = convertAvifToJpeg(file);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("key", keyUpload);

        String contentType = "image/jpeg";
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<Resource> imagePart = new HttpEntity<>(file.getResource(), partHeaders);
        body.add("image", imagePart);

        body.add("name", file.getOriginalFilename());
        body.add("expiration", "60");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.imgbb.com/1/upload", request, Map.class
        );

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return data.get("url").toString();
    }

    private String detectRealContentType(byte[] bytes) {
        if (bytes.length < 12) return "application/octet-stream";

        // AVIF: bytes[4..7] = 'ftyp', bytes[8..11] = 'avif'
        if (bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p' &&
                bytes[8] == 'a' && bytes[9] == 'v' && bytes[10] == 'i' && bytes[11] == 'f') {
            return "image/avif";
        }

        // JPEG: FF D8
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }

        // PNG: 89 50 4E 47
        if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }

        return "application/octet-stream";
    }

    private MultipartFile convertAvifToJpeg(MultipartFile original) throws IOException, InterruptedException {
        File tempInput = File.createTempFile("avif_", ".avif");
        Path currentDir = Path.of("").toAbsolutePath();
        File tempOutput = new File(currentDir+"/chat-service/src/main/resources/converted"+original.getOriginalFilename());
        tempOutput.createNewFile();
        original.transferTo(tempInput);

        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", tempInput.getAbsolutePath(),
                "-y", tempOutput.getAbsolutePath());
        Process process = pb.start();
        process.waitFor();

        MultipartFile converted = new MockMultipartFile("file", tempOutput.getName(),
                "image/jpeg", Files.readAllBytes(tempOutput.toPath()));
        File file=new File(original.getOriginalFilename());
        file.delete();
        tempInput.delete();
        original=converted;
        return original;
    }
    public HttpEntity<Map<String, Object>> formPostForAnalyzeImage(String key, String imageUrl) throws IOException {


        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "meta-llama/llama-4-maverick-17b-128e-instruct");
        requestBody.put("max_completion_tokens", 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "Есть ли человеческое лицо на изображении? Если есть, то опиши состояние кожи и глаз. Формат ответа: Да/Нет. Далее подробное описание.");
        content.add(textContent);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");

        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", imageUrl);
        imageContent.put("image_url", imageUrlMap);

        content.add(imageContent);

        message.put("content", content);
        messages.add(message);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(key);
        return new HttpEntity<>(requestBody, headers);
    }

}
