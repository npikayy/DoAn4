package doan3.tourdulich.khang.controller;

import ch.qos.logback.core.model.Model;
import doan3.tourdulich.khang.service.VnPayService;
import doan3.tourdulich.khang.service.bookingService;
import doan3.tourdulich.khang.service.EmailService;
import doan3.tourdulich.khang.repository.tourBookingRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class vnPayController {
    @Autowired
    private VnPayService vnPayService;
    @Autowired
    private bookingService bookingService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private tourBookingRepo tourBookingRepo;

    @PostMapping("/submitOrder")
    public String submidOrder(@RequestParam("amount") int orderTotal,
                              @RequestParam("orderInfo") String orderInfo,
                              @RequestParam("bookingId") Integer booking_id,
                              HttpServletRequest request){
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String vnpayUrl = vnPayService.createOrder(orderTotal, orderInfo, baseUrl, booking_id);
        System.out.println(vnpayUrl);
        // Redirect directly to VNPay
        return "redirect:" + vnpayUrl;
    }

    @GetMapping("/vnpay-payment")
    public String processPaymentResult(HttpServletRequest request, Model model) {
        int paymentStatus = vnPayService.orderReturn(request);

        // Lấy booking_id từ tham số trả về
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_OrderInfo = request.getParameter("vnp_OrderInfo");

        Integer bookingId = null;
        try {
            // Ưu tiên lấy từ vnp_TxnRef trước
            bookingId = Integer.parseInt(vnp_TxnRef);
        } catch (NumberFormatException e) {

        }

        if (bookingId != null) {
            // Cập nhật trạng thái thanh toán
            if (paymentStatus == 1) {
                bookingService.updateBookingStatus(bookingId, "Paid");
                bookingService.sendPaymentSuccessNotification(tourBookingRepo.findByBooking_id(bookingId).getUser_email(), bookingId);
            }

            // Thêm bookingId vào redirect URL
            return paymentStatus == 1
                    ? "redirect:/Client/orderBooking/booking_success/" + bookingId
                    : "redirect:/Client/orderBooking/payment_failed/" + bookingId;
        } else {
            // Xử lý trường hợp không lấy được bookingId
            return "redirect:/booking/error";
        }
    }
}
