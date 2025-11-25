package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.entity.Rank;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.entity.vouchers;
import doan3.tourdulich.khang.repository.RankRepository;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
import doan3.tourdulich.khang.dto.MailBody;
import doan3.tourdulich.khang.entity.tour_bookings;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.userRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import doan3.tourdulich.khang.repository.BookingSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class bookingService {

    private final tourBookingRepo tourBookingRepo;
    private final tourRepo tourRepo;
    private final userRepo userRepo;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final RankService rankService; // Changed from GradeService
    private final userService userService;
    private final RankRepository rankRepository; // Changed from GradeRepository

    public tour_bookings addBooking(
            String tourId, String userId, String userFullName, String userEmail,
            String userPhoneNumber, String userAddress, String tourName,
            LocalDate startDate, Integer totalPrice, Integer numberOfAdults,
            Integer numberOfChildren, Integer numberOfInfants, Integer voucherDiscount, String note, String bookingType) {

        Integer duration = tourRepo.findById(tourId).get().getTour_duration();
        LocalDate endDate = startDate.plusDays(duration);

        tour_bookings booking = tour_bookings.builder()
                .tour(tourRepo.findById(tourId).get())
                .user_id(userId)
                .user_full_name(userFullName)
                .user_email(userEmail)
                .user_phone_number(userPhoneNumber)
                .user_address(userAddress)
                .status("Pending payment")
                .booking_date(LocalDateTime.now())
                .tour_name(tourName)
                .start_date(startDate)
                .end_date(endDate)
                .total_price(totalPrice)
                .number_of_adults(numberOfAdults)
                .number_of_children(numberOfChildren)
                .number_of_infants(numberOfInfants)
                .voucher_discount(voucherDiscount)
                .note(note)
                .bookingType(bookingType)
                .build();


        tourBookingRepo.save(booking);
        sendBookingConfirmation(booking);
        return booking;

    }

    public Page<tour_bookings> findAllPaginatedAndFiltered(String status, String userId, String searchQuery, Pageable pageable) {
        Specification<tour_bookings> spec = Specification.where(null);

        if (org.springframework.util.StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            spec = spec.and(BookingSpecifications.withStatus(status));
        }

        if (org.springframework.util.StringUtils.hasText(userId) && !"all".equalsIgnoreCase(userId)) {
            spec = spec.and(BookingSpecifications.withUserId(userId));
        }

        if (org.springframework.util.StringUtils.hasText(searchQuery)) {
            spec = spec.and(BookingSpecifications.withSearchQuery(searchQuery));
        }

        return tourBookingRepo.findAll(spec, pageable);
    }
    public void updateBookingStatus(Integer bookingId, String status) {
        tourBookingRepo.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(status);
            tourBookingRepo.save(booking);
        });
    }

    @Async
    public void handleTourCompletionTasks(tour_bookings booking) {
        int pointsAdded = booking.getTotal_price() / 100000;
        sendThankYouEmail(booking.getUser_email(), booking.getBooking_id(), pointsAdded);
        users user = userRepo.findById(booking.getUser_id()).get();

        booking.setStatus("Completed");
        tourBookingRepo.save(booking);

        userService.addPointsForCompletedTour(booking);
        userService.updateUserRank(user); // Call updateUserRank, which will now calculate completed tours
    }

    @Async
    public void sendBookingConfirmation(tour_bookings booking) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(booking.getTotal_price());

        Context context = new Context();
        context.setVariable("customerName", booking.getUser_full_name());
        context.setVariable("bookingId", "TOD-" + booking.getBooking_id());
        context.setVariable("tourName", booking.getTour().getTour_name());
        context.setVariable("startDate", booking.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String guestCount = booking.getNumber_of_adults() + " người lớn" +
                (booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "") +
                (booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "");
        context.setVariable("guestCount", guestCount);
        context.setVariable("totalPrice", formattedAmount);
        context.setVariable("paymentLink", "http://localhost:8080/Client/orderBooking/Payment/" + booking.getBooking_id());

        String htmlContent = templateEngine.process("client_html/booking_confirmation_email", context);

        MailBody mailBody = MailBody.builder()
                .to(booking.getUser_email())
                .subject("[Tôi Đi Du Lịch] Xác nhận đặt tour thành công - Mã đơn: " + booking.getBooking_id())
                .text(htmlContent)
                .build();

        log.info("Sending booking confirmation email to: {}", booking.getUser_email());
        emailService.sendHtmlMessage(mailBody);
    }

    @Async
    public void sendPaymentSuccessNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(booking.getTotal_price());

        Context context = new Context();
        context.setVariable("customerName", booking.getUser_full_name());
        context.setVariable("bookingId", "TOD-" + booking.getBooking_id());
        context.setVariable("tourName", booking.getTour().getTour_name());
        context.setVariable("startDate", booking.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String guestCount = booking.getNumber_of_adults() + " người lớn" +
                (booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "") +
                (booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "");
        context.setVariable("guestCount", guestCount);
        context.setVariable("totalPrice", formattedAmount);
        context.setVariable("paymentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String htmlContent = templateEngine.process("client_html/payment_success_email", context);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Thanh toán thành công - Mã đơn: " + booking_id)
                .text(htmlContent)
                .build();

        log.info("Sending payment success email to: {}", email);
        emailService.sendHtmlMessage(mailBody);
    }

    @Async
    public void sendCashPaymentNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        Context context = new Context();
        context.setVariable("customerName", booking.getUser_full_name());
        context.setVariable("bookingId", "TOD-" + booking.getBooking_id());
        context.setVariable("tourName", booking.getTour().getTour_name());
        context.setVariable("startDate", booking.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String guestCount = booking.getNumber_of_adults() + " người lớn" +
                (booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "") +
                (booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "");
        context.setVariable("guestCount", guestCount);
        context.setVariable("totalPrice", NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(booking.getTotal_price()));

        String htmlContent = templateEngine.process("client_html/cash_payment_email", context);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Xác nhận đặt tour - Mã đơn: " + booking_id)
                .text(htmlContent)
                .build();

        log.info("Sending cash payment confirmation email to: {}", email);
        emailService.sendHtmlMessage(mailBody);
    }
    private String buildCancellationContent(tour_bookings booking) {
        StringBuilder content = new StringBuilder();

        content.append("Kính chào ").append(booking.getUser_full_name()).append(",\n\n");
        content.append("Chúng tôi xin thông báo đơn đặt tour của bạn đã bị hủy.\n\n");

        content.append("Thông tin đơn hàng đã hủy:\n");
        content.append("- Mã đơn: TOD-").append(booking.getBooking_id()).append("\n");
        content.append("- Tên tour: ").append(booking.getTour().getTour_name()).append("\n");
        content.append("- Ngày khởi hành: ").append(booking.getStart_date()).append("\n");
        content.append("- Số lượng khách: ")
                .append(booking.getNumber_of_adults()).append(" người lớn")
                .append(booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "")
                .append(booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "")
                .append("\n");

        if (booking.getCancel_reason() != null && !booking.getCancel_reason().isEmpty()) {
            content.append("- Lý do hủy: ").append(booking.getCancel_reason()).append("\n");
        }

        content.append("\n");

        if (booking.getStatus().equals("PAID")) {
            content.append("Thông tin hoàn tiền:\n");
            content.append("- Số tiền sẽ được hoàn: ")
                    .append(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(booking.getTotal_price()))
                    .append("\n");
            content.append("- Thời gian hoàn tiền: Trong vòng 7-10 ngày làm việc\n");
            content.append("- Phương thức hoàn tiền: Theo phương thức thanh toán ban đầu\n\n");
        }

        content.append("Liên hệ hỗ trợ:\n");
        content.append("- Hotline: 1900 1234 (24/7)\n");
        content.append("- Email: support@toididulich.com\n\n");

        content.append("Chúng tôi rất tiếc vì sự bất tiện này và mong có cơ hội phục vụ bạn trong các chuyến đi sau.\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ Tôi Đi Du Lịch");

        return content.toString();
    }

    @Async
    public void sendCancelNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        Context context = new Context();
        context.setVariable("customerName", booking.getUser_full_name());
        context.setVariable("bookingId", "TOD-" + booking.getBooking_id());
        context.setVariable("tourName", booking.getTour().getTour_name());
        context.setVariable("startDate", booking.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        String guestCount = booking.getNumber_of_adults() + " người lớn" +
                (booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "") +
                (booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "");
        context.setVariable("guestCount", guestCount);
        context.setVariable("cancelReason", booking.getCancel_reason());
        context.setVariable("isPaid", "PAID".equals(booking.getStatus()));
        context.setVariable("refundAmount", NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(booking.getTotal_price()));

        String htmlContent = templateEngine.process("client_html/cancellation_email", context);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Thông báo hủy đơn - Mã đơn: TOD-" + booking_id)
                .text(htmlContent)
                .build();

        log.info("Sending cancellation notification email to: {}", email);
        emailService.sendHtmlMessage(mailBody);
    }

    private String buildThankYouContent(tour_bookings booking) {
        StringBuilder content = new StringBuilder();

        content.append("Kính gửi quý khách ").append(booking.getUser_full_name()).append(",\n\n");
        content.append("Từ toàn thể đội ngũ Tôi Đi Du Lịch, chúng tôi xin được gửi lời cảm ơn chân thành nhất \n");
        content.append("vì đã tin tưởng lựa chọn và đồng hành cùng chúng tôi trong chuyến đi vừa qua.\n\n");

        content.append("Thông tin chuyến đi:\n");
        content.append("- Mã đơn: TOD-").append(booking.getBooking_id()).append("\n");
        content.append("- Tên tour: ").append(booking.getTour().getTour_name()).append("\n");
        content.append("- Ngày khởi hành: ").append(booking.getStart_date()).append("\n\n");

        content.append("Sự đồng hành của quý khách là niềm vinh hạnh lớn lao đối với chúng tôi. \n");
        content.append("Mong rằng quý khách đã có những trải nghiệm đáng nhớ và khoảnh khắc tuyệt vời \n");
        content.append("trong suốt hành trình.\n\n");

        content.append("Để tri ân sự tin yêu của quý khách:\n");
        content.append("- Ưu đãi đặc biệt: Giảm 10% cho chuyến đi tiếp theo\n");
        content.append("- Mã ưu đãi: CAMON").append(booking.getBooking_id()).append("\n");
        content.append("- Thời hạn: Áp dụng trong 6 tháng kể từ ngày nhận email\n\n");

        content.append("Chúng tôi luôn mong được tiếp tục đồng hành cùng quý khách trong \n");
        content.append("những hành trình khám phá mới đầy thú vị sắp tới.\n\n");

        content.append("Trân trọng cảm ơn,\n");
        content.append("Đội ngũ Tôi Đi Du Lịch\n");
        content.append("---\n");
        content.append("Liên hệ hỗ trợ:\n");
        content.append("- Hotline: 1900 1234\n");
        content.append("- Email: thankyou@toididulich.com\n");
        content.append("- Website: https://toididulich.com");

        return content.toString();
    }



    @Async
    public void sendThankYouEmail(String email, Integer booking_id, int pointsAdded) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        Context context = new Context();
        context.setVariable("customerName", booking.getUser_full_name());
        context.setVariable("bookingId", "TOD-" + booking.getBooking_id());
        context.setVariable("tourName", booking.getTour().getTour_name());
        context.setVariable("startDate", booking.getStart_date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("pointsAdded", pointsAdded);

        String htmlContent = templateEngine.process("client_html/thank_you_email", context);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Lời cảm ơn chân thành từ đội ngũ chúng tôi")
                .text(htmlContent)
                .build();

        log.info("Sending thank you email to: {}", email);
        emailService.sendHtmlMessage(mailBody);
    }
    @Async
    public void sendRankPromotionEmail(users user, String oldRank, String newRank) {
        Context context = new Context();
        context.setVariable("customerName", user.getFull_name());
        context.setVariable("oldRank", oldRank);
        context.setVariable("newRank", newRank);

        String htmlContent = templateEngine.process("client_html/rank_promotion_email", context);

        MailBody mailBody = MailBody.builder()
                .to(user.getEmail())
                .subject("[Tôi Đi Du Lịch] Chúc mừng bạn đã được thăng hạng thành viên!")
                .text(htmlContent)
                .build();

        emailService.sendHtmlMessage(mailBody);
    }
}
