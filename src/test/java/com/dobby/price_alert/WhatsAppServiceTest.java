package com.dobby.price_alert;

import com.dobby.price_alert.service.TelegramService;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WhatsAppServiceTest {
    @Autowired
    private TelegramService telegramService;

    @Test
    public void test() {

        telegramService.sendMessage(
                "Hello ghjfjhgfjhdgtdjfgchgvjhgcvjg"
        );

    }

}
