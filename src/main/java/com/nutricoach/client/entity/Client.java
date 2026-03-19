package com.nutricoach.client.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(name = "whatsapp_number", length = 15)
    private String whatsappNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_pref", length = 20)
    private DietaryPref dietaryPref;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 20)
    private ActivityLevel activityLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "health_conditions")   // Hibernate picks jsonb on Postgres, text on H2
    private List<String> healthConditions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergies")
    private List<String> allergies;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ONBOARDING;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum Gender { MALE, FEMALE, OTHER }
    public enum Goal { WEIGHT_LOSS, WEIGHT_GAIN, MAINTENANCE, MUSCLE_GAIN }
    public enum DietaryPref { VEG, NON_VEG, VEGAN, JAIN, EGGETARIAN }
    public enum ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
    public enum Status { ONBOARDING, ACTIVE, INACTIVE }
}
