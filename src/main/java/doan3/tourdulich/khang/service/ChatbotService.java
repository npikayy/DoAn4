package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.ChatRequest;
import doan3.tourdulich.khang.dto.ChatResponse;
import doan3.tourdulich.khang.entity.chatMessage;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.entity.KhuyenMai; // Import KhuyenMai entity
import doan3.tourdulich.khang.repository.chatMessageRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.KhuyenMaiRepository; // Import KhuyenMaiRepository
import doan3.tourdulich.khang.service.GeminiApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatbotService {

    @Autowired
    private GeminiApiService geminiApiService;

    @Autowired
    private tourRepo tourRepo; // Direct dependency for keyword RAG

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository; // New dependency for promotions

    @Autowired
    private chatMessageRepo chatMessageRepo;

    @Value("${ai.chatbot.enabled}")
    private boolean enabled;

    @Value("${tour.detail.base-url}")
    private String tourDetailBaseUrl;

    @Value("${promotion.detail.base-url}")
    private String promotionDetailBaseUrl; // New property for promotion links

    public ChatResponse chat(ChatRequest request) {
        if (!enabled) {
            return ChatResponse.builder()
                    .success(false)
                    .error("Chatbot is currently disabled")
                    .build();
        }

        try {
            String sessionId = request.getSessionId() != null ?
                    request.getSessionId() : UUID.randomUUID().toString();

            // Step 1: RAG - Search for relevant tour information directly from DB using keyword search
            List<tours> relevantTours = searchToursDirectly(request.getMessage());
            log.info("Found {} relevant tours from direct DB search", relevantTours.size());

            // Step 1.5: RAG - Search for relevant promotions directly from DB using keyword search
            List<KhuyenMai> relevantPromotions = searchPromotionsDirectly(request.getMessage());
            log.info("Found {} relevant promotions from direct DB search", relevantPromotions.size());

            // Step 2: Build context
            String context = buildContext(relevantTours, relevantPromotions);

            // Step 3: Generate response using Gemini
            String aiResponse = generateResponse(request.getMessage(), context, sessionId);

            // Step 4: Save conversation
            saveConversation(sessionId, request.getMessage(), aiResponse);

            return ChatResponse.builder()
                    .response(aiResponse)
                    .sessionId(sessionId)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error in chat processing: ", e);
            return ChatResponse.builder()
                    .success(false)
                    .error("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.")
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    private List<tours> searchToursDirectly(String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<String> keywords = new ArrayList<>();

        // Basic tokenization and stop word removal for Vietnamese
        List<String> stopWords = List.of("tìm", "tour", "cho", "tôi", "muốn", "có", "không", "gì", "ở", "đâu", "về", "các", "những", "nào", "hấp dẫn", "đẹp", "tốt", "giá", "rẻ", "đắt", "bao nhiêu", "thông tin", "chi tiết", "giúp", "tư vấn", "xin", "chào", "bạn", "nhé", "hiện", "rất", "điểm đến", "tham khảo", "tại đây", "hành trình", "khám phá", "trọn vẹn", "miền", "sông nước", "đi qua", "nhiều", "tỉnh thành", "khác", "theo", "ngày", "khởi hành", "có", "khuyến mãi", "dưới", "trên", "triệu", "vnd");

        String[] words = lowerCaseQuery.split("\s+");
        for (String word : words) {
            if (!stopWords.contains(word)) { // Removed word.length() > 2 condition
                keywords.add(word);
            }
        }

        if (keywords.isEmpty()) {
            // If no significant keywords, try to use the original query as a single keyword
            keywords.add(lowerCaseQuery);
        }

        List<tours> filteredTours = tourRepo.findAll();

        // Filter by keywords (name, region, location, description)
        if (!keywords.isEmpty()) {
            filteredTours = filteredTours.stream()
                    .filter(tour -> keywords.stream().anyMatch(keyword ->
                        tour.getTour_name() != null && tour.getTour_name().toLowerCase().contains(keyword) ||
                        tour.getTour_region() != null && tour.getTour_region().toLowerCase().contains(keyword) ||
                        tour.getTour_end_location() != null && tour.getTour_end_location().toLowerCase().contains(keyword) ||
                        tour.getTour_description() != null && tour.getTour_description().toLowerCase().contains(keyword)
                    ))
                    .collect(Collectors.toList());
        }

        // Filter by promotion
        if (lowerCaseQuery.contains("khuyến mãi") || lowerCaseQuery.contains("ưu đãi") || lowerCaseQuery.contains("giảm giá")) {
            filteredTours = filteredTours.stream()
                    .filter(tour -> tour.getSpecial_offer() != null && !tour.getSpecial_offer().isEmpty())
                    .collect(Collectors.toList());
        }

        return filteredTours.stream().limit(5).collect(Collectors.toList());
    }

    private List<KhuyenMai> searchPromotionsDirectly(String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<String> keywords = new ArrayList<>();

        // Use the same stop words as for tours, or a refined list for promotions
        List<String> stopWords = List.of("tìm", "khuyến mãi", "ưu đãi", "giảm giá", "cho", "tôi", "muốn", "có", "không", "gì", "ở", "đâu", "về", "các", "những", "nào", "hấp dẫn", "đẹp", "tốt", "giá", "rẻ", "đắt", "bao nhiêu", "thông tin", "chi tiết", "giúp", "tư vấn", "xin", "chào", "bạn", "nhé", "hiện", "rất", "điểm đến", "tham khảo", "tại đây", "hành trình", "khám phá", "trọn vẹn", "miền", "sông nước", "đi qua", "nhiều", "tỉnh thành", "khác", "du lịch", "chuyến đi", "ngắn ngày", "khám phá", "thủ phủ", "và", "nhiều", "tỉnh thành", "khác", "theo", "ngày", "khởi hành", "dưới", "trên", "triệu", "vnd");

        String[] words = lowerCaseQuery.split("\s+");
        for (String word : words) {
            if (!stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        if (keywords.isEmpty()) {
            keywords.add(lowerCaseQuery);
        }

        List<KhuyenMai> filteredPromotions = khuyenMaiRepository.findAll();

        if (!keywords.isEmpty()) {
            filteredPromotions = filteredPromotions.stream()
                    .filter(promo -> keywords.stream().anyMatch(keyword ->
                        promo.getTenKhuyenMai() != null && promo.getTenKhuyenMai().toLowerCase().contains(keyword) ||
                        promo.getMoTa() != null && promo.getMoTa().toLowerCase().contains(keyword)
                    ))
                    .collect(Collectors.toList());
        }

        return filteredPromotions.stream().limit(3).collect(Collectors.toList()); // Limit promotions
    }

    private String buildContext(List<tours> tours, List<KhuyenMai> promotions) {
        StringBuilder context = new StringBuilder();

        if (!tours.isEmpty()) {
            context.append("Thông tin về các tour du lịch liên quan:\n\n");
            for (int i = 0; i < tours.size(); i++) {
                tours tour = tours.get(i);
                context.append("Tour ").append(i + 1).append(":\n");
                context.append("Tên: ").append(tour.getTour_name() != null ? tour.getTour_name() : "Không rõ").append(". ");
                context.append("Khu vực: ").append(tour.getTour_region() != null ? tour.getTour_region() : "Không rõ").append(". ");
                context.append("Địa điểm: ").append(tour.getTour_end_location() != null ? tour.getTour_end_location() : "Không rõ").append(". ");
                context.append("Thời gian: ").append(tour.getTour_duration() != null ? tour.getTour_duration() : "Không rõ").append(". ");
//                context.append("Ngày khởi hành: ").append(tour.getTour_start_date() != null ? tour.getTour_start_date() : "Không rõ").append(". ");
                context.append("Giá người lớn: ").append(tour.getTour_adult_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                context.append("Giá trẻ em: ").append(tour.getTour_child_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                context.append("Giá em bé: ").append(tour.getTour_infant_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                if (tour.getSpecial_offer() != null && !tour.getSpecial_offer().isEmpty()) {
                    context.append("Khuyến mãi: ").append(tour.getSpecial_offer()).append(". ");
                }
                context.append("Mô tả: ").append(tour.getTour_description() != null ? tour.getTour_description() : "Không có mô tả").append(". ");
                context.append("Link chi tiết: ").append(tourDetailBaseUrl).append(tour.getTour_id() != null ? tour.getTour_id() : "").append("\n\n");
            }
        }

        if (!promotions.isEmpty()) {
            context.append("Thông tin về các chương trình khuyến mãi liên quan:\n\n");
            for (int i = 0; i < promotions.size(); i++) {
                KhuyenMai promo = promotions.get(i);
                context.append("Khuyến mãi ").append(i + 1).append(":\n");
                context.append("Tên: ").append(promo.getTenKhuyenMai() != null ? promo.getTenKhuyenMai() : "Không rõ").append(". ");
                context.append("Mô tả: ").append(promo.getMoTa() != null ? promo.getMoTa() : "Không có mô tả").append(". ");
                context.append("Phần trăm giảm giá: ").append(promo.getPhanTramGiamGia()).append("%. ");
                context.append("Ngày bắt đầu: ").append(promo.getNgayBatDau() != null ? promo.getNgayBatDau().toString() : "Không rõ").append(". ");
                context.append("Ngày kết thúc: ").append(promo.getNgayKetThuc() != null ? promo.getNgayKetThuc().toString() : "Không rõ").append(". ");
                context.append("Link chi tiết: ").append(promotionDetailBaseUrl).append(promo.getId()).append("\n\n");
            }
        }

        return context.toString();
    }

    private String generateResponse(String userMessage, String context, String sessionId) {
        try {
            // Get conversation history
            List<chatMessage> history = chatMessageRepo.findRecentMessagesBySession(sessionId)
                    .stream()
                    .limit(2)
                    .collect(Collectors.toList());

            // Build prompt
            String prompt = buildPrompt(userMessage, context, history);
            log.info("Prompt: {}", prompt);
            // Call Gemini API
            return geminiApiService.generateResponse(prompt);

        } catch (Exception e) {
            log.error("Error generating response: ", e);
            return generateFallbackResponse(userMessage, context);
        }
    }

    private String buildPrompt(String userMessage, String context, List<chatMessage> history) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là trợ lý tư vấn du lịch thông minh của website tour du lịch Việt Nam.\n");
        prompt.append("Nhiệm vụ: tư vấn và giới thiệu các tour và chương trình khuyến mãi phù hợp. **Hãy sử dụng thông tin và các link chi tiết được cung cấp dưới đây để trả lời một cách chính xác và hữu ích. Tuyệt đối không tạo ra các link ví dụ hoặc link không có trong thông tin được cung cấp.** Trả lời ngắn gọn, thân thiện.\n\n");

        if (!context.isEmpty()) {
            prompt.append("THÔNG TIN LIÊN QUAN:\n").append(context).append("\n");
        }

        if (!history.isEmpty()) {
            prompt.append("Lịch sử hội thoại:\n");
            for (chatMessage msg : history) {
                prompt.append("User: ").append(msg.getUser_message()).append("\n");
                prompt.append("Bot: ").append(msg.getBot_response()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Câu hỏi hiện tại: ").append(userMessage).append("\n\n");
        prompt.append("Hãy trả lời bằng tiếng Việt, ngắn gọn và hữu ích:");

        return prompt.toString();
    }

    private String generateFallbackResponse(String userMessage, String context) {
        String msg = userMessage.toLowerCase();
        StringBuilder response = new StringBuilder();

        if (msg.contains("xin chào") || msg.contains("chào") || msg.contains("hello")) {
            response.append("Xin chào! Tôi là trợ lý tư vấn du lịch. ");
        }

        if (!context.isEmpty()) {
            response.append("Tôi đã tìm thấy một số thông tin phù hợp:\n\n");
            response.append(context);
            response.append("\nBạn có muốn biết thêm chi tiết không?");
        } else {
            response.append("Tôi có thể giúp bạn tìm tour du lịch hoặc khuyến mãi. ");
            response.append("Bạn muốn tìm gì? (VD: Tour Đà Nẵng, Khuyến mãi hè...)");
        }

        return response.toString();
    }

    private void saveConversation(String sessionId, String userMessage, String botResponse) {
        try {
            chatMessage message = chatMessage.builder()
                    .sessionId(sessionId)
                    .user_message(userMessage)
                    .bot_response(botResponse)
                    .build();
            chatMessageRepo.save(message);
        } catch (Exception e) {
            log.error("Error saving conversation: ", e);
        }
    }

    public List<chatMessage> getChatHistory(String sessionId) {
        return chatMessageRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}
