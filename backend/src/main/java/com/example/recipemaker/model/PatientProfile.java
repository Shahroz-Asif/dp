package com.example.recipemaker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class PatientProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int age;
    private String notes;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private AppUser user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_doctor_id")
    @JsonIgnore
    private AppUser assignedDoctor;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "patient_profile_conditions",
        joinColumns = @JoinColumn(name = "patient_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "condition_id")
    )
    @Builder.Default
    private Set<PatientCondition> conditions = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;
}
