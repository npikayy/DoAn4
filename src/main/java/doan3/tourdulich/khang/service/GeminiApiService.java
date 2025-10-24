package doan3.tourdulich.khang.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class GeminiApiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final WebClient webClient;

    public GeminiApiService() {
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public String generateResponse(String prompt) {
        try {
            GeminiRequest request = new GeminiRequest();
            request.setContents(List.of(new Content(List.of(new Part(prompt)))));

            GenerationConfig config = new GenerationConfig();
            config.setTemperature(0.7);
            config.setMaxOutputTokens(2048);
            request.setGenerationConfig(config);

            String fullUrl = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    model, apiKey
            );

            log.debug("Calling Gemini API: {}", fullUrl);

            String response = webClient.post()
                    .uri(fullUrl)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return extractContent(response);

        } catch (Exception e) {
            log.error("Gemini API error: ", e);
            return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại.";
        }
    }

    private String extractContent(String response) {
        log.info("Raw Gemini API response: {}", response); // Add this line for debugging
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            GeminiResponse geminiResponse = mapper.readValue(response, GeminiResponse.class);

            if (geminiResponse.getCandidates() != null &&
                    !geminiResponse.getCandidates().isEmpty()) {

                Candidate candidate = geminiResponse.getCandidates().get(0);

                if (candidate.getFinishReason() != null && candidate.getFinishReason().equals("MAX_TOKENS")) {
                    return "Model đã đạt giới hạn token và không thể hoàn thành câu trả lời. Vui lòng thử lại với câu hỏi ngắn hơn hoặc tăng giới hạn token.";
                }

                if (candidate.getContent() != null &&
                        candidate.getContent().getParts() != null &&
                        !candidate.getContent().getParts().isEmpty() &&
                        candidate.getContent().getParts().get(0).getText() != null) {

                    return candidate.getContent().getParts().get(0).getText();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response: ", e);
        }

        return "Không thể trích xuất nội dung phản hồi.";
    }

    @Data
    public static class GeminiRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;
    }


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
        public Content() {}
        public Content(List<Part> parts) { this.parts = parts; }
    }

    @Data
@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
        public Part() {}
        public Part(String text) { this.text = text; }
    }

    @Data
    public static class GenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
        @JsonProperty("finishReason")
        private String finishReason;
    }
}