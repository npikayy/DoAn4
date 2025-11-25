package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.pointHistory;
import doan3.tourdulich.khang.entity.PointHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface pointHistoryRepository extends JpaRepository<pointHistory, Integer> {
    @Query("SELECT ph FROM pointHistory ph WHERE ph.user.user_id = ?1 ORDER BY ph.creationDate DESC")
    List<pointHistory> findByUser_id(String userId);

    @Query("SELECT COUNT(ph) FROM pointHistory ph WHERE ph.user.user_id = :userId AND ph.type = :type AND ph.description LIKE :descriptionPrefix AND ph.creationDate BETWEEN :startDate AND :endDate")
    int countByUserAndTypeAndDescriptionStartingWithAndCreationDateBetween(
            @Param("userId") String userId,
            @Param("type") PointHistoryType type,
            @Param("descriptionPrefix") String descriptionPrefix,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}