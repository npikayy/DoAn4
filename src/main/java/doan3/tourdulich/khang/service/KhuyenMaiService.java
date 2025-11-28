package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.KhuyenMai;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.repository.KhuyenMaiRepository;
import doan3.tourdulich.khang.repository.tourRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KhuyenMaiService {

    private final KhuyenMaiRepository khuyenMaiRepository;
    private final tourRepo tourRepo;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/khuyenmai";



    public org.springframework.data.domain.Page<KhuyenMai> searchKhuyenMai(String name, String status, Date startDate, Date endDate, org.springframework.data.domain.Pageable pageable) {
        Specification<KhuyenMai> spec = Specification.where(null);

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("tenKhuyenMai")), "%" + name.toLowerCase() + "%"));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("ngayBatDau"), endDate));
        }

        if (status != null && !status.isEmpty()) {
            Date now = new Date();
            if ("ongoing".equals(status)) {
                spec = spec.and((root, query, cb) -> cb.and(cb.lessThanOrEqualTo(root.get("ngayBatDau"), now), cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now)));
            } else if ("upcoming".equals(status)) {
                spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("ngayBatDau"), now));
            } else if ("finished".equals(status)) {
                spec = spec.and((root, query, cb) -> cb.lessThan(root.get("ngayKetThuc"), now));
            }
        }

        return khuyenMaiRepository.findAll(spec, pageable);
    }

    public org.springframework.data.domain.Page<KhuyenMai> findActivePromotionsPaginated(org.springframework.data.domain.Pageable pageable) {
        Date now = new Date();
        Specification<KhuyenMai> spec = Specification.where((root, query, cb) ->
            cb.and(
                cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now)
            )
        );
        return khuyenMaiRepository.findAll(spec, pageable);
    }

    public List<KhuyenMai> getAllKhuyenMai() {
        return khuyenMaiRepository.findAll();
    }

    public List<KhuyenMai> getActivePromotionsWithImages() {
        return khuyenMaiRepository.findAll();
    }

    public Optional<KhuyenMai> getKhuyenMaiById(int id) {
        return khuyenMaiRepository.findById(id);
    }

    public KhuyenMai saveKhuyenMai(KhuyenMai khuyenMai) {
        return khuyenMaiRepository.save(khuyenMai);
    }

    public String uploadKhuyenMaiImage(MultipartFile file, String oldImageUrl) {
        try {
            // Delete old image if it exists
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    String oldImageFilename = oldImageUrl.replace("/uploads/khuyenmai/", "");
                    Path oldImagePath = Paths.get(UPLOAD_DIR, oldImageFilename);
                    if (Files.exists(oldImagePath)) {
                        Files.delete(oldImagePath);
                    }
                } catch (IOException e) {
                    // Log the error, but don't fail the whole upload
                    System.err.println("Failed to delete old image: " + oldImageUrl + " with error: " + e.getMessage());
                }
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            String fileName = UUID.randomUUID().toString() + "." + org.apache.commons.io.FilenameUtils.getExtension(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/khuyenmai/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    public void saveKhuyenMaiWithTours(KhuyenMai khuyenMai, List<String> tourIds) {
        // Lấy danh sách tour hiện đang có khuyến mãi
        List<tours> tourDiscounted = tourRepo.findToursByDiscount_promotionId(khuyenMai.getId());
        khuyenMaiRepository.save(khuyenMai);
        // Xử lý khi không có tour nào được chọn
        if (tourIds == null || tourIds.isEmpty()) {
            for (tours tour : tourDiscounted) {
                tour.setDiscount_promotion(null);
                tourRepo.save(tour);
            }
            khuyenMaiRepository.save(khuyenMai); // Cập nhật khuyến mãi (ví dụ: hình ảnh)
            return;
        }

        // Lấy danh sách tour được chọn
        List<tours> selectedTours = tourRepo.findAllById(tourIds);

        // Tạo Set để kiểm tra nhanh
        Set<String> selectedTourIds = selectedTours.stream()
                .map(tours::getTour_id)
                .collect(Collectors.toSet());

        Set<String> discountedTourIds = tourDiscounted.stream()
                .map(tours::getTour_id)
                .collect(Collectors.toSet());

        // 1. Xóa khuyến mãi khỏi các tour không được chọn
        for (tours tour : tourDiscounted) {
            if (!selectedTourIds.contains(tour.getTour_id())) {
                tour.setDiscount_promotion(null);
                tourRepo.save(tour);
            }
        }

        // 2. Thêm khuyến mãi vào các tour được chọn mà chưa có
        for (tours tour : selectedTours) {
            if (!discountedTourIds.contains(tour.getTour_id())) {
                tour.setDiscount_promotion(khuyenMai);
                tourRepo.save(tour);
            }
        }

        // Lưu khuyến mãi một lần

    }

    public void deleteKhuyenMai(int id) {
        khuyenMaiRepository.findById(id).ifPresent(khuyenMai -> {
            // Delete image file
            String imageUrl = khuyenMai.getHinhAnh();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                    Path imagePath = Paths.get(UPLOAD_DIR, filename);
                    Files.deleteIfExists(imagePath);
                } catch (IOException e) {
                    System.err.println("Failed to delete image file: " + imageUrl + " with error: " + e.getMessage());
                }
            }

            // Break the relationship with tours before deleting
            for (tours tour : khuyenMai.getTours()) {
                tour.setDiscount_promotion(null);
                tourRepo.save(tour);
            }
            khuyenMai.getTours().clear();
            // The relationship is broken, now we can delete the promotion
            khuyenMaiRepository.delete(khuyenMai);
        });
    }

}
