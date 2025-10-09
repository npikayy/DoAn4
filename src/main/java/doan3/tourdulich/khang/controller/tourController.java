package doan3.tourdulich.khang.controller;

import doan3.tourdulich.khang.entity.tour_start_date;
import doan3.tourdulich.khang.entity.tours;
import doan3.tourdulich.khang.repository.tourPicRepo;
import doan3.tourdulich.khang.service.tourService;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.scheduleRepo;
import doan3.tourdulich.khang.repository.ratingRepo;
import doan3.tourdulich.khang.repository.startDateRepo;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RequestMapping("/admin/tours")
@RestController
public class tourController {
    @Autowired
    private tourService tourService;
    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private tourPicRepo tourPicRepo;
    @Autowired
    private scheduleRepo scheduleRepo;
    @Autowired
    private ratingRepo ratingRepo;
    @Autowired
    private startDateRepo startDateRepo;

    public String priceChange(Integer number) {
        String formatted = String.format("%,d", number).replace(',', '.');
        return formatted;
    }
    private Integer parsePrice(@Nullable String priceString) {
        if (priceString == null || priceString.isEmpty()) return 0;
        return Integer.parseInt(priceString.replace(".", ""));
    }

    @GetMapping()
    public ModelAndView tourPage(ModelAndView modelAndView) {
        // Lấy danh sách tất cả tour
        List<tours> tours = tourRepo.findAll();

        // Map chứa ảnh đại diện cho từng tour
        Map<String, String> tourThumbnails = new HashMap<>();

        // Map chứa danh sách ngày khởi hành cho từng tour
        Map<String, List<tour_start_date>> tourStartDates = new HashMap<>();

        for (tours tour : tours) {
            String tourId = tour.getTour_id();

            // Lấy ảnh đại diện
            String thumbnail = tourPicRepo.findOnePicByTour(tourId);
            tourThumbnails.put(tourId, thumbnail != null ? thumbnail : "");

            // Lấy danh sách ngày khởi hành
            List<tour_start_date> startDates = startDateRepo.findByTourId(tourId);
            tourStartDates.put(tourId, startDates);
        }
        // Lấy danh sách điểm đến duy nhất
        List<String> uniqueLocations = tourRepo.findDistinctEndLocations();

        // Thêm các đối tượng vào model
        modelAndView.addObject("tours", tours);
        modelAndView.addObject("tourThumbnails", tourThumbnails);
        modelAndView.addObject("tourStartDates", tourStartDates);
        modelAndView.addObject("uniqueLocations", uniqueLocations);
        modelAndView.setViewName("admin_html/tour/tour_management");
        return modelAndView;
    }
    @GetMapping("/detail/{tour_id}")
    public ModelAndView tourPage(@PathVariable String tour_id, ModelAndView modelAndView) {
        modelAndView.addObject("tour", tourRepo.findById(tour_id).get());
        modelAndView.addObject("tourPics",tourPicRepo.findByTourId(tour_id) );
        modelAndView.addObject("schedules", scheduleRepo.findByTourId(tour_id));
        modelAndView.addObject("startDates", startDateRepo.findByTourId(tour_id));
        modelAndView.addObject("tourRatings", ratingRepo.findRatingByTourId(tour_id));
        modelAndView.setViewName("admin_html/tour/tour_detail");
        return modelAndView;
    }
    @GetMapping("/addTour")
    public ModelAndView addTourPage(ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", "null");
        modelAndView.setViewName("admin_html/tour/tour_adding");
        return modelAndView;
    }
    @GetMapping("/addTourDomestic")
    public ModelAndView addTourDomesticPage(ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", "null");
        modelAndView.setViewName("admin_html/tour/tour_adding_domestic");
        return modelAndView;
    }
    @GetMapping("/addTourInternational")
    public ModelAndView addTourInternationalPage(ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", "null");
        modelAndView.setViewName("admin_html/tour/tour_adding_international");
        return modelAndView;
    }
    @PostMapping("/addTour")
    public ModelAndView addTour(String tour_id, String tour_name,
                                @Nullable String tour_description, String tour_start_location,
                                String tour_end_location,String tour_type, String tour_transportation,Integer tour_duration, Integer tour_max_number_of_people, RedirectAttributes redirectAttributes) throws IOException {

        if (tourRepo.findById(tour_id).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã tour đã tồn tại!");
            return new ModelAndView("redirect:/admin/tours/addTour");
        }

        tourService.addTour(tour_id, tour_name, tour_description, tour_start_location,
                tour_end_location,tour_type,tour_transportation, tour_duration, tour_max_number_of_people);

        return new ModelAndView("redirect:/admin/tours");
    }
    @PostMapping("/addTourDomestic")
    public ModelAndView addTourDomestic(String tour_id, String tour_name,
                                @Nullable String tour_description, String tour_start_location,
                                String tour_end_location,String tour_type, String tour_transportation,Integer tour_duration, Integer tour_max_number_of_people, RedirectAttributes redirectAttributes) throws IOException {

        if (tourRepo.findById(tour_id).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã tour đã tồn tại!");
            return new ModelAndView("redirect:/admin/tours/addTourDomestic");
        }

        tourService.addTour(tour_id, tour_name, tour_description, tour_start_location,
                tour_end_location,tour_type,tour_transportation, tour_duration, tour_max_number_of_people);

        return new ModelAndView("redirect:/admin/tours");
    }
    @PostMapping("/addTourInternational")
    public ModelAndView addTourInternational(String tour_id, String tour_name,
                                @Nullable String tour_description, String tour_start_location,
                                String tour_end_location,String tour_type, String tour_transportation,Integer tour_duration, Integer tour_max_number_of_people, RedirectAttributes redirectAttributes) throws IOException {

        if (tourRepo.findById(tour_id).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã tour đã tồn tại!");
            return new ModelAndView("redirect:/admin/tours/addTourInternational");
        }

        tourService.addTourInternational(tour_id, tour_name, tour_description, tour_start_location,
                tour_end_location,tour_type,tour_transportation, tour_duration, tour_max_number_of_people);

        return new ModelAndView("redirect:/admin/tours");
    }

    @GetMapping("/editTour/{tour_id}")
    public ModelAndView editTourPage(@PathVariable String tour_id, ModelAndView modelAndView) {
        tours tour = tourRepo.findById(tour_id).get();
        modelAndView.addObject("tour", tour);
        modelAndView.addObject("tour_id", tour_id);
        if (tour.getTour_adult_price() != null) {
            Integer number = tour.getTour_adult_price();
            modelAndView.addObject("tour_adult_price", priceChange(number));
        }
        if (tour.getTour_child_price() != null) {
            Integer number = tour.getTour_child_price();
            modelAndView.addObject("tour_child_price", priceChange(number));
        }
        if (tour.getTour_infant_price() != null) {
            Integer number = tour.getTour_infant_price();
            modelAndView.addObject("tour_infant_price", priceChange(number));
        }

        // Route to appropriate template based on tour type
        if (tour.getIs_abroad() != null && tour.getIs_abroad()) {
            modelAndView.setViewName("admin_html/tour/tour_adding_international");
        } else {
            modelAndView.setViewName("admin_html/tour/tour_adding_domestic");
        }
        return modelAndView;
    }
    @PostMapping("/editTour/{tour_id}")
    public ModelAndView editTour(@PathVariable String tour_id, String tour_name, String tour_description, String tour_start_location,
                                    String tour_end_location,String tour_type, String tour_transportation,Integer tour_duration, Integer tour_max_number_of_people) throws IOException {

        tourService.editTour(tour_id, tour_name, tour_description, tour_start_location,
                tour_end_location,tour_type,tour_transportation,tour_duration,tour_max_number_of_people);

        return new ModelAndView("redirect:/admin/tours/detail/" + tour_id);
    }
    @GetMapping("/deleteTour")
    public ModelAndView deleteTourPage(@RequestParam String tour_id) throws IOException {
        tourService.deleteTour(tour_id);
        return new ModelAndView("redirect:/admin/tours");
    }

    @GetMapping("/addImage/{tour_id}")
    public ModelAndView uploadPage(@PathVariable String tour_id, ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", tour_id);
        modelAndView.setViewName("admin_html/tour/image_adding");
        return modelAndView;
    }

    @PostMapping(value = "/addImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleFileUpload(@RequestParam("files") MultipartFile[] files, @RequestParam("tourId") String tourId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> imageUrls = tourService.uploadTourImages(files, tourId);
            response.put("success", true);
            response.put("imageUrls", imageUrls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @PostMapping("/deleteImage")
    public ModelAndView deleteImagePage(@RequestParam String imageUrl) {
        String[] parts = imageUrl.split("/");  // Tách theo dấu "/"
        String tourId = parts[3];  // "ABC1" (phần tử thứ 4 trong mảng)
        tourService.deleteTourImages(imageUrl);

        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
    @GetMapping("/tourSchedule/{tour_id}")
    public ModelAndView tourSchedulePage(@PathVariable String tour_id, ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", tour_id);
        modelAndView.addObject("tourDuration", tourRepo.findById(tour_id).get().getTour_duration());
        modelAndView.addObject("tourSchedules", scheduleRepo.findByTourId(tour_id));
        modelAndView.setViewName("admin_html/tour/tour_schedule");
        return modelAndView;
    }
    @PostMapping("/tourSchedule/updateSchedule/{tour_id}/{day}")
    public ModelAndView updateSchedule(
            @PathVariable String tour_id,
            @PathVariable Integer day,
            @RequestParam String title,
            @RequestParam String body,
            RedirectAttributes redirectAttributes) {
        tourService.editSchedule(tour_id, day, title, body);
        return new ModelAndView("redirect:/admin/tours/tourSchedule/" + tour_id);
    }
    @GetMapping("/tourSchedule/imageEdit/{tour_id}/{day}")
    public ModelAndView addImagePage(@PathVariable String tour_id, @PathVariable Integer day, ModelAndView modelAndView) {
        modelAndView.addObject("tour_id", tour_id);
        modelAndView.addObject("day", day);
        return new ModelAndView("admin_html/tour/schedule_image_adding");
    }
    @PostMapping("/tourSchedule/imageEdit")
    public ResponseEntity<Map<String, Object>> addImagePage(@RequestParam String tourId, @RequestParam Integer day, @RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageUrls = tourService.uploadScheduleImage(tourId, day, file);
            response.put("success", true);
            response.put("imageUrls", imageUrls);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi upload: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    @GetMapping("/tourSchedule/imageDelete/{tour_id}/{day}")
    public ModelAndView deleteScheduleImage(@PathVariable String tour_id, @PathVariable Integer day){
        try {
            tourService.deleteScheduleImage(tour_id, day);
            return new ModelAndView("redirect:/admin/tours/tourSchedule/" + tour_id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @PostMapping("/updateDiscount/{tourId}")
    public ModelAndView updateDiscount(@PathVariable String tourId,
                                       @RequestParam(required = false) Integer discount,
                                       RedirectAttributes redirectAttributes) {
        try {
            tours tour = tourRepo.findById(tourId).get();
            tour.setTour_discount(discount);
            tourRepo.save(tour);

            redirectAttributes.addFlashAttribute("success", "Cập nhật giảm giá thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật giảm giá");
        }
        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
    @PostMapping("/updatePrices/{tourId}")
    public ModelAndView updatePrice(@PathVariable String tourId,
                                    @RequestParam(required = false) String adultPrice,
                                    @RequestParam(required = false) String childPrice,
                                    @RequestParam(required = false) String infantPrice,
                                    RedirectAttributes redirectAttributes) {
        try {
            tours tour = tourRepo.findById(tourId).get();
            tour.setTour_adult_price(parsePrice(adultPrice));
            tour.setTour_child_price(parsePrice(childPrice));
            tour.setTour_infant_price(parsePrice(infantPrice));
            tourRepo.save(tour);

            redirectAttributes.addFlashAttribute("success", "Cập nhật giá công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật giá");
        }
        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
    @PostMapping("/updateSpecialOffer/{tourId}")
    public ModelAndView updateSpecialOffer(@PathVariable String tourId,
                                           @RequestParam(required = false) String specialOffer,
                                           RedirectAttributes redirectAttributes) {
        try {
            tours tour = tourRepo.findById(tourId).get();
            tour.setSpecial_offer(specialOffer);
            tourRepo.save(tour);

            redirectAttributes.addFlashAttribute("success", "Cập nhật khuyen mai công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật khuyen mai");
        }
        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
    @PostMapping("/addStartDate/{tourId}")
    public ModelAndView addStartDate(@PathVariable String tourId,
                                     @RequestParam(required = false) String startDate,
                                     RedirectAttributes redirectAttributes) {
        try {
            tour_start_date tourStartDate = new tour_start_date();
            tourStartDate.setTour(tourRepo.findById(tourId).get());
            tourStartDate.setStart_date(LocalDate.parse(startDate));
            tourStartDate.setGuest_number(0);
            startDateRepo.save(tourStartDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật ngày khóa");
        }
        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
    @GetMapping("/deleteStartDate/{tourId}/{startDateId}")
    public ModelAndView deleteStartDate(@PathVariable String tourId, @PathVariable Integer startDateId) {
        startDateRepo.deleteById(startDateId);
        return new ModelAndView("redirect:/admin/tours/detail/" + tourId);
    }
}
