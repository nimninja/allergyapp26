package com.ingredientchecker.app.domain

object AppConstants {
    val ALLERGEN_OPTIONS = listOf(
        "milk" to "Milk / dairy",
        "eggs" to "Eggs",
        "fish" to "Fish",
        "shellfish" to "Shellfish (crustacean)",
        "tree_nuts" to "Tree nuts",
        "peanuts" to "Peanuts",
        "wheat" to "Wheat / gluten (wheat)",
        "soy" to "Soy",
        "sesame" to "Sesame",
    )

    const val DISCLAIMER =
        "This tool is not medical advice. OCR can misread labels; ingredients vary by region. " +
            "Always read the actual packaging. Cross-contact (“may contain”) is not fully detectable " +
            "from ingredients alone."

    fun parseExtraAvoid(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text.replace(",", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
