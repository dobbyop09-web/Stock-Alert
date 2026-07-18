package com.dobby.price_alert;

import com.dobby.price_alert.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void sendMail() {

        emailService.sendEmail(
                "aman.verma2k02@gmail.com",
                "Spring Boot Test",
                "Hello from localhost 🚀"
        );

    }

}