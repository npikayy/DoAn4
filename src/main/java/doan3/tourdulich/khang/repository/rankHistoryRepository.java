package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.rankHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface rankHistoryRepository extends JpaRepository<rankHistory, Integer> {
    @Query("SELECT r FROM rankHistory r WHERE r.user.user_id = ?1")
    List<rankHistory> findByUser_id(String userId);
}