package com.example.recipemaker.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@DiscriminatorValue("MODIFIABLE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModifiableComponent extends RecipeComponent {
    @Override
    public boolean isModifiable() {
        return true;
    }
}
