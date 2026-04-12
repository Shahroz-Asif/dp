package com.example.recipemaker.model;

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

    @ManyToOne
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

    /**
     * Returns all components (main + modifiable) in this recipe.
     */
    @Transient
    public Set<RecipeComponent> getAllComponents() {
        Set<RecipeComponent> all = new HashSet<>(modifiableComponents);
        if (mainComponent != null) {
            all.add(mainComponent);
        }
        return all;
    }
}
