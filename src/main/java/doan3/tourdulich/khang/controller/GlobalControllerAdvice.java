package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.RankRepository;
import doan3.tourdulich.khang.repository.userRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private userRepo userRepository;

    @Autowired
    private RankRepository rankRepository; // Changed from GradeRepository

    @ModelAttribute("currentUser")
    public users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                username = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            }

            if (username != null) {
                return userRepository.findByUsername(username);
            }
        }
        return null;
    }

    @ModelAttribute("userRank") // Changed from currentUserGrade
    public Rank getUserRank() { // Changed method name and return type
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                username = ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttribute("email");
            }

            if (username != null) {
                users user = userRepository.findByUsername(username);
                if (user != null) {
                    return rankRepository.findByUser_id(user.getUser_id()); // Changed from gradeRepository
                }
            }
        }
        return null;
    }
}