package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.entity.tour_bookings;
import doan3.tourdulich.khang.entity.tour_ratings;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.ratingRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@RestController
@RequestMapping("/admin/statistics")
public class statisticsController {
    @Autowired
    private userRepo userRepo;
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private ratingRepo ratingRepo;

    @GetMapping()
    public ModelAndView statistics(ModelAndView modelAndView) {
        modelAndView.setViewName("admin_html/statistics");

        // Các dữ liệu hiện có
        List<users> users = userRepo.findAll();
        List<tours> tours = tourRepo.findAll();
        List<tour_bookings> bookings = tourBookingRepo.find5LatestBookings();
        List<tour_bookings> Allbookings = tourBookingRepo.findAll();
        List<tour_ratings> reviews = ratingRepo.findAll();

        // Đếm rating
        long rating1 = reviews.stream().filter(r -> r.getRating() == 1).count();
        long rating2 = reviews.stream().filter(r -> r.getRating() == 2).count();
        long rating3 = reviews.stream().filter(r -> r.getRating() == 3).count();
        long rating4 = reviews.stream().filter(r -> r.getRating() == 4).count();
        long rating5 = reviews.stream().filter(r -> r.getRating() == 5).count();

        // Lấy doanh thu theo tháng
        List<Object[]> monthlyRevenue = tourBookingRepo.getMonthlyRevenue();

        // Lấy số lượng đặt tour theo tháng
        List<Object[]> bookingCounts = tourBookingRepo.getBookingCountsByMonth();

        // Tính tổng doanh thu
        Long totalRevenue = monthlyRevenue.stream()
                .mapToLong(m -> ((Number)m[2]).longValue())
                .sum();

        // Chuẩn bị dữ liệu cho biểu đồ kết hợp
        Map<String, Double> revenueMap = new HashMap<>();
        Map<String, Long> bookingsMap = new HashMap<>();
        Set<String> monthKeys = new TreeSet<>(Collections.reverseOrder()); // Sắp xếp giảm dần

        // Xử lý dữ liệu doanh thu
        for (Object[] row : monthlyRevenue) {
            String key = row[1] + "-" + row[0]; // Format: "year-month"
            revenueMap.put(key, ((Number)row[2]).doubleValue());
            monthKeys.add(key);
        }

        // Xử lý dữ liệu số lượng đặt tour
        for (Object[] row : bookingCounts) {
            String key = row[1] + "-" + row[0]; // Format: "year-month"
            bookingsMap.put(key, ((Number)row[2]).longValue());
            monthKeys.add(key);
        }

        // Tạo labels và data cho biểu đồ
        List<String> monthLabels = new ArrayList<>();
        List<Double> revenueData = new ArrayList<>();
        List<Long> bookingCountData = new ArrayList<>();

        for (String key : monthKeys) {
            String[] parts = key.split("-");
            monthLabels.add("Tháng " + parts[1] + "/" + parts[0]); // Format: "Tháng month/year"
            revenueData.add(revenueMap.getOrDefault(key, 0.0));
            bookingCountData.add(bookingsMap.getOrDefault(key, 0L));
        }



        // Thêm dữ liệu vào model
        modelAndView.addObject("users", users);
        modelAndView.addObject("tours", tours);
        modelAndView.addObject("bookings", bookings);
        modelAndView.addObject("AllBookings", Allbookings);
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("rating1", rating1);
        modelAndView.addObject("rating2", rating2);
        modelAndView.addObject("rating3", rating3);
        modelAndView.addObject("rating4", rating4);
        modelAndView.addObject("rating5", rating5);
        modelAndView.addObject("monthlyRevenue", monthlyRevenue);
        modelAndView.addObject("totalRevenue", totalRevenue);
        modelAndView.addObject("popularTours", tourBookingRepo.findPopularTours());
        modelAndView.addObject("userGrowth", userRepo.getUserGrowthByMonth());

        Collections.reverse(monthLabels);
        Collections.reverse(revenueData);
        Collections.reverse(bookingCountData);
        // Thêm dữ liệu cho biểu đồ kết hợp
        modelAndView.addObject("monthLabels", monthLabels);
        modelAndView.addObject("revenueData", revenueData);
        modelAndView.addObject("bookingCountData", bookingCountData);

        return modelAndView;
    }
}
