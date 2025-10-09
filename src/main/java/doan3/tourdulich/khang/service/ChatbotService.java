package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.ChatRequest;
import doan3.tourdulich.khang.dto.ChatResponse;
import doan3.tourdulich.khang.entity.chatMessage;
import doan3.tourdulich.khang.repository.chatMessageRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatbotService {

    @Autowired
    private GeminiApiService geminiApiService;  // THAY THẾ

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private chatMessageRepo chatMessageRepo;

    @Value("${ai.chatbot.enabled}")
    private boolean enabled;

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

            // Step 1: RAG - Search for relevant tour information
            List<VectorStoreService.DocumentEmbedding> relevantDocs =
                    vectorStoreService.searchSimilarDocuments(request.getMessage());

            log.info("Found {} relevant documents", relevantDocs.size());

            // Step 2: Build context
            String context = buildContext(relevantDocs);

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

    private String buildContext(List<VectorStoreService.DocumentEmbedding> docs) {
        if (docs.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("Thông tin về các tour du lịch:\n\n");
        for (int i = 0; i < docs.size(); i++) {
            context.append(i + 1).append(". ").append(docs.get(i).getContent()).append("\n\n");
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
        prompt.append("Nhiệm vụ: tư vấn và giới thiệu các tour phù hợp. Trả lời ngắn gọn, thân thiện.\n\n");

        if (!context.isEmpty()) {
            prompt.append("THÔNG TIN TOUR:\n").append(context).append("\n");
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
            response.append("Tôi đã tìm thấy một số tour phù hợp:\n\n");
            response.append(context);
            response.append("\nBạn có muốn biết thêm chi tiết không?");
        } else {
            response.append("Tôi có thể giúp bạn tìm tour du lịch. ");
            response.append("Bạn muốn đi đâu? (VD: Đà Nẵng, Phú Quốc, Sapa...)");
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