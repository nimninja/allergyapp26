package com.ingredientchecker.app.domain

import com.ingredientchecker.app.data.MatchingRules
import com.ingredientchecker.app.data.RulesRepository
import com.ingredientchecker.app.data.Violation
import com.ingredientchecker.app.data.Warning

data class MatchResult(
    val rawText: String,
    val normalizedRaw: String,
    val normalized: String,
    val violations: List<Violation>,
    val warnings: List<Warning>,
    val mayContainNotices: List<String>,
)

/** Port of src/match.py — keyword logic with OCR fixes and synonyms. */
class RuleMatcher(private val rules: RulesRepository) {

    fun matchText(
        rawText: String,
        selectedAllergens: List<String>,
        vegan: Boolean,
        vegetarian: Boolean,
        extraAvoid: List<String>,
    ): MatchResult {
        val matching = rules.matchingRules()
        val rawNormalized = RuleMatcher.normalizeText(rawText)
        val normalized = FuzzyMatcher.applyOcrFixes(rawNormalized, matching)
        val violations = mutableListOf<Violation>()
        val warnings = mutableListOf<Warning>()
        val mayContain = collectMayContain(rawText)
        val seenPairs = mutableSetOf<Pair<String, String>>()
        val seenWarningTerms = mutableSetOf<String>()

        fun addViolation(category: String, keyword: String, match: KeywordMatch) {
            val key = category to keyword
            if (key in seenPairs) return
            seenPairs.add(key)
            violations.add(
                Violation(
                    category = category,
                    keyword = keyword,
                    matchMethod = match.method.id,
                    matchedText = match.matchedText,
                    matchDetail = match.detail,
                ),
            )
        }

        fun matches(keyword: String): KeywordMatch? =
            FuzzyMatcher.findKeywordMatch(rawNormalized, normalized, keyword, matching)

        val allergensData = rules.allergensData()
        for (allergenId in selectedAllergens) {
            val entry = allergensData[allergenId] ?: continue
            val label = entry["label"]?.toString() ?: allergenId
            @Suppress("UNCHECKED_CAST")
            val keywords = entry["keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String) {
                    matches(kw)?.let { addViolation("Allergen: $label", kw, it) }
                }
            }
        }

        if (vegan) {
            @Suppress("UNCHECKED_CAST")
            val keywords = rules.veganData()["violation_keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String) {
                    matches(kw)?.let { addViolation("Vegan", kw, it) }
                }
            }
            addAmbiguousWarnings(
                rules.ambiguousData()["vegan_ambiguous"],
                rawNormalized,
                normalized,
                matching,
                "Ambiguous on labels; verify source.",
                warnings,
                seenWarningTerms,
            )
        }

        if (vegetarian) {
            @Suppress("UNCHECKED_CAST")
            val keywords = rules.vegetarianData()["violation_keywords"] as? List<Any> ?: emptyList()
            for (kw in keywords) {
                if (kw is String) {
                    matches(kw)?.let { addViolation("Vegetarian", kw, it) }
                }
            }
            addAmbiguousWarnings(
                rules.ambiguousData()["vegetarian_ambiguous"],
                rawNormalized,
                normalized,
                matching,
                "May or may not be animal-derived.",
                warnings,
                seenWarningTerms,
            )
        }

        for (term in extraAvoid) {
            matches(term)?.let { addViolation("Custom avoid: $term", term, it) }
        }

        return MatchResult(rawText, rawNormalized, normalized, violations, warnings, mayContain)
    }

    private fun addAmbiguousWarnings(
        items: Any?,
        rawNormalized: String,
        normalized: String,
        matching: MatchingRules,
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
            if (term.isNotEmpty() &&
                FuzzyMatcher.findKeywordMatch(rawNormalized, normalized, term, matching) != null &&
                term !in seen
            ) {
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
