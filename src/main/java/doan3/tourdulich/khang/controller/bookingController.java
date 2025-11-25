package doan3.tourdulich.khang.controller;

import org.springframework.util.StringUtils;
import doan3.tourdulich.khang.entity.tour_bookings;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.service.bookingService;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.userRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ModelAndView getAllBookings(@RequestParam(name = "status", defaultValue = "Pending payment") String status,
                                       @RequestParam(name = "userId", required = false) String userId,
                                       @RequestParam(name = "searchQuery", required = false) String searchQuery,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "9") int size) {
        ModelAndView modelAndView = new ModelAndView();
        // Sort.by("booking_date").descending() was causing an error, so it's removed for now.
        Pageable pageable = PageRequest.of(page, size);
        Page<tour_bookings> bookingPage = bookingService.findAllPaginatedAndFiltered(status, userId, searchQuery, pageable);

        modelAndView.setViewName("admin_html/booking/booking_management");
        modelAndView.addObject("bookingPage", bookingPage);

        // Add filter and pagination info to the model
        modelAndView.addObject("status", status);
        modelAndView.addObject("userId", userId);
        modelAndView.addObject("searchQuery", searchQuery);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", bookingPage.getTotalPages());

        // For the user filter dropdown
        List<String> distinctUsers = tourBookingRepo.findDistinctUsers();
        modelAndView.addObject("distinctUsers", userRepo.findAllById(distinctUsers));

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
        });
        response.put("success", true);
        response.put("booking_id", booking_id);
        response.put("message", "Thanh toán thanh cong");
        return ResponseEntity.ok(response);
    }
}
