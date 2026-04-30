package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "workout_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutTemplate extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "cover_gradient", length = 500)
    private String coverGradient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equipment")
    private List<String> equipment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", nullable = false)
    private List<Section> sections;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Section {
        private String title;
        private String style;
        private List<Item> items;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private String name;
        private String thumb;
        private String reps;
        private String note;
    }
}
