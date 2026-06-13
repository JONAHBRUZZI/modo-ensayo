package com.modoensayo.payments.service;

import com.modoensayo.associates.repository.AssociateRepository;
import com.modoensayo.classes.domain.Class;
import com.modoensayo.classes.repository.ClassRepository;
import com.modoensayo.payments.domain.CartItem;
import com.modoensayo.payments.domain.Enrollment;
import com.modoensayo.payments.domain.Payment;
import com.modoensayo.payments.enums.PaymentStatus;
import com.modoensayo.payments.repository.CartItemRepository;
import com.modoensayo.payments.repository.EnrollmentRepository;
import com.modoensayo.payments.repository.PaymentRepository;
import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import com.modoensayo.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CartItemRepository cartItemRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final AssociateRepository associateRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void addToCart(UUID ownerId, UUID classId, String beneficiaryType, UUID beneficiaryId) {
        Class c = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        CartItem item = CartItem.builder()
                .ownerId(ownerId).classId(classId)
                .classTitle(c.getTitle()).discipline(c.getDiscipline() != null ? c.getDiscipline().name() : null)
                .level(c.getLevel() != null ? c.getLevel().name() : null).price(c.getPrice())
                .beneficiaryType(beneficiaryType != null ? beneficiaryType : "USER")
                .beneficiaryId(beneficiaryId)
                .build();
        cartItemRepository.save(item);
    }

    public List<CartItem> getCart(UUID ownerId) {
        return cartItemRepository.findByOwnerId(ownerId);
    }

    @Transactional
    public void removeFromCart(UUID ownerId, UUID itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Cart item not found");
        }
        cartItemRepository.deleteById(itemId);
    }

    @Transactional
    public void clearCart(UUID ownerId) {
        cartItemRepository.deleteByOwnerId(ownerId);
    }

    @Transactional
    public Map<String, Object> checkout(UUID ownerId) {
        List<CartItem> items = cartItemRepository.findByOwnerId(ownerId);
        List<Map<String, Object>> processed = new ArrayList<>();
        double total = 0;

        for (CartItem item : items) {
            UUID beneficiaryId = item.getBeneficiaryId() != null ? item.getBeneficiaryId() : ownerId;
            if (enrollmentRepository.existsByClassIdAndBeneficiaryId(item.getClassId(), beneficiaryId)) {
                continue;
            }
            Enrollment enrollment = Enrollment.builder()
                    .classId(item.getClassId())
                    .beneficiaryType(item.getBeneficiaryType() != null ? item.getBeneficiaryType() : "USER")
                    .beneficiaryId(beneficiaryId)
                    .status("ACTIVE")
                    .build();
            enrollmentRepository.save(enrollment);

            Payment payment = Payment.builder()
                    .enrollment(enrollment)
                    .amount(item.getPrice() != null ? item.getPrice().intValue() : 0)
                    .status(PaymentStatus.RETAINED)
                    .build();
            paymentRepository.save(payment);
            total += item.getPrice() != null ? item.getPrice() : 0;

            Map<String, Object> detail = new HashMap<>();
            detail.put("classTitle", item.getClassTitle());
            detail.put("amount", item.getPrice());
            detail.put("status", "RETAINED");
            processed.add(detail);
        }

        cartItemRepository.deleteByOwnerId(ownerId);

        // Notificar al alumno
        notificationRepository.save(Notification.builder()
                .userId(ownerId)
                .title("Pago completado")
                .message("Se ha completado el pago de " + processed.size() + " clase(s) por $" + (int) total)
                .type("PAYMENT_COMPLETED").build());

        // Notificar al profesor de cada clase
        for (CartItem item : items) {
            classRepository.findById(item.getClassId()).ifPresent(c -> {
                if (c.getTeacherId() != null) {
                    notificationRepository.save(Notification.builder()
                            .userId(c.getTeacherId())
                            .title("Nuevo alumno inscrito")
                            .message("Un alumno se ha inscrito a tu clase \"" + c.getTitle() + "\"")
                            .type("STUDENT_ENROLLED").build());
                }
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", processed);
        result.put("total", total);
        result.put("count", processed.size());
        result.put("status", "COMPLETED");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyEnrollments(UUID userId) {
        List<Enrollment> all = new ArrayList<>(enrollmentRepository.findByBeneficiaryId(userId));

        // Incluir clases de asociados del usuario
        var associates = associateRepository.findByOwnerId(userId);
        for (var assoc : associates) {
            all.addAll(enrollmentRepository.findByBeneficiaryId(assoc.getId()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : all) {
            classRepository.findById(e.getClassId()).ifPresent(c -> {
                Map<String, Object> item = new HashMap<>();
                item.put("enrollmentId", e.getId().toString());
                item.put("classId", c.getId().toString());
                item.put("title", c.getTitle());
                item.put("discipline", c.getDiscipline() != null ? c.getDiscipline().name() : null);
                item.put("level", c.getLevel() != null ? c.getLevel().name() : null);
                item.put("startTime", c.getStartTime());
                item.put("endTime", c.getEndTime());
                item.put("status", c.getStatus().name());
                item.put("price", c.getPrice());
                item.put("enrollmentStatus", e.getStatus());

                boolean isOwner = userId.equals(e.getBeneficiaryId());
                item.put("isOwner", isOwner);
                String beneName = "Yo";
                if (!isOwner) {
                    var assoc = associates.stream().filter(a -> a.getId().equals(e.getBeneficiaryId())).findFirst().orElse(null);
                    beneName = assoc != null && assoc.getName() != null ? assoc.getName() : "Asociado";
                }
                item.put("beneficiaryName", beneName);
                item.put("beneficiaryId", e.getBeneficiaryId() != null ? e.getBeneficiaryId().toString() : null);

                result.add(item);
            });
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyPaymentHistory(UUID userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByBeneficiaryId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Optional<Class> classOpt = classRepository.findById(e.getClassId());
            String classTitle = classOpt.map(Class::getTitle).orElse("Clase eliminada");
            List<Payment> payments = paymentRepository.findByEnrollmentId(e.getId());
            for (Payment p : payments) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId().toString());
                item.put("classTitle", classTitle);
                item.put("amount", p.getAmount());
                item.put("status", p.getStatus().name());
                item.put("createdAt", p.getCreatedAt());
                result.add(item);
            }
        }
        result.sort((a, b) -> {
            var da = (java.time.Instant) a.get("createdAt");
            var db = (java.time.Instant) b.get("createdAt");
            return db.compareTo(da);
        });
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherEarnings(UUID teacherId) {
        List<Class> clases = classRepository.findByTeacherId(teacherId);
        List<Map<String, Object>> pagos = new ArrayList<>();
        long totalRetenido = 0;
        long totalLiberado = 0;

        for (Class c : clases) {
            for (Enrollment e : enrollmentRepository.findByClassId(c.getId())) {
                for (Payment p : paymentRepository.findByEnrollmentId(e.getId())) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId().toString());
                    item.put("classId", c.getId().toString());
                    item.put("classTitle", c.getTitle());
                    item.put("amount", p.getAmount());
                    item.put("status", p.getStatus().name());
                    item.put("date", p.getCreatedAt());
                    pagos.add(item);
                    if (p.getStatus() == PaymentStatus.RETAINED) totalRetenido += p.getAmount();
                    else if (p.getStatus() == PaymentStatus.RELEASED) totalLiberado += p.getAmount();
                }
            }
        }

        pagos.sort((a, b) -> {
            Instant da = (Instant) a.get("date");
            Instant db = (Instant) b.get("date");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        Instant primerDiaMes = YearMonth.now().atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long liberadoMes = pagos.stream()
                .filter(p -> PaymentStatus.RELEASED.name().equals(p.get("status")))
                .filter(p -> p.get("date") != null && ((Instant) p.get("date")).isAfter(primerDiaMes))
                .mapToLong(p -> ((Number) p.get("amount")).longValue())
                .sum();

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalRetenido", totalRetenido);
        resumen.put("totalLiberadoMes", liberadoMes);
        resumen.put("totalLiberadoAcumulado", totalLiberado);

        Map<String, Object> result = new HashMap<>();
        result.put("resumen", resumen);
        result.put("pagos", pagos);
        return result;
    }

    public Map<String, Object> createMercadoPagoPreference(UUID ownerId) {
        List<CartItem> items = cartItemRepository.findByOwnerId(ownerId);
        double total = items.stream().mapToDouble(CartItem::getPrice).sum();
        String prefId = UUID.randomUUID().toString().substring(0, 16);

        Map<String, Object> resp = new HashMap<>();
        resp.put("preferenceId", prefId);
        resp.put("initPoint", "/payment/pending");
        resp.put("total", total);
        resp.put("items", items);
        return resp;
    }
}
