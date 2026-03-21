package com.nutricoach.coach.service;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.dto.DashboardResponse;
import com.nutricoach.plans.repository.MealPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository clientRepository;
    private final MealPlanRepository mealPlanRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID coachId) {
        long totalClients    = clientRepository.countByCoachIdAndDeletedAtIsNull(coachId);
        long activeClients   = clientRepository.countByCoachIdAndStatusAndDeletedAtIsNull(coachId, Client.Status.ACTIVE);
        long onboarding      = clientRepository.countByCoachIdAndStatusAndDeletedAtIsNull(coachId, Client.Status.ONBOARDING);
        long inactive        = clientRepository.countByCoachIdAndStatusAndDeletedAtIsNull(coachId, Client.Status.INACTIVE);
        long totalMealPlans  = mealPlanRepository.countByCoachIdAndDeletedAtIsNull(coachId);
        long needingPlan     = clientRepository.findClientsWithoutMealPlan(coachId).size();

        List<DashboardResponse.RecentClient> recentClients = clientRepository
                .findTop5ByCoachIdAndDeletedAtIsNullOrderByCreatedAtDesc(coachId)
                .stream()
                .map(c -> new DashboardResponse.RecentClient(
                        c.getId(), c.getName(), c.getPhone(), c.getStatus().name()))
                .toList();

        return new DashboardResponse(
                totalClients, activeClients, onboarding, inactive,
                totalMealPlans, needingPlan, recentClients
        );
    }
}
