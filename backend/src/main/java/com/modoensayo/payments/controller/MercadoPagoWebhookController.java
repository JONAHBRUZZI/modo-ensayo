package com.modoensayo.payments.controller;

import com.modoensayo.payments.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final MercadoPagoService mercadopagoService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        log.info("MP webhook received: {}", payload);
        try {
            String paymentId = mercadopagoService.extractPaymentIdFromWebhook(payload);
            if (paymentId != null) {
                mercadopagoService.processWebhookPayment(paymentId);
            }
        } catch (Exception e) {
            log.error("Error processing MP webhook", e);
        }
        return ResponseEntity.ok().build();
    }
}
