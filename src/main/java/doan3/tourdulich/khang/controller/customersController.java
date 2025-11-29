package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.pointHistoryRepository;
import doan3.tourdulich.khang.repository.rankHistoryRepository;
import doan3.tourdulich.khang.repository.ratingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/admin/customers")
public class customersController {
    @Autowired
    private userService userService;
    @Autowired
    private userRepo userRepo;
    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private pointHistoryRepository pointHistoryRepository;
    @Autowired
    private rankHistoryRepository rankHistoryRepository;
    @Autowired
    private ratingRepo ratingRepo;
    @Autowired
    private doan3.tourdulich.khang.service.bookingService bookingService;

    @GetMapping
    public ModelAndView customers(ModelAndView modelAndView,
                                  @RequestParam(required = false) String searchQuery,
                                  @RequestParam(required = false) String gender,
                                  @RequestParam(required = false) String ageRange,
                                  @RequestParam(required = false) String registrationDate,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "4") int size) {
        modelAndView.setViewName("admin_html/customer/customers_management");
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page customerPage = userService.findFilteredUsers(searchQuery, gender, ageRange, registrationDate,null, pageable);
        modelAndView.addObject("customerPage", customerPage);
        
        // Add filter params to model to set the state of the inputs on the frontend
        modelAndView.addObject("searchQuery", searchQuery);
        modelAndView.addObject("gender", gender);
        modelAndView.addObject("ageRange", ageRange);
        modelAndView.addObject("registrationDate", registrationDate);

        // Add pagination data
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", customerPage.getTotalPages());
        
        return modelAndView;
    }
    @GetMapping("/customer_detail/{user_id}")
    public ModelAndView customerDetail(
                                       @PathVariable String user_id,
                                       @RequestParam(name = "status", required = false) String status,
                                       @RequestParam(name = "searchQuery", required = false) String searchQuery,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "4") int size) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin_html/customer/customer_detail");
        modelAndView.addObject("customer", userRepo.findByIdWithRank(user_id));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<doan3.tourdulich.khang.entity.tour_bookings> bookingPage = bookingService.findAllPaginatedAndFilteredForUser(status, user_id, searchQuery, pageable);

        modelAndView.addObject("bookingPage", bookingPage);
        modelAndView.addObject("pointHistory", pointHistoryRepository.findByUser_id(user_id));
        modelAndView.addObject("rankHistory", rankHistoryRepository.findByUser_id(user_id));
        modelAndView.addObject("reviews", ratingRepo.findRatingByUserId(user_id));
        modelAndView.addObject("completedTours", tourBookingRepo.countByUser_idAndStatus(user_id, "Completed"));
        
        // Add filter and pagination info to the model
        modelAndView.addObject("status", status);
        modelAndView.addObject("searchQuery", searchQuery);
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", bookingPage.getTotalPages());

        return modelAndView;
    }

    @PostMapping("/{id}/terminate")
    public ModelAndView terminateMembership(@PathVariable("id") String id) {
        userService.terminateMembership(id);
        return new ModelAndView("redirect:/admin/customers/customer_detail/" + id);
    }
}
