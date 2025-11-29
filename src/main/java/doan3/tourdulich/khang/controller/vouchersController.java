package doan3.tourdulich.khang.controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.stream.Collectors;
import doan3.tourdulich.khang.service.RankService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final RankService rankService;

    @GetMapping("")
    public ModelAndView index(@RequestParam(required = false) String voucherType,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String userId,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date expiryDateStart,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date expiryDateEnd,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "9") int size) {
        ModelAndView modelAndView = new ModelAndView("admin_html/voucher/voucher_management");
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<vouchers> voucherPage = voucherService.searchVouchers(voucherType, status, userId, expiryDateStart, expiryDateEnd, pageable);
        List<tours> tourList = tourRepo.findAll(); // New
        List<PointVoucher> pointVouchers = pointVoucherService.getAllPointVouchers(); // New
        log.info("Searching vouchers with params: voucherType={}, status={}, userId={}, expiryDateStart={}, expiryDateEnd={}", voucherType, status, userId, expiryDateStart, expiryDateEnd);
        modelAndView.addObject("voucherPage", voucherPage); // Changed from vouchers

        modelAndView.addObject("tourList", tourList); // New
        modelAndView.addObject("pointVouchers", pointVouchers); // New
        modelAndView.addObject("voucherType", voucherType);
        modelAndView.addObject("status", status);
        modelAndView.addObject("userId", userId);
        modelAndView.addObject("expiryDateStart", expiryDateStart);
        modelAndView.addObject("expiryDateEnd", expiryDateEnd);

        // Add pagination data
        modelAndView.addObject("currentPage", page);
        modelAndView.addObject("totalPages", voucherPage.getTotalPages());

        return modelAndView;
    }

    @GetMapping("/new")
    public ModelAndView newVoucherPage() {
        ModelAndView modelAndView = new ModelAndView("admin_html/voucher/voucher_creation");
        modelAndView.addObject("ranks", rankService.getDistinctRanks());
        return modelAndView;
    }

    @GetMapping("/api/users")
    @ResponseBody
    public Map<String, Object> searchUsers(@RequestParam(value = "search", required = false) String search,
                                           @RequestParam(value = "rank", required = false) String rank,
                                           Pageable pageable) {
        Page<users> userPage = userService.searchAndFilterUsers(search, rank, pageable);
        
        List<Map<String, Object>> userList = userPage.getContent().stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getUser_id());
            userMap.put("email", user.getEmail());
            userMap.put("fullName", user.getFull_name());
            userMap.put("rank", user.getRank() != null ? user.getRank().getRank() : "Chưa có");
            return userMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("results", userList);
        response.put("pagination", Map.of("more", !userPage.isLast()));
        response.put("totalPages", userPage.getTotalPages());

        return response;
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

    @PostMapping("/delete-expired")
    public String deleteExpiredVouchers(RedirectAttributes redirectAttributes) {
        voucherService.deleteExpiredVouchers();
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tất cả voucher hết hạn thành công.");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/addVoucher")
    public ModelAndView addVoucher(@RequestParam("voucherType") String voucherType,
                                   @RequestParam("giaTriGiam") String giaTriGiamStr,
                                   @RequestParam("userIds") List<String> userIds,
                                   @RequestParam("ngayHetHan") String ngayHetHan,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (userIds != null && userIds.contains("ALL_USERS")) {
                asyncVoucherService.createVouchersForAllUsers(voucherType, giaTriGiamStr, ngayHetHan);
                redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu tạo voucher cho tất cả người dùng đã được gửi. Quá trình này sẽ chạy trong nền.");
            } else if (userIds != null && !userIds.isEmpty()) {
                asyncVoucherService.createVouchersForSpecificUsers(voucherType, giaTriGiamStr, ngayHetHan, userIds);
                redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu tạo voucher cho người dùng đã chọn đã được gửi. Quá trình này sẽ chạy trong nền.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có người dùng nào được chọn để tạo voucher.");
            }
        } catch (Exception e) {
            log.error("Error dispatching voucher creation job: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi yêu cầu tạo voucher: " + e.getMessage());
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
