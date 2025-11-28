package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.dto.ChatRequest;
import doan3.tourdulich.khang.dto.ChatResponse;
import doan3.tourdulich.khang.entity.chatMessage;
import doan3.tourdulich.khang.entity.users; // Import users entity
import doan3.tourdulich.khang.service.ChatbotService;
import doan3.tourdulich.khang.service.userService; // Import userService
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@Slf4j
public class ChatbotController {
    
    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private userService userService; // Autowire userService
    
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

        // Retrieve authenticated user's ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            String username = null;

            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
            }

            if (username != null) {
                users user = userService.findByUsername(username);
                if (user != null) {
                    request.setUserId(user.getUser_id()); // Set the userId in the ChatRequest
                }
            }
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
