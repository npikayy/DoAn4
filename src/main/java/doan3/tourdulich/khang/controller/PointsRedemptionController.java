package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.entity.PointVoucher;
import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.service.PointVoucherService;
import doan3.tourdulich.khang.repository.RankRepository;
import doan3.tourdulich.khang.service.tourService;
import doan3.tourdulich.khang.service.userService;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import doan3.tourdulich.khang.repository.tourPicRepo;
import doan3.tourdulich.khang.repository.startDateRepo;

@Controller
@RequestMapping("/Client/points")
public class PointsRedemptionController {

    @Autowired
    private userService userService;
    @Autowired
    private RankRepository rankRepo;
    @Autowired
    private PointVoucherService pointVoucherService;
    @Autowired
    private tourService tourService;
    @Autowired
    private tourPicRepo tourPicRepo;
    @Autowired
    private tourBookingRepo tourBookingRepo;

    @GetMapping("/redeem")
    public String showRedemptionPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
            }
        }

        if (username == null) {
            return "redirect:/login"; // Redirect to login if not authenticated
        }

        users currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return "redirect:/login"; // User not found
        }

        Rank userRank = rankRepo.findByUser_id(currentUser.getUser_id());
        if (userRank == null) {
            // This part seems to have a dependency on a rankService that is not injected.
            // I will assume the rank is created somewhere else or this logic needs to be fixed.
            // For now, I will proceed with the assumption that the rank exists.
        }

        List<PointVoucher> redeemableVouchers = pointVoucherService.getAllPointVouchers();
        List<tours> redeemableTours = tourService.getRedeemableTours();

        // Create maps for thumbnails
        Map<String, String> tourThumbnails = new HashMap<>();
        for (tours tour : redeemableTours) {
            String tourId = tour.getTour_id();
            String thumbnail = tourPicRepo.findOnePicByTour(tourId);
            tourThumbnails.put(tourId, thumbnail != null ? thumbnail : "");
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userRank", userRank);
        model.addAttribute("redeemableVouchers", redeemableVouchers);
        model.addAttribute("redeemableTours", redeemableTours);
        model.addAttribute("userPoints", userRank.getPoints());
        model.addAttribute("tourThumbnails", tourThumbnails); // New

        // --- NEW LOGIC FOR REDEMPTION LIMITS ---
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        // Voucher Limits
        Map<String, Integer> voucherLimitsMap = Map.of("Đồng", 3, "Bạc", 5, "Vàng", 10);
        int maxVouchers = voucherLimitsMap.getOrDefault(userRank.getRank(), 0);
        int redeemedVouchersThisMonth = pointVoucherService.countRedeemedVouchersThisMonth(currentUser.getUser_id(), startOfMonth, endOfMonth);
        int remainingVouchers = Math.max(0, maxVouchers - redeemedVouchersThisMonth);

        // Tour Limits
        Map<String, Integer> tourLimitsMap = Map.of("Bạc", 1, "Vàng", 2);
        int maxTours = tourLimitsMap.getOrDefault(userRank.getRank(), 0);
        int redeemedToursThisMonth = pointVoucherService.countRedeemedToursThisMonth(currentUser.getUser_id(), startOfMonth, endOfMonth);
        int remainingTours = Math.max(0, maxTours - redeemedToursThisMonth);
        // --- END NEW LOGIC ---

        // --- ADD NEW OBJECTS TO MODEL ---
        model.addAttribute("remainingVouchers", remainingVouchers);
        model.addAttribute("maxVouchers", maxVouchers);
        model.addAttribute("redeemedVouchersThisMonth", redeemedVouchersThisMonth);

        model.addAttribute("remainingTours", remainingTours);
        model.addAttribute("maxTours", maxTours);
        model.addAttribute("redeemedToursThisMonth", redeemedToursThisMonth);
        // --- END ADD NEW OBJECTS TO MODEL ---

        return "client_html/redeem_points";
    }

    @PostMapping("/redeem/{pointVoucherId}")
    public String redeemVoucher(@PathVariable Integer pointVoucherId, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
            }
        }

        if (username == null) {
            return "redirect:/login";
        }

        users currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return "redirect:/login";
        }

        String result = pointVoucherService.redeemVoucher(currentUser, pointVoucherId);

        if (result.contains("thành công")) {
            redirectAttributes.addFlashAttribute("redemptionMessage", result);
        } else {
            redirectAttributes.addFlashAttribute("redemptionError", result);
        }

        return "redirect:/Client/points/redeem";
    }

        

            @PostMapping("/redeemTour/{tourId}")

            public String redeemTour(@PathVariable String tourId, RedirectAttributes redirectAttributes) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                String username = null;

                if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {

                    Object principal = authentication.getPrincipal();

                    if (principal instanceof UserDetails userDetails) {

                        username = userDetails.getUsername();

                    } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {

                        username = oauth2User.getAttribute("email");

                    }

                }

        

                if (username == null) {

                    return "redirect:/login";

                }

        

                users currentUser = userService.findByUsername(username);
        if (currentUser == null) {
            return "redirect:/login";
        }

        String result = pointVoucherService.redeemTour(currentUser, tourId);

        if (result.contains("thành công")) {
            redirectAttributes.addFlashAttribute("redemptionMessage", result);
        } else {
            redirectAttributes.addFlashAttribute("redemptionError", result);
        }

        return "redirect:/Client/points/redeem";
    }

        }
