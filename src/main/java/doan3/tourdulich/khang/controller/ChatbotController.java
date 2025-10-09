package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.dto.ChatRequest;
import doan3.tourdulich.khang.dto.ChatResponse;
import doan3.tourdulich.khang.entity.chatMessage;
import doan3.tourdulich.khang.service.ChatbotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@Slf4j
public class ChatbotController {
    
    @Autowired
    private ChatbotService chatbotService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ChatResponse.builder()
                    .success(false)
                    .error("Message cannot be empty")
                    .build()
            );
        }
        
        ChatResponse response = chatbotService.chat(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<chatMessage>> getHistory(@PathVariable String sessionId) {
        List<chatMessage> history = chatbotService.getChatHistory(sessionId);
        return ResponseEntity.ok(history);
    }
}
