package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.repository.bannerRepo;
import doan3.tourdulich.khang.repository.userRepo;
import doan3.tourdulich.khang.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
@RestController
public class loginController {
    @Autowired
    private userService userService;

    @Autowired
    private userRepo userRepository;

    @Autowired
    private bannerRepo bannerRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public ModelAndView register2(ModelAndView modelAndView) {
        modelAndView.setViewName("register");
        return modelAndView;
    }
    @PostMapping("/register")
    public ModelAndView register(String fullName, String username, String password, String email) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        if (username.toLowerCase().contains("admin")) {
            modelAndView.addObject("errorMessage", "Tên tài khoản không được sử dụng.");
            return modelAndView;
        }
        else if (userRepository.findByUsername(username)!= null) {
            modelAndView.addObject("errorMessage", "Tên tài khoản đã tồn tại.");
            return modelAndView;
        }
        else if (userRepository.findByEmail(email)!= null) {
            modelAndView.addObject("errorMessage", "Email đã tồn tại.");
            return modelAndView;
        }
        else if (password.length() < 8) {
            modelAndView.addObject("errorMessage", "Mật khẩu phải có ít nhất 8 ký tự.");
            return modelAndView;
        }
        else if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            modelAndView.addObject("errorMessage", "Mật khẩu không được chứa kí tự đặc biệt.");
            return modelAndView;
        }
        else if (!password.matches(".*[A-Z].*")) {
            modelAndView.addObject("errorMessage", "Mật khẩu phải chứa ít nhất 1 chữ cái in hoa.");
            return modelAndView;
        }
        else if (!password.matches(".*[a-z].*")) {
            modelAndView.addObject("errorMessage", "Mật khẩu phải chứa ít nhất 1 chữ cái in thường.");
            return modelAndView;
        }

        else {
        userService.saveUser(fullName, username, passwordEncoder.encode(password), email);
        return new ModelAndView("redirect:/login");
        }
    }
    @GetMapping("/login")
    public ModelAndView login2(@RequestParam(required = false) String error, ModelAndView modelAndView) {
        if (error != null) {
            modelAndView.addObject("errorMessage", "Tên tài khoản hoặc mật khẩu không đúng!");
        }
        modelAndView.addObject("banners", bannerRepo.findAll());
        modelAndView.setViewName("login");
        return modelAndView;
    }


}
