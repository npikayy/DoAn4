package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.dto.UpdateInfoRequest;
import doan3.tourdulich.khang.dto.rating;
import doan3.tourdulich.khang.entity.*;
import doan3.tourdulich.khang.repository.*;
import doan3.tourdulich.khang.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequestMapping("/Client")
@RestController
public class clientController {

    @Autowired
    private bannerRepo bannerRepo;
    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private scheduleRepo scheduleRepo;
    @Autowired
    private tourPicRepo tourPicRepo;
    @Autowired
    private userRepo userRepo;
    @Autowired
    private startDateRepo startDateRepo;
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private historyRepo historyRepo;
    @Autowired
    private ratingRepo ratingRepo;
    @Autowired
    private bookingService bookingService;
    @Autowired
    private KhuyenMaiService khuyenMaiService;
    @Autowired
    private tourService tourService;
    @Autowired
    private userService userService;

    @GetMapping("/khuyen-mai")
    public ModelAndView showAllPromotions(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "4") int size,
                                          @RequestParam(required = false) String status) { // status can now be null or empty
        ModelAndView modelAndView = new ModelAndView(); // Instantiate ModelAndView inside the method
        getCurrentUser(modelAndView);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        // Pass status directly, searchKhuyenMai handles null/empty as "all"
        org.springframework.data.domain.Page<KhuyenMai> promotionPage = khuyenMaiService.searchKhuyenMai(null, status, null, null, pageable);
        modelAndView.addObject("promotionPage", promotionPage);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", promotionPage.getTotalPages());
        modelAndView.addObject("status", status); // Pass the actual status for frontend highlighting
        modelAndView.setViewName("client_html/promotions_list");
        return modelAndView;
    }

    @GetMapping("/khuyen-mai/{id}")
    public ModelAndView showPromotionDetail(@PathVariable("id") int id, ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        KhuyenMai promotion = khuyenMaiService.getKhuyenMaiById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid promotion Id:" + id));
        
        List<tours> tours = promotion.getTours();

        modelAndView.addObject("promotion", promotion);
        prepareTourData(modelAndView, tours, "tourThumbnails", "tourStartDates");
        
        modelAndView.setViewName("client_html/promotion_detail");
        return modelAndView;
    }

    public void getCurrentUser(ModelAndView modelAndView) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        users user = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            // Handle regular user authentication
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            }
            // Handle Google OAuth2 authentication
            else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email"); // Get email from Google account
            }

            if (username != null) {
                user = userRepo.findByUsername(username);
                if (user != null) {
                    log.info("User accessed main page: {}", username);
                    modelAndView.addObject("user_id", user.getUser_id());
                    modelAndView.addObject("user", user);
                } else {
                    log.warn("User not found in database: {}", username);
                }
            }
        }
    }

    public void saveHistory(String region, String user_id) {
    List<userHistory> history = historyRepo.findByUser_id(user_id);
    if (history.size() >= 10) {
        historyRepo.delete(history.get(9));
    }

    userHistory userHistory = new userHistory();
    userHistory.setUser(userRepo.findById(user_id).get());
    userHistory.setTimeStamp(LocalDateTime.now());
    userHistory.setRegion(region);
    historyRepo.save(userHistory);
    }

    @GetMapping("/Home")
    public ModelAndView TrangChu(ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        List<tours> tours = tourRepo.find3Tour_discount(new Date());
        modelAndView.addObject("banners", bannerRepo.findAll());
        modelAndView.addObject("promotionBanners", khuyenMaiService.getActivePromotionsWithImages()); // Add this line
        String RecommendedTourThumbnails = "RecommendedTourThumbnails";
        String RecommendedTourStartDates = "RecommendedTourStartDates";
        // Kiểm tra nếu user đã đăng nhập
        if (modelAndView.getModel().get("user_id") != null) {
            String userId = (String) modelAndView.getModel().get("user_id");

            // 1. Tìm region được xem nhiều nhất bởi user này
            Set<String> mostViewedRegion = historyRepo.findTopRegionByUserId(userId);
            if (mostViewedRegion.size() > 0) {
                System.out.println("mostViewedRegion: " + mostViewedRegion);
            }
            if (mostViewedRegion.size() > 0) {
                // 2. Lấy 6 tour thuộc region đó
                List<tours> recommendedTours = tourRepo.find6ToursByRegion(mostViewedRegion);
                modelAndView.addObject("recommendedTours", recommendedTours);
                prepareTourData(modelAndView, recommendedTours, RecommendedTourThumbnails, RecommendedTourStartDates);
            } else {
                // Nếu không có lịch sử, lấy tour ngẫu nhiên
                List<tours> recommendedTours = tourRepo.find6RandomTours();
                modelAndView.addObject("recommendedTours", recommendedTours);
                prepareTourData(modelAndView, recommendedTours , RecommendedTourThumbnails, RecommendedTourStartDates);
            }
        } else {
            // Nếu là khách, lấy tour ngẫu nhiên
            List<tours> recommendedTours = tourRepo.find6RandomTours();
            modelAndView.addObject("recommendedTours", recommendedTours);
            prepareTourData(modelAndView, recommendedTours, RecommendedTourThumbnails, RecommendedTourStartDates);
        }

        String tourThumbnails = "tourThumbnails";
        String tourStartDates = "tourStartDates";
        prepareTourData(modelAndView, tours, tourThumbnails, tourStartDates);
        modelAndView.setViewName("client_html/travel");
        return modelAndView;
    }


    @GetMapping("/ToursList")
    public ModelAndView ToursList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isAbroad,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String transportation,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean hasPromotion,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            ModelAndView modelAndView) {
        
        getCurrentUser(modelAndView);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<tours> tourPage = tourService.findToursPaginated(keyword, isAbroad, null, duration, transportation, priceRange, location, region, hasPromotion, null, startDate, null, rating, null, pageable);

        List<String> uniqueLocations = tourRepo.findDistinctEndLocations();
        modelAndView.addObject("uniqueLocations", uniqueLocations);

        // Add filter params to model to set the state of the inputs on the frontend
        modelAndView.addObject("keyword", keyword);
        modelAndView.addObject("isAbroad", isAbroad);
        modelAndView.addObject("duration", duration);
        modelAndView.addObject("transportation", transportation);
        modelAndView.addObject("priceRange", priceRange);
        modelAndView.addObject("location", location);
        modelAndView.addObject("region", region);
        modelAndView.addObject("hasPromotion", hasPromotion);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("rating", rating);

        prepareTourData(modelAndView, tourPage.getContent(), "tourThumbnails", "tourStartDates"); // Pass content of the page
        modelAndView.addObject("tourPage", tourPage); // Add the Page object
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", tourPage.getTotalPages());

        modelAndView.setViewName("client_html/tours_list");
        return modelAndView;
    }

    @GetMapping("/TourDetail/{tour_id}")
    public ModelAndView TourDetail(@PathVariable String tour_id, ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        tours tour = tourRepo.findByIdWithPromotions(tour_id).get();

        // Get active promotion for this tour
        KhuyenMai activePromotion = tour.getDiscount_promotion();
        modelAndView.addObject("activePromotion", activePromotion);

        modelAndView.addObject("tourRatings", ratingRepo.findRatingByTourId(tour_id));
        modelAndView.addObject("tour_id", tour_id);
        modelAndView.addObject("tour", tour);
        modelAndView.addObject("tourPics", tourPicRepo.findByTourId(tour_id));
        modelAndView.addObject("schedules", scheduleRepo.findByTourId(tour_id));
        modelAndView.addObject("startDates", startDateRepo.findByTourId(tour_id));

        List<tours> relatedTours = tourRepo.findRelatedTour(tour.getTour_end_location(), tour_id);

        String tourThumbnails = "tourThumbnails";
        String tourStartDates = "tourStartDates";
        prepareTourData(modelAndView, relatedTours, tourThumbnails, tourStartDates);

        modelAndView.addObject("relatedTours", relatedTours);

        modelAndView.setViewName("client_html/tour_detail");
        return modelAndView;
    }



    private void prepareTourData(ModelAndView modelAndView,
                                    List<tours> tours,
                                    String thumbnailsModelName,
                                    String startDatesModelName) {
        if (tours != null) {
            modelAndView.addObject("tours", tours);

            // Create maps for thumbnails, start dates, and active promotions
            Map<String, String> tourThumbnails = new HashMap<>();
            Map<String, Object> tourStartDatesMap = new HashMap<>();

            for (tours tour : tours) {
                String tourId = tour.getTour_id();

                // Get thumbnail
                String thumbnail = tourPicRepo.findOnePicByTour(tourId);
                tourThumbnails.put(tourId, thumbnail != null ? thumbnail : "");

                // Get start dates
                List<tour_start_date> startDates = startDateRepo.findByTourId(tourId);
                tourStartDatesMap.put(tourId, startDates);

            }

            // Add maps to the model
            modelAndView.addObject(thumbnailsModelName, tourThumbnails);
            modelAndView.addObject(startDatesModelName, tourStartDatesMap);
        }
    }
    @GetMapping("/booking_search/{booking_id}")
    public ModelAndView order_not_found(@PathVariable Integer booking_id,ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        modelAndView.addObject("booking", null);
        Optional<tour_bookings> booking = tourBookingRepo.findById(booking_id);
        if (booking.isPresent()) {
            modelAndView.addObject("booking", booking.get());
        }

        modelAndView.setViewName("/client_html/booking_search");
        return modelAndView;
    }
    @PostMapping("/Tra_cuu")
    public ModelAndView search(@RequestParam("booking_id") Integer booking_id,ModelAndView modelAndView) {
        return new ModelAndView("redirect:/Client/booking_search/"+booking_id);
    }
    @PostMapping("/Rate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateInfo(@RequestBody rating requestData) {
        Map<String, Object> response = new HashMap<>();
        try {

            log.info("Received data: {}", requestData);

            tour_ratings rating = tour_ratings.builder()
                    .rating(requestData.getRating())
                    .tour(tourRepo.findById(requestData.getTour_id()).get())
                    .user(userRepo.findById(requestData.getUser_id()).get())
                    .rating_date(LocalDate.now())
                    .comment(requestData.getComment())
                    .build();

            ratingRepo.save(rating);

            float temp_rating = 0;
            List<Float> ratings = ratingRepo.findByTourId(requestData.getTour_id());
            for (int i = 0; i < ratings.size(); i++) {
                temp_rating += ratings.get(i);
            }
            temp_rating = temp_rating / ratings.size();
            temp_rating = Math.round(temp_rating * 10) / 10.0f;

            tours tour = tourRepo.findById(requestData.getTour_id()).get();
            tour.setTour_rating(temp_rating);
            tourRepo.save(tour);

            bookingService.handleTourCompletionTasks(tourBookingRepo.findById(requestData.getBooking_id()).get());

            response.put("success", true);
            response.put("message", "Cảm ơn bạn đã dành thời gian đánh giá chuyến đi! Đánh giá của bạn giúp chúng tôi cải thiện dịch vụ tốt hơn.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Xin lỗi, có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại sau.");
            log.error("Error updating tour rating: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
