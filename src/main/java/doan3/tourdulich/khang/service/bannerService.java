package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.carouselBanner;
import doan3.tourdulich.khang.entity.tour_pictures;
import doan3.tourdulich.khang.repository.bannerRepo;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class bannerService {
    @Autowired
    private bannerRepo bannerRepo;
    static final String uploadDir = "src/main/resources/static/uploads/banner";
    public List<String> uploadBanners(MultipartFile[] files) {
        List<String> fileNames = new ArrayList<>();

        try {
            // Tạo thư mục nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Xử lý từng file
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                // Tạo tên file unique
                String fileName = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(file.getOriginalFilename());

                // Lưu file
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                fileName = "/uploads/banner/" + fileName;
                fileNames.add("/uploads/banner/" + fileName);

                bannerRepo.save(
                        carouselBanner.builder()
                                .banner_url(fileName)
                                .is_showed(true)
                                .build()
                );
            }

            return fileNames;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi upload hình ảnh: " + e.getMessage());
        }
    }
    public void deleteBanner(Integer id) {
        carouselBanner banner = bannerRepo.findById(id).get();
        String bannerUrl = banner.getBanner_url().replace("/uploads/banner/", "");
        try {
            // Xóa tài liệu liên quan đến tour
            bannerRepo.deleteById(id);
            Path tourPath = Paths.get(uploadDir + "/" + bannerUrl);
            if (Files.exists(tourPath)) {
                Files.walk(tourPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Lỗi khi xoá file: " + path, e);
                            }
                        });
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống khi xử lý xoá tài liệu: " + e.getMessage(), e);
        }
    }
    public void changeStatus(Integer id) {
        carouselBanner banner = bannerRepo.findById(id).get();
        banner.setIs_showed(!banner.getIs_showed());
        bannerRepo.save(banner);
    }
}
