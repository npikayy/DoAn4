package doan3.tourdulich.khang.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class chatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String message_id;

    @Column(nullable = false)
    private String sessionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String user_message;

    @Column(columnDefinition = "TEXT")
    private String bot_response;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
