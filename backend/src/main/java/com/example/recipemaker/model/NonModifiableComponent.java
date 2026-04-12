package com.example.recipemaker.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@DiscriminatorValue("NON_MODIFIABLE")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NonModifiableComponent extends RecipeComponent {
    @Override
    public boolean isModifiable() {
        return false;
    }
}
