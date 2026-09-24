package com.dobby.price_alert.service;

import com.dobby.price_alert.config.KotakNeoConfig;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import org.springframework.stereotype.Service;

@Service
public class KotakTotpService {

    private final KotakNeoConfig config;

    public KotakTotpService(KotakNeoConfig config) {
        this.config = config;
    }

    public String generateTotp() {

        String secret = config.getTotpSecret();

        CodeGenerator codeGenerator = new DefaultCodeGenerator();

        try {
            long currentTime = System.currentTimeMillis() / 1000;
            long counter = currentTime / 30;

            return codeGenerator.generate(secret, counter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Kotak TOTP", e);
        }
    }
}