package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.repository.bannerRepo;
import doan3.tourdulich.khang.service.bannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/banners")
public class bannerController {
    @Autowired
    private bannerRepo bannerRepo;

    @Autowired
    private bannerService bannerService;

    @GetMapping
    public ModelAndView banner(ModelAndView modelAndView) {
        modelAndView.setViewName("admin_html/banner_management");
        modelAndView.addObject("banners", bannerRepo.findAll());
        return modelAndView;
    }
    @PostMapping(value = "/addBanner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleFileUpload(@RequestParam("files") MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> imageUrls = bannerService.uploadBanners(files);
            response.put("success", true);
            response.put("imageUrls", imageUrls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @GetMapping("/deleteBanner/{id}")
    public ModelAndView deleteBannerPage(@PathVariable String id) throws IOException {
        bannerService.deleteBanner(Integer.valueOf(id));
        return new ModelAndView("redirect:/admin/banners");
    }
    @GetMapping("/changeStatus/{id}")
    public ModelAndView changeStatus(@PathVariable String id) throws IOException {
        bannerService.changeStatus(Integer.valueOf(id));
        return new ModelAndView("redirect:/admin/banners");
    }
}
