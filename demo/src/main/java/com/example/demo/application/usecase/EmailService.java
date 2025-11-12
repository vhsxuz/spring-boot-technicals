// application/usecase/EmailService.java
package com.example.demo.application.usecase;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  private final JavaMailSender mailSender;

  public EmailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendOtpEmail(String to, String otpCode) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("drealexander.dev@gmail.com");
    message.setTo(to);
    message.setSubject("Your OTP Code");
    message.setText(String.format(
        "Your OTP code is: %s\n\n" +
        "This code will expire in 10 minutes.\n\n" +
        "If you didn't request this code, please ignore this email.",
        otpCode
    ));

    mailSender.send(message);
  }
}
