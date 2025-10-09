package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_pictures;
import doan3.tourdulich.khang.entity.tours;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface tourPicRepo extends JpaRepository<tour_pictures, String> {
    @Query("SELECT p.picture_url FROM tour_pictures p WHERE p.tour.tour_id = ?1")
    List<String> findByTourId(String tour_id);
    @Query("SELECT p.picture_url FROM tour_pictures p WHERE p.tour.tour_id = ?1 ORDER BY p.picture_id DESC LIMIT 1")
    String findOnePicByTour(String tour_id);
    @Query("SELECT p FROM tour_pictures p WHERE p.picture_id = ?1")
    tour_pictures findByPicture_id(String picture_id);

    List<tour_pictures> findByTour(tours tour);
}
