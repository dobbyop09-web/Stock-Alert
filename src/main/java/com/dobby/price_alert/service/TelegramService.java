package com.dobby.price_alert.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


@Component
public class TelegramService {

    @Value("${telegram.bot.token}")
    private  String BOT_TOKEN;
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
