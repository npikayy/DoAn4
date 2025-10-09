package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.MailBody;
import doan3.tourdulich.khang.entity.tour_bookings;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import doan3.tourdulich.khang.repository.tourRepo;
import doan3.tourdulich.khang.repository.userRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@Service
@Slf4j
public class bookingService {

    @Autowired
    private tourBookingRepo tourBookingRepo;
    @Autowired
    private tourRepo tourRepo;
    @Autowired
    private userRepo userRepo;
    @Autowired
    private EmailService emailService;

    public tour_bookings addBooking(
            String tourId, String userId, String userFullName, String userEmail,
            String userPhoneNumber, String userAddress, String tourName,
            LocalDate startDate, Integer totalPrice, Integer numberOfAdults,
            Integer numberOfChildren, Integer numberOfInfants, Integer voucherDiscount, String note) {

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
                .build();


        tourBookingRepo.save(booking);
        sendBookingConfirmation(userEmail,booking.getBooking_id());
        return booking;

    }
    public void updateBookingStatus(Integer bookingId, String status) {
        tourBookingRepo.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(status);
            tourBookingRepo.save(booking);
        });
    }
    public void sendBookingConfirmation(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(booking.getTotal_price());

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Xác nhận đặt tour thành công - Mã đơn: " + booking_id)
                .text(buildBookingConfirmationContent(booking, formattedAmount))
                .build();

        log.info("Sending booking confirmation email to: {}", email);
        emailService.sendSimpleMessage(mailBody);
    }

    public void sendPaymentSuccessNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(booking.getTotal_price());

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Thanh toán thành công - Mã đơn: " + booking_id)
                .text(buildPaymentSuccessContent(booking, formattedAmount))
                .build();

        log.info("Sending payment success email to: {}", email);
        emailService.sendSimpleMessage(mailBody);
    }

    private String buildBookingConfirmationContent(tour_bookings booking, String formattedAmount) {
        StringBuilder content = new StringBuilder();

        content.append("Kính chào ").append(booking.getUser_full_name()).append(",\n\n");
        content.append("Cảm ơn bạn đã đặt tour tại Tôi Đi Du Lịch!\n\n");
        content.append("Thông tin đơn hàng:\n");
        content.append("- Mã đơn: TOD-").append(booking.getBooking_id()).append("\n");
        content.append("- Tên tour: ").append(booking.getTour().getTour_name()).append("\n");
        content.append("- Ngày khởi hành: ").append(booking.getStart_date()).append("\n");
        content.append("- Số lượng khách: ")
                .append(booking.getNumber_of_adults()).append(" người lớn")
                .append(booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "")
                .append(booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "")
                .append("\n");
        content.append("- Tổng tiền: ").append(formattedAmount).append("\n\n");

        content.append("Vui lòng thanh toán trong vòng 24 giờ để hoàn tất đặt chỗ.\n");
        content.append("Các hình thức thanh toán:\n");
        content.append("1. Thanh toán trực tiếp tại văn phòng\n");
        content.append("   Địa chỉ: Cần Thơ\n");
        content.append("   Thời gian: 8:00 - 17:00 từ Thứ 2 đến Thứ 6\n\n");
        content.append("2. Thanh toán online (VNPay)\n");
        content.append("   Bấm vào link sau để thanh toán: [localhost:8080/Client/orderBooking/Payment/" + booking.getBooking_id() + "]\n\n");
        content.append("Sau khi thanh toán thành công, hệ thống sẽ gửi email xác nhận đến bạn.\n\n");
        content.append("Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ:\n");
        content.append("- Hotline: 1900 1234 (24/7)\n");
        content.append("- Email: support@toididulich.com\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ Tôi Đi Du Lịch");

        return content.toString();
    }

    private String buildPaymentSuccessContent(tour_bookings booking, String formattedAmount) {
        StringBuilder content = new StringBuilder();

        content.append("Kính chào ").append(booking.getUser_full_name()).append(",\n\n");
        content.append("Cảm ơn bạn đã thanh toán thành công cho đơn đặt tour tại Tôi Đi Du Lịch!\n\n");
        content.append("Thông tin đơn hàng:\n");
        content.append("- Mã đơn: TOD-").append(booking.getBooking_id()).append("\n");
        content.append("- Tên tour: ").append(booking.getTour().getTour_name()).append("\n");
        content.append("- Ngày khởi hành: ").append(booking.getStart_date()).append("\n");
        content.append("- Số lượng khách: ")
                .append(booking.getNumber_of_adults()).append(" người lớn")
                .append(booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "")
                .append(booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "")
                .append("\n");
        content.append("- Số tiền thanh toán: ").append(formattedAmount).append("\n");
        content.append("- Ngày thanh toán: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

        content.append("Thông tin hướng dẫn:\n");
        content.append("- Vui lòng có mặt tại điểm tập trung trước giờ khởi hành ít nhất 30 phút\n");
        content.append("- Mang theo CMND/CCCD bản gốc hoặc hộ chiếu khi đi tour\n");
        content.append("- In email này hoặc trình mã đơn TOD-").append(booking.getBooking_id()).append(" khi làm thủ tục\n\n");

        content.append("Liên hệ hỗ trợ:\n");
        content.append("- Hotline: 1900 1234 (24/7)\n\n");
        content.append("Chúc bạn có một chuyến đi thú vị!\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ Tôi Đi Du Lịch");

        return content.toString();
    }
    private String buildCashPaymentContent(tour_bookings booking) {
        StringBuilder content = new StringBuilder();

        content.append("Kính chào ").append(booking.getUser_full_name()).append(",\n\n");
        content.append("Cảm ơn bạn đã đặt tour tại Tôi Đi Du Lịch và lựa chọn hình thức thanh toán tiền mặt!\n\n");
        content.append("Thông tin đơn hàng:\n");
        content.append("- Mã đơn: TOD-").append(booking.getBooking_id()).append("\n");
        content.append("- Tên tour: ").append(booking.getTour().getTour_name()).append("\n");
        content.append("- Ngày khởi hành: ").append(booking.getStart_date()).append("\n");
        content.append("- Số lượng khách: ")
                .append(booking.getNumber_of_adults()).append(" người lớn")
                .append(booking.getNumber_of_children() > 0 ? ", " + booking.getNumber_of_children() + " trẻ em" : "")
                .append(booking.getNumber_of_infants() > 0 ? ", " + booking.getNumber_of_infants() + " em bé" : "")
                .append("\n");
        content.append("- Tổng số tiền: ").append(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(booking.getTotal_price())).append("\n");
        content.append("- Hình thức thanh toán: Tiền mặt khi làm thủ tục\n\n");

        content.append("Hướng dẫn thanh toán:\n");
        content.append("- Vui lòng thanh toán toàn bộ số tiền khi làm thủ tục trước ngày khởi hành\n");
        content.append("- Chúng tôi sẽ liên hệ trước ngày khởi hành để xác nhận\n\n");

        content.append("Thông tin hướng dẫn:\n");
        content.append("- Vui lòng có mặt tại điểm tập trung trước giờ khởi hành ít nhất 30 phút\n");
        content.append("- Mang theo CMND/CCCD bản gốc hoặc hộ chiếu khi đi tour\n");
        content.append("- In email này hoặc trình mã đơn TOD-").append(booking.getBooking_id()).append(" khi làm thủ tục\n\n");

        content.append("Liên hệ hỗ trợ:\n");
        content.append("- Hotline: 1900 1234 (24/7)\n\n");
        content.append("Chúc bạn có một chuyến đi thú vị!\n\n");
        content.append("Trân trọng,\n");
        content.append("Đội ngũ Tôi Đi Du Lịch");

        return content.toString();
    }

    public void sendCashPaymentNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Xác nhận đặt tour - Mã đơn: " + booking_id)
                .text(buildCashPaymentContent(booking))
                .build();

        log.info("Sending cash payment confirmation email to: {}", email);
        emailService.sendSimpleMessage(mailBody);
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

    public void sendCancelNotification(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Thông báo hủy đơn - Mã đơn: TOD-" + booking_id)
                .text(buildCancellationContent(booking))
                .build();

        log.info("Sending cancellation notification email to: {}", email);
        emailService.sendSimpleMessage(mailBody);
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

    public void sendThankYouEmail(String email, Integer booking_id) {
        tour_bookings booking = tourBookingRepo.findByBooking_id(booking_id);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Lời cảm ơn chân thành từ đội ngũ chúng tôi")
                .text(buildThankYouContent(booking))
                .build();

        log.info("Sending thank you email to: {}", email);
        emailService.sendSimpleMessage(mailBody);
    }
}
