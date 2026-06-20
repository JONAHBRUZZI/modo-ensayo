package com.modoensayo.payments.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoConfiguration.class);

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("MERCADOPAGO_ACCESS_TOKEN no configurado — pagos deshabilitados en este entorno.");
            return;
        }
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}
