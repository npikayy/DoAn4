package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.userHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface historyRepo extends JpaRepository<userHistory, Integer> {
    @Query("SELECT h FROM userHistory h WHERE h.user.user_id = ?1 order by h.timeStamp desc")
    List<userHistory> findByUser_id(String user_id);
    @Query("SELECT uh.region FROM userHistory uh WHERE uh.user.user_id = :userId GROUP BY uh.region ORDER BY COUNT(uh.region) DESC, MAX(uh.timeStamp)")
    Set<String> findTopRegionByUserId(String userId);
}
