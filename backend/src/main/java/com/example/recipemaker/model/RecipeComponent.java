package com.example.recipemaker.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "component_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ModifiableComponent.class, name = "MODIFIABLE"),
    @JsonSubTypes.Type(value = NonModifiableComponent.class, name = "NON_MODIFIABLE")
})
public abstract class RecipeComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "component_incompatibility",
        joinColumns = @JoinColumn(name = "component_id"),
        inverseJoinColumns = @JoinColumn(name = "condition_id")
    )
    private Set<PatientCondition> incompatibleConditions;

    @Column(nullable = false)
    private boolean deleted = false;

    public abstract boolean isModifiable();
}
