package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.repository.tourRepo;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VectorStoreService {

    @Autowired
    private tourRepo tourRepo;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${ai.chatbot.rag.top-k}")
    private int topK;

    @Value("${ai.chatbot.rag.similarity-threshold}")
    private float similarityThreshold;

    private List<DocumentEmbedding> documentStore;

    @Data
    @AllArgsConstructor
    public static class DocumentEmbedding {
        private String tourId;
        private String content;
        private float[] embedding;
        private float score;

        public DocumentEmbedding(String tourId, String content, float[] embedding) {
            this.tourId = tourId;
            this.content = content;
            this.embedding = embedding;
            this.score = 0.0f;
        }
    }

    @PostConstruct
    public void initializeVectorStore() {
        log.info("Initializing vector store with tour data...");
        documentStore = new ArrayList<>();

        try {
            List<tours> allTours = tourRepo.findAll();
            log.info("Found {} tours to index", allTours.size());

            for (tours tour : allTours) {
                String content = buildTourContent(tour);
                float[] embedding = embeddingService.generateEmbedding(content);
                documentStore.add(new DocumentEmbedding(tour.getTour_id(), content, embedding));
            }

            log.info("Vector store initialized with {} documents", documentStore.size());
        } catch (Exception e) {
            log.error("Error initializing vector store: {}", e.getMessage());
        }
    }

    private String buildTourContent(tours tour) {
        StringBuilder content = new StringBuilder();
        content.append("Tour: ").append(tour.getTour_name()).append(". ");
        content.append("Khu vực: ").append(tour.getTour_region()).append(". ");
        content.append("Địa điểm: ").append(tour.getTour_end_location()).append(". ");
        content.append("Khởi hành: ").append(tour.getTour_start_location()).append(". ");
        content.append("Kết thúc: ").append(tour.getTour_end_location()).append(". ");
        content.append("Thời gian: ").append(tour.getTour_duration()).append(". ");
        content.append("Giá: ").append(tour.getTour_adult_price()).append(" VNĐ. ");

        if (tour.getTour_discount() > 0) {
            content.append("Giảm giá: ").append(tour.getTour_discount()).append("%. ");
        }

        if (tour.getSpecial_offer() != null && !tour.getSpecial_offer().isEmpty()) {
            content.append("Ưu đãi đặc biệt: ").append(tour.getSpecial_offer()).append(". ");
        }

        if (tour.getTour_description() != null && !tour.getTour_description().isEmpty()) {
            content.append("Dịch vụ bao gồm: ").append(tour.getTour_description()).append(". ");
        }

        return content.toString();
    }

    public List<DocumentEmbedding> searchSimilarDocuments(String query) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        return documentStore.stream()
            .peek(doc -> {
                float similarity = embeddingService.cosineSimilarity(queryEmbedding, doc.getEmbedding());
                doc.setScore(similarity);
            })
            .filter(doc -> doc.getScore() >= similarityThreshold)
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    public void refreshVectorStore() {
        log.info("Refreshing vector store...");
        initializeVectorStore();
    }
}
