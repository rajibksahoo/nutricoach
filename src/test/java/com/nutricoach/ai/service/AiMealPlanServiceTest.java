package com.nutricoach.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nutricoach.ai.entity.AiJob;
import com.nutricoach.ai.repository.AiJobRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.config.OpenAiProperties;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.entity.Meal;
import com.nutricoach.plans.entity.MealItem;
import com.nutricoach.plans.entity.MealPlanDay;
import com.nutricoach.plans.repository.FoodItemRepository;
import com.nutricoach.plans.repository.MealItemRepository;
import com.nutricoach.plans.repository.MealPlanDayRepository;
import com.nutricoach.plans.repository.MealPlanRepository;
import com.nutricoach.plans.repository.MealRepository;
import com.nutricoach.plans.service.MealPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiMealPlanServiceTest {

    @Mock
    private AiJobRepository aiJobRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MealPlanRepository mealPlanRepository;

    @Mock
    private MealPlanDayRepository dayRepository;

    @Mock
    private MealRepository mealRepository;

    @Mock
    private MealItemRepository mealItemRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private MealPlanService mealPlanService;

    @Mock
    private OpenAiProperties openAiProperties;

    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiMealPlanService aiMealPlanService;

    private void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = com.nutricoach.common.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createJob_ClientFound_CreatesPendingJob() {
        UUID coachId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        
        when(clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId))
                .thenReturn(Optional.of(new Client()));
        
        AiJob mockJob = AiJob.builder().status(AiJob.Status.PENDING).build();
        when(aiJobRepository.save(any(AiJob.class))).thenReturn(mockJob);
        
        AiJob job = aiMealPlanService.createJob(coachId, clientId);
        
        assertNotNull(job);
        verify(aiJobRepository).save(argThat(j -> j.getStatus() == AiJob.Status.PENDING));
    }

    @Test
    void createJob_ClientNotFound_ThrowsException() {
        UUID coachId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        
        when(clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId))
                .thenReturn(Optional.empty());
        
        assertThrows(NutriCoachException.class, () -> aiMealPlanService.createJob(coachId, clientId));
    }

    @Test
    void processJob_JobNotFound_ThrowsException() {
        UUID jobId = UUID.randomUUID();
        when(aiJobRepository.findById(jobId)).thenReturn(Optional.empty());
        
        assertThrows(NutriCoachException.class, () -> aiMealPlanService.processJob(jobId));
    }

    @Test
    void processJob_ClientNotFound_FailsJob() {
        UUID jobId = UUID.randomUUID();
        AiJob job = AiJob.builder()
                .coachId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .status(AiJob.Status.PENDING)
                .build();
        
        when(aiJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(job.getClientId(), job.getCoachId()))
                .thenReturn(Optional.empty());
                
        aiMealPlanService.processJob(jobId);
        
        assertEquals(AiJob.Status.FAILED, job.getStatus());
        assertTrue(job.getErrorMessage().contains("Client not found"));
        verify(aiJobRepository, atLeastOnce()).save(job);
    }
    
    @Test
    void processJob_LocalStub_Success() throws Exception {
        UUID jobId = UUID.randomUUID();
        AiJob job = AiJob.builder()
                .coachId(UUID.randomUUID())
                .clientId(UUID.randomUUID())
                .status(AiJob.Status.PENDING)
                .build();
        
        Client client = new Client();
        
        when(aiJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(job.getClientId(), job.getCoachId()))
                .thenReturn(Optional.of(client));
        
        when(openAiProperties.getApiKey()).thenReturn("local-key");
        

        
        MealPlan savedPlan = MealPlan.builder().build();
        setId(savedPlan, UUID.randomUUID());
        when(mealPlanRepository.save(any(MealPlan.class))).thenReturn(savedPlan);

        // The stub response is now persisted as real days/meals/items, so those
        // saves must return entities carrying ids.
        when(foodItemRepository.findAll()).thenReturn(java.util.List.of());
        when(dayRepository.save(any(MealPlanDay.class))).thenAnswer(inv -> {
            MealPlanDay d = inv.getArgument(0);
            setId(d, UUID.randomUUID());
            return d;
        });
        when(mealRepository.save(any(Meal.class))).thenAnswer(inv -> {
            Meal m = inv.getArgument(0);
            setId(m, UUID.randomUUID());
            return m;
        });
        when(mealItemRepository.save(any(MealItem.class))).thenAnswer(inv -> inv.getArgument(0));
        
        aiMealPlanService.processJob(jobId);
        
        assertEquals(AiJob.Status.COMPLETED, job.getStatus());
        verify(mealPlanRepository).save(argThat(p -> p.getName().equals("7-Day Weight Loss Plan") && p.isAiGenerated()));
    }
}
