package com.ingredientchecker.app.data

data class MatchingRules(
    val ocrTokenFixes: Map<String, String> = emptyMap(),
    val synonyms: Map<String, List<String>> = emptyMap(),
)
