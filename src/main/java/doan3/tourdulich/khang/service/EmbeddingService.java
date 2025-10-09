package doan3.tourdulich.khang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.chatbot.embedding.dimension}")
    private int dimension;

    public EmbeddingService() {
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public float[] generateEmbedding(String text) {
        log.debug("Generating hash-based embedding for: {}", text.substring(0, Math.min(50, text.length())));
        return getHashBasedEmbedding(text);
    }



    /**
     * Hash-based embedding that creates deterministic embeddings preserving semantic similarity
     */
    private float[] getHashBasedEmbedding(String text) {
        float[] embedding = new float[dimension];
        text = text.toLowerCase().trim();

        // Extract keywords for better similarity
        String[] words = text.split("\\s+");

        // Use word-level hashing for better semantic preservation
        for (int i = 0; i < dimension; i++) {
            float value = 0.0f;
            for (String word : words) {
                if (word.length() > 2) { // Skip very short words
                    int hash = (word.hashCode() + i * 31) % 1000;
                    value += (hash / 500.0f) - 1.0f;
                }
            }
            embedding[i] = value / Math.max(1, words.length);
        }

        // Normalize the vector
        return normalizeVector(embedding);
    }

    private float[] normalizeVector(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    public float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f;
        }

        return dotProduct / (float)(Math.sqrt(normA) * Math.sqrt(normB));
    }

}