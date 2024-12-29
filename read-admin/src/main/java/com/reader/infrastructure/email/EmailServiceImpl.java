package com.reader.infrastructure.email;

import com.reader.domain.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import jakarta.mail.internet.MimeMessage;


@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    public Mono<Void> sendEmail(String to, String subject, String content) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(emailProperties.getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(content);
                mailSender.send(message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send email", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
} 