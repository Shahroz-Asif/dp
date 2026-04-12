package com.example.recipemaker.repository;

import com.example.recipemaker.model.RecipeComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeComponentRepository extends JpaRepository<RecipeComponent, Long> {
}
