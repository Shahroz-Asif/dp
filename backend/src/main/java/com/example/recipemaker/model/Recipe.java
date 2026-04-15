package com.example.recipemaker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private MealCourse mealCourse;

    @Enumerated(EnumType.STRING)
    private MealType mealType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "main_component_id", nullable = false)
    private RecipeComponent mainComponent;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "recipe_modifiable_components",
        joinColumns = @JoinColumn(name = "recipe_id"),
        inverseJoinColumns = @JoinColumn(name = "component_id")
    )
    @Builder.Default
    private Set<RecipeComponent> modifiableComponents = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnore
    private AppUser createdBy;

    @Transient
    public Set<RecipeComponent> getAllComponents() {
        Set<RecipeComponent> all = new HashSet<>(modifiableComponents);
        if (mainComponent != null) {
            all.add(mainComponent);
        }
        return all;
    }
}
