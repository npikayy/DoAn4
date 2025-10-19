package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.entity.KhuyenMai;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.service.KhuyenMaiService;
import doan3.tourdulich.khang.service.VoucherService;
import doan3.tourdulich.khang.service.userService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/vouchers")
@RequiredArgsConstructor
public class vouchersController {
    private final VoucherService voucherService;
    private final KhuyenMaiService khuyenMaiService;
    private final userService userService;

    @GetMapping("")
    public ModelAndView index(ModelAndView modelAndView) {
        List<vouchers> voucherList = voucherService.getAllVouchers();
        List<KhuyenMai> khuyenMaiList = khuyenMaiService.getAllKhuyenMai();
        List<users> userList = userService.getAllUsers();

        modelAndView.addObject("vouchers", voucherList);
        modelAndView.addObject("khuyenMaiList", khuyenMaiList);
        modelAndView.addObject("userList", userList);

        modelAndView.setViewName("admin_html/voucher/voucher_management");
        return modelAndView;
    }
    @GetMapping("/deleteVoucher/{id}")
    public ModelAndView delete(@PathVariable("id") int id, ModelAndView modelAndView) {
        voucherService.deleteVoucher(id);
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @PostMapping("/addVoucher")
    public ModelAndView addVoucher(@RequestParam("khuyenMaiId") int khuyenMaiId,
                                   @RequestParam("userId") String userId,
                                   @RequestParam("ngayHetHan") String ngayHetHan) {
        try {
            vouchers newVoucher = new vouchers();

            // Generate a unique voucher code
            String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            newVoucher.setMaVoucher(code);

            // Set expiry date
            newVoucher.setNgayHetHan(java.sql.Date.valueOf(ngayHetHan));

            // Set status
            newVoucher.setTrangThai("ACTIVE");

            // Set relationships
            khuyenMaiService.getKhuyenMaiById(khuyenMaiId).ifPresent(newVoucher::setKhuyenMai);
            userService.findByUserId(userId).ifPresent(newVoucher::setUser);

            voucherService.saveVoucher(newVoucher);

        } catch (Exception e) {
            log.error("Error creating voucher: {}", e.getMessage());
        }
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @PostMapping("/api/validate")
    public ResponseEntity<Map<String, Object>> validateVoucher(@RequestBody Map<String, String> payload) {
        String maVoucher = payload.get("maVoucher");
        String tourId = payload.get("tourId");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để sử dụng voucher.");
            return ResponseEntity.status(401).body(response);
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        users currentUser = userService.findByUsername(userDetails.getUsername());

        Map<String, Object> validationResult = voucherService.validateVoucher(maVoucher, currentUser.getUser_id(), tourId);
        return ResponseEntity.ok(validationResult);
    }
}
