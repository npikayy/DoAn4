package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.entity.KhuyenMai;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.service.KhuyenMaiService;
import doan3.tourdulich.khang.service.tourService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class KhuyenMaiAdminController {

    private final KhuyenMaiService khuyenMaiService;
    private final tourService tourService;



    @GetMapping
    public String showKhuyenMaiManagement(Model model,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<KhuyenMai> khuyenMaiList = khuyenMaiService.searchKhuyenMai(name, status, startDate, endDate);
        model.addAttribute("khuyenMaiList", khuyenMaiList);
        model.addAttribute("name", name);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin_html/khuyen_mai_management";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        List<tours> allTours = tourService.getToursWithoutPromotion();
        Map<String, List<tours>> groupedTours = allTours.stream().collect(Collectors.groupingBy(tours::getTour_region));
        model.addAttribute("khuyenMai", new KhuyenMai());
        model.addAttribute("groupedTours", groupedTours);
        return "admin_html/khuyen_mai_adding";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        KhuyenMai khuyenMai = khuyenMaiService.getKhuyenMaiById(id).orElseThrow(() -> new IllegalArgumentException("Invalid promotion Id:" + id));
        List<tours> toursWithoutPromotion = tourService.getToursWithoutPromotion();
        List<tours> currentPromotionTours = khuyenMai.getTours();
        toursWithoutPromotion.addAll(currentPromotionTours);
        Map<String, List<tours>> groupedTours = toursWithoutPromotion.stream().collect(Collectors.groupingBy(tours::getTour_region));
        model.addAttribute("khuyenMai", khuyenMai);
        model.addAttribute("groupedTours", groupedTours);
        return "admin_html/khuyen_mai_editing";
    }

    @PostMapping("/add")
    public String addPromotion(@ModelAttribute("khuyenMai") KhuyenMai khuyenMai, @RequestParam(value = "tourIds", required = false) List<String> tourIds, @RequestParam(value = "hinhAnh", required = false) String hinhAnh) {
        khuyenMai.setHinhAnh(hinhAnh);
        khuyenMaiService.saveKhuyenMaiWithTours(khuyenMai, tourIds);
        return "redirect:/admin/khuyen-mai";
    }

    @PostMapping("/update")
    public String updatePromotion(@ModelAttribute("khuyenMai") KhuyenMai khuyenMai, @RequestParam(value = "tourIds", required = false) List<String> tourIds) {
        // Image is handled separately, so we only save the main details here.
        // We need to preserve the existing image if a new one isn't uploaded.
        KhuyenMai existing = khuyenMaiService.getKhuyenMaiById(khuyenMai.getId()).orElseThrow();
        khuyenMai.setHinhAnh(existing.getHinhAnh());

        khuyenMaiService.saveKhuyenMaiWithTours(khuyenMai, tourIds);
        return "redirect:/admin/khuyen-mai";
    }

    @GetMapping("/image/add/{id}")
    public String showImageAddForm(@PathVariable("id") int id, Model model) {
        KhuyenMai khuyenMai = khuyenMaiService.getKhuyenMaiById(id).orElseThrow(() -> new IllegalArgumentException("Invalid promotion Id:" + id));
        model.addAttribute("khuyenMaiId", id);
        model.addAttribute("khuyenMai", khuyenMai);
        return "admin_html/khuyen_mai_image_adding";
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam("khuyenMaiId") int id) {
        Map<String, Object> response = new HashMap<>();
        try {
            KhuyenMai khuyenMai = khuyenMaiService.getKhuyenMaiById(id).orElseThrow(() -> new RuntimeException("Promotion not found"));
            String oldImageUrl = khuyenMai.getHinhAnh();

            String newImageUrl = khuyenMaiService.uploadKhuyenMaiImage(file, oldImageUrl);
            khuyenMai.setHinhAnh(newImageUrl);
            khuyenMaiService.saveKhuyenMai(khuyenMai);

            response.put("success", true);
            response.put("imageUrl", newImageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteKhuyenMai(@PathVariable("id") int id) {
        khuyenMaiService.deleteKhuyenMai(id);
        return "redirect:/admin/khuyen-mai";
    }
}
