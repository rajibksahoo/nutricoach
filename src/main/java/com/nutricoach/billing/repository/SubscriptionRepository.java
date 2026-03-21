package com.nutricoach.billing.repository;

import com.nutricoach.billing.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByCoachIdOrderByCreatedAtDesc(UUID coachId);

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    List<Subscription> findByCoachIdOrderByCreatedAtDesc(UUID coachId);
}
