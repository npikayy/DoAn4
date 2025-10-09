package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping
    public ModelAndView customers(ModelAndView modelAndView) {
        modelAndView.setViewName("admin_html/customer/customers_management");
        modelAndView.addObject("customers", userRepo.findAllUsers());
        return modelAndView;
    }
    @GetMapping("/customer_detail/{user_id}")
    public ModelAndView customerDetail(ModelAndView modelAndView, @PathVariable String user_id) {
        modelAndView.setViewName("admin_html/customer/customer_detail");
        modelAndView.addObject("customer", userRepo.findById(user_id).get());
        modelAndView.addObject("bookings", tourBookingRepo.findByUserId(user_id));
        return modelAndView;
    }
}
