package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tours;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface tourRepo extends JpaRepository<tours, String>, JpaSpecificationExecutor<tours> {

    @Query("SELECT t FROM tours t LEFT JOIN FETCH t.discount_promotion WHERE t.tour_id = :tourId")
    Optional<tours> findByIdWithPromotions(@Param("tourId") String tourId);

    @Query("SELECT t FROM tours t WHERE t.tour_region = ?1")
    public List<tours> findByRegion(String region);

    @Query("SELECT t FROM tours t WHERE t.tour_end_location = ?1")
    public List<tours> findByTour_location(String location);

    @Query("SELECT t FROM tours t WHERE t.tour_end_location = ?1 and t.tour_id not like ?2 ORDER BY t.tour_rating DESC LIMIT 3")
    public List<tours> findRelatedTour(String location, String tour_id);

    @Query(value = "SELECT t FROM tours t ORDER BY RANDOM() LIMIT 6")
    List<tours> find6RandomTours();

    @Query("SELECT t FROM tours t WHERE t.tour_region in ?1 order by t.tour_rating DESC limit 6")
    public List<tours> find6ToursByRegion(Set<String> region);

    @Query("SELECT t FROM tours t where t.discount_promotion.phanTramGiamGia >= 50 order by t.discount_promotion.phanTramGiamGia DESC LIMIT 3")
    public List<tours> find3Tour_discount();

    @Query("SELECT t FROM tours t where t.discount_promotion.phanTramGiamGia >= 50")
    public List<tours> findTour_discount();

    @Query("SELECT DISTINCT t FROM tours t " +
            "JOIN t.tour_start_date tsd " +
            "WHERE t.tour_end_location ILIKE %:location% " +
            "AND t.tour_adult_price BETWEEN :minPrice AND :maxPrice " +
            "AND tsd.start_date >= :startDate")
    List<tours> findByLocationAndPriceAndStartDate(String location, Integer minPrice, Integer maxPrice, LocalDate startDate);

    @Query("SELECT DISTINCT t.tour_end_location FROM tours t")
    List<String> findDistinctEndLocations();

    @Query("SELECT t FROM tours t WHERE t.discount_promotion.id = :khuyenMaiId")
    List<tours> findToursByDiscount_promotionId(Integer khuyenMaiId);

    @Query("SELECT DISTINCT t FROM tours t LEFT JOIN FETCH t.tourPictures WHERE LOWER(t.tour_name) LIKE %:keyword% OR LOWER(t.tour_region) LIKE %:keyword% OR LOWER(t.tour_end_location) LIKE %:keyword% OR LOWER(t.tour_description) LIKE %:keyword%")
    List<tours> findByKeywordInNameRegionLocationDescription(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT t FROM tours t LEFT JOIN FETCH t.tourPictures WHERE (LOWER(t.tour_name) LIKE %:keyword% OR LOWER(t.tour_region) LIKE %:keyword% OR LOWER(t.tour_end_location) LIKE %:keyword% OR LOWER(t.tour_description) LIKE %:keyword%) AND t.special_offer IS NOT NULL AND t.special_offer != ''")
    List<tours> findByKeywordAndSpecialOffer(@Param("keyword") String keyword);

    @Query("SELECT t FROM tours t WHERE t.discount_promotion IS NULL")
    List<tours> findByDiscount_promotionIsNull();
}
