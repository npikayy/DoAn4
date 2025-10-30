package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.MailBody;
import doan3.tourdulich.khang.entity.users;
import doan3.tourdulich.khang.entity.vouchers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class AsyncVoucherService {

    private final userService userService;
    private final VoucherService voucherService;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    public AsyncVoucherService(userService userService, VoucherService voucherService, EmailService emailService, TemplateEngine templateEngine) {
        this.userService = userService;
        this.voucherService = voucherService;
        this.emailService = emailService;
        this.templateEngine = templateEngine;
    }

    @Async
    public void createVouchersForAllUsers(String voucherType, String giaTriGiamStr, String ngayHetHan) {
        log.info("Starting asynchronous job to create vouchers for all users.");

        try {
            int giaTriGiam = Integer.parseInt(giaTriGiamStr.replace(".", ""));
            int page = 0;
            int size = 1000; // Process 1000 users at a time
            Page<users> userPage;

            do {
                userPage = userService.getAllUsersExceptAdmin(PageRequest.of(page, size));
                for (users user : userPage.getContent()) {
                    try {
                        vouchers newVoucher = new vouchers();
                        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        newVoucher.setMaVoucher(code);
                        newVoucher.setVoucherType(voucherType);
                        newVoucher.setGiaTriGiam(giaTriGiam);
                        newVoucher.setNgayHetHan(java.sql.Date.valueOf(ngayHetHan));
                        newVoucher.setTrangThai("ACTIVE");
                        newVoucher.setUser(user);
                        vouchers savedVoucher = voucherService.saveVoucher(newVoucher);

                        sendVoucherNotificationEmail(savedVoucher.getUser().getEmail(), savedVoucher);
                    } catch (Exception e) {
                        log.error("Failed to create voucher or send email for user {}: {}", user.getUser_id(), e.getMessage(), e);
                    }
                }
                page++;
            } while (userPage.hasNext());

            log.info("Finished processing voucher creation job.");
        } catch (Exception e) {
            log.error("Error in voucher creation job: {}", e.getMessage(), e);
        }
    }

    public void sendVoucherNotificationEmail(String email, vouchers voucher) {
        Context context = new Context();
        context.setVariable("customerName", voucher.getUser().getFull_name());
        context.setVariable("voucherCode", voucher.getMaVoucher());
        String discountValue = "";
        if ("PERCENTAGE".equals(voucher.getVoucherType())) {
            discountValue = voucher.getGiaTriGiam() + "%";
        } else if ("AMOUNT".equals(voucher.getVoucherType())) {
            discountValue = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(voucher.getGiaTriGiam());
        }
        context.setVariable("discountValue", discountValue);
        context.setVariable("expiryDate", new java.text.SimpleDateFormat("dd/MM/yyyy").format(voucher.getNgayHetHan()));

        String htmlContent = templateEngine.process("client_html/voucher_notification_email", context);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("[Tôi Đi Du Lịch] Quà tặng tri ân - Bạn đã nhận được một voucher mới")
                .text(htmlContent)
                .build();

        log.info("Sending voucher notification email to: {}", email);
        emailService.sendHtmlMessage(mailBody);
    }
}
