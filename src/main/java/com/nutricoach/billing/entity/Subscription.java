package com.nutricoach.billing.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 20)
    private PlanTier planTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "razorpay_subscription_id", length = 50)
    private String razorpaySubscriptionId;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public enum PlanTier { STARTER, PROFESSIONAL, ENTERPRISE }
    public enum Status { TRIAL, ACTIVE, PAST_DUE, CANCELLED }
}
