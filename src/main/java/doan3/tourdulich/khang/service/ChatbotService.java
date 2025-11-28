package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.ChatRequest;
import doan3.tourdulich.khang.dto.ChatResponse;
import doan3.tourdulich.khang.entity.chatMessage;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.entity.KhuyenMai; // Import KhuyenMai entity
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.*;
import doan3.tourdulich.khang.service.GeminiApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

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
    private tourPicRepo tourPicRepo;

    @Autowired
    private chatMessageRepo chatMessageRepo;

    @Autowired
    private RankRepository rankRepository; // New dependency for ranks
    @Autowired
    private tourBookingRepo tourBookingRepo; // New dependency for user tour data
    @Autowired
    private userRepo userRepo;
    @Value("${ai.chatbot.enabled}")
    private boolean enabled;

    @Value("${tour.detail.base-url}")
    private String tourDetailBaseUrl;

    @Value("${promotion.detail.base-url}")
    private String promotionDetailBaseUrl; // New property for promotion links


    @Transactional
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

            // Step 1.6: RAG - Search for relevant ranks directly from predefined data, potentially personalized
            List<Map<String, Object>> relevantRanks = searchPredefinedRanks(request.getMessage(), request.getUserId());
            log.info("Found {} relevant ranks from predefined data", relevantRanks.size());

            // Step 2: Build context
            String context = buildContext(relevantTours, relevantPromotions, relevantRanks);

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

    private List<String> extractKeywords(String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<String> keywords = new ArrayList<>();

        // Comprehensive stop words list for Vietnamese
        List<String> stopWords = List.of(
            "tìm", "tour", "cho", "tôi", "muốn", "có", "không", "gì", "ở", "đâu", "về", "các", "những", "nào", 
            "hấp dẫn", "đẹp", "tốt", "giá", "rẻ", "đắt", "bao nhiêu", "thông tin", "chi tiết", "giúp", "tư vấn", 
            "xin", "chào", "bạn", "nhé", "hiện", "rất", "điểm đến", "tham khảo", "tại đây", "hành trình", 
            "khám phá", "trọn vẹn", "miền", "sông nước", "đi qua", "nhiều", "tỉnh thành", "khác", "theo", 
            "ngày", "khởi hành", "có", "khuyến mãi", "ưu đãi", "giảm giá", "dưới", "trên", "triệu", "vnd",
            "du lịch", "chuyến đi", "ngắn ngày", "thủ phủ", "và", "đi"
        );

        String[] words = lowerCaseQuery.split("\\s+");
        for (String word : words) {
            if (!stopWords.contains(word.trim())) {
                keywords.add(word.trim());
            }
        }

        if (keywords.isEmpty()) {
            keywords.add(lowerCaseQuery);
        }
        System.out.println("Keywords: " + keywords);
        return keywords;
    }

    private List<tours> searchToursDirectly(String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<String> keywords = extractKeywords(query);

        List<tours> resultTours = new ArrayList<>();

        if (lowerCaseQuery.contains("khuyến mãi") || lowerCaseQuery.contains("ưu đãi") || lowerCaseQuery.contains("giảm giá")) {
            // If promotion is explicitly mentioned, search for tours with special offers
            for (String keyword : keywords) {
                resultTours.addAll(tourRepo.findByKeywordAndSpecialOffer("%" + keyword + "%"));
            }
        } else {
            // Otherwise, search for tours by general keywords
            for (String keyword : keywords) {
                resultTours.addAll(tourRepo.findByKeywordInNameRegionLocationDescription("%" + keyword + "%"));
            }
        }

        return resultTours.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private List<KhuyenMai> searchPromotionsDirectly(String query) {
        List<String> keywords = extractKeywords(query);
        List<KhuyenMai> resultPromotions = new ArrayList<>();

        for (String keyword : keywords) {
            resultPromotions.addAll(khuyenMaiRepository.findByKeywordInNameOrDescription("%" + keyword + "%"));
        }

        return resultPromotions.stream().distinct().limit(3).collect(Collectors.toList()); // Limit promotions
    }

    private List<Map<String, Object>> searchPredefinedRanks(String query, String userId) {
        String lowerCaseQuery = query.toLowerCase();
        List<Map<String, Object>> resultRanks = new ArrayList<>();

        // Define ranks directly within the method, including more detailed info
        List<Map<String, Object>> allRanks = List.of(
                Map.of("name", "Đồng", "tourThreshold", 1, "spendThreshold", 0L, "discount", 5, "benefits", "Ưu tiên hỗ trợ khách hàng, thông tin tour mới nhất, đổi 3 voucher/tháng", "description", "Hoàn thành ít nhất 1 tour để đạt hạng Đồng."),
                Map.of("name", "Bạc", "tourThreshold", 10, "spendThreshold", 35000000L, "discount", 5, "benefits", "Giảm giá 5% cho tất cả các tour, ưu tiên chọn chỗ ngồi, quà tặng sinh nhật, truy cập sớm tour mới, đổi 5 voucher/tháng, đổi 1 tour/tháng", "description", "Hoàn thành ít nhất 10 tour và tổng chi tiêu 35 triệu VNĐ để đạt hạng Bạc."),
                Map.of("name", "Vàng", "tourThreshold", 20, "spendThreshold", 80000000L, "discount", 15, "benefits", "Giảm giá 15% cho tất cả các tour, hỗ trợ khách hàng 24/7, nâng hạng phòng khách sạn miễn phí, ưu đãi độc quyền từ đối tác, mời tham gia sự kiện VIP, đổi 10 voucher/tháng, đổi 2 tour/tháng", "description", "Hoàn thành ít nhất 20 tour và tổng chi tiêu 80 triệu VNĐ để đạt hạng Vàng.")
        );

        // Keywords that broadly indicate a question about ranks
        List<String> generalRankKeywords = List.of(
            "hạng", "cấp bậc", "rank", "điểm", "lợi ích", "ưu đãi", "quyền lợi", "thành viên", "chương trình", "của tôi", "của bạn"
        );

        boolean isGeneralRankQuestion = generalRankKeywords.stream()
            .anyMatch(lowerCaseQuery::contains);

        // --- Personalized Rank Information ---
        if (userId != null && !userId.isEmpty() && (isGeneralRankQuestion || lowerCaseQuery.contains("của tôi") || lowerCaseQuery.contains("của bạn") || lowerCaseQuery.contains("tôi đang ở hạng nào"))) {
            Optional<users> userOptional = userRepo.findById(userId); // Need userRepository
            if (userOptional.isPresent()) {
                doan3.tourdulich.khang.entity.users user = userOptional.get();
                doan3.tourdulich.khang.entity.Rank userRank = rankRepository.findByUser_id(user.getUser_id());
                
                if (userRank == null) {
                    // This scenario should ideally be handled during user creation
                    // For now, if no rank, assume basic and inform
                    Map<String, Object> noRankInfo = new HashMap<>();
                    noRankInfo.put("type", "personal_no_rank");
                    noRankInfo.put("currentRankName", "Chưa có");
                    noRankInfo.put("currentPoints", 0);
                    noRankInfo.put("completedTours", 0);
                    noRankInfo.put("totalSpend", 0L);
                    noRankInfo.put("message", "Bạn chưa có hạng thành viên. Hãy hoàn thành ít nhất 1 tour để bắt đầu tích lũy hạng.");
                    resultRanks.add(noRankInfo);
                } else {
                    int completedTours = tourBookingRepo.countByUser_idAndStatus(user.getUser_id(), "Completed");
                    Integer totalSpendObj = tourBookingRepo.findTotalSpendByUserIdAndStatus(user.getUser_id(), "Completed");
                    long totalSpend = (totalSpendObj != null) ? totalSpendObj : 0L;

                    Map<String, Object> userRankInfo = new HashMap<>();
                    userRankInfo.put("type", "personal_rank");
                    userRankInfo.put("currentRankName", userRank.getRank());
                    userRankInfo.put("currentPoints", userRank.getPoints());
                    userRankInfo.put("completedTours", completedTours);
                    userRankInfo.put("totalSpend", totalSpend);
                    
                    String nextRankName = null;
                    int toursNeeded = 0;
                    long spendNeeded = 0;
                    double currentDiscount = 0;
                    String currentBenefits = "";
                    String currentDescription = "";

                    // Find current rank benefits and next rank requirements
                    for (Map<String, Object> rankDef : allRanks) {
                        if (userRank.getRank().equals(rankDef.get("name"))) {
                            currentDiscount = (Integer) rankDef.get("discount");
                            currentBenefits = (String) rankDef.get("benefits");
                            currentDescription = (String) rankDef.get("description");
                        }
                    }

                    if (userRank.getRank().equals("Đồng")) {
                        nextRankName = "Bạc";
                        Map<String, Object> silverRank = allRanks.stream().filter(r -> r.get("name").equals("Bạc")).findFirst().orElse(null);
                        if (silverRank != null) {
                            toursNeeded = Math.max(0, (Integer)silverRank.get("tourThreshold") - completedTours);
                            spendNeeded = Math.max(0, (Long)silverRank.get("spendThreshold") - totalSpend);
                        }
                    } else if (userRank.getRank().equals("Bạc")) {
                        nextRankName = "Vàng";
                        Map<String, Object> goldRank = allRanks.stream().filter(r -> r.get("name").equals("Vàng")).findFirst().orElse(null);
                        if (goldRank != null) {
                            toursNeeded = Math.max(0, (Integer)goldRank.get("tourThreshold") - completedTours);
                            spendNeeded = Math.max(0, (Long)goldRank.get("spendThreshold") - totalSpend);
                        }
                    } else if (userRank.getRank().equals("Vàng")) {
                        nextRankName = "Bạn đã đạt hạng cao nhất!";
                    }

                    userRankInfo.put("currentDiscount", currentDiscount);
                    userRankInfo.put("currentBenefits", currentBenefits);
                    userRankInfo.put("currentDescription", currentDescription);
                    userRankInfo.put("nextRankName", nextRankName);
                    userRankInfo.put("toursNeededForNextRank", toursNeeded);
                    userRankInfo.put("spendNeededForNextRank", spendNeeded);
                    
                    resultRanks.add(userRankInfo); // Add personalized info
                }
            }
        }
        // --- End Personalized Rank Information ---

        // If not a personalized query or userId is null/empty, proceed with general rank definitions search
        if (userId == null || userId.isEmpty() || resultRanks.isEmpty() || !lowerCaseQuery.contains("của tôi") && !lowerCaseQuery.contains("của bạn")) {
            if (isGeneralRankQuestion) {
                resultRanks.addAll(allRanks); // Return all definitions
            } else {
                for (Map<String, Object> rank : allRanks) {
                    if (((String) rank.get("name")).toLowerCase().contains(lowerCaseQuery) || rank.get("description").toString().toLowerCase().contains(lowerCaseQuery)) {
                        resultRanks.add(rank);
                    }
                }
            }
        }
        
        return resultRanks.stream().distinct().collect(Collectors.toList());
    }

    private String buildContext(List<tours> tours, List<KhuyenMai> promotions, List<Map<String, Object>> ranks) {
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

                if (tour.getTour_start_date() != null && !tour.getTour_start_date().isEmpty()) {
                    context.append("Ngày khởi hành: ");
                    tour.getTour_start_date().forEach(date -> context.append(date.getStart_date()).append(" (").append(tour.getTour_max_number_of_people()-date.getGuest_number()).append(" chỗ trống), "));
                    context.setLength(context.length() - 2); // Remove trailing comma and space
                    context.append(". ");
                }

                if (tour.getTourSchedules() != null && !tour.getTourSchedules().isEmpty()) {
                    context.append("Lịch trình: ");
                    tour.getTourSchedules().forEach(schedule -> {
                        context.append("Ngày ").append(schedule.getDay()).append(": ").append(schedule.getTitle()).append(" - ").append(schedule.getBody()).append(". ");

                    });
                    context.append("\n");
                }


                if (tourPicRepo.findOnePicByTour(tour.getTour_id()) != null) {
                    context.append("<img src=\"").append(tourPicRepo.findOnePicByTour(tour.getTour_id())).append("\" alt=\"Hình ảnh tour\"\"> ");
                }
                context.append("Giá người lớn: ").append(tour.getTour_adult_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                context.append("Giá trẻ em: ").append(tour.getTour_child_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                context.append("Giá em bé: ").append(tour.getTour_infant_price()).append(" VNĐ. "); // Assuming price is never null or defaults to 0
                if (tour.getSpecial_offer() != null && !tour.getSpecial_offer().isEmpty()) {
                    context.append("Khuyến mãi: ").append(tour.getSpecial_offer()).append(". ");
                }
                context.append("Mô tả: ").append(tour.getTour_description() != null ? tour.getTour_description() : "Không có mô tả").append(". ");
                context.append("<a href=\"").append(tourDetailBaseUrl).append(tour.getTour_id() != null ? tour.getTour_id() : "").append("\" target=\"_blank\" class=\"detail-button\">Xem chi tiết Tour</a>\n\n");
            }
        }

        if (!promotions.isEmpty()) {
            context.append("Thông tin về các chương trình khuyến mãi liên quan:\n\n");
            java.util.Date now = new java.util.Date();
            for (int i = 0; i < promotions.size(); i++) {
                KhuyenMai promo = promotions.get(i);
                context.append("Khuyến mãi ").append(i + 1).append(":\n");
                context.append("Tên: ").append(promo.getTenKhuyenMai() != null ? promo.getTenKhuyenMai() : "Không rõ").append(". ");

                // Add promotion status
                if (promo.getNgayBatDau() != null && promo.getNgayKetThuc() != null) {
                    if (now.after(promo.getNgayBatDau()) && now.before(promo.getNgayKetThuc())) {
                        context.append("Trạng thái: Đang diễn ra. ");
                    } else if (now.before(promo.getNgayBatDau())) {
                        context.append("Trạng thái: Sắp diễn ra. ");
                    } else {
                        context.append("Trạng thái: Đã kết thúc. ");
                    }
                }

                context.append("Mô tả: ").append(promo.getMoTa() != null ? promo.getMoTa() : "Không có mô tả").append(". ");
                context.append("Phần trăm giảm giá: ").append(promo.getPhanTramGiamGia()).append("%. ");
                context.append("Ngày bắt đầu: ").append(promo.getNgayBatDau() != null ? promo.getNgayBatDau().toString() : "Không rõ").append(". ");
                context.append("Ngày kết thúc: ").append(promo.getNgayKetThuc() != null ? promo.getNgayKetThuc().toString() : "Không rõ").append(". ");
                if (promo.getHinhAnh() != null && !promo.getHinhAnh().isEmpty()) {
                    context.append("<img src=\"").append(promo.getHinhAnh()).append("\" alt=\"Hình ảnh khuyến mãi\"> ");
                }
                context.append("<a href=\"").append(promotionDetailBaseUrl).append(promo.getId()).append("\" target=\"_blank\" class=\"detail-button\">Xem chi tiết Khuyến mãi</a>\n\n");
            }
        }

        if (!ranks.isEmpty()) {
            context.append("Thông tin về các hạng thành viên:\n\n");
            for (Map<String, Object> rank : ranks) {
                if (rank.containsKey("type") && rank.get("type").equals("personal_rank")) {
                    // Personalized user rank info
                    context.append("THÔNG TIN HẠNG THÀNH VIÊN CỦA BẠN:\n");
                    context.append("Hạng hiện tại: ").append(rank.get("currentRankName")).append(". ");
                    context.append("Điểm tích lũy: ").append(rank.get("currentPoints")).append(" điểm. ");
                    context.append("Số tour đã hoàn thành: ").append(rank.get("completedTours")).append(". ");
                    context.append("Tổng chi tiêu: ").append(String.format("%,d", (Long)rank.get("totalSpend"))).append(" VNĐ. ");
                    context.append("Mô tả hạng hiện tại: ").append(rank.get("currentDescription")).append(". ");
                    context.append("Quyền lợi bạn đang có: ").append(rank.get("currentBenefits")).append(". ");
                    context.append("Ưu đãi giảm giá tour hiện tại: ").append(rank.get("currentDiscount")).append("%.\n");

                    if (!rank.get("currentRankName").equals("Vàng")) {
                        context.append("Để lên hạng ").append(rank.get("nextRankName")).append(" bạn cần hoàn thành thêm ");
                        context.append(rank.get("toursNeededForNextRank")).append(" tour và chi tiêu thêm ");
                        context.append(String.format("%,d", (Long)rank.get("spendNeededForNextRank"))).append(" VNĐ.\n\n");
                    } else {
                        context.append("Bạn đã đạt hạng cao nhất. Chúc mừng!\n\n");
                    }
                } else if (rank.containsKey("type") && rank.get("type").equals("personal_no_rank")) {
                    context.append("THÔNG TIN HẠNG THÀNH VIÊN CỦA BẠN:\n");
                    context.append(rank.get("message")).append("\n\n");
                }
                else {
                    // Generic rank definition
                    context.append("Tên hạng: ").append(rank.get("name")).append(". ");
                    context.append("Mô tả: ").append(rank.get("description")).append(" ");
                    context.append("Yêu cầu số tour hoàn thành: ").append(rank.get("tourThreshold")).append(". ");
                    context.append("Mức ưu đãi giảm giá tour: ").append(rank.get("discount")).append("%.\n");
                    context.append("Quyền lợi: ").append(rank.get("benefits")).append(".\n\n");
                }
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
            if (userMessage.toLowerCase().contains("hình") || userMessage.toLowerCase().contains("ảnh")) {
                prompt += "\n\n**Quan trọng: Người dùng muốn xem hình ảnh. Hãy trả về thẻ `<img>` từ السياق.**";
            }
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
        prompt.append("Nhiệm vụ: tư vấn và giới thiệu các tour, chương trình khuyến mãi và hệ thống hạng thành viên phù hợp. **Hãy đặc biệt chú ý đến thông tin về các hạng thành viên được cung cấp trong phần 'THÔNG TIN LIÊN QUAN'. Sử dụng chi tiết về yêu cầu và quyền lợi của từng hạng để trả lời câu hỏi của người dùng một cách chính xác và đầy đủ. ");
        prompt.append("Sử dụng thông tin và các link chi tiết, bao gồm cả các thẻ `<img>` được cung cấp dưới đây để trả lời một cách chính xác và hữu ích. Nếu người dùng hỏi về hình ảnh, hãy trả về thẻ `<img>` từ السياق.** Trả lời ngắn gọn, thân thiện.\n\n");

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
            response.append("\n Bạn có muốn biết thêm chi tiết không?");
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