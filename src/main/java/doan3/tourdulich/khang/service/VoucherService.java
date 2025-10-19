package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.KhuyenMai;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public List<vouchers> getAllVouchers() {
        return voucherRepository.findAll();
    }

    public List<vouchers> findByUserId(String userId) {
        return voucherRepository.findByUser(userId);
    }

    public vouchers saveVoucher(vouchers voucher) {
        return voucherRepository.save(voucher);
    }

    public void deleteVoucher(int id) {
        voucherRepository.deleteById(id);
    }

    public Map<String, Object> validateVoucher(String maVoucher, String userId, String tourId) {
        Map<String, Object> response = new HashMap<>();
        Optional<vouchers> voucherOpt = voucherRepository.findByMaVoucher(maVoucher);

        // 1. Check if voucher exists
        if (voucherOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Mã voucher không hợp lệ.");
            return response;
        }

        vouchers voucher = voucherOpt.get();

        // 2. Check if it's assigned to the correct user
        if (voucher.getUser() == null || !voucher.getUser().getUser_id().equals(userId)) {
            response.put("success", false);
            response.put("message", "Bạn không thể sử dụng voucher này.");
            return response;
        }

        // 3. Check status
        if (!"ACTIVE".equalsIgnoreCase(voucher.getTrangThai())) {
            response.put("success", false);
            response.put("message", "Voucher đã được sử dụng hoặc đã hết hiệu lực.");
            return response;
        }

        // 4. Check expiry date
        if (voucher.getNgayHetHan().before(new Date())) {
            response.put("success", false);
            response.put("message", "Voucher đã hết hạn.");
            return response;
        }

        // 5. Check if the tour is eligible for the promotion
        KhuyenMai promotion = voucher.getKhuyenMai();
        if (promotion == null) {
            response.put("success", false);
            response.put("message", "Voucher không liên kết với chương trình khuyến mãi nào.");
            return response;
        }

        boolean tourEligible = false;
        for (tours tour : promotion.getTours()) {
            if (tour.getTour_id().equals(tourId)) {
                tourEligible = true;
                break;
            }
        }

        if (!tourEligible) {
            response.put("success", false);
            response.put("message", "Voucher không áp dụng cho tour này.");
            return response;
        }

        // All checks passed
        response.put("success", true);
        response.put("message", "Áp dụng voucher thành công!");
        response.put("discountPercentage", promotion.getPhanTramGiamGia());
        response.put("maVoucher", voucher.getMaVoucher());
        return response;
    }

    public void markVoucherAsUsed(String maVoucher) {
        voucherRepository.findByMaVoucher(maVoucher).ifPresent(voucher -> {
            voucher.setTrangThai("USED");
            voucherRepository.save(voucher);
        });
    }
}
