package com.nutricoach.coach.dto;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        long totalClients,
        long activeClients,
        long onboardingClients,
        long inactiveClients,
        long totalMealPlans,
        long clientsNeedingPlan,
        List<RecentClient> recentClients
) {
    public record RecentClient(
            UUID id,
            String name,
            String phone,
            String status
    ) {}
}
