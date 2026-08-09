package com.nutricoach.coach;

import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.messaging.entity.Message;
import com.nutricoach.messaging.repository.MessageRepository;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.progress.entity.CheckIn;
import com.nutricoach.progress.repository.CheckInRepository;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardOverviewIntegrationTest extends AbstractIntegrationTest {

    private static final String COACH_PHONE = "9720000001";
    private static final String OTHER_COACH_PHONE = "9720000099";
    private static final String URL = "/api/v1/coach/dashboard/overview";

    @Autowired MockMvc mockMvc;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MealPlanRepository mealPlanRepository;
    @Autowired MealPlanDayRepository mealPlanDayRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired CheckInRepository checkInRepository;
    @Autowired ProgressLogRepository progressLogRepository;
    @Autowired ProgressPhotoRepository progressPhotoRepository;
    @Autowired JwtService jwtService;

    private Coach coach;
    private String jwt;
    private LocalDate today;
    private final Map<UUID, UUID> backingPlans = new HashMap<>();

    @BeforeEach
    void setup() {
        backingPlans.clear();
        purge(COACH_PHONE);
        purge(OTHER_COACH_PHONE);

        coach = coachRepository.save(Coach.builder()
                .phone(COACH_PHONE)
                .name("Overview Coach")
                .subscriptionTier(Coach.SubscriptionTier.STARTER)
                .subscriptionStatus(Coach.SubscriptionStatus.TRIAL)
                .trialEndsAt(Instant.now().plusSeconds(10 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
        today = LocalDate.now(ZoneId.systemDefault());
    }

    /** Children before parents — FK constraints. */
    private void purge(String phone) {
        coachRepository.findByPhone(phone).ifPresent(existing -> {
            clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId())
                        .forEach(l -> progressPhotoRepository.deleteAll(
                                progressPhotoRepository.findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(
                                        existing.getId(), l.getId())));
                progressLogRepository.deleteAll(
                        progressLogRepository.findByClientIdAndCoachIdOrderByLoggedDateDesc(c.getId(), existing.getId()));
                checkInRepository.deleteAll(
                        checkInRepository.findByClientIdAndCoachIdOrderByCheckInDateDesc(c.getId(), existing.getId()));
                messageRepository.deleteAll(
                        messageRepository.findByCoachIdAndClientIdOrderByCreatedAtAsc(existing.getId(), c.getId()));
                mealPlanRepository.findByClientIdAndCoachIdAndDeletedAtIsNull(c.getId(), existing.getId())
                        .forEach(plan -> {
                            mealPlanDayRepository.deleteAll(
                                    mealPlanDayRepository.findByMealPlanIdOrderByDayNumber(plan.getId()));
                            mealPlanRepository.delete(plan);
                        });
            });
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });
    }

    // ── Happy path / shape ───────────────────────────────────────────────────

    @Test
    void overview_noClients_returnsEmptyStructuresNotNull() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.counts.totalClients").value(0))
                .andExpect(jsonPath("$.data.counts.unansweredMessages").value(0))
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(0))
                .andExpect(jsonPath("$.data.actionQueue").isArray())
                .andExpect(jsonPath("$.data.actionQueue.length()").value(0))
                .andExpect(jsonPath("$.data.today.sessions").isArray())
                .andExpect(jsonPath("$.data.today.checkIns").isArray())
                .andExpect(jsonPath("$.data.roster.recentClients").isArray())
                .andExpect(jsonPath("$.data.activity").isArray());
    }

    @Test
    void overview_countsClientsByStatus() throws Exception {
        saveClient("9720000002", "Alice", Client.Status.ACTIVE);
        saveClient("9720000003", "Bob", Client.Status.ACTIVE);
        saveClient("9720000004", "Carol", Client.Status.ONBOARDING);
        saveClient("9720000005", "Dan", Client.Status.INACTIVE);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.totalClients").value(4))
                .andExpect(jsonPath("$.data.counts.activeClients").value(2))
                .andExpect(jsonPath("$.data.counts.onboardingClients").value(1))
                .andExpect(jsonPath("$.data.counts.inactiveClients").value(1))
                // no plans anywhere yet
                .andExpect(jsonPath("$.data.counts.clientsNeedingPlan").value(4));
    }

    @Test
    void overview_subscriptionReportsTrialAndTierLimit() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscription.status").value("TRIAL"))
                .andExpect(jsonPath("$.data.subscription.tier").value("STARTER"))
                .andExpect(jsonPath("$.data.subscription.daysLeftInTrial").value(9))
                // TRIAL status caps at 5 regardless of tier — same rule SubscriptionGate enforces
                .andExpect(jsonPath("$.data.roster.clientLimit").value(5));
    }

    // ── Action queue sources ─────────────────────────────────────────────────

    @Test
    void overview_unreadClientMessage_surfacesInActionQueue() throws Exception {
        Client c = saveClient("9720000006", "Esha", Client.Status.ACTIVE);
        messageRepository.save(Message.builder()
                .coachId(coach.getId()).clientId(c.getId())
                .senderType(Message.SenderType.CLIENT)
                .content("Can we move tomorrow's session?")
                .build());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.unansweredMessages").value(1))
                // priority 1 — sorts to the top of the queue
                .andExpect(jsonPath("$.data.actionQueue[0].type").value("UNANSWERED_MESSAGE"))
                .andExpect(jsonPath("$.data.actionQueue[0].clientName").value("Esha"))
                .andExpect(jsonPath("$.data.actionQueue[0].detail").value("1 unread message"));
    }

    @Test
    void overview_readClientMessage_isNotUnanswered() throws Exception {
        Client c = saveClient("9720000007", "Farid", Client.Status.ACTIVE);
        messageRepository.save(Message.builder()
                .coachId(coach.getId()).clientId(c.getId())
                .senderType(Message.SenderType.CLIENT)
                .content("Thanks!")
                .readAt(Instant.now())
                .build());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.unansweredMessages").value(0));
    }

    @Test
    void overview_staleCheckIn_surfacesAsOverdue() throws Exception {
        Client c = saveClient("9720000008", "Gita", Client.Status.ACTIVE);
        saveCheckIn(c, today.minusDays(10), 70);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(1))
                .andExpect(jsonPath("$.data.actionQueue[0].type").value("OVERDUE_CHECKIN"))
                .andExpect(jsonPath("$.data.actionQueue[0].title").value("No check-in for 10 days"));
    }

    @Test
    void overview_recentCheckIn_isNotOverdue() throws Exception {
        Client c = saveClient("9720000009", "Hari", Client.Status.ACTIVE);
        saveCheckIn(c, today.minusDays(2), 90);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(0));
    }

    @Test
    void overview_newClientWithoutCheckIn_isNotOverdue() throws Exception {
        // Joined just now, never checked in — must not be flagged; the join date is the fallback.
        saveClient("9720000010", "Ira", Client.Status.ACTIVE);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(0));
    }

    @Test
    void overview_inactiveClient_isNotChasedForCheckIns() throws Exception {
        Client c = saveClient("9720000011", "Jai", Client.Status.INACTIVE);
        saveCheckIn(c, today.minusDays(45), 50);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(0));
    }

    @Test
    void overview_expiringMealPlan_surfacesInActionQueue() throws Exception {
        Client c = saveClient("9720000012", "Kiran", Client.Status.ACTIVE);
        mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId()).clientId(c.getId())
                .name("Cut — Block 2")
                .status(MealPlan.Status.ACTIVE)
                .startDate(today.minusDays(23))
                .endDate(today.plusDays(3))
                .build());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.plansExpiringSoon").value(1))
                .andExpect(jsonPath("$.data.counts.clientsNeedingPlan").value(0))
                .andExpect(jsonPath("$.data.actionQueue[0].type").value("PLAN_EXPIRING"))
                .andExpect(jsonPath("$.data.actionQueue[0].title").value("Meal plan ends in 3 days"))
                .andExpect(jsonPath("$.data.actionQueue[0].detail").value("Cut — Block 2"));
    }

    @Test
    void overview_planExpiringBeyondWindow_isNotFlagged() throws Exception {
        Client c = saveClient("9720000013", "Lata", Client.Status.ACTIVE);
        mealPlanRepository.save(MealPlan.builder()
                .coachId(coach.getId()).clientId(c.getId())
                .name("Maintenance")
                .status(MealPlan.Status.ACTIVE)
                .startDate(today)
                .endDate(today.plusDays(30))
                .build());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.plansExpiringSoon").value(0));
    }

    @Test
    void overview_onboardingClient_surfacesAsNewClient() throws Exception {
        saveClient("9720000014", "Manu", Client.Status.ONBOARDING);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                // NO_MEAL_PLAN (priority 4) sorts above NEW_CLIENT (priority 5)
                .andExpect(jsonPath("$.data.actionQueue[0].type").value("NO_MEAL_PLAN"))
                .andExpect(jsonPath("$.data.actionQueue[1].type").value("NEW_CLIENT"))
                .andExpect(jsonPath("$.data.actionQueue[1].clientName").value("Manu"));
    }

    // ── Today panel ──────────────────────────────────────────────────────────

    @Test
    void overview_todaysCheckIn_appearsInTodayPanel() throws Exception {
        Client c = saveClient("9720000015", "Nisha", Client.Status.ACTIVE);
        saveCheckIn(c, today, 85);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today.checkIns.length()").value(1))
                .andExpect(jsonPath("$.data.today.checkIns[0].clientName").value("Nisha"))
                .andExpect(jsonPath("$.data.today.checkIns[0].adherencePercent").value(85));
    }

    // ── Activity feed ────────────────────────────────────────────────────────

    @Test
    void overview_activityFeed_includesMessagesAndCheckIns() throws Exception {
        Client c = saveClient("9720000016", "Omar", Client.Status.ACTIVE);
        saveCheckIn(c, today, 75);
        messageRepository.save(Message.builder()
                .coachId(coach.getId()).clientId(c.getId())
                .senderType(Message.SenderType.CLIENT)
                .content("Logged everything today")
                .build());

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                // CLIENT_JOINED + CHECK_IN + MESSAGE
                .andExpect(jsonPath("$.data.activity.length()").value(3));
    }

    // ── Tenant isolation & auth ──────────────────────────────────────────────

    @Test
    void overview_doesNotLeakAnotherCoachsData() throws Exception {
        Coach other = coachRepository.save(Coach.builder()
                .phone(OTHER_COACH_PHONE).name("Other Coach")
                .trialEndsAt(Instant.now().plusSeconds(3600))
                .build());
        Client otherClient = clientRepository.save(Client.builder()
                .coachId(other.getId()).phone("9720000098").name("Not Mine")
                .status(Client.Status.ACTIVE).build());
        messageRepository.save(Message.builder()
                .coachId(other.getId()).clientId(otherClient.getId())
                .senderType(Message.SenderType.CLIENT).content("secret").build());
        // Expiring inside the window, and a long-stale check-in — both must stay invisible to us.
        MealPlan theirPlan = mealPlanRepository.save(MealPlan.builder()
                .coachId(other.getId()).clientId(otherClient.getId())
                .name("Their plan").status(MealPlan.Status.ACTIVE)
                .startDate(today).endDate(today.plusDays(1)).build());
        checkInRepository.save(CheckIn.builder()
                .coachId(other.getId()).clientId(otherClient.getId()).mealPlanId(theirPlan.getId())
                .checkInDate(today.minusDays(30)).adherencePercent(10).build());

        // Our own coach has exactly one client and nothing else.
        saveClient("9720000017", "Mine", Client.Status.ACTIVE);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.totalClients").value(1))
                .andExpect(jsonPath("$.data.counts.unansweredMessages").value(0))
                .andExpect(jsonPath("$.data.counts.overdueCheckIns").value(0))
                .andExpect(jsonPath("$.data.counts.plansExpiringSoon").value(0))
                .andExpect(jsonPath("$.data.roster.recentClients.length()").value(1))
                .andExpect(jsonPath("$.data.roster.recentClients[0].name").value("Mine"));
    }

    @Test
    void overview_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Client saveClient(String phone, String name, Client.Status status) {
        return clientRepository.save(Client.builder()
                .coachId(coach.getId()).phone(phone).name(name).status(status).build());
    }

    /**
     * check_ins.meal_plan_id is NOT NULL — a check-in always hangs off a plan.
     * This backing plan runs well past the expiry window so it never pollutes
     * the "expiring soon" assertions of a check-in test.
     */
    private CheckIn saveCheckIn(Client client, LocalDate date, int adherence) {
        UUID planId = backingPlans.computeIfAbsent(client.getId(), id -> mealPlanRepository.save(
                MealPlan.builder()
                        .coachId(coach.getId()).clientId(id)
                        .name("Backing plan").status(MealPlan.Status.ACTIVE)
                        .startDate(today.minusDays(30)).endDate(today.plusDays(60))
                        .build()).getId());

        return checkInRepository.save(CheckIn.builder()
                .coachId(coach.getId()).clientId(client.getId()).mealPlanId(planId)
                .checkInDate(date).adherencePercent(adherence).build());
    }
}
