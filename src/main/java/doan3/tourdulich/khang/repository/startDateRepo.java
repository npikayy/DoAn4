package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_start_date;
import doan3.tourdulich.khang.entity.tours;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface startDateRepo extends JpaRepository<tour_start_date, Integer> {

    @Query("SELECT s FROM tour_start_date s WHERE s.tour.tour_id = ?1")
    List<tour_start_date> findByTourId(String tour_id);

    @Query("SELECT s FROM tour_start_date s WHERE s.start_date = ?1 AND s.tour.tour_id = ?2")
    tour_start_date findByStartDateAndTourId(LocalDate startDate, String tourId);
}
