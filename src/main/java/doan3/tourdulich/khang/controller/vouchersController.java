package doan3.tourdulich.khang.controller;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.repository.voucherRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/admin/vouchers")
public class vouchersController {
    @Autowired
    private voucherRepo voucherRepo;

    @GetMapping("")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.addObject("vouchers", voucherRepo.findAll());
        modelAndView.setViewName("admin_html/voucher/voucher_management");
        return modelAndView;
    }
    @GetMapping("/deleteVoucher/{id}")
    public ModelAndView delete(@PathVariable("id") Long id, ModelAndView modelAndView) {
        voucherRepo.deleteById(id);
        return new ModelAndView("redirect:/admin/vouchers");
    }
    @PostMapping("/addVoucher")
    public ResponseEntity<Map<String, Object>> add(@RequestBody vouchers voucher) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Received data: {}", voucher);

            voucherRepo.save(voucher);

            response.put("success", true);
            response.put("message", "Tạo voucher thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi tạo voucher: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
