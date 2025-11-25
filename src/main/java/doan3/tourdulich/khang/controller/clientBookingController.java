package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.dto.TourBookingRequest;
import doan3.tourdulich.khang.entity.*;
import doan3.tourdulich.khang.repository.tourPicRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.RankRepository;
import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.repository.voucherRepo;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.startDateRepo;
import doan3.tourdulich.khang.service.KhuyenMaiService;
import doan3.tourdulich.khang.service.VoucherService;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.service.bookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/Client/orderBooking")
public class clientBookingController {

    @Autowired
    private userRepo userRepo;
    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private userService userService;
    @Autowired
    private tourPicRepo tourPicRepo;
    @Autowired
    private voucherRepo voucherRepo;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private bookingService bookingService;
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private startDateRepo startDateRepo;
    @Autowired
    private KhuyenMaiService khuyenMaiService;
    @Autowired
    private RankRepository rankRepository;

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
    @PostMapping("/api/validate")
    public ResponseEntity<Map<String, Object>> validateVoucher(@RequestBody Map<String, String> payload) {
        String maVoucher = payload.get("maVoucher");
        String tourId = payload.get("tourId");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        // Handle regular user authentication
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        }
        // Handle Google OAuth2 authentication
        else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            username = oauth2User.getAttribute("email"); // Get email from Google account
        }
        if (username == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để sử dụng voucher.");
            System.out.println("Validation Result: " + response.get("message"));
            return ResponseEntity.status(401).body(response);
        }


        users currentUser = userService.findByUsername(username);

        Map<String, Object> validationResult = voucherService.validateVoucher(maVoucher, currentUser.getUser_id(), tourId);
        System.out.println("Validation Result: " + validationResult.get("message"));
        return ResponseEntity.ok(validationResult);
    }
    public void updateNumGuest(String tour_id, LocalDate start_date, int num_guest) {
        tour_start_date startDate = startDateRepo.findByStartDateAndTourId(start_date, tour_id);
        if (startDate != null) {
            startDate.setGuest_number(startDate.getGuest_number() + num_guest);
        }
        startDateRepo.save(startDate);
    }
    @GetMapping("/{tour_id}/{start_date}")
    public ModelAndView clientBooking(@PathVariable String tour_id,
                                      @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start_date,
                                      ModelAndView modelAndView) {
        // Manually retrieve current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        users currentUser = null;
        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
            }

            if (username != null) {
                currentUser = userService.findByUsername(username);
            }
        }

        // Add currentUser and user_id to model if user is found
        if (currentUser != null) {
            modelAndView.addObject("currentUser", currentUser);
            modelAndView.addObject("user_id", currentUser.getUser_id());
            log.info("DEBUG: clientBooking - Current User ID: {}", currentUser.getUser_id());
        } else {
            log.info("DEBUG: clientBooking - No user authenticated.");
        }

        Optional<tours> tourOptional = tourRepo.findById(tour_id);
        System.out.println(tourOptional.isPresent());

        if (!tourOptional.isPresent()) {
            modelAndView.setViewName("redirect:/error");
            return modelAndView;
        }

        Integer num_guest = startDateRepo.findByStartDateAndTourId(start_date, tour_id).getGuest_number();
        if (num_guest == 0) {
            modelAndView.addObject("max_num_guest", tourOptional.get().getTour_max_number_of_people());
        }
        else {
            modelAndView.addObject("max_num_guest", tourOptional.get().getTour_max_number_of_people() - num_guest);
        }

        // Use currentUser.getUser_id() here
        if (currentUser != null && tourBookingRepo.existsByTourIdAndUserIdAndStartDate(tour_id, currentUser.getUser_id(), start_date)) {
            tour_bookings tourBooking = tourBookingRepo.findByIdAndUserIdAndStartDate(tour_id, currentUser.getUser_id(), start_date);
            if (tourBooking.getStatus().equals("Pending payment")) {
                modelAndView.addObject("booking_id", tourBookingRepo.findByIdAndUserIdAndStartDate(tour_id, tourBooking.getUser_id(), start_date).getBooking_id());
            }
        }
            tours tour = tourOptional.get();
            LocalDate endDate = start_date.plusDays(tour.getTour_duration() - 1);

            // Find and add active promotion
            KhuyenMai activePromotion = tour.getDiscount_promotion();
            modelAndView.addObject("discount_promotion", activePromotion);


            // --- NEW RANK DISCOUNT LOGIC ---
            double rankDiscountPercentage = 0.0;
            // Get user_id from currentUser directly, after null check
            String user_id = (currentUser != null) ? currentUser.getUser_id() : null;
            log.info("DEBUG: clientBooking - user_id for rank check: {}", user_id);

            if (user_id != null) {
                Rank userRank = rankRepository.findByUser_id(user_id);
                if (userRank != null) {
                    log.info("DEBUG: clientBooking - User Rank found: {}", userRank.getRank());
                    if ("Bạc".equals(userRank.getRank())) {
                        rankDiscountPercentage = 0.05; // 5%
                    } else if ("Vàng".equals(userRank.getRank())) {
                        rankDiscountPercentage = 0.15; // 15%
                    }
                    modelAndView.addObject("userRank", userRank);
                } else {
                    log.info("DEBUG: clientBooking - No Rank object found for user_id: {}", user_id);
                }
            }
        log.info("DEBUG: clientBooking - Final rankDiscountPercentage: {}", rankDiscountPercentage);
        modelAndView.addObject("rankDiscountPercentage", rankDiscountPercentage);
            // --- END NEW RANK DISCOUNT LOGIC ---

            modelAndView.addObject("tourPics", tourPicRepo.findByTourId(tour_id));
            modelAndView.addObject("start_date", start_date);
            modelAndView.addObject("end_date", endDate);
            modelAndView.addObject("tour", tour);
            // Use currentUser.getUser_id() here
            if (currentUser != null) {
                modelAndView.addObject("vouchers", voucherService.findByUserId(currentUser.getUser_id()));
            }
            modelAndView.setViewName("client_html/order_booking");
            return modelAndView;
        }

    @PostMapping("/addBooking")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addBooking(@RequestBody TourBookingRequest request) {
        Map<String, Object> response = new HashMap<>();
        log.info("tourBooking info",request.getTotal_price());
        try {
            // Re-validate voucher on server-side before booking
            int discountValue = 0;
            String voucherType = "";
            if (request.getVoucherCode() != null && !request.getVoucherCode().isEmpty()) {
                Map<String, Object> validationResult = voucherService.validateVoucher(request.getVoucherCode(), request.getUser_id(), request.getTour_id());
                if ((boolean) validationResult.get("success")) {
                    discountValue = (int) validationResult.get("discountValue");
                    voucherType = (String) validationResult.get("voucherType");
                } else {
                    // If voucher is invalid, stop the booking process
                    response.put("success", false);
                    response.put("message", "Voucher không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            tour_bookings booking = bookingService.addBooking(
                    request.getTour_id(),
                    request.getUser_id(),
                    request.getUser_full_name(),
                    request.getUser_email(),
                    request.getUser_phone_number(),
                    request.getUser_address(),
                    request.getTour_name(),
                    request.getStart_date(),
                    request.getTotal_price(), // Use the price from the request
                    request.getNumber_of_adults(),
                    request.getNumber_of_children(),
                    request.getNumber_of_infants(),
                    discountValue, // Save the applied discount value
                    request.getNote(),
                    request.getBookingType()
            );

            // Mark voucher as used if applied
            if (discountValue > 0) {
                voucherService.markVoucherAsUsed(request.getVoucherCode());
            }

            if (request.getBookingType().equals("group")) {
                updateNumGuest(request.getTour_id(), request.getStart_date(), request.getNumber_of_adults() + request.getNumber_of_children() + request.getNumber_of_infants());

            }

            response.put("success", true);
            response.put("booking_id", booking.getBooking_id());
            response.put("message", "Đặt tour thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi đặt tour: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @PostMapping("/cancelBooking/{booking_id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBooking(@PathVariable Integer booking_id, @RequestBody String cancel_reason) {
        Optional<tour_bookings> booking = tourBookingRepo.findById(booking_id);

        booking.get().setStatus("Cancelled");
        booking.get().setCancel_reason(cancel_reason);
        tourBookingRepo.save(booking.get());

        bookingService.sendCancelNotification(booking.get().getUser_email(), booking_id);

        if (booking.get().getBookingType() == "group") {
            updateNumGuest(booking.get().getTour().getTour_id(), booking.get().getStart_date(), -(booking.get().getNumber_of_adults() + booking.get().getNumber_of_children() + booking.get().getNumber_of_infants()));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/Payment/{booking_id}")
    public ModelAndView home(@PathVariable Integer booking_id,ModelAndView modelAndView){
        getCurrentUser(modelAndView);
        Optional<tour_bookings> booking = tourBookingRepo.findById(booking_id);

        String bookingStatus = booking.get().getStatus();

        if (bookingStatus.equals("Cancelled")) {
            return new ModelAndView("redirect:/Client/orderBooking/booking_cancelled/"+booking_id);
        } else if (bookingStatus.equals("Paid")||bookingStatus.equals("Cash")) {
            return new ModelAndView("redirect:/Client/orderBooking/booking_success/"+booking_id);
        }

        modelAndView.addObject("booking",booking.get());
        modelAndView.addObject("tour",tourRepo.findById(booking.get().getTour().getTour_id()).get());
        modelAndView.addObject("start_date",booking.get().getStart_date());
        modelAndView.setViewName("client_html/payment_method");
        return modelAndView;
    }
    @GetMapping("/booking_success/{booking_id}")
    public ModelAndView success(@PathVariable Integer booking_id, ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        modelAndView.addObject("booking",tourBookingRepo.findById(booking_id).get());
        modelAndView.setViewName("client_html/booking_success");
        return modelAndView;
    }
    @GetMapping("/payment_failed/{booking_id}")
    public ModelAndView failed(@PathVariable Integer booking_id,ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        modelAndView.addObject("booking",tourBookingRepo.findById(booking_id).get());
        modelAndView.setViewName("client_html/payment_failed");
        return modelAndView;
    }
    @GetMapping("/booking_cancelled/{booking_id}")
    public ModelAndView cancel(@PathVariable Integer booking_id,ModelAndView modelAndView) {
        getCurrentUser(modelAndView);
        modelAndView.addObject("booking",tourBookingRepo.findById(booking_id).get());
        modelAndView.setViewName("client_html/booking_cancelled");
        return modelAndView;
    }
    @GetMapping("/cash_payment/{booking_id}")
    public ResponseEntity<Map<String, Object>> pending(@PathVariable Integer booking_id) {
        Map<String, Object> response = new HashMap<>();
        tourBookingRepo.findById(booking_id).ifPresent(booking -> {
            booking.setStatus("Cash");
            tourBookingRepo.save(booking);
            bookingService.sendCashPaymentNotification(booking.getUser_email(), booking_id);
        });
        response.put("success", true);
        response.put("booking_id", booking_id);
        response.put("message", "Đặt tour thành công");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/confirm_cash_payment/{booking_id}")
    public ResponseEntity<Map<String, Object>> confirmCashPayment(@PathVariable Integer booking_id) {
        Map<String, Object> response = new HashMap<>();
        tourBookingRepo.findById(booking_id).ifPresent(booking -> {
            booking.setStatus("Paid");
            tourBookingRepo.save(booking);
            bookingService.sendPaymentSuccessNotification(booking.getUser_email(), booking_id);
        });
        response.put("success", true);
        response.put("booking_id", booking_id);
        response.put("message", "Thanh toán thanh cong");
        return ResponseEntity.ok(response);
    }
}
