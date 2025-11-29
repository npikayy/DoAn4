package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.*;
import doan3.tourdulich.khang.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import doan3.tourdulich.khang.entity.PointHistoryType; // New


@Service
@Slf4j
public class userService implements UserDetailsService {
    static final String PIC_DIR = "src/main/resources/static/UserProfilePics/";
    @Autowired
    private userRepo userRepository;
    @Autowired
    private RankService rankService; // Changed from GradeService
    @Autowired
    private tourBookingRepo tourBookingRepository;
    @Autowired
    private RankRepository rankRepository; // Changed from GradeRepository
    @Autowired
    private rankHistoryRepository rankHistoryRepo; // Inject RankHistoryRepository
    @Autowired
    private EmailService emailService; // Inject EmailService
    @Autowired
    private pointHistoryRepository pointHistoryRepository;

    public void saveUser(String fullName, String username, String password, String email) {
        users user = users.builder()
                .full_name(fullName)
                .username(username)
                .password(password)
                .email(email)
                .role("ROLE_USER")
                .provider("LOCAL")
                .created_at(java.time.LocalDate.now())
                .build();
        userRepository.save(user);
        rankService.createDefaultRank(user); // Changed from gradeService.createDefaultGrade
    }

    public void addPointsForCompletedTour(tour_bookings booking) {
        Rank rank = rankRepository.findByUser_id(booking.getUser_id());
        if (rank != null) {
            if ("Đã chấm dứt".equals(rank.getRank())) {
                return; // Do not add points for terminated users
            }

            int pointsToAdd = booking.getTotal_price() / 100000;
            rank.setPoints(rank.getPoints() + pointsToAdd);
            rankRepository.save(rank);

            pointHistory history = pointHistory.builder()
                    .user(userRepository.findById(booking.getUser_id()).get())
                    .pointsChange(pointsToAdd) // Renamed
                    .description("Cộng điểm từ tour: " + booking.getTour_name()) // New
                    .type(PointHistoryType.ADDITION) // New
                    .creationDate(LocalDateTime.now())
                    .build();
            pointHistoryRepository.save(history);
        }
    }

    public Page<users> searchUsers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return userRepository.findAllUsersExceptAdmin(pageable);
        }
        return userRepository.searchUsers(searchTerm, pageable);
    }

    public void updateUserRank(users user) {
        Rank rank = rankRepository.findByUser_id(user.getUser_id()); // Changed from Grade grade
        if (rank == null || "Đã chấm dứt".equals(rank.getRank())) {
            return; // Do not update rank for terminated users or if rank is not found
        }
        String oldRankName = rank.getRank();

        int completedTours = tourBookingRepository.countByUser_idAndStatus(user.getUser_id(), "Completed");
        // rank.setCompletedTours(completedTours); // This line is removed as completedTours is no longer in Rank entity

        Integer totalSpend = tourBookingRepository.findTotalSpendByUserIdAndStatus(user.getUser_id(), "Completed");
        if (totalSpend == null) {
            totalSpend = 0;
        }

        if (completedTours >= 20 && totalSpend >= 80000000) {
            rank.setRank("Vàng");
        } else if (completedTours >= 10 && totalSpend >= 35000000) {
            rank.setRank("Bạc");
        } else if (completedTours >= 1) {
            rank.setRank("Đồng");
        } else { // If completedTours is 0
            rank.setRank("Chưa có");
        }
        rankService.saveRank(rank); // Changed from gradeService.saveGrade

        if (!oldRankName.equals(rank.getRank())) {
            rankHistory history = new rankHistory();
            history.setUser(user);
            history.setNewRank(rank.getRank());
            history.setOldRank(oldRankName);
            history.setDescription("Đã thăng hạng từ " + oldRankName + " lên " + rank.getRank());
            history.setPromotionDate(LocalDate.now());
            rankHistoryRepo.save(history); // Save rank history
            emailService.sendRankPromotionEmail(user, oldRankName, rank.getRank()); // Send email notification
        }
    }

    public Page<users> getAllUsersExceptAdmin(Pageable pageable) {
        return (Page<users>) userRepository.findAllUsersExceptAdmin(pageable);
    }

    public List<users> getAllUsersExceptAdmin() {
        List<users> users = userRepository.findAllUsers();
        return users.stream()
                .filter(user -> !"admin@admin.com".equals(user.getEmail()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<users> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<users> searchAndFilterUsers(String searchTerm, String rank, Pageable pageable) {
        Specification<users> spec = UserSpecifications.withFilters(searchTerm, null, null, null, rank);
        return userRepository.findAll(spec, pageable);
    }

    public Page<users> findFilteredUsers(String searchQuery, String gender, String ageRange, String registrationDate, String rank, org.springframework.data.domain.Pageable pageable) {
        Specification<users> spec = UserSpecifications.withFilters(searchQuery, gender, ageRange, registrationDate, rank);
        return userRepository.findAll(spec, pageable);
    }

    public void updateUserInfo(String user_id,String full_name,String phone_number,String address,String gender,String date_of_birth) {
        Optional<users> user = userRepository.findById(user_id);
        if (user.isPresent()) {
            users updatedUser = user.get();
            updatedUser.setFull_name(full_name);
            updatedUser.setPhone_number(phone_number);
            updatedUser.setAddress(address);
            updatedUser.setGender(gender);
            if (!date_of_birth.isEmpty()){
                updatedUser.setDate_of_birth(LocalDate.parse(date_of_birth));
            }
            userRepository.save(updatedUser);
        }
    }

    public Optional<users> findByUserId(String userId) {
        return userRepository.findById(userId);
    }

    public users findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void terminateMembership(String userId) {
        Optional<users> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            users user = userOptional.get();
            Rank rank = rankRepository.findByUser_id(userId);
            if (rank != null) {
                rank.setRank("Đã chấm dứt");
                rank.setPoints(0);
                rankRepository.save(rank);

                // Send termination email
                emailService.sendTerminationEmail(user);
            }
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        users user = userRepository.findByUsername(username);
        if (user == null) {
            log.error("Login failed - User not found: {}", username);
            throw new UsernameNotFoundException("Invalid username or password");
        }

        log.info("User logged in successfully: {}", username);
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().replace("ROLE_", ""))
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
