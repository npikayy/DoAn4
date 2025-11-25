package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;


    public List<vouchers> searchVouchers(String voucherType, String status, String userId, Date expiryDateStart, Date expiryDateEnd) {
        Specification<vouchers> spec = Specification.where(null);

        if (voucherType != null && !voucherType.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("voucherType"), voucherType));
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("trangThai"), status));
        }

        if (userId != null && !userId.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("user_id"), userId));
        }

        if (expiryDateStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ngayHetHan"), expiryDateStart));
        }

        if (expiryDateEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("ngayHetHan"), expiryDateEnd));
        }

        return voucherRepository.findAll(spec);
    }

    public List<vouchers> getAllVouchers() {
        return voucherRepository.findAll();
    }

    public List<vouchers> findByUserId(String userId) {
        return voucherRepository.findByUser(userId);
    }

    public vouchers saveVoucher(vouchers voucher) {
        return voucherRepository.save(voucher);
    }


    @Transactional
    public int updateExpiredVouchers() {
        List<vouchers> expiredVouchers = voucherRepository.findActiveAndExpiredVouchers();
        for (vouchers voucher : expiredVouchers) {
            voucher.setTrangThai("EXPIRED");
        }
        voucherRepository.saveAll(expiredVouchers);
        return expiredVouchers.size();
    }

    public void restoreVoucher(int id) {
        voucherRepository.findById(id).ifPresent(voucher -> {
            voucher.setTrangThai("ACTIVE");
            voucherRepository.save(voucher);
        });
    }

    public void revokeVoucher(int id) {
        voucherRepository.findById(id).ifPresent(voucher -> {
            voucher.setTrangThai("Đã hủy");
            voucherRepository.save(voucher);
        });
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
        if (voucher.getTour() != null) {
            if (!voucher.getTour().getTour_id().equals(tourId)) {
                response.put("success", false);
                response.put("message", "Voucher này chỉ được áp dụng cho tour: " + voucher.getTour().getTour_name() + ".");
                return response;
            }
        }

        // All checks passed
        response.put("success", true);
        response.put("message", "Áp dụng voucher thành công!");
        response.put("voucherType", voucher.getVoucherType());
        response.put("discountValue", voucher.getGiaTriGiam());
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
