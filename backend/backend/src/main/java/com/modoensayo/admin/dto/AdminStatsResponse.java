package com.modoensayo.admin.dto;

import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        long totalClasses,
        long totalVenues,
        long totalRooms,
        long pendingIdentityVerifications,
        long pendingVenues,
        Map<String, Long> payments
) {
}
