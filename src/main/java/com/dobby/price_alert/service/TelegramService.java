package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.MessageFormat;
import com.dobby.price_alert.dto.StockMessageDto;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramService {

    private static final String BOT_TOKEN =
            "8007362878:AAGkSnRgJm8lezZ-rDeKPsmaIjbk1aDeAqE";
    private final RestTemplate restTemplate =
            new RestTemplate();
    String[] chatId = new String[]{"5625154206","5706533722"};


    public void sendMessage(String message) {

    for(String id:chatId){
        String url =
                "https://api.telegram.org/bot"
                        + BOT_TOKEN
                        + "/sendMessage";

        MultiValueMap<String,String> body =
                new LinkedMultiValueMap<>();

        body.add("chat_id", id);
        body.add("text", message);

        restTemplate.postForObject(
                url,
                body,
                String.class);
        }


    }
}
