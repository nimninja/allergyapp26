package com.ingredientchecker.app.domain

import com.ingredientchecker.app.data.RulesRepository
import com.ingredientchecker.app.data.Violation
import com.ingredientchecker.app.data.Warning

data class MatchResult(
    val rawText: String,
    val normalized: String,
    val violations: List<Violation>,
    val warnings: List<Warning>,
    val mayContainNotices: List<String>,
)

/** Port of src/match.py — same keyword logic as desktop/API. */
class RuleMatcher(private val rules: RulesRepository) {

    fun matchText(
        rawText: String,
        selectedAllergens: List<String>,
        vegan: Boolean,
        vegetarian: Boolean,
        extraAvoid: List<String>,
    ): MatchResult {
        val normalized = normalizeText(rawText)
        val violations = mutableListOf<Violation>()
        val warnings = mutableListOf<Warning>()
        val mayContain = collectMayContain(rawText)
        val seenPairs = mutableSetOf<Pair<String, String>>()
        val seenWarningTerms = mutableSetOf<String>()

        fun addViolation(category: String, keyword: String) {
            val key = category to keyword
            if (key in seenPairs) return
            seenPairs.add(key)
            violations.add(Violation(category, keyword))
        }

        val allergensData = rules.allergensData()
        for (allergenId in selectedAllergens) {
            val entry = allergensData[allergenId] ?: continue
            val label = entry["label"]?.toString() ?: allergenId
            @Suppress("UNCHECKED_CAST")
            val keywords = entry["keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String && findKeywordInNormalized(normalized, kw)) {
                    addViolation("Allergen: $label", kw)
                }
            }
        }

        if (vegan) {
            @Suppress("UNCHECKED_CAST")
            val keywords = rules.veganData()["violation_keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String && findKeywordInNormalized(normalized, kw)) {
                    addViolation("Vegan", kw)
                }
            }
            addAmbiguousWarnings(
                rules.ambiguousData()["vegan_ambiguous"],
                normalized,
                "Ambiguous on labels; verify source.",
                warnings,
                seenWarningTerms,
            )
        }

        if (vegetarian) {
            @Suppress("UNCHECKED_CAST")
            val keywords = rules.vegetarianData()["violation_keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String && findKeywordInNormalized(normalized, kw)) {
                    addViolation("Vegetarian", kw)
                }
            }
            addAmbiguousWarnings(
                rules.ambiguousData()["vegetarian_ambiguous"],
                normalized,
                "May or may not be animal-derived.",
                warnings,
                seenWarningTerms,
            )
        }

        for (term in extraAvoid) {
            if (findKeywordInNormalized(normalized, term)) {
                addViolation("Custom avoid: $term", term)
            }
        }

        return MatchResult(rawText, normalized, violations, warnings, mayContain)
    }

    private fun addAmbiguousWarnings(
        items: Any?,
        normalized: String,
        defaultReason: String,
        warnings: MutableList<Warning>,
        seen: MutableSet<String>,
    ) {
        @Suppress("UNCHECKED_CAST")
        val list = items as? List<Any> ?: return
        for (item in list) {
            val (term, reason) = when (item) {
                is Map<*, *> -> {
                    item["term"]?.toString().orEmpty() to
                        (item["reason"]?.toString() ?: defaultReason)
                }
                else -> item.toString() to defaultReason
            }
            if (term.isNotEmpty() && findKeywordInNormalized(normalized, term) && term !in seen) {
                seen.add(term)
                warnings.add(Warning(term, reason))
            }
        }
    }

    companion object {
        fun normalizeText(text: String): String {
            if (text.isBlank()) return ""
            return text.lowercase()
                .replace(Regex("[^a-z0-9\\s-/]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun findKeywordInNormalized(normalized: String, keyword: String): Boolean {
            val kw = keyword.trim().lowercase()
            if (kw.isEmpty()) return false
            if (kw.contains(' ')) {
                val normKw = normalizeText(kw)
                return normKw.isNotEmpty() && normalized.contains(normKw)
            }
            return Regex("\\b${Regex.escape(kw)}\\b").containsMatchIn(normalized)
        }

        fun collectMayContain(originalText: String): List<String> {
            val phrases = listOf(
                "may contain",
                "may also contain",
                "processed in a facility",
                "processed on equipment",
                "manufactured in a facility",
                "made in a facility",
                "shared equipment",
            )
            return originalText.lineSequence()
                .map { it.trim() }
                .filter { line ->
                    line.isNotEmpty() && phrases.any { p -> p in line.lowercase() }
                }
                .toList()
        }
    }
}
