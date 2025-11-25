package doan3.tourdulich.khang.service;

import doan3.tourdulich.khang.dto.MailBody;
import doan3.tourdulich.khang.entity.users;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }
    public void sendSimpleMessage(MailBody mailBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailBody.to());
        message.setFrom("khangnguyen41888@gmail.com");
        message.setSubject(mailBody.subject());
        message.setText(mailBody.text());

        javaMailSender.send(message);
    }

    public void sendHtmlMessage(MailBody mailBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(mailBody.to());
            helper.setFrom("khangnguyen41888@gmail.com");
            helper.setSubject(mailBody.subject());
            helper.setText(mailBody.text(), true); // true indicates HTML

            javaMailSender.send(message);
        } catch (MessagingException e) {
            // Handle exception
        }
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

        sendHtmlMessage(mailBody);
    }

    @Async
    public void sendTerminationEmail(users user) {
        Context context = new Context();
        context.setVariable("customerName", user.getFull_name());

        String htmlContent = templateEngine.process("client_html/termination_email", context);

        MailBody mailBody = MailBody.builder()
                .to(user.getEmail())
                .subject("[Tôi Đi Du Lịch] Thông báo về việc chấm dứt tư cách thành viên")
                .text(htmlContent)
                .build();

        sendHtmlMessage(mailBody);
    }
}
