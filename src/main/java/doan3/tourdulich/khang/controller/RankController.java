package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.RankRepository;
import doan3.tourdulich.khang.repository.pointHistoryRepository;
import doan3.tourdulich.khang.repository.rankHistoryRepository;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.service.RankService; // Changed from GradeService
import doan3.tourdulich.khang.service.bookingService;
import doan3.tourdulich.khang.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/Client")
public class RankController {

    @Autowired
    private userService userService;
    @Autowired
    private RankRepository rankRepository; // Changed from GradeRepository
    @Autowired
    private RankService rankService; // Added RankService
    @Autowired
    private tourBookingRepo bookingRepo;
    @Autowired
    private rankHistoryRepository rankHistoryRepo;
    @Autowired
    private pointHistoryRepository pointHistoryRepository;
    @Autowired
    private clientInfoController clientInfoController;

    @GetMapping("/rank-system")
    public ModelAndView rankSystem(ModelAndView model) {
        model.setViewName("/client_html/rank_system");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = authentication != null && authentication.isAuthenticated();
        if (isLoggedIn) {
            String username = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                username = oauth2User.getAttribute("email");
            }

            if (username != null) {
                users user = userService.findByUsername(username);
                if (user != null) {
                    Rank rank = rankRepository.findByUser_id(user.getUser_id());
                    if (rank == null) {
                        rankService.createDefaultRank(user);
                        rank = rankRepository.findByUser_id(user.getUser_id()); // Fetch newly created rank
                    }
                    int completedTours = bookingRepo.countByUser_idAndStatus(user.getUser_id(), "Completed"); // Get completedTours dynamically

                    // Calculate spending
                    Integer totalSpend = bookingRepo.findTotalSpendByUserIdAndStatus(user.getUser_id(), "Completed");
                    if (totalSpend == null) {
                        totalSpend = 0;
                    }

                    // --- START NEW PROGRESS CALCULATION LOGIC ---
                    final int SILVER_TOUR_REQ = 10;
                    final int GOLD_TOUR_REQ = 20;
                    final long SILVER_SPEND_REQ = 35000000;
                    final long GOLD_SPEND_REQ = 80000000;

                    long tourProgressPercentage = 0;
                    long spendProgressPercentage = 0;

                    String nextRankText = "";
                    int toursToNextTier = 0;
                    int toursNeededForNextRank = 0;
                    long spendToNextTier = 0;
                    long spendNeededForNextRank = 0;

                    if (rank.getRank().equals("Vàng")) {
                        nextRankText = "Đã đạt hạng cao nhất";
                        tourProgressPercentage = 100;
                        spendProgressPercentage = 100;
                        toursToNextTier = completedTours;
                        toursNeededForNextRank = 0;
                        spendToNextTier = totalSpend;
                        spendNeededForNextRank = 0;

                    } else if (rank.getRank().equals("Bạc")) {
                        nextRankText = "Vàng";
                        toursToNextTier = GOLD_TOUR_REQ;
                        toursNeededForNextRank = Math.max(0, toursToNextTier - completedTours);
                        spendToNextTier = GOLD_SPEND_REQ;
                        spendNeededForNextRank = Math.max(0, GOLD_SPEND_REQ - totalSpend);

                        long tourProgress = Math.max(0, completedTours - SILVER_TOUR_REQ);
                        long tourTierDistance = GOLD_TOUR_REQ - SILVER_TOUR_REQ;
                        if (tourTierDistance > 0) {
                            tourProgressPercentage = Math.min(100, (tourProgress * 100L) / tourTierDistance);
                        }

                        long spendProgress = Math.max(0, totalSpend - SILVER_SPEND_REQ);
                        long spendTierDistance = GOLD_SPEND_REQ - SILVER_SPEND_REQ;
                        if (spendTierDistance > 0) {
                            spendProgressPercentage = Math.min(100, (spendProgress * 100L) / spendTierDistance);
                        }

                    } else { // Đồng or Chưa có
                        nextRankText = "Bạc";
                        toursToNextTier = SILVER_TOUR_REQ;
                        toursNeededForNextRank = Math.max(0, toursToNextTier - completedTours);
                        spendToNextTier = SILVER_SPEND_REQ;
                        spendNeededForNextRank = Math.max(0, SILVER_SPEND_REQ - totalSpend);

                        if (SILVER_TOUR_REQ > 0) {
                            tourProgressPercentage = Math.min(100, (completedTours * 100L) / SILVER_TOUR_REQ);
                        }
                        if (SILVER_SPEND_REQ > 0) {
                            spendProgressPercentage = Math.min(100, (totalSpend * 100L) / SILVER_SPEND_REQ);
                        }
                    }

                    model.addObject("userRank", rank);
                    model.addObject("completedTours", completedTours);
                    model.addObject("nextRank", nextRankText);
                    model.addObject("toursToNextTier", toursToNextTier);
                    model.addObject("toursNeededForNextRank", toursNeededForNextRank);
                    model.addObject("userPoints", rank.getPoints());
                    model.addObject("rankHistory", rankHistoryRepo.findByUser_id(user.getUser_id()));
                    model.addObject("pointHistory", pointHistoryRepository.findByUser_id(user.getUser_id()));
                    model.addObject("totalSpend", totalSpend);
                    model.addObject("spendToNextTier", spendToNextTier);
                    model.addObject("spendNeededForNextRank", spendNeededForNextRank);
                    model.addObject("tourProgressPercentage", tourProgressPercentage);
                    model.addObject("spendProgressPercentage", spendProgressPercentage);
                    // --- END NEW LOGIC ---

                }
            }
        }
        return model;
    }
}
