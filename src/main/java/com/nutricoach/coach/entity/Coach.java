package com.nutricoach.coach.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coaches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coach extends BaseEntity {

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(length = 15)
    private String gstin;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    private SubscriptionTier subscriptionTier = SubscriptionTier.STARTER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false, length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "razorpay_customer_id", length = 50)
    private String razorpayCustomerId;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum SubscriptionTier { STARTER, PROFESSIONAL, ENTERPRISE }
    public enum SubscriptionStatus { TRIAL, ACTIVE, PAST_DUE, CANCELLED }
}
