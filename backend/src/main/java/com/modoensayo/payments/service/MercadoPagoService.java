package com.modoensayo.payments.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.associates.repository.AssociateRepository;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.PaymentSession;
import com.modoensayo.payments.dto.MercadoPagoPreferenceResponse;
import com.modoensayo.payments.enums.PaymentSessionStatus;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.payments.repository.PaymentSessionRepository;
import com.modoensayo.shared.exceptions.BusinessException;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final CartItemRepository cartItemRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final AssociateRepository associateRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Transactional
    public MercadoPagoPreferenceResponse createPreference(String ownerId) throws MPException, MPApiException {
        UUID ownerUuid = UUID.fromString(ownerId);
        List<CartItem> cartItems = cartItemRepository.findByOwnerId(ownerUuid);
        if (cartItems.isEmpty()) {
            throw new BusinessException("El carrito esta vacio");
        }

        List<CartSnapshotItem> snapshot = new ArrayList<>();
        List<PreferenceItemRequest> items = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Class classEntity = classRepository.findById(cartItem.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada: " + cartItem.getClassId()));

            snapshot.add(new CartSnapshotItem(
                    cartItem.getClassId(),
                    cartItem.getBeneficiaryType(),
                    cartItem.getBeneficiaryId(),
                    classEntity.getPrice(),
                    classEntity.getTitle()
            ));

            items.add(PreferenceItemRequest.builder()
                    .id(classEntity.getId().toString())
                    .title(classEntity.getTitle())
                    .description("Inscripcion a clase: " + classEntity.getTitle())
                    .quantity(1)
                    .unitPrice(new BigDecimal(classEntity.getPrice()))
                    .currencyId("CLP")
                    .build());
        }

        String externalReference = UUID.randomUUID().toString();
        PaymentSession session = PaymentSession.builder()
                .ownerId(ownerUuid)
                .externalReference(externalReference)
                .cartSnapshot(writeSnapshot(snapshot))
                .status(PaymentSessionStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        paymentSessionRepository.save(session);

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendUrl + "/payment/success")
                .failure(frontendUrl + "/payment/failure")
                .pending(frontendUrl + "/payment/pending")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(externalReference)
                .notificationUrl(backendUrl + "/api/payments/webhook")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        session.setPreferenceId(preference.getId());
        paymentSessionRepository.save(session);

        log.info("MP preference created. preferenceId={}, externalReference={}, ownerId={}",
                preference.getId(), externalReference, ownerId);

        return new MercadoPagoPreferenceResponse(
                preference.getId(),
                preference.getInitPoint(),
                preference.getSandboxInitPoint()
        );
    }

    @Transactional
    public void processWebhookPayment(String paymentId) {
        try {
            Payment paymentInfo = new PaymentClient().get(Long.parseLong(paymentId));
            String externalReference = paymentInfo.getExternalReference();
            if (externalReference == null || externalReference.isBlank()) {
                log.warn("MP webhook payment without externalReference. paymentId={}", paymentId);
                return;
            }

            PaymentSession session = paymentSessionRepository.findByExternalReference(externalReference)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment session not found for externalReference"));

            if (session.getStatus() == PaymentSessionStatus.APPROVED) {
                log.info("MP payment already processed. paymentId={}, externalReference={}", paymentId, externalReference);
                return;
            }

            if (!"approved".equalsIgnoreCase(paymentInfo.getStatus())) {
                session.setStatus(PaymentSessionStatus.FAILED);
                session.setMercadoPagoPaymentId(paymentId);
                session.setProcessedAt(Instant.now());
                paymentSessionRepository.save(session);
                log.info("MP payment not approved. paymentId={}, status={}", paymentId, paymentInfo.getStatus());
                return;
            }

            List<CartSnapshotItem> snapshot = readSnapshot(session.getCartSnapshot());
            List<String> failedItems = new ArrayList<>();

            // Validate all items first (R5: report which items fail)
            for (CartSnapshotItem item : snapshot) {
                Class classEntity = classRepository.findById(item.classId())
                        .orElse(null);
                if (classEntity == null) {
                    failedItems.add(item.classTitle() + ": clase no encontrada");
                    continue;
                }

                UUID beneficiaryId = item.beneficiaryId();
                UUID ownerId = session.getOwnerId();

                if (beneficiaryId != null && !beneficiaryId.equals(ownerId)) {
                    boolean isAssociate = associateRepository.findByOwnerId(ownerId).stream()
                            .anyMatch(a -> a.getId().equals(beneficiaryId));
                    if (!isAssociate) {
                        failedItems.add(item.classTitle() + ": beneficiario no vinculado a tu cuenta");
                        continue;
                    }
                }

                boolean exists = enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
                        classEntity.getId(), item.beneficiaryType(), beneficiaryId);
                if (exists) {
                    failedItems.add(item.classTitle() + ": ya inscrito en esta clase");
                    continue;
                }

                long current = enrollmentRepository.countByClassId(classEntity.getId());
                if (classEntity.getCapacity() != null && current >= classEntity.getCapacity()) {
                    failedItems.add(item.classTitle() + ": sin cupos disponibles");
                    continue;
                }
            }

            if (!failedItems.isEmpty()) {
                throw new BusinessException("No se pudo procesar el pago. Items fallidos: " + String.join(" | ", failedItems));
            }

            // Process all items atomically (R6)
            for (CartSnapshotItem item : snapshot) {
                Class classEntity = classRepository.findWithLockById(item.classId())
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

                UUID beneficiaryId = item.beneficiaryId();
                boolean exists = enrollmentRepository.existsByClassIdAndBeneficiaryTypeAndBeneficiaryId(
                        classEntity.getId(), item.beneficiaryType(), beneficiaryId);
                if (exists) continue;

                Enrollment enrollment = Enrollment.builder()
                        .classId(classEntity.getId())
                        .beneficiaryType(item.beneficiaryType())
                        .beneficiaryId(beneficiaryId)
                        .status("ACTIVE")
                        .build();
                enrollmentRepository.save(enrollment);

                com.modoensayo.payments.domain.Payment payment = com.modoensayo.payments.domain.Payment.builder()
                        .enrollment(enrollment)
                        .amount(item.price() != null ? item.price().intValue() : null)
                        .status(PaymentStatus.RETAINED)
                        .build();
                paymentRepository.save(payment);
            }

            cartItemRepository.deleteByOwnerId(session.getOwnerId());
            session.setStatus(PaymentSessionStatus.APPROVED);
            session.setMercadoPagoPaymentId(paymentId);
            session.setProcessedAt(Instant.now());
            paymentSessionRepository.save(session);
            log.info("MP payment processed. paymentId={}, externalReference={}", paymentId, externalReference);
        } catch (MPException | MPApiException e) {
            log.error("Error processing MP webhook payment {}", paymentId, e);
            throw new BusinessException("No se pudo validar el pago con Mercado Pago");
        }
    }

    private String writeSnapshot(List<CartSnapshotItem> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudo crear snapshot de pago");
        }
    }

    private List<CartSnapshotItem> readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudo leer snapshot de pago");
        }
    }

    public String extractPaymentIdFromWebhook(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object id = dataMap.get("id");
            if (id != null) {
                return String.valueOf(id);
            }
        }
        Object id = payload.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    private record CartSnapshotItem(
            UUID classId,
            String beneficiaryType,
            UUID beneficiaryId,
            Double price,
            String classTitle
    ) {}
}
