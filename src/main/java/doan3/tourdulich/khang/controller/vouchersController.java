package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.entity.PointVoucher; // New
import doan3.tourdulich.khang.entity.RedeemableVoucherType; // New
import doan3.tourdulich.khang.entity.tours; // New
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.service.AsyncVoucherService;
import doan3.tourdulich.khang.service.PointVoucherService; // New
import doan3.tourdulich.khang.service.VoucherService;
import doan3.tourdulich.khang.service.bookingService;
import doan3.tourdulich.khang.repository.tourRepo; // New
import doan3.tourdulich.khang.service.userService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@Slf4j
@RequestMapping("/admin/vouchers")
@RequiredArgsConstructor
public class vouchersController {
    private final VoucherService voucherService;
    private final userService userService;
    private final bookingService bookingService;
    private final AsyncVoucherService asyncVoucherService;
    private final PointVoucherService pointVoucherService; // New
    private final tourRepo tourRepo; // New

    @GetMapping("")
    public ModelAndView index(@RequestParam(required = false) String voucherType,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String userId,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date expiryDateStart,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date expiryDateEnd) {
        ModelAndView modelAndView = new ModelAndView("admin_html/voucher/voucher_management");
        List<vouchers> voucherList = voucherService.searchVouchers(voucherType, status, userId, expiryDateStart, expiryDateEnd);
        List<users> userList = userService.getAllUsersExceptAdmin();
        List<tours> tourList = tourRepo.findAll(); // New
        List<PointVoucher> pointVouchers = pointVoucherService.getAllPointVouchers(); // New
        log.info("Searching vouchers with params: voucherType={}, status={}, userId={}, expiryDateStart={}, expiryDateEnd={}", voucherType, status, userId, expiryDateStart, expiryDateEnd);
        modelAndView.addObject("vouchers", voucherList);
        modelAndView.addObject("userList", userList);
        modelAndView.addObject("tourList", tourList); // New
        modelAndView.addObject("pointVouchers", pointVouchers); // New
        modelAndView.addObject("voucherType", voucherType);
        modelAndView.addObject("status", status);
        modelAndView.addObject("userId", userId);
        modelAndView.addObject("expiryDateStart", expiryDateStart);
        modelAndView.addObject("expiryDateEnd", expiryDateEnd);

        return modelAndView;
    }


    @GetMapping("/update-expired")
    public String updateExpiredVouchers(RedirectAttributes redirectAttributes) {
        int updatedCount = voucherService.updateExpiredVouchers();
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái cho " + updatedCount + " voucher hết hạn.");
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/restoreVoucher/{id}")
    public ModelAndView restoreVoucher(@PathVariable("id") int id) {
        voucherService.restoreVoucher(id);
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @GetMapping("/revokeVoucher/{id}")
    public ModelAndView revokeVoucher(@PathVariable("id") int id) {
        voucherService.revokeVoucher(id);
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @PostMapping("/addVoucher")
    public ModelAndView addVoucher(@RequestParam("voucherType") String voucherType,
                                   @RequestParam("giaTriGiam") String giaTriGiamStr,
                                   @RequestParam("userId") String userId,
                                   @RequestParam("ngayHetHan") String ngayHetHan) {
        try {
            if ("ALL_USERS".equals(userId)) {
                asyncVoucherService.createVouchersForAllUsers(voucherType, giaTriGiamStr, ngayHetHan);
            } else {
                int giaTriGiam = Integer.parseInt(giaTriGiamStr.replace(".", ""));
                vouchers newVoucher = new vouchers();
                String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                newVoucher.setMaVoucher(code);
                newVoucher.setVoucherType(voucherType);
                newVoucher.setGiaTriGiam(giaTriGiam);
                newVoucher.setNgayHetHan(java.sql.Date.valueOf(ngayHetHan));
                newVoucher.setTrangThai("ACTIVE");
                userService.findByUserId(userId).ifPresent(newVoucher::setUser);
                vouchers savedVoucher = voucherService.saveVoucher(newVoucher);
            }
        } catch (Exception e) {
            log.error("Error creating voucher: {}", e.getMessage());
        }
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @PostMapping("/addPointVoucher")
    public ModelAndView addPointVoucher(@RequestParam("name") String name,
                                        @RequestParam("pointsCost") Integer pointsCost,
                                        @RequestParam("quantity") Integer quantity, // New
                                        @RequestParam(value = "voucherType", required = false) String voucherType, // PERCENTAGE or AMOUNT
                                        @RequestParam(value = "giaTriGiam", required = false) String giaTriGiamStr) { // Discount value
        try {
            PointVoucher newPointVoucher = new PointVoucher();
            newPointVoucher.setName(name);
            newPointVoucher.setPointsCost(pointsCost);
            newPointVoucher.setQuantity(quantity); // New
            newPointVoucher.setRedeemableVoucherType(RedeemableVoucherType.DISCOUNT); // Always DISCOUNT

            newPointVoucher.setVoucherType(voucherType);
            if (giaTriGiamStr != null && !giaTriGiamStr.isEmpty()) {
                newPointVoucher.setGiaTriGiam(Integer.parseInt(giaTriGiamStr.replace(".", "")));
            }
            pointVoucherService.savePointVoucher(newPointVoucher);
        } catch (Exception e) {
            log.error("Error creating point voucher: {}", e.getMessage());
        }
        return new ModelAndView("redirect:/admin/vouchers");
    }

    @GetMapping("/deletePointVoucher/{id}")
    public ModelAndView deletePointVoucher(@PathVariable("id") int id) {
        pointVoucherService.deletePointVoucher(id);
        return new ModelAndView("redirect:/admin/vouchers");
    }


}
