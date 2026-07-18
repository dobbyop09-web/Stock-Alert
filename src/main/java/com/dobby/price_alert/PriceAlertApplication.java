package com.dobby.price_alert;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableScheduling
@SpringBootApplication
public class PriceAlertApplication {

	public static void main(String[] args) {


		SpringApplication.exit(SpringApplication.run(PriceAlertApplication.class, args));

	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
