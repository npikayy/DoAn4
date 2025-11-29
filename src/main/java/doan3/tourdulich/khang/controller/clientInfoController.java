package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.dto.UpdateInfoRequest;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.*;
import doan3.tourdulich.khang.service.VoucherService;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.service.bookingService; // Import bookingService
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/Client")
@RestController
public class clientInfoController {

    @Autowired
    private userRepo userRepo;
    @Autowired
    private userService userService;
    @Autowired
    private bookingService bookingService; // Inject bookingService
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private ratingRepo ratingRepo;
    @Autowired
    private VoucherService voucherService;

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

    @GetMapping("/Information")
    public ModelAndView clientInfo(ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        modelAndView.setViewName("/client_html/client_info");
        modelAndView.addObject("vouchers", voucherService.findByUserId(modelAndView.getModel().get("user_id").toString()));
        return modelAndView;
    }

    @PostMapping("/Information/updateInfo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateInfo(@RequestBody UpdateInfoRequest requestData) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Received data: {}", requestData);

            if (requestData.getDate_of_birth() == null) {
                requestData.setDate_of_birth(null);
            }

            userService.updateUserInfo(
                    requestData.getUser_id(),
                    requestData.getFull_name(),
                    requestData.getPhone_number(),
                    requestData.getAddress(),
                    requestData.getGender(),
                    requestData.getDate_of_birth()

            );

            response.put("success", true);
            response.put("message", "Cập nhật thông tin thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật thông tin: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @GetMapping("/Orders")
    public ModelAndView clientBooking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "") String searchQuery) {
        ModelAndView modelAndView = new ModelAndView();
        getCurrentUser(modelAndView);
        String userId = modelAndView.getModel().get("user_id").toString();

        Pageable pageable = PageRequest.of(page, size);
        Page<doan3.tourdulich.khang.entity.tour_bookings> bookingPage = bookingService.findAllPaginatedAndFilteredForUser(status, userId, searchQuery, pageable);


        modelAndView.addObject("bookingPage", bookingPage);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("status", status);
        modelAndView.addObject("searchQuery", searchQuery);
        modelAndView.setViewName("/client_html/client_orders");
        return modelAndView;
    }
    @GetMapping("/MyTourRating")
    public ModelAndView myTourRating(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "all") String rating,
            @RequestParam(defaultValue = "") String searchQuery,
            ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        String userId = modelAndView.getModel().get("user_id").toString();

        Pageable pageable = PageRequest.of(page, size);

        Specification<doan3.tourdulich.khang.entity.tour_ratings> spec = Specification.where(RatingSpecifications.withUserId(userId));

        if (org.springframework.util.StringUtils.hasText(rating)) {
            spec = spec.and(RatingSpecifications.withRating(rating));
        }

        if (org.springframework.util.StringUtils.hasText(searchQuery)) {
            spec = spec.and(RatingSpecifications.withSearchQuery(searchQuery));
        }

        Page<doan3.tourdulich.khang.entity.tour_ratings> ratingPage = ratingRepo.findAll(spec, pageable);

        modelAndView.addObject("ratingPage", ratingPage);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("rating", rating);
        modelAndView.addObject("searchQuery", searchQuery);
        modelAndView.setViewName("/client_html/client_rating");
        return modelAndView;
    }
}