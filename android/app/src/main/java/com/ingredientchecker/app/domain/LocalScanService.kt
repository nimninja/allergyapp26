package com.ingredientchecker.app.domain

import android.content.Context
import android.net.Uri
import com.ingredientchecker.app.data.RulesRepository
import com.ingredientchecker.app.data.ScanResponse
import com.ingredientchecker.app.data.UserRestrictions
import com.ingredientchecker.app.ocr.MlKitOcr

/** Phase 2: ML Kit OCR + local YAML rules (works offline). */
class LocalScanService(context: Context) {
    private val appContext = context.applicationContext
    private val matcher = RuleMatcher(RulesRepository(appContext))

    suspend fun scan(uri: Uri, restrictions: UserRestrictions): ScanResponse {
        val rawText = MlKitOcr.recognizeText(appContext, uri)
        val result = matcher.matchText(
            rawText = rawText,
            selectedAllergens = restrictions.selectedAllergens.toList(),
            vegan = restrictions.vegan,
            vegetarian = restrictions.vegetarian,
            extraAvoid = AppConstants.parseExtraAvoid(restrictions.extraAvoid),
        )
        return toScanResponse(result)
    }

    private fun toScanResponse(result: MatchResult): ScanResponse {
        val (summary, level) = summarize(result)
        return ScanResponse(
            rawText = result.rawText,
            normalized = result.normalized,
            violations = result.violations,
            warnings = result.warnings,
            mayContainNotices = result.mayContainNotices,
            summary = summary,
            summaryLevel = level,
            disclaimer = AppConstants.DISCLAIMER,
        )
    }

    private fun summarize(result: MatchResult): Pair<String, String> {
        if (result.rawText.isBlank()) {
            return "No text could be read. Try better lighting or a sharper photo." to "no_text"
        }
        if (result.violations.isNotEmpty()) {
            return "Potential violations: ${result.violations.size} — selected restrictions may not be met." to "violation"
        }
        if (result.warnings.isNotEmpty()) {
            return "No strong violations matched; review the items below." to "warning"
        }
        return "No keyword violations matched (still verify the label yourself)." to "ok"
    }
}
