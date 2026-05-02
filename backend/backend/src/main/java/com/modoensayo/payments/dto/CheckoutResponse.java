package com.modoensayo.payments.dto;

import java.util.List;

public record CheckoutResponse(
        String paymentId,
        Integer totalAmount,
        String status,
        List<EnrollmentItem> enrollments
) {
    public record EnrollmentItem(
            String enrollmentId,
            String classId,
            Integer amount
    ) {}
}
