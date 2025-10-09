package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.chatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface chatMessageRepo extends JpaRepository<chatMessage, String> {
    List<chatMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    @Query("SELECT c FROM chatMessage c WHERE c.sessionId = ?1 ORDER BY c.createdAt DESC")
    List<chatMessage> findRecentMessagesBySession(String sessionId);
}
