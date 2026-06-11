package com.ingredientchecker.app.data

data class DietProfileToggle(
    val id: String,
    val label: String,
    val description: String = "",
    val default: Boolean = false,
    val whenEnabledAllergens: Set<String> = emptySet(),
    val whenEnabledKeywords: List<String> = emptyList(),
    val whenEnabledRemoveAllergens: Set<String> = emptySet(),
)

data class DietProfile(
    val id: String,
    val label: String,
    val description: String = "",
    val setsVegan: Boolean = false,
    val setsVegetarian: Boolean = false,
    val allergens: Set<String> = emptySet(),
    val extraAvoid: List<String> = emptyList(),
    val includes: List<String> = emptyList(),
    val toggles: List<DietProfileToggle> = emptyList(),
)

data class ProfileEffect(
    val allergens: Set<String> = emptySet(),
    val extraAvoid: List<String> = emptyList(),
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
)

data class ResolvedProfileDetails(
    val profile: DietProfile,
    val toggleStates: Map<String, Boolean>,
    val effect: ProfileEffect,
    val activeKeywords: List<String>,
    val activeAllergenLabels: List<String>,
)
