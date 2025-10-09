package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_schedules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface scheduleRepo extends JpaRepository<tour_schedules, Integer> {
    @Query("SELECT s FROM tour_schedules s WHERE s.tour.tour_id = ?1 order by s.day ASC")
    List<tour_schedules> findByTourId(String tourId);

    @Query("SELECT s FROM tour_schedules s WHERE s.tour.tour_id = ?1 AND s.day = ?2")
    tour_schedules findByTourIDandDay(String tourId, Integer day);

    @Query("SELECT s FROM tour_schedules s WHERE s.tour.tour_id = ?1")
    List<tour_schedules> findByTour(String tourId);

}
