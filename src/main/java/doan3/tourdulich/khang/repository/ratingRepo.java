package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_ratings;
import jakarta.persistence.OneToMany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ratingRepo extends JpaRepository<tour_ratings, Integer> {

    @Query("SELECT r.rating FROM tour_ratings r WHERE r.tour.tour_id = ?1")
    List<Float> findByTourId(String tour_id);
    @Query("SELECT r FROM tour_ratings r WHERE r.tour.tour_id = ?1")
    List<tour_ratings> findRatingByTourId(String tour_id);

    @Query("SELECT r FROM tour_ratings r WHERE r.user.user_id = ?1")
    List<tour_ratings> findRatingByUserId(String user_id);

    @Query(value = "SELECT COUNT(r) FROM tour_ratings r " +
            "WHERE EXTRACT(MONTH FROM r.rating_date) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM r.rating_date) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    long countCurrentMonth();

    @Query(value = "SELECT COUNT(r) FROM tour_ratings r " +
            "WHERE EXTRACT(MONTH FROM r.rating_date) = EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month') " +
            "AND EXTRACT(YEAR FROM r.rating_date) = EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month')",
            nativeQuery = true)
    long countPreviousMonth();
}
