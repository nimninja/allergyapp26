package com.ingredientchecker.app.data

import com.google.gson.annotations.SerializedName

data class AllergenOption(
    val id: String,
    val label: String,
)

data class AllergensResponse(
    val allergens: List<AllergenOption>,
    val disclaimer: String,
)

data class Violation(
    val category: String,
    val keyword: String,
)

data class Warning(
    val term: String,
    val reason: String,
)

data class ScanResponse(
    @SerializedName("raw_text") val rawText: String,
    val normalized: String,
    val violations: List<Violation>,
    val warnings: List<Warning>,
    @SerializedName("may_contain_notices") val mayContainNotices: List<String>,
    val summary: String,
    @SerializedName("summary_level") val summaryLevel: String,
    val disclaimer: String,
)
