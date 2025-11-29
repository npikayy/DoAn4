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
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import java.time.LocalDate;
import java.time.Period;
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

        // Fetch all necessary data
        List<users> users = userRepo.findAll();
        List<tours> tours = tourRepo.findAll();
        List<tour_bookings> Allbookings = tourBookingRepo.findAll();
        List<tour_ratings> reviews = ratingRepo.findAll();

        // Rating statistics
        long rating1 = reviews.stream().filter(r -> r.getRating() == 1).count();
        long rating2 = reviews.stream().filter(r -> r.getRating() == 2).count();
        long rating3 = reviews.stream().filter(r -> r.getRating() == 3).count();
        long rating4 = reviews.stream().filter(r -> r.getRating() == 4).count();
        long rating5 = reviews.stream().filter(r -> r.getRating() == 5).count();

        // Monthly revenue and booking counts
        List<Object[]> monthlyRevenue = tourBookingRepo.getMonthlyRevenue();
        List<Object[]> bookingCounts = tourBookingRepo.getBookingCountsByMonth();

        // Total revenue
        Long totalRevenue = monthlyRevenue.stream()
                .mapToLong(m -> ((Number)m[2]).longValue())
                .sum();

        // Prepare data for combined chart
        Map<String, Double> revenueMap = new HashMap<>();
        Map<String, Long> bookingsMap = new HashMap<>();
        Set<String> monthKeys = new TreeSet<>(Collections.reverseOrder());

        for (Object[] row : monthlyRevenue) {
            String key = row[1] + "-" + row[0];
            revenueMap.put(key, ((Number)row[2]).doubleValue());
            monthKeys.add(key);
        }

        for (Object[] row : bookingCounts) {
            String key = row[1] + "-" + row[0];
            bookingsMap.put(key, ((Number)row[2]).longValue());
            monthKeys.add(key);
        }

        List<String> monthLabels = new ArrayList<>();
        List<Double> revenueData = new ArrayList<>();
        List<Long> bookingCountData = new ArrayList<>();

        for (String key : monthKeys) {
            String[] parts = key.split("-");
            monthLabels.add("Tháng " + parts[1] + "/" + parts[0]);
            revenueData.add(revenueMap.getOrDefault(key, 0.0));
            bookingCountData.add(bookingsMap.getOrDefault(key, 0L));
        }
        Collections.reverse(monthLabels);
        Collections.reverse(revenueData);
        Collections.reverse(bookingCountData);

        // Booking status data
        List<Object[]> statusCounts = tourBookingRepo.countByStatus();
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusData = new ArrayList<>();
        for (Object[] row : statusCounts) {
            statusLabels.add((String) row[0]);
            statusData.add((Long) row[1]);
        }
        
        // Gender data
        List<Object[]> genderCounts = userRepo.countUsersByGender();
        List<String> genderLabels = new ArrayList<>();
        List<Long> genderData = new ArrayList<>();
        for (Object[] row : genderCounts) {
            String gender = (String) row[0];
            genderLabels.add(gender == null || gender.isEmpty() ? "Không rõ" : gender);
            genderData.add((Long) row[1]);
        }

        // Age range data (Java-based calculation)
        Map<String, Long> ageRangeCountsMap = new LinkedHashMap<>();
        ageRangeCountsMap.put("Dưới 18", 0L);
        ageRangeCountsMap.put("18-25", 0L);
        ageRangeCountsMap.put("26-35", 0L);
        ageRangeCountsMap.put("36-50", 0L);
        ageRangeCountsMap.put("Trên 50", 0L);
        ageRangeCountsMap.put("Không rõ", 0L);

        LocalDate today = LocalDate.now();

        for (users user : users) {
            if ("ROLE_USER".equals(user.getRole())) {
                if (user.getDate_of_birth() != null) {
                    int age = Period.between(user.getDate_of_birth(), today).getYears();
                    if (age < 18) {
                        ageRangeCountsMap.merge("Dưới 18", 1L, Long::sum);
                    } else if (age <= 25) {
                        ageRangeCountsMap.merge("18-25", 1L, Long::sum);
                    } else if (age <= 35) {
                        ageRangeCountsMap.merge("26-35", 1L, Long::sum);
                    } else if (age <= 50) {
                        ageRangeCountsMap.merge("36-50", 1L, Long::sum);
                    } else {
                        ageRangeCountsMap.merge("Trên 50", 1L, Long::sum);
                    }
                } else {
                    ageRangeCountsMap.merge("Không rõ", 1L, Long::sum);
                }
            }
        }
        
        List<String> ageRangeLabels = new ArrayList<>(ageRangeCountsMap.keySet());
        List<Long> ageRangeData = new ArrayList<>(ageRangeCountsMap.values());
        
        // Popular tours data
        List<Object[]> popularToursData = tourBookingRepo.findPopularTours(PageRequest.of(0, 5));
        List<String> popularToursLabels = new ArrayList<>();
        List<Long> popularToursValues = new ArrayList<>();
        for (Object[] row : popularToursData) {
            popularToursLabels.add((String) row[0]);
            popularToursValues.add(((Number) row[1]).longValue());
        }

        // Add all data to model
        modelAndView.addObject("users", users);
        modelAndView.addObject("tours", tours);
        modelAndView.addObject("AllBookings", Allbookings);
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("rating1", rating1);
        modelAndView.addObject("rating2", rating2);
        modelAndView.addObject("rating3", rating3);
        modelAndView.addObject("rating4", rating4);
        modelAndView.addObject("rating5", rating5);
        modelAndView.addObject("monthlyRevenue", monthlyRevenue);
        modelAndView.addObject("totalRevenue", totalRevenue);
        modelAndView.addObject("popularToursLabels", popularToursLabels);
        modelAndView.addObject("popularToursValues", popularToursValues);
        modelAndView.addObject("userGrowth", userRepo.getUserGrowthByMonth());
        modelAndView.addObject("averageRating", ratingRepo.getAverageRating());
        modelAndView.addObject("cancelledBookings", tourBookingRepo.countByStatus("Cancelled"));
        modelAndView.addObject("monthLabels", monthLabels);
        modelAndView.addObject("revenueData", revenueData);
        modelAndView.addObject("bookingCountData", bookingCountData);
        modelAndView.addObject("statusLabels", statusLabels);
        modelAndView.addObject("statusData", statusData);
        modelAndView.addObject("genderLabels", genderLabels);
        modelAndView.addObject("genderData", genderData);
        modelAndView.addObject("ageRangeLabels", ageRangeLabels);
        modelAndView.addObject("ageRangeData", ageRangeData);

        return modelAndView;
    }
}