package com.modoensayo.payments.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("mercadopago.access-token no configurado. Agrega MERCADOPAGO_ACCESS_TOKEN a tus variables de entorno.");
        }
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}
