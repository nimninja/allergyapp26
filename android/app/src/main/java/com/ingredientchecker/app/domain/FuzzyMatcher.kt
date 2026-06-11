package com.ingredientchecker.app.domain

import com.ingredientchecker.app.data.MatchingRules

enum class MatchMethod(val id: String, val label: String) {
    DIRECT("direct", "Direct"),
    OCR_FIX("ocr_fix", "OCR fix"),
    SYNONYM("synonym", "Synonym"),
    ;

    companion object {
        fun fromId(id: String): MatchMethod? = entries.find { it.id == id }
    }
}

data class KeywordMatch(
    val method: MatchMethod,
    val keyword: String,
    val matchedText: String,
    val detail: String,
)

/** OCR token fixes and synonym phrases — no fuzzy edit-distance (too many false positives). */
object FuzzyMatcher {

    fun normalizeText(text: String, rules: MatchingRules): String {
        val base = RuleMatcher.normalizeText(text)
        if (base.isBlank() || rules.ocrTokenFixes.isEmpty()) return base
        return applyOcrFixes(base, rules)
    }

    fun applyOcrFixes(normalized: String, rules: MatchingRules): String =
        normalized.split(' ')
            .joinToString(" ") { token -> rules.ocrTokenFixes[token] ?: token }
            .trim()

    fun findKeyword(normalized: String, keyword: String, rules: MatchingRules): Boolean =
        findKeywordMatch(normalized, normalized, keyword, rules) != null

    /**
     * @param rawNormalized text after basic normalize, before OCR token fixes
     * @param fixedNormalized text after OCR token fixes
     */
    fun findKeywordMatch(
        rawNormalized: String,
        fixedNormalized: String,
        keyword: String,
        rules: MatchingRules,
    ): KeywordMatch? {
        val kw = keyword.trim().lowercase()
        if (kw.isEmpty()) return null

        if (RuleMatcher.findKeywordInNormalized(rawNormalized, kw)) {
            return KeywordMatch(
                method = MatchMethod.DIRECT,
                keyword = kw,
                matchedText = kw,
                detail = "Exact match in label text",
            )
        }

        if (RuleMatcher.findKeywordInNormalized(fixedNormalized, kw) &&
            !RuleMatcher.findKeywordInNormalized(rawNormalized, kw)
        ) {
            if (!kw.contains(' ')) {
                rawNormalized.split(' ').firstOrNull { token ->
                    rules.ocrTokenFixes[token] == kw
                }?.let { rawToken ->
                    return KeywordMatch(
                        method = MatchMethod.OCR_FIX,
                        keyword = kw,
                        matchedText = rawToken,
                        detail = "OCR fix: “$rawToken” → “$kw”",
                    )
                }
            }
            return KeywordMatch(
                method = MatchMethod.OCR_FIX,
                keyword = kw,
                matchedText = kw,
                detail = "Matched after OCR token cleanup",
            )
        }

        if (RuleMatcher.findKeywordInNormalized(fixedNormalized, kw)) {
            return KeywordMatch(
                method = MatchMethod.DIRECT,
                keyword = kw,
                matchedText = kw,
                detail = "Exact match in label text",
            )
        }

        for (phrase in rules.synonyms[kw].orEmpty()) {
            val phraseNorm = phrase.trim().lowercase()
            if (phraseNorm.isEmpty()) continue
            if (RuleMatcher.findKeywordInNormalized(rawNormalized, phraseNorm) ||
                RuleMatcher.findKeywordInNormalized(fixedNormalized, phraseNorm)
            ) {
                return KeywordMatch(
                    method = MatchMethod.SYNONYM,
                    keyword = kw,
                    matchedText = phraseNorm,
                    detail = "Synonym phrase for “$kw”",
                )
            }
        }

        return null
    }
}
