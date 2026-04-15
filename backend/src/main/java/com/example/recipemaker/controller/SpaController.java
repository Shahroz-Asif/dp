package com.example.recipemaker.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards any unmatched GET request that is not under /api/ or a static asset
 * back to index.html so the React Router SPA handles client-side navigation.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/login",
            "/recipes/**",
            "/components",
            "/conditions",
            "/orders/**",
            "/kitchen",
            "/doctor",
            "/profile",
    })
    public String forwardToIndex(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
