package com.nutricoach.client.repository;

import com.nutricoach.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByCoachIdAndDeletedAtIsNull(UUID coachId);

    List<Client> findByCoachIdAndStatusAndDeletedAtIsNull(UUID coachId, Client.Status status);

    Optional<Client> findByIdAndCoachIdAndDeletedAtIsNull(UUID id, UUID coachId);

    boolean existsByCoachIdAndPhoneAndDeletedAtIsNull(UUID coachId, String phone);

    List<Client> findAllByCoachId(UUID coachId);
}
