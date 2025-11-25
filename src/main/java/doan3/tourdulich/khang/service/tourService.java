package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.VietnamProvinces;
import doan3.tourdulich.khang.entity.tour_pictures;
import doan3.tourdulich.khang.entity.tour_schedules;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.repository.tourPicRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.scheduleRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.data.jpa.domain.Specification;
import doan3.tourdulich.khang.repository.TourSpecifications;

@Service
@Slf4j
public class tourService {

    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private tourPicRepo tourPicRepo;
    @Autowired
    private scheduleRepo scheduleRepo;

    public List<tours> findTours(
            String keyword,
            Boolean isAbroad,
            String tourType,
            String duration,
            String transportation,
            String priceRange,
            String location,
            String region,
            Boolean hasPromotion,
            String promotionStatus,
            String startDate,
            String departureStatus,
            String rating,
            Boolean redeemableWithPoints) {

        Specification<tours> spec = TourSpecifications.withFilters(keyword, isAbroad, tourType, duration, transportation, priceRange, location, region, hasPromotion, promotionStatus, startDate, departureStatus, rating, redeemableWithPoints);

        List<tours> tours = tourRepo.findAll(spec);
        log.info("Found {} tours", tours.size());
        return tours;
    }

    public List<tours> getAllTours() {
        return tourRepo.findAll();
    }

    public List<tours> getToursWithoutPromotion() {
        return tourRepo.findByDiscount_promotionIsNull();
    }

    public void addTour(String tour_id, String tour_name, String tour_description, String tour_start_location,
                        String tour_end_location, String tour_type, String tour_transportation,Integer tour_duration,Integer tour_max_number_of_people) throws IOException {

        // Xác định region dựa trên tour_location
        VietnamProvinces.Region region = VietnamProvinces.getRegionByProvince(tour_end_location);
        String tourRegion = region.name();

        if (tour_description == null || tour_description.isEmpty()) tour_description = "Cùng đi du lịch và khám phá các địa danh nổi tiếng ở " + tour_end_location + " nào!";

        tours tour = tours.builder()
                .tour_id(tour_id)
                .tour_name(tour_name)
                .tour_description(tour_description)
                .tour_type(tour_type)
                .tour_transportation(tour_transportation)
                .tour_start_location(tour_start_location)
                .tour_end_location(tour_end_location)
                .tour_adult_price(0)
                .tour_region(tourRegion)
                .tour_duration(tour_duration)
                .tour_max_number_of_people(tour_max_number_of_people)
                .is_abroad(false)
                .build();
        tourRepo.save(tour);
        autoAddTourSchedule(tour_id);
    }
    public void addTourInternational(String tour_id, String tour_name, String tour_description, String tour_start_location,
                        String tour_end_location, String tour_type, String tour_transportation,Integer tour_duration,Integer tour_max_number_of_people) throws IOException {

        // Đối với tour nước ngoài, set region là INTERNATIONAL
        String tourRegion = "INTERNATIONAL";

        if (tour_description == null || tour_description.isEmpty()) tour_description = "Cùng đi du lịch và khám phá các địa danh nổi tiếng ở " + tour_end_location + " nào!";

        tours tour = tours.builder()
                .tour_id(tour_id)
                .tour_name(tour_name)
                .tour_description(tour_description)
                .tour_type(tour_type)
                .tour_transportation(tour_transportation)
                .tour_start_location(tour_start_location)
                .tour_end_location(tour_end_location)
                .tour_adult_price(0)
                .tour_region(tourRegion)
                .tour_duration(tour_duration)
                .tour_max_number_of_people(tour_max_number_of_people)
                .is_abroad(true)
                .build();
        tourRepo.save(tour);
        autoAddTourSchedule(tour_id);
    }
    public void editTour(String tour_id, String tour_name, String tour_description, String tour_start_location,
                            String tour_end_location,String tour_type, String tour_transportation,Integer newTourDuration, Integer tour_max_number_of_people) throws IOException {


        Integer oldTourDuration = tourRepo.findById(tour_id).get().getTour_duration();
        System.out.println(newTourDuration + " " + oldTourDuration);

        VietnamProvinces.Region region = VietnamProvinces.getRegionByProvince(tour_end_location);
        String tourRegion = region.name();

        tours tour = tourRepo.findById(tour_id).get();
        tour.setTour_name(tour_name);
        tour.setTour_description(tour_description);
        tour.setTour_duration(newTourDuration);
        tour.setTour_type(tour_type);
        tour.setTour_transportation(tour_transportation);
        tour.setTour_start_location(tour_start_location);
        tour.setTour_end_location(tour_end_location);
        tour.setTour_region(tourRegion);
        tour.setTour_max_number_of_people(tour_max_number_of_people);
        tourRepo.save(tour);
        if (newTourDuration > oldTourDuration || newTourDuration < oldTourDuration) {
            updateTourSchedule(tour_id);
        }
    }
    public void updateGuestNumber(String tour_id, Integer guestNumber) {

    }
    public void deleteTour(String tour_id) throws IOException {
        try {
            // Xóa tất cả hình ảnh liên quan đến tour từ database
            List<tour_pictures> tourPics = tourPicRepo.findByTour(tourRepo.findById(tour_id).get());
            for (tour_pictures pic : tourPics) {
                deleteTourImages(pic.getPicture_url());
            }


            // Xóa tài liệu liên quan đến tour
            List<tour_schedules> tourSchedules = scheduleRepo.findByTour(tourRepo.findById(tour_id).get().getTour_id());
            for (tour_schedules schedule : tourSchedules) {
                scheduleRepo.deleteById(schedule.getSchedule_id());
            }

            // Xóa thư mục chúa tài liệu của tour
            Path tourPath = Paths.get(uploadDir + "/tours/" + tour_id);
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

            // Xóa tour từ database
            tourRepo.deleteById(tour_id);

        } catch (Exception e) {
            throw new IOException("Lỗi khi xoá tour: " + e.getMessage(), e);
        }
    }
    static final String uploadDir = "src/main/resources/static/uploads";
    
    public List<String> uploadTourImages(MultipartFile[] files, String tourId) {
        List<String> fileNames = new ArrayList<>();
        
        try {
            // Tạo thư mục nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir + "/tours/" + tourId);
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
                
                fileNames.add("/uploads/tours/" + tourId + "/" + fileName);

                // Luu vao database
                tourPicRepo.save(
                    tour_pictures.builder()
                    .picture_id(tourId + "/" + fileName)
                    .tour(tourRepo.findById(tourId).get())
                    .picture_url("/uploads/tours/" + tourId + "/" + fileName)
                    .build()
                );
            }
            
            return fileNames;
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi upload hình ảnh: " + e.getMessage());
        }
    }
    public void deleteTourImages(String imageUrl) {
        try {
            // Xử lý đường dẫn ảnh
            String normalizedPath = imageUrl.replace("/uploads/tours/", "");
            if (tourPicRepo.findByPicture_id(normalizedPath)!=null) {
                String imagePath = uploadDir +"/tours/"+ normalizedPath;
                File oldFile = new File(imagePath);
                System.out.println(oldFile.exists());
                if (oldFile.exists()) {
                    oldFile.delete();
                    tourPicRepo.deleteById(normalizedPath);
                }

            } else {
                throw new RuntimeException("File ảnh không tồn tại: " + imageUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống khi xử lý xoá ảnh: " + e.getMessage(), e);
        }
    }
    public void autoAddTourSchedule(String tourId) throws IOException {
        List<tour_schedules> tourSchedules = scheduleRepo.findByTourId(tourId);
        if (tourSchedules.size() == 0) {
            Integer duration = tourRepo.findById(tourId).get().getTour_duration();
            for (int i = 0; i < duration; i++) {
                tour_schedules schedule = tour_schedules.builder()
                        .tour(tourRepo.findById(tourId).get())  // Sử dụng đối tượng tour đã lấy ở trên
                        .title("Ngày " + (i + 1))
                        .body("Ngày " + (i + 1))
                        .day(i + 1)
                        .image_url(null)  // Có thể là null nếu không có ảnh
                        .build();

                scheduleRepo.save(schedule);
            }
        }
    }
    public void updateTourSchedule(String tourId) throws IOException {
        // Lấy thông tin tour mới
        tours tour = tourRepo.findById(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour không tồn tại"));

        // Tính toán duration mới
        Integer duration = tour.getTour_duration();

        // Lấy danh sách lịch trình hiện tại
        List<tour_schedules> existingSchedules = scheduleRepo.findByTourId(tourId);

        // Xử lý xóa các ngày thừa nếu duration giảm
        if (existingSchedules.size() > duration) {
            for (int i = (int)duration; i < existingSchedules.size(); i++) {
                scheduleRepo.deleteById(existingSchedules.get(i).getSchedule_id());
            }
        }

        // Cập nhật ngày cho các lịch trình hiện có
        for (int i = 0; i < Math.min(existingSchedules.size(), duration); i++) {
            tour_schedules schedule = existingSchedules.get(i);
            schedule.setDay(i + 1);
            scheduleRepo.save(schedule);
        }

        // Thêm lịch trình mới nếu duration tăng
        if (existingSchedules.size() < duration) {
            for (int i = existingSchedules.size(); i < duration; i++) {
                tour_schedules newSchedule = tour_schedules.builder()
                        .tour(tour)
                        .title("Ngày " + (i + 1))
                        .body("Lịch trình sẽ được cập nhật")
                        .day(i + 1)
                        .image_url(null)
                        .build();
                scheduleRepo.save(newSchedule);
            }
        }
    }
    public void editSchedule(String tourId, Integer day, String title, String body) {
        tour_schedules schedule = scheduleRepo.findByTourIDandDay(tourId, day);
        schedule.setTitle(title);
        schedule.setBody(body);
        scheduleRepo.save(schedule);
    }
    public String uploadScheduleImage(String tourId, Integer day, MultipartFile file) throws IOException {
        tour_schedules schedule = scheduleRepo.findByTourIDandDay(tourId, day);
        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir + "/tours/" + tourId + "/TourSchedulePics");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        schedule.setImage_url("/uploads/tours/" + tourId + "/TourSchedulePics/" + fileName);
        scheduleRepo.save(schedule);
        return fileName;
    }
    public void deleteScheduleImage(String tourId, Integer day) throws IOException {
        tour_schedules schedule = scheduleRepo.findByTourIDandDay(tourId, day);
        String imageUrl = schedule.getImage_url();
        String normalizedPath = imageUrl.replace("/uploads", "");
        if (imageUrl != null) {
            Path imagePath = Paths.get(uploadDir + normalizedPath);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
            }
            schedule.setImage_url(null);
            scheduleRepo.save(schedule);
        }
    }

    public List<tours> getRedeemableTours() {
        return tourRepo.findByRedeemableWithPoints(true);
    }
}
