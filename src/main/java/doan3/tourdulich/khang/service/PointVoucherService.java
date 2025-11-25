package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.PointVoucher;
import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.entity.tours; // New
import doan3.tourdulich.khang.entity.pointHistory; // New
import doan3.tourdulich.khang.entity.PointHistoryType; // New
import doan3.tourdulich.khang.repository.PointVoucherRepository;
import doan3.tourdulich.khang.repository.RankRepository;
import doan3.tourdulich.khang.repository.tourRepo; // New
import doan3.tourdulich.khang.repository.pointHistoryRepository; // New
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime; // New
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PointVoucherService {

    private final PointVoucherRepository pointVoucherRepository;
    private final RankRepository rankRepository;
    private final VoucherService voucherService;
    private final tourRepo tourRepository; // New
    private final pointHistoryRepository pointHistoryRepository; // New

    public PointVoucher savePointVoucher(PointVoucher pointVoucher) {
        return pointVoucherRepository.save(pointVoucher);
    }

    public List<PointVoucher> getAllPointVouchers() {
        return pointVoucherRepository.findAll();
    }

    public Optional<PointVoucher> getPointVoucherById(Integer id) {
        return pointVoucherRepository.findById(id);
    }

    public void deletePointVoucher(Integer id) {
        pointVoucherRepository.deleteById(id);
    }

    public String redeemVoucher(users user, Integer pointVoucherId) {
        Optional<PointVoucher> optionalPointVoucher = pointVoucherRepository.findById(pointVoucherId);
        if (optionalPointVoucher.isEmpty()) {
            return "Voucher không hợp lệ.";
        }

        PointVoucher pointVoucher = optionalPointVoucher.get();
        Rank userRank = rankRepository.findByUser_id(user.getUser_id());

        if (userRank == null) {
            return "Không tìm thấy thông tin hạng của người dùng.";
        }

        // Check redemption limits
        Map<String, Integer> voucherLimits = Map.of("Đồng", 3, "Bạc", 5, "Vàng", 10);
        int limit = voucherLimits.getOrDefault(userRank.getRank(), 0);

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        int redeemedThisMonth = pointHistoryRepository.countByUserAndTypeAndDescriptionStartingWithAndCreationDateBetween(
                user.getUser_id(),
                PointHistoryType.REDEMPTION,
                "Đổi voucher:%",
                startOfMonth,
                endOfMonth
        );

        if (redeemedThisMonth >= limit) {
            return "Bạn đã đạt giới hạn đổi voucher tháng này cho hạng của mình (" + redeemedThisMonth + "/" + limit + ").";
        }

        if (userRank.getPoints() < pointVoucher.getPointsCost()) {
            return "Bạn không đủ điểm để đổi voucher này.";
        }

        if (pointVoucher.getQuantity() <= 0) { // New check
            return "Voucher này đã hết số lượng.";
        }

        userRank.setPoints(userRank.getPoints() - pointVoucher.getPointsCost());
        rankRepository.save(userRank);

        pointVoucher.setQuantity(pointVoucher.getQuantity() - 1); // Decrement quantity
        pointVoucherRepository.save(pointVoucher); // Save updated pointVoucher

        // Create point history entry for redemption
        pointHistory history = pointHistory.builder()
                .user(user)
                .pointsChange(-pointVoucher.getPointsCost()) // Negative for redemption
                .description("Đổi voucher: " + pointVoucher.getName())
                .type(PointHistoryType.REDEMPTION)
                .creationDate(LocalDateTime.now())
                .build();
        pointHistoryRepository.save(history);

        vouchers newVoucher = new vouchers();
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        newVoucher.setMaVoucher(code);
        newVoucher.setTrangThai("ACTIVE");
        newVoucher.setUser(user);

        newVoucher.setVoucherType(pointVoucher.getVoucherType());
        newVoucher.setGiaTriGiam(pointVoucher.getGiaTriGiam());

        // Expiry date? The original ExchangeVoucher didn't have it.
        // I will set it to 30 days from now.
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 30);
        newVoucher.setNgayHetHan(new java.sql.Date(cal.getTimeInMillis()));

        voucherService.saveVoucher(newVoucher);

        return "Đổi voucher thành công!";
    }
        public String redeemTour(users user, String tourId) {
        Optional<tours> optionalTour = tourRepository.findById(tourId);
        if (optionalTour.isEmpty()) {
            return "Tour không hợp lệ.";
        }

        tours tour = optionalTour.get();
        Rank userRank = rankRepository.findByUser_id(user.getUser_id());

        if (userRank == null) {
            return "Không tìm thấy thông tin hạng của người dùng.";
        }

        // Check redemption limits
        Map<String, Integer> tourLimits = Map.of("Bạc", 1, "Vàng", 2);
        int limit = tourLimits.getOrDefault(userRank.getRank(), 0);

        if (limit == 0) {
            return "Hạng của bạn không được phép đổi tour bằng điểm.";
        }

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        int redeemedThisMonth = pointHistoryRepository.countByUserAndTypeAndDescriptionStartingWithAndCreationDateBetween(
                user.getUser_id(),
                PointHistoryType.REDEMPTION,
                "Đổi tour:%",
                startOfMonth,
                endOfMonth
        );

        if (redeemedThisMonth >= limit) {
            return "Bạn đã đạt giới hạn đổi tour tháng này cho hạng của mình (" + redeemedThisMonth + "/" + limit + ").";
        }

        int requiredPoints = tour.getTour_adult_price() / 10000; // Assuming tour_adult_price is accessible
        if (userRank.getPoints() < requiredPoints) {
            return "Bạn không đủ điểm để đổi tour này.";
        }

        userRank.setPoints(userRank.getPoints() - requiredPoints);
        rankRepository.save(userRank);

        // Create point history entry for redemption
        pointHistory history = pointHistory.builder()
                .user(user)
                .pointsChange(-requiredPoints) // Negative for redemption
                .description("Đổi tour: " + tour.getTour_name())
                .type(PointHistoryType.REDEMPTION)
                .creationDate(LocalDateTime.now())
                .build();
        pointHistoryRepository.save(history);

        vouchers newVoucher = new vouchers();
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        newVoucher.setMaVoucher(code);
        newVoucher.setVoucherType("PERCENTAGE");
        newVoucher.setGiaTriGiam(100); // 100% discount
        newVoucher.setTrangThai("ACTIVE");
        newVoucher.setUser(user);
        newVoucher.setTour(tour); // This line was already removed as per user's previous instruction

        // Expiry date: 30 days from now
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 30);
        newVoucher.setNgayHetHan(new java.sql.Date(cal.getTimeInMillis()));

        voucherService.saveVoucher(newVoucher);

        return "Đổi tour thành công! Voucher đã được thêm vào tài khoản của bạn.";
    }

    public int countRedeemedVouchersThisMonth(String userId, LocalDateTime startOfMonth, LocalDateTime endOfMonth) {
        return pointHistoryRepository.countByUserAndTypeAndDescriptionStartingWithAndCreationDateBetween(
                userId,
                PointHistoryType.REDEMPTION,
                "Đổi voucher:%",
                startOfMonth,
                endOfMonth
        );
    }

    public int countRedeemedToursThisMonth(String userId, LocalDateTime startOfMonth, LocalDateTime endOfMonth) {
        return pointHistoryRepository.countByUserAndTypeAndDescriptionStartingWithAndCreationDateBetween(
                userId,
                PointHistoryType.REDEMPTION,
                "Đổi tour:%",
                startOfMonth,
                endOfMonth
        );
    }
}
