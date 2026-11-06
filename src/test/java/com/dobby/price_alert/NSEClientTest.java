package com.dobby.price_alert;

import com.dobby.price_alert.client.NSEClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

@SpringBootTest
class NSEClientTest {

    @Autowired
    private NSEClient nseClient;

    @Test
    void testParticipantWiseOiReport() {

        String response = nseClient.getParticipantWiseOiReport(
                LocalDate.of(2026, 11, 6)
        );

        System.out.println("NSE Response:");

        if (response != null) {
            System.out.println(
                    response.substring(0, Math.min(response.length(), 1000))
            );
        }

        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}