package com.modoensayo.reschedules.service;

import com.modoensayo.reschedules.domain.Notification;
import com.modoensayo.reschedules.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Async
    public void enviar(UUID userId, String type, String title, String message) {
        String resolvedType  = (type  != null && !type.isBlank())  ? type  : "sistema";
        String resolvedTitle = (title != null && !title.isBlank()) ? title : inferTitle(resolvedType);
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(resolvedType)
                .title(resolvedTitle)
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build());
    }

    private String inferTitle(String type) {
        return switch (type) {
            case "pago", "PAYMENT_COMPLETED"         -> "Pago procesado";
            case "STUDENT_ENROLLED"                  -> "Nuevo alumno inscrito";
            case "VENUE_REGISTERED"                  -> "Sede registrada";
            case "CONTEXTO_SEDE_ACTIVADO"            -> "Sede aprobada";
            case "CONTEXTO_PROFESOR_ACTIVADO"        -> "Contexto Maestro activado";
            case "reschedule"                        -> "Reagendamiento";
            default                                  -> "Notificación";
        };
    }
}
