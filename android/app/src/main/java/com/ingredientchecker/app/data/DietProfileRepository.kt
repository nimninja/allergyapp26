package com.ingredientchecker.app.data

import android.content.Context
import com.ingredientchecker.app.domain.AppConstants
import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader

class DietProfileRepository(context: Context) {
    private val appContext = context.applicationContext
    private val yaml = Yaml()
    private var cached: List<DietProfile>? = null

    @Synchronized
    fun allProfiles(): List<DietProfile> {
        cached?.let { return it }
        val loaded = loadProfiles()
        cached = loaded
        return loaded
    }

    fun profileById(id: String): DietProfile? = allProfiles().find { it.id == id }

    private fun loadProfiles(): List<DietProfile> {
        appContext.assets.open("rules/profiles.yaml").use { stream ->
            @Suppress("UNCHECKED_CAST")
            val root = yaml.load(InputStreamReader(stream)) as? Map<String, Any> ?: return emptyList()
            @Suppress("UNCHECKED_CAST")
            val profilesMap = root["profiles"] as? Map<String, Map<String, Any>> ?: return emptyList()
            return profilesMap.map { (id, data) -> parseProfile(id, data) }
        }
    }

    private fun parseProfile(id: String, data: Map<String, Any>): DietProfile {
        @Suppress("UNCHECKED_CAST")
        val togglesRaw = data["toggles"] as? List<Map<String, Any>> ?: emptyList()
        return DietProfile(
            id = id,
            label = data["label"]?.toString() ?: id,
            description = data["description"]?.toString().orEmpty(),
            setsVegan = data["sets_vegan"] as? Boolean ?: false,
            setsVegetarian = data["sets_vegetarian"] as? Boolean ?: false,
            allergens = stringList(data["allergens"]).toSet(),
            extraAvoid = stringList(data["extra_avoid"]),
            includes = stringList(data["includes"]),
            toggles = togglesRaw.map { parseToggle(it) },
        )
    }

    private fun parseToggle(data: Map<String, Any>): DietProfileToggle {
        @Suppress("UNCHECKED_CAST")
        val whenEnabled = data["when_enabled"] as? Map<String, Any> ?: emptyMap()
        return DietProfileToggle(
            id = data["id"]?.toString().orEmpty(),
            label = data["label"]?.toString().orEmpty(),
            description = data["description"]?.toString().orEmpty(),
            default = data["default"] as? Boolean ?: false,
            whenEnabledAllergens = stringList(whenEnabled["allergens"]).toSet(),
            whenEnabledKeywords = stringList(whenEnabled["keywords"]),
            whenEnabledRemoveAllergens = stringList(whenEnabled["remove_allergens"]).toSet(),
        )
    }

    private fun stringList(value: Any?): List<String> {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> if (value.isBlank()) emptyList() else listOf(value)
            else -> emptyList()
        }
    }

    fun allergenLabel(allergenId: String): String =
        AppConstants.ALLERGEN_OPTIONS.find { it.first == allergenId }?.second ?: allergenId
}
