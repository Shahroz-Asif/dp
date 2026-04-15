package com.example.recipemaker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class PatientCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_doctor_id")
    @JsonIgnore
    private AppUser createdByDoctor;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;
}
