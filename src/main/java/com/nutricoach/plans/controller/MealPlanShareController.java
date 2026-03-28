package com.nutricoach.plans.controller;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.notifications.entity.NotificationLog;
import com.nutricoach.notifications.service.NotificationService;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meal-plans/{planId}/share")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meal Plan Sharing", description = "Share meal plans with clients via WhatsApp")
public class MealPlanShareController {

    private final MealPlanRepository mealPlanRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @PostMapping("/whatsapp")
    @Operation(summary = "Share meal plan via WhatsApp", description = "Sends the meal plan to the client's WhatsApp number")
    public ResponseEntity<ApiResponse<Void>> shareViaWhatsApp(@PathVariable UUID planId) {
        UUID coachId = securityUtils.getCurrentCoachId();

        MealPlan plan = mealPlanRepository.findByIdAndCoachIdAndDeletedAtIsNull(planId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Meal plan not found"));

        Client client = clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(plan.getClientId(), coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));

        String phone = StringUtils.hasText(client.getWhatsappNumber())
                ? client.getWhatsappNumber()
                : client.getPhone();

        String messageText = String.format(
                "Hi %s! Your coach has shared a new meal plan '%s'. Please follow it as guided. - NutriCoach",
                client.getName(), plan.getName());

        notificationService.sendWhatsApp(
                coachId,
                client.getId(),
                phone,
                NotificationLog.Type.MEAL_PLAN_SHARE,
                messageText);

        return ResponseEntity.ok(ApiResponse.ok("Meal plan shared via WhatsApp", null));
    }
}
