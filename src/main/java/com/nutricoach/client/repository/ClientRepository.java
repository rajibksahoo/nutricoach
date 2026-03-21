package com.nutricoach.client.repository;

import com.nutricoach.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByCoachIdAndDeletedAtIsNull(UUID coachId);

    List<Client> findByCoachIdAndStatusAndDeletedAtIsNull(UUID coachId, Client.Status status);

    Optional<Client> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    boolean existsByCoachIdAndPhoneAndDeletedAtIsNull(UUID coachId, String phone);

    List<Client> findAllByCoachId(UUID coachId);

    long countByCoachIdAndDeletedAtIsNull(UUID coachId);

    long countByCoachIdAndStatusAndDeletedAtIsNull(UUID coachId, Client.Status status);

    List<Client> findTop5ByCoachIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID coachId);

    @Query("SELECT c FROM Client c WHERE c.coachId = :coachId AND c.deletedAt IS NULL " +
           "AND NOT EXISTS (SELECT mp FROM MealPlan mp WHERE mp.clientId = c.id AND mp.deletedAt IS NULL)")
    List<Client> findClientsWithoutMealPlan(@Param("coachId") UUID coachId);
}
