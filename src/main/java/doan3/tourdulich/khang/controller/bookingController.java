package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.service.bookingService;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.userRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tourBookings")
public class bookingController {
    @Autowired
    private bookingService bookingService;
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private userRepo userRepo;

    @GetMapping
    public ModelAndView getAllBookings(ModelAndView modelAndView) {
        modelAndView.setViewName("admin_html/booking/booking_management");
        // Lấy danh sách người dùng riêng biệt cho dropdown
        List<String> distinctUsers = tourBookingRepo.findDistinctUsers();
        modelAndView.addObject("distinctUsers", userRepo.findAllById(distinctUsers));
        modelAndView.addObject("bookings", tourBookingRepo.findAll());
        return modelAndView;
    }
    @GetMapping("/detail/{booking_id}")
    public ModelAndView getBookingDetail(@PathVariable Integer booking_id, ModelAndView modelAndView) {
        modelAndView.setViewName("admin_html/booking/booking_detail");
        modelAndView.addObject("booking", tourBookingRepo.findById(booking_id).get());
        return modelAndView;
    }
    @PostMapping("/confirm_completed/{booking_id}")
    public ResponseEntity<Map<String, Object>> confirmCashPayment(@PathVariable Integer booking_id) {
        Map<String, Object> response = new HashMap<>();
        tourBookingRepo.findById(booking_id).ifPresent(booking -> {
            booking.setStatus("Completed");
            tourBookingRepo.save(booking);
            bookingService.sendThankYouEmail(booking.getUser_email(), booking_id);
        });
        response.put("success", true);
        response.put("booking_id", booking_id);
        response.put("message", "Thanh toán thanh cong");
        return ResponseEntity.ok(response);
    }
}
