package doan3.tourdulich.khang.repository;

import doan3.tourdulich.khang.entity.tour_bookings;
import doan3.tourdulich.khang.entity.users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface tourBookingRepo extends JpaRepository<tour_bookings, Integer> {
    @Query("SELECT COUNT(b) > 0 FROM tour_bookings b WHERE b.tour.tour_id = :tourId AND b.user_id = :userId AND b.start_date = :startDate AND b.status = 'Pending payment'")
    boolean existsByTourIdAndUserIdAndStartDate(String tourId, String userId, LocalDate startDate);

    @Query("SELECT b FROM tour_bookings b WHERE b.tour.tour_id = :tourId AND b.user_id = :userId AND b.start_date = :startDate AND b.status = 'Pending payment'")
    tour_bookings findByIdAndUserIdAndStartDate(String tourId, String userId, LocalDate startDate);
    @Query("SELECT b FROM tour_bookings b WHERE b.user_id = ?1")
    List<tour_bookings> findByUserId(String user_id);

    @Query("SELECT b FROM tour_bookings b WHERE b.booking_id = ?1")
    tour_bookings findByBooking_id(Integer booking_id);

    @Query("SELECT DISTINCT b.user_id FROM tour_bookings b")
    List<String> findDistinctUsers();

    @Query("SELECT extract(month from b.booking_date) as month, " +
            "extract(year from b.booking_date) as year, " +
            "SUM(b.total_price) as revenue " +
            "FROM tour_bookings b " +
            "WHERE b.status NOT IN ('Cancelled') " +
            "GROUP BY extract(year from b.booking_date), extract(month from b.booking_date) " +
            "ORDER BY extract(year from b.booking_date), extract(month from b.booking_date)")
    List<Object[]> getMonthlyRevenue();

    // Query số lượng đặt tour theo tháng (mới thêm)
    @Query(value = "SELECT EXTRACT(MONTH FROM booking_date) as month, " +
            "EXTRACT(YEAR FROM booking_date) as year, " +
            "COUNT(booking_id) as booking_count " +
            "FROM tour_bookings " +
            "WHERE status NOT IN ('Cancelled') " +
            "GROUP BY EXTRACT(YEAR FROM booking_date), EXTRACT(MONTH FROM booking_date) " +
            "ORDER BY year DESC, month",
            nativeQuery = true)
    List<Object[]> getBookingCountsByMonth();

    @Query(value = "SELECT b.tour_name, COUNT(b.booking_id) as booking_count " +
            "FROM tour_bookings b " +
            "GROUP BY b.tour.tour_id, b.tour_name " +
            "ORDER BY booking_count DESC LIMIT 5")
    List<Object[]> findPopularTours();

    @Query("SELECT b FROM tour_bookings b ORDER BY b.booking_date DESC LIMIT 5")
    List<tour_bookings> find5LatestBookings();
}
